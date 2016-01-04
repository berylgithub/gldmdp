
/*-----------------------------------------------------------------------
 * Copyright (C) 2001 Green Light District Team, Utrecht University 
 * Copyright of the TC3 algorithm (C) Marco Wiering, Utrecht University
 *
 * This program (Green Light District) is free software.
 * You may redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation (version 2 or later).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * See the documentation of Green Light District for further information.
 *------------------------------------------------------------------------*/

package gld.algo.tlc;

import gld.*;
import gld.sim.*;
import gld.algo.tlc.*;
import gld.infra.*;
import gld.utils.*;
import gld.xml.*;

import java.io.IOException;
import java.util.*;
import java.awt.Point;

/* This algorithm should optimize waitingtimes considerably. Right now we are aware of some possible issues.
 * Gains do rise too high, but only under certain specific very busy situations. If you lower the spawning-rates
 * traffic will flow through righlty, and TC3 manages to get full control over the waiting times...
 * TC3 does make some rather awkward decisions now and then, when a more likeley decision could be expected.
 * Summing up, we have the feeling that there might be something wrong with this implementation
 * of this algorithm.
 * Having bugfixed this implementation for over many weeks and hours, we've come to a point where we have to say:
 * This is TC3, the GLD way, as we see fit at the 29th of June 2001.
 */
 
/**
 *
 * This algorithm works like TC1 with extra functionality. It outcome is adjusted by reinforcement learning.
 * The Q values are created overseeing the whole environment of each traffic light.
 * @see gld.algo.tlc.TC1TLC
 *
 * @author Group Algorithms
 * @version 1.0
 */
public class TC3Opt extends TLController implements Colearning,InstantiationAssistant
{	
	// TLC vars
	protected Infrastructure infrastructure;
	protected TrafficLight[][] tls;
	protected Node[] allnodes;
	protected int num_nodes;
	
	// TC3 vars	
	protected Vector[][][] count, p_table, pKtl_table;	//qa_table respresents the q'_table
	protected float [][][][] q_table, qa_table;			//sign, pos, des, color (red=0, green=1)
	protected float [][][]   v_table, va_table;
	protected float gamma=0.95f;						//Discount Factor; used to decrease the influence of previous V values, that's why: 0 < gamma < 1
	protected static final boolean red=false, green=true;
	protected static final int green_index=0, red_index=1;
	public static final String shortXMLName="tlc-tc3b1";
	protected static float random_chance=0.01f;				//A random gain setting is chosen instead of the on the TLC dictates with this chance
	private Random random_number;

	/**
	 * The constructor for TL controllers
	 * @param The model being used.
	 */
	public TC3Opt(Infrastructure infra) throws InfraException
	{	super(infra);
	}
	
	public void setInfrastructure( Infrastructure infra )
	{	super.setInfrastructure(infra);
	
		Node[] nodes = infra.getAllNodes();
		num_nodes = nodes.length;
	
		try{
			int numSigns = infra.getAllInboundLanes().size();
		
			q_table 	= new float [numSigns][][][];
			qa_table	= new float [numSigns][][][];
			v_table 	= new float [numSigns][][];
			va_table	= new float [numSigns][][];
			count		= new Vector[numSigns][][];
			p_table 	= new Vector[numSigns][][];
			pKtl_table	= new Vector[numSigns][][];
			
			int num_specialnodes = infra.getNumSpecialNodes();

			for (int i=0; i<num_nodes; i++) {
				Node n = nodes[i];
				Drivelane[] dls = dls = n.getInboundLanes();
				int num_dls = num_dls = dls.length;
				// huh?
				Drivelane [] lanes = new Drivelane[numSigns];
				infra.getAllInboundLanes().copyInto(lanes);
				
				for (int j=0; j<num_dls; j++) {
					Drivelane d = dls[j];
					Sign s = d.getSign();
					int id = d.getId();
					int num_pos_on_dl = d.getCompleteLength();

					q_table[id]		= new float [num_pos_on_dl][][];
					qa_table[id]	= new float [num_pos_on_dl][][];
					v_table[id]		= new float [num_pos_on_dl][];
					va_table[id]	= new float [num_pos_on_dl][];
					count[id]		= new Vector[num_pos_on_dl][];
					p_table[id]		= new Vector[num_pos_on_dl][];
					pKtl_table[id]	= new Vector[num_pos_on_dl][];
					
					for (int k=0; k<num_pos_on_dl; k++) {
						q_table[id][k]	= new float[num_specialnodes][2];
						qa_table[id][k]	= new float[num_specialnodes][2];
						v_table[id][k]	= new float[num_specialnodes];
						va_table[id][k]	= new float[num_specialnodes];
						count[id][k]	= new Vector[num_specialnodes];
						p_table[id][k]	= new Vector[num_specialnodes];
						pKtl_table[id][k] =  new Vector[num_specialnodes];
						
						for (int l=0; l<num_specialnodes;l++) {
							q_table[id][k][l][0]	= 0.0f;
							q_table[id][k][l][1]	= 0.0f;
							qa_table[id][k][l][0]	= 0.0f;
							qa_table[id][k][l][1]	= 0.0f;
							v_table[id][k][l]		= 0.0f;
							va_table[id][k][l]		= 0.0f;
							count[id][k][l]			= new Vector();
							p_table[id][k][l]		= new Vector();
							pKtl_table[id][k][l]	= new Vector();
						}
					}
				}
			} 
			random_number = new Random();
		}
		catch(Exception e) { System.out.println("Error."); }
	}
	
	
	/**
	* Calculates how every traffic light should be switched
	* Per node, per sign the waiting roadusers are passed and per each roaduser the gain is calculated.
	* @param The TLDecision is a tuple consisting of a traffic light and a reward (Q) value, for it to be green
	* @see gld.algo.tlc.TLDecision
	*/	
	public TLDecision[][] decideTLs()
	{
	    int num_dec, currenttlID, waitingsize, pos, destID;
	    float gain =0, passenger_factor;
	    Sign currenttl;
	    Drivelane currentlane;
	    ListIterator queue;
	    Roaduser ru;
	    
		//Determine wheter it should be random or not
		boolean randomrun = false;
		if (random_number.nextFloat() < random_chance) randomrun = true;
		
	    for (int i=0;i<num_nodes;i++) {
	    	num_dec = tld[i].length;
	    	for(int j=0;j<num_dec;j++) {
	    		currenttl = tld[i][j].getTL();
	    		currenttlID = currenttl.getId();
	    		currentlane = currenttl.getLane();
	    		
	    		waitingsize = currentlane.getNumRoadusersWaiting();
	    		queue = currentlane.getCompleteQueue().listIterator();
	    		gain = 0;
	    		
	    		for(; waitingsize>0; waitingsize--) {
	    			ru = (Roaduser) queue.next();
	    			pos = ru.getPosition();
	    			destID = ru.getDestNode().getId();
	    			passenger_factor = ru.getNumPassengers();
			    			
	    			gain += passenger_factor * (q_table[currenttlID][pos][destID][red_index] - q_table[currenttlID][pos][destID][green_index]);  //red - green
	    		}
	    		
				if(trackNode!=-1 && i==trackNode) {
					Drivelane currentlane2 = tld[i][j].getTL().getLane();
					boolean[] targets = currentlane2.getTargets();
					System.out.println("node: "+i+" light: "+j+" gain: "+gain+" "+targets[0]+" "+targets[1]+" "+targets[2]+" "+currentlane2.getNumRoadusersWaiting());
				}
				
                if(randomrun)
                	gain = random_number.nextFloat();
                	
				tld[i][j].setGain(gain);
			}
		}
		return tld;
	}

	public void updateRoaduserMove(Roaduser ru, Drivelane prevlane, Sign prevsign, int prevpos, Drivelane dlanenow, Sign signnow, int posnow, PosMov[] posMovs, Drivelane desired)
	{
		// Roaduser has just left the building
		if(dlanenow == null || signnow == null) {
			return;
		}
		
		//This ordening is important for the execution of the algorithm!
		int Ktl = dlanenow.getNumRoadusersWaiting();
		if(prevsign.getType()==Sign.TRAFFICLIGHT && (signnow.getType()==Sign.TRAFFICLIGHT || signnow.getType()==Sign.NO_SIGN)) {
			boolean light = prevsign.mayDrive();
			Node dest = ru.getDestNode();
			recalcP(prevsign, prevpos, dest, light, signnow, posnow, Ktl);			
			recalcVa(prevsign, prevpos, dest);
			recalcV(prevsign, prevpos, dest, light, Ktl);			
			recalcQa(prevsign, prevpos, dest, light, signnow, posnow, posMovs);
			recalcQ(prevsign, prevpos, dest, light, signnow, posnow, posMovs, Ktl);
		}
	}
	
	protected void recalcP(Sign tl, int pos, Node destination, boolean light, Sign tl_new, int pos_new, int Ktl)
	{
		int tlId = tl.getId();
		int desId = destination.getId();
		//Update the count table
		CountEntry currentsituation = new  CountEntry(tl, pos, destination, light, tl_new, pos_new, Ktl);
		int count_index = count[tlId][pos][desId].indexOf(currentsituation);
		if (count_index>=0) {
			currentsituation = (CountEntry) count[tlId][pos][desId].elementAt(count_index);
			currentsituation.incrementValue();
		}
		else {
			count[tlId][pos][desId].add(currentsituation);
		}
		//Update the p_table
		PEntry currentchance = new PEntry(tl, pos, destination, light, tl_new, pos_new);		
		
		int dest=0, source=0;
		
		Enumeration enum = count[tlId][pos][desId].elements();
		while(enum.hasMoreElements()) {
			CountEntry current = (CountEntry) enum.nextElement();
			dest += current.sameSourceDifferentKtl(currentsituation);
			source += current.sameSource(currentsituation);
		}
		
		if(source == 0) currentchance.setValue(0);
		else currentchance.setValue((float)dest/(float)source);
		
		int p_index = p_table[tlId][pos][desId].indexOf(currentchance);
		if(p_index>=0) p_table[tlId][pos][desId].setElementAt(currentchance, p_index);
		else { 
			p_table[tlId][pos][desId].add(currentchance);
			p_index = p_table[tlId][pos][desId].indexOf(currentchance);
		}
		
		// Change the rest of the p_table, Also check the other chances for updates
		int size = p_table[tlId][pos][desId].size()-1;
		for(; size>=0; size--) {
			PEntry P = (PEntry) p_table[tlId][pos][desId].elementAt(size);
			float pvalue = P.sameSource(currentsituation);
			if(pvalue > -1.0f) {
				if(size != p_index)
					P.setValue(pvalue * (float)(source-1) / (float)source);
			}
		}

		//update the p'_table ......		
		PKtlEntry currentchance2 = new PKtlEntry(tl, pos, destination, light, tl_new, pos_new, Ktl);
		source=0;

		enum = count[tlId][pos][desId].elements();
		while(enum.hasMoreElements()) {
			source += ((CountEntry) enum.nextElement()).sameSourceWithKtl(currentsituation);
		}
		
		dest = currentsituation.getValue();
		if(source == 0) currentchance2.setValue(0);
		else currentchance2.setValue((float)dest/(float)source);
	
		p_index = pKtl_table[tlId][pos][desId].indexOf(currentchance2);
		if(p_index>=0) pKtl_table[tlId][pos][desId].setElementAt(currentchance2, p_index);
		else {
			pKtl_table[tlId][pos][desId].add(currentchance2);
			p_index = pKtl_table[tlId][pos][desId].indexOf(currentchance2);
		}
		
		// Change the rest of the pKtl_table, Also check the other chances for updates
		size = pKtl_table[tlId][pos][desId].size()-1;
		for(; size>=0; size--) {
			PKtlEntry P = (PKtlEntry) pKtl_table[tlId][pos][desId].elementAt(size);
			float pvalue = P.sameSource(currentsituation);
			if(pvalue > -1) {
				if(size != p_index) {
					P.setValue(pvalue * (float)(source-1) / (float)source);
				}
			}
		}

		if(currentchance.getValue() >1  ||currentchance2.getValue() >1 || currentchance.getValue() <0  ||currentchance2.getValue() <0 )	System.out.println("Serious error !!!!!!!!!1");
	}

	protected void recalcQ(Sign tl, int pos, Node destination, boolean light, Sign tl_new, int pos_new, PosMov[] posMovs, int Ktl)
	{
		/* The calculation of the Q values in TC-3 */
		float newQvalue = qa_table[tl.getId()][pos][destination.getId()][light?green_index:red_index];
		float V=0;

// Waarom splitst TC2 wel op rood/groen, en TC3 niet??
		CountEntry currentsituation = new CountEntry (tl, pos, destination, light, tl_new, pos_new, Ktl);		
		Enumeration e = pKtl_table[tl.getId()][pos][destination.getId()].elements();
		
		while(e.hasMoreElements()) {
			PKtlEntry P = (PKtlEntry) e.nextElement();
			if(P.sameSourceKtl(currentsituation) != -1.0f) {
				try {
					V = v_table[P.tl_new.getId()][P.pos_new][destination.getId()];
				}
				catch (Exception excep) {
					System.out.println("ERROR in q");
				}
// Moet er hier geen reward functie??				
				newQvalue += P.getValue() *gamma * V;
			}
		}
		
		q_table[tl.getId()][pos][destination.getId()][light?green_index:red_index] = newQvalue; //sign, pos, des, color (red=0, green=1)
	}

	protected void recalcV(Sign tl, int pos, Node destination, boolean light, int Ktl)
	{
		/* The calculation of the V values in TC-3 */
		float newVvalue;
		float tempSumGreen=0, tempSumRed=0;
		float V;
		int[] amount = count(tl, pos, destination);
		int tlId = tl.getId();
		int desId = destination.getId();
		float total = (float) amount[green_index] + (float) amount[red_index];

		newVvalue = va_table[tl.getId()][pos][destination.getId()];
		
		CountEntry currentsituation_green = new CountEntry (tl, pos, destination, green, tl, pos, Ktl);
		CountEntry currentsituation_red = new CountEntry (tl, pos, destination, red, tl, pos, Ktl);
		
		Enumeration e = pKtl_table[tlId][pos][desId].elements();
		
		while(e.hasMoreElements()) {
			//Green part
			PKtlEntry P = (PKtlEntry) e.nextElement();
			
			if(P.sameSourceKtl(currentsituation_green) != -1) {
				try {				
					V = v_table[P.tl_new.getId()][P.pos_new][destination.getId()];
					tempSumGreen += P.getValue() *gamma * V;
				}
				catch (Exception excep) {
					System.out.println(excep+"");
					excep.printStackTrace();
				}
			}
			//Red Part
			if(P.sameSourceKtl(currentsituation_red) != -1) {
				try {				
					V = v_table[P.tl_new.getId()][P.pos_new][destination.getId()];
					tempSumRed += P.getValue() *gamma * V;
				}
				catch (Exception excep) {
					System.out.println("ERROR in recalc V2");
					System.out.println(excep+"");
					excep.printStackTrace();
				}
			}
		}
		
		newVvalue += ((float)amount[green_index]/ (float)total) * tempSumGreen + ((float)amount[red_index]/ (float)total) * tempSumRed;
		try {
			v_table[tl.getId()][pos][destination.getId()] = newVvalue;
		}
		catch (Exception excep) {
			System.out.println("Error in v");
		}
	}
	
	protected void recalcVa(Sign tl, int pos, Node destination)
	{
		float newWvalue;
		float qa_red = qa_table[tl.getId()][pos][destination.getId()][red_index];
		float qa_green = qa_table[tl.getId()][pos][destination.getId()][green_index];
		int[] amount = count(tl, pos, destination);
		float total = (float) amount[green_index] + (float) amount[red_index];
		
		newWvalue = ((float)amount[green_index]/(float)total)*qa_green + ((float)amount[red_index]/(float)total)*qa_red;
		
		try {
			va_table[tl.getId()][pos][destination.getId()] = newWvalue;
		}
		catch (Exception e) {
			System.out.println("Error in recalc W");
		}

	}

	protected void recalcQa(Sign tl, int pos, Node destination, boolean light, Sign tl_new, int pos_new, PosMov[] posMovs)
	{
		float newQvalue=0;
		int size = tl.getLane().getCompleteLength()-1;
		int R;
		int tlId = tl.getId();
		int desId = destination.getId();
		float Va;

		for(; size>=0; size--) {
			PEntry P = new PEntry(tl, pos, destination, light, tl, size);

			int p_index = p_table[tlId][pos][desId].indexOf(P);
			if(p_index>=0) {
				try {
					P = (PEntry) p_table[tlId][pos][desId].elementAt(p_index);
					Va = va_table[tlId][size][desId];	
					R = rewardFunction(tl_new, pos_new, posMovs);
					newQvalue += P.getValue() *(((float)R) + gamma * Va);
				}
				catch (Exception e) {
					System.out.println("Error in recalc Q'");
				}
			}
		}

		try {
			qa_table[tl.getId()][pos][destination.getId()][light?green_index:red_index] = newQvalue;
		}
		catch (Exception e) {
			System.out.println("ERROR, Zwaluw is not found");
		}
	}
	
	
	/*
				==========================================================================
							Additional methods, used by the recalc methods 
				==========================================================================
	*/

	protected int[] count(Sign tl, int pos, Node destination)
	{
		int tlId = tl.getId();
		int desId = destination.getId();
		int[] counters;
		counters = new int[2];
		
		//See the green_index definitions above !!!!
		counters[green_index] = 0;
		counters[red_index] = 0;
		
		//Calcs the number of entries in the table matching the given characteristics, and returns the count
		int psize = p_table[tlId][pos][desId].size()-1;
		for(; psize>=0; psize--)
		{
			PEntry candidate = (PEntry) p_table[tlId][pos][desId].elementAt(psize);
			if(candidate.tl.getId() == tlId && candidate.pos == pos && candidate.destination.getId() == desId) {
					if(candidate.light == green) {
						counters[green_index]++;
					}
					else {
						counters[red_index]++;
					}
			}
		}
		return counters;
	}
	
	protected int rewardFunction(Sign tl_new, int pos_new, PosMov[] posMovs)
	{
		//Ok, the reward function is actually very simple; it searches for the tuple (tl_new, pos_new) in the given set
		int size = posMovs.length;
		
		for(int i=0; i<size; i++)
			if( posMovs[i].tlId==tl_new.getId())
				if(posMovs[i].pos != pos_new)
						return 0;
		return 1;
	}
	
	public float getVValue(Sign sign, Node des, int pos)
	{
		try {
			return v_table[sign.getId()][pos][des.getId()];
		}
		catch (Exception e) {
			System.out.print("Error in v_table");
			return 0;
		}
	}

	public float getColearnValue(Sign sign_new, Sign sign, Node destination, int pos)
	{
		int Ktl = sign.getLane().getNumRoadusersWaiting();
		int tlId = sign.getId();
		int desId = destination.getId();
	
		// Calculate the colearning value
		float newCovalue=0;
		int size = sign.getLane().getCompleteLength()-1;

		for(; size>=0; size--) {
			float V;
			PKtlEntry P = new PKtlEntry(sign, 0, destination, green, sign_new, size, Ktl);
			int p_index = pKtl_table[tlId][pos][desId].indexOf(P);
			
			if(p_index>=0) {
				try {
					P = (PKtlEntry) pKtl_table[tlId][pos][desId].elementAt(p_index);
					V = v_table[tlId][size][desId];
					newCovalue += P.getValue() * V;
				}
				catch (Exception e) {
					System.out.println("Error");
				}
			}
		}
		return newCovalue;
	}

	/*
				==========================================================================
					Internal Classes to provide a way to put entries into the tables 
				==========================================================================
	*/

	public class CountEntry implements XMLSerializable, TwoStageLoader
	{
		Sign tl;
		int pos;
		Node destination;
		boolean light;
		Sign tl_new;
		int pos_new;
		int Ktl;
		int value;
		String parentName="model.tlc";
		TwoStageLoaderData loadData=new TwoStageLoaderData();
		
		CountEntry(Sign _tl, int _pos, Node _destination, boolean _light, Sign _tl_new, int _pos_new, int _Ktl) {
			tl = _tl;
			pos = _pos;
			destination = _destination;
			light = _light;
			tl_new = _tl_new;
			pos_new = _pos_new;
			Ktl = _Ktl;
			value=1;
		}
		
		public CountEntry ()
		{ // Empty constructor for loading
		}
		
		public void incrementValue() {
			value++;
		}
		
		public int getValue() {
			return value;
		}

		public boolean equals(Object other) {
			if(other != null && other instanceof CountEntry)
			{
				CountEntry countnew = (CountEntry) other;
				if(!countnew.tl.equals(tl)) return false;
				if(countnew.pos!=pos) return false;
				if(!countnew.destination.equals(destination)) return false;
				if(countnew.light!=light) return false;
				if(!countnew.tl_new.equals(tl_new)) return false;
				if(countnew.pos_new!=pos_new) return false;
				if(countnew.Ktl!=Ktl) return false;
				return true;
			}
			return false;
		}

		public int sameSource(CountEntry other) {
			if(other.tl.equals(tl) && other.pos == pos && other.light==light && other.destination.equals(destination)) {
				return value;
			}
			else {
				return 0;
			}
		}
		
		public int sameSourceDifferentKtl(CountEntry other) {
			if(other.tl.equals(tl) && other.pos == pos && other.light==light && other.destination.equals(destination) && other.tl_new.equals(tl_new) && other.pos_new == pos_new) {
				return value;
			}
			else {
				return 0;
			}
		}
		
		public int sameSourceWithKtl(CountEntry other) {
			if(other.tl.equals(tl) && other.pos == pos && other.light==light && other.destination.equals(destination) && other.Ktl == Ktl) {
				return value;
			}
			else {
				return 0;
			}
		}
		
		// XMLSerializable implementation of CountEntry
		
		public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
		{	pos=myElement.getAttribute("pos").getIntValue();
		   	loadData.oldTlId=myElement.getAttribute("tl-id").getIntValue();
		   	loadData.destNodeId=myElement.getAttribute("destination").getIntValue();
		   	light=myElement.getAttribute("light").getBoolValue();
			loadData.newTlId=myElement.getAttribute("newtl-id").getIntValue();
			pos_new=myElement.getAttribute("new-pos").getIntValue();
			Ktl=myElement.getAttribute("ktl").getIntValue();
			value=myElement.getAttribute("value").getIntValue(); 
		}

		public XMLElement saveSelf () throws XMLCannotSaveException
		{ 	XMLElement result=new XMLElement("count");
			result.addAttribute(new XMLAttribute("tl-id",tl.getId()));
			result.addAttribute(new XMLAttribute("pos",pos));
			result.addAttribute(new	XMLAttribute("destination",destination.getId()));
			result.addAttribute(new XMLAttribute("light",light));
			result.addAttribute(new XMLAttribute("newtl-id",tl_new.getId()));
			result.addAttribute(new XMLAttribute("new-pos",pos_new));
			result.addAttribute(new XMLAttribute("ktl",Ktl));
			result.addAttribute(new XMLAttribute("value",value));
			if ( ! infrastructure.laneDictionary.containsKey
			     (new Integer (tl.getId())))
			{     
			     System.out.println
			     ("WARNING : Unknown Trafficlight ID "+tl.getId()+
			      " in TC3$CountEntry. Loading will go wrong");
			}
	  		return result;
		}
  
		public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
		{ 	// A count entry has no child objects
		}

		public String getXMLName ()
		{ 	return parentName+".count";
		}
		
		public void setParentName (String parentName)
		{	this.parentName=parentName; 
		}
	
		// TwoStageLoader implementation of CountEntry

		class TwoStageLoaderData 
		{ 	int oldTlId,newTlId,destNodeId;
		}
		
		public void loadSecondStage (Dictionary dictionaries) throws XMLInvalidInputException
		{ 	Dictionary laneDictionary=(Dictionary)(dictionaries.get("lane")),
		             nodeDictionary=(Dictionary)(dictionaries.get("node"));
			Drivelane lane=(Drivelane)(laneDictionary.get(new
					Integer(loadData.oldTlId)));
			if ( ! ((Hashtable)(laneDictionary)).containsKey 
			       (new Integer(loadData.oldTlId)))
			       throw new XMLInvalidInputException
			       ("Trying to load non-existant TL with id "+
			       loadData.oldTlId);		
		  	tl=((Drivelane)(laneDictionary.get(
		      		new Integer(loadData.oldTlId)))).getSign();
		  	tl_new=((Drivelane)(laneDictionary.get(
		      		new Integer(loadData.newTlId)))).getSign();
		  	destination=(Node)(nodeDictionary.get(
		      		new Integer(loadData.destNodeId)));
		}
		
	}
	
	public class PEntry implements XMLSerializable, TwoStageLoader
	{
		Sign tl;
		int pos;
		Node destination;
		boolean light;
		Sign tl_new;
		int pos_new;
		float value;
		TwoStageLoaderData loadData=new TwoStageLoaderData();
		String parentName="model.tlc";
		
		PEntry(Sign _tl, int _pos, Node _destination, boolean _light, Sign _tl_new, int _pos_new) {
			tl = _tl;
			pos = _pos;
			destination = _destination;
			light = _light;
			tl_new = _tl_new;
			pos_new = _pos_new;
			value=0;
		}
		
		public PEntry ()
		{	// Empty constructor for loading
		}
		
		public void setValue(float v) {
			value = v;
		}
		
		public float getValue() {
			return value;
		}

		public boolean equals(Object other) {
			if(other != null && other instanceof PEntry)
			{
				PEntry pnew = (PEntry) other;
				if(!pnew.tl.equals(tl)) return false;
				if(pnew.pos!=pos) return false;
				if(!pnew.destination.equals(destination)) return false;
				if(pnew.light!=light) return false;
				if(!pnew.tl_new.equals(tl_new)) return false;
				if(pnew.pos_new!=pos_new) return false;
				return true;
			}
			return false;
		}
		
		public float sameSource(CountEntry other) {
			if(other.tl.equals(tl) && other.pos == pos && other.light==light && other.destination.equals(destination)) {
				return value;
			}
			else {
				return -1;
			}
		}
		
		// XMLSerializable implementation of PEntry
		
		public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
		{	pos=myElement.getAttribute("pos").getIntValue();
		   	loadData.oldTlId=myElement.getAttribute("tl-id").getIntValue();
			loadData.destNodeId=myElement.getAttribute("destination").getIntValue();
		   	light=myElement.getAttribute("light").getBoolValue();
			loadData.newTlId=myElement.getAttribute("newtl-id").getIntValue();
			pos_new=myElement.getAttribute("new-pos").getIntValue();
			value=myElement.getAttribute("value").getFloatValue(); 
		}

		public XMLElement saveSelf () throws XMLCannotSaveException
		{ 	XMLElement result=new XMLElement("pval");
			result.addAttribute(new XMLAttribute("tl-id",tl.getId()));
			result.addAttribute(new XMLAttribute("pos",pos));
			result.addAttribute(new	XMLAttribute("destination",destination.getId()));
			result.addAttribute(new XMLAttribute("light",light));
			result.addAttribute(new XMLAttribute("newtl-id",tl_new.getId()));
			result.addAttribute(new XMLAttribute("new-pos",pos_new));
			result.addAttribute(new XMLAttribute("value",value));
	  		return result;
		}
  
		public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
		{ 	// A P-entry has no child objects
		}

		public String getXMLName ()
		{ 	return parentName+".pval";
		}
		
		public void setParentName (String parentName)
		{	this.parentName=parentName; 
		}
				
		// TwoStageLoader implementation of PEntry

		class TwoStageLoaderData 
		{  	int oldTlId,newTlId,destNodeId;
		}
		
		public void loadSecondStage (Dictionary dictionaries)
		{ 	Dictionary laneDictionary=(Dictionary)(dictionaries.get("lane")),
           		     nodeDictionary=(Dictionary)(dictionaries.get("node"));
		  	tl=((Drivelane)(laneDictionary.get(
		       		new Integer(loadData.oldTlId)))).getSign();
		  	tl_new=((Drivelane)(laneDictionary.get(
		        	new Integer(loadData.newTlId)))).getSign();
		  	destination=(Node)(nodeDictionary.get(
		        	new Integer(loadData.destNodeId)));
		}
	}

	public class PKtlEntry implements XMLSerializable, TwoStageLoader
	{
		Sign tl;
		int pos;
		Node destination;
		boolean light;
		Sign tl_new;
		int pos_new;
		int Ktl;
		float value;
		TwoStageLoaderData loadData=new TwoStageLoaderData();
		String parentName="model.tlc";
		
		PKtlEntry(Sign _tl, int _pos, Node _destination, boolean _light, Sign _tl_new, int _pos_new, int _Ktl) {
			tl = _tl;
			pos = _pos;
			destination = _destination;
			light = _light;
			tl_new = _tl_new;
			pos_new = _pos_new;
			Ktl = _Ktl;
			value=0;
		}
		
		PKtlEntry ()
		{	// Empty constructor for loading
		}
		
		public void setValue(float v) {
			value = v;
		}
		
		public float getValue() {
			return value;
		}

		public boolean equals(Object other) {
			if(other != null && other instanceof PKtlEntry)
			{
				PKtlEntry pnew = (PKtlEntry) other;
				if(!pnew.tl.equals(tl)) return false;
				if(pnew.pos!=pos) return false;
				if(!pnew.destination.equals(destination)) return false;
				if(pnew.light!=light) return false;
				if(!pnew.tl_new.equals(tl_new)) return false;
				if(pnew.pos_new!=pos_new) return false;
				if(pnew.Ktl!=Ktl) return false;
				return true;
			}
			return false;
		}
		
		public float sameSource(CountEntry other) {
			if(other.tl.equals(tl) && other.pos == pos && other.light==light && other.destination.equals(destination)) {
				return value;
			}
			else {
				return -1;
			}
		}
		
		public float sameSourceKtl(CountEntry other) {
			if(other.tl.equals(tl) && other.pos == pos && other.light==light && other.destination.equals(destination) && other.Ktl == Ktl ) {
				return value;
			}
			else {
				return -1;
			}
		}
		
		// XMLSerializable implementation of PKtlEntry
		
		public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
		{	pos=myElement.getAttribute("pos").getIntValue();
		   	loadData.oldTlId=myElement.getAttribute("tl-id").getIntValue();
			loadData.destNodeId=myElement.getAttribute("destination").getIntValue();
		   	light=myElement.getAttribute("light").getBoolValue();
			loadData.newTlId=myElement.getAttribute("newtl-id").getIntValue();
			pos_new=myElement.getAttribute("new-pos").getIntValue();
			Ktl=myElement.getAttribute("ktl").getIntValue();
			value=myElement.getAttribute("value").getFloatValue(); 
		}

		public XMLElement saveSelf () throws XMLCannotSaveException
		{ 	XMLElement result=new XMLElement("pktlval");
			result.addAttribute(new XMLAttribute("tl-id",tl.getId()));
			result.addAttribute(new XMLAttribute("pos",pos));
			result.addAttribute(new	XMLAttribute("destination",destination.getId()));
			result.addAttribute(new XMLAttribute("light",light));
			result.addAttribute(new XMLAttribute("newtl-id",tl_new.getId()));
			result.addAttribute(new XMLAttribute("new-pos",pos_new));
			result.addAttribute(new XMLAttribute("value",value));
			result.addAttribute(new XMLAttribute("ktl",Ktl));
	  		return result;
		}
  
		public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
		{ 	// A Pktl-entry has no child objects
		}

		public String getXMLName ()
		{ 	return parentName+".pktlval";
		}
		
		public void setParentName (String parentName)
		{	this.parentName=parentName; 
		}
			
		// TwoStageLoader implementation of PktlEntry

		class TwoStageLoaderData 
		{ 	int oldTlId,newTlId,destNodeId;
		}
		
		public void loadSecondStage (Dictionary dictionaries) throws XMLInvalidInputException,XMLTreeException
		{ 	Dictionary laneDictionary=(Dictionary)(dictionaries.get("lane")),
           		     nodeDictionary=(Dictionary)(dictionaries.get("node"));
		  	tl=((Drivelane)(laneDictionary.get(
		       		new Integer(loadData.oldTlId)))).getSign();
		  	tl_new=((Drivelane)(laneDictionary.get(
		        	new Integer(loadData.newTlId)))).getSign();
		  	destination=(Node)(nodeDictionary.get(
		        	new Integer(loadData.destNodeId)));
		}
		
	}	
	
	protected class Target implements XMLSerializable , TwoStageLoader
	{
		Sign tl;
		int pos;
		TwoStageLoaderData loadData=new TwoStageLoaderData();
		String parentName="model.tlc";
		
		Target(Sign _tl, int _pos) {
			tl = _tl;
			pos = _pos;
		}
		
		public Sign getTL() {
			return tl;
		}
		
		public int getP() {
			return pos;
		}

		public boolean equals(Object other) {
			if(other != null && other instanceof Target)
			{
				Target qnew = (Target) other;
				if(!qnew.tl.equals(tl)) return false;
				if(qnew.pos!=pos) return false;
				return true;
			}
			return false;
		}
		
		// XMLSerializable implementation of Target
		
		public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
		{	pos=myElement.getAttribute("pos").getIntValue();
		   	loadData.tlId=myElement.getAttribute("tl-id").getIntValue();
		}
		
		public XMLElement saveSelf () throws XMLCannotSaveException
		{ 	XMLElement result=new XMLElement("target");
			result.addAttribute(new XMLAttribute("tl-id",tl.getId()));
			result.addAttribute(new XMLAttribute("pos",pos));
	  		return result;
		}
  
		public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
		{ 	// A Target has no child objects
		}

		public String getXMLName ()
		{ 	return parentName+".target";
		}
		
		public void setParentName (String parentName)
		{	this.parentName=parentName; 
		}
		
		// TwoStageLoader implementation of Target

		class TwoStageLoaderData 
		{ 	int tlId;
		}
		
		public void loadSecondStage (Dictionary dictionaries)
		{ 	Dictionary laneDictionary=(Dictionary)(dictionaries.get("lane"));
		  	tl=((Drivelane)(laneDictionary.get(
		                 new Integer(loadData.tlId)))).getSign();
		}
		
	}
	
	public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
	{ 	super.load(myElement,loader);
		gamma=myElement.getAttribute("gamma").getFloatValue();
		random_chance=myElement.getAttribute("random-chance").getFloatValue();
		q_table=(float[][][][])XMLArray.loadArray(this,loader);
		qa_table=(float[][][][])XMLArray.loadArray(this,loader);
		v_table=(float[][][])XMLArray.loadArray(this,loader);
		va_table=(float[][][])XMLArray.loadArray(this,loader);
		count=(Vector[][][])XMLArray.loadArray(this,loader,this);
		p_table=(Vector[][][])XMLArray.loadArray(this,loader,this);
		pKtl_table=(Vector[][][])XMLArray.loadArray(this,loader,this);
	}

	public XMLElement saveSelf () throws XMLCannotSaveException
	{ 	XMLElement result=super.saveSelf();
		result.setName(shortXMLName);
		result.addAttribute(new XMLAttribute ("random-chance",random_chance));
		result.addAttribute(new XMLAttribute ("gamma",gamma));
	  	return result;
	}
	
	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
	{ 	super.saveChilds(saver);
		XMLArray.saveArray(q_table,this,saver,"q-table");
		XMLArray.saveArray(qa_table,this,saver,"qa-table");
		XMLArray.saveArray(v_table,this,saver,"v-table");
		XMLArray.saveArray(va_table,this,saver,"va-table");
		XMLArray.saveArray(count,this,saver,"count");
		XMLArray.saveArray(p_table,this,saver,"p-table");
		XMLArray.saveArray(pKtl_table,this,saver,"pKtl_table");
	}

	public String getXMLName ()
	{ 	return "model."+shortXMLName;
	}
	
	public void loadSecondStage (Dictionary dictionaries) throws XMLInvalidInputException,XMLTreeException
	{ 	super.loadSecondStage(dictionaries);
		for(int i=0;i<count.length;i++)
					for(int j=0;j<count[i].length;j++)
						for(int k=0;k<count[i][j].length;k++)
							XMLUtils.loadSecondStage(count[i][j][k].elements(),dictionaries);		
		for(int i=0;i<p_table.length;i++)
					for(int j=0;j<p_table[i].length;j++)
						for(int k=0;k<p_table[i][j].length;k++)
							XMLUtils.loadSecondStage(p_table[i][j][k].elements(),dictionaries);		
		for(int i=0;i<pKtl_table.length;i++)
					for(int j=0;j<pKtl_table[i].length;j++)
						for(int k=0;k<pKtl_table[i][j].length;k++)
							XMLUtils.loadSecondStage(pKtl_table[i][j][k].elements(),dictionaries);		
		System.out.println("TC3 second stage load finished.");			
	}
		
	public boolean canCreateInstance (Class request)
	{ 	return CountEntry.class.equals(request) ||
			PEntry.class.equals(request) ||
			PKtlEntry.class.equals(request);
	}
	
	public Object createInstance (Class request) throws ClassNotFoundException,InstantiationException,IllegalAccessException
	{ 	if (CountEntry.class.equals(request))
		{  return new CountEntry();
		}
		else if ( PEntry.class.equals(request))
		{ return new PEntry();
		}
		else if ( PKtlEntry.class.equals(request))
		{ return new PKtlEntry();
		}
		else
		{ throw new ClassNotFoundException
		  ("TC3 IntstantiationAssistant cannot make instances of "+
		   request);
		}
	}	
	
	// Config dingetje
	
	public void showSettings(Controller c)
	{
		String[] descs = {"Gamma (discount factor)", "Random decision chance"};
		float[] floats = {gamma, random_chance};
		TLCSettings settings = new TLCSettings(descs, null, floats);
				
		settings = doSettingsDialog(c, settings);
		gamma = settings.floats[0];
		random_chance = settings.floats[1];
	}
}
