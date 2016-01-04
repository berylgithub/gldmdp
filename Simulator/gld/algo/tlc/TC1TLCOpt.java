
/*-----------------------------------------------------------------------
 * Copyright (C) 2001 Green Light District Team, Utrecht University 
 * Copyright of the TC1 algorithm (C) Marco Wiering, Utrecht University
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

/**
 *
 * This controller will decide it's Q values for the traffic lights according to the traffic situation on
 * the lane connected to the TrafficLight. It will learn how to alter it's outcome by reinforcement learning.
 * Now Optimized 2.0
 * Now Long-Fixed, so run run run, Forrest!
 *
 * @author Arne K, Jilles V
 * @version 2.0
 */
public class TC1TLCOpt extends TLController implements Colearning, InstantiationAssistant
{	
	// TLC vars
	protected Infrastructure infrastructure;
	protected TrafficLight[][] tls;
	protected Node[] allnodes;
	protected int num_nodes;
	
	// TC1 vars
	protected Vector count[][][], pTable[][][];
	protected float [][][][] qTable; //sign, pos, des, color (red=0, green=1)
	protected float [][][]   vTable;
	protected static float gamma=0.90f;				//Discount Factor; used to decrease the influence of previous V values, that's why: 0 < gamma < 1
	protected final static boolean red=false, green=true;
	protected final static int green_index=0, red_index=1;
	protected final static String shortXMLName="tlc-tc1o1";
	protected static float random_chance=0.01f;				//A random gain setting is chosen instead of the on the TLC dictates with this chance
	private Random random_number;
	
	/**
	 * The constructor for TL controllers
	 * @param The model being used.
	 */
 
	public TC1TLCOpt ( Infrastructure infra ) throws InfraException
	{	super(infra);
	}
	
	public void setInfrastructure(Infrastructure infra) {
		super.setInfrastructure(infra);
		try{
			Node[] nodes = infra.getAllNodes(); 
			num_nodes = nodes.length;

			int numSigns = infra.getAllInboundLanes().size();
			qTable = new float [numSigns][][][];
			vTable = new float [numSigns][][];
			count	= new Vector[numSigns][][];
			pTable = new Vector[numSigns][][];
    
			int num_specialnodes = infra.getNumSpecialNodes();
			for (int i=0; i<num_nodes; i++)	{
				Node n = nodes[i];
				Drivelane [] dls = n.getInboundLanes();
				for (int j=0; j<dls.length; j++) {
				    Drivelane d = dls[j];
				    Sign s = d.getSign();
				    int id = s.getId();
				    int num_pos_on_dl = d.getCompleteLength();
    				
				    qTable[id] = new float [num_pos_on_dl][][];
				    vTable[id] = new float [num_pos_on_dl][];
				    count[id] = new Vector[num_pos_on_dl][];
				    pTable[id] = new Vector[num_pos_on_dl][];
    				
				    for (int k=0; k<num_pos_on_dl; k++)	{
					    qTable[id][k]=new float[num_specialnodes][];
					    vTable[id][k]=new float[num_specialnodes];
					    count[id][k] = new Vector[num_specialnodes];
					    pTable[id][k] = new Vector[num_specialnodes];
    					
					    for (int l=0; l<num_specialnodes;l++)	{
						    qTable[id][k][l]	= new float [2];
						    qTable[id][k][l][0]= 0.0f;
						    qTable[id][k][l][1]= 0.0f;
						    vTable[id][k][l]	= 0.0f;
						    count[id][k][l] 	= new Vector();
						    pTable[id][k][l]	= new Vector();
					    }
				    }
			    }
		    }		
		}
		catch(Exception e) {}
		random_number = new Random();
	}

	
	/**
	* Calculates how every traffic light should be switched
	* Per node, per sign the waiting roadusers are passed and per each roaduser the gain is calculated.
	* @param The TLDecision is a tuple consisting of a traffic light and a reward (Q) value, for it to be green
	* @see gld.algo.tlc.TLDecision
	*/	
	public TLDecision[][] decideTLs() {
		/* gain = 0
		 * For each TL
		 *  For each Roaduser waiting
		 *   gain = gain + pf*(Q([tl,pos,des],red) - Q([tl,pos,des],green))
		 */		 
		int num_dec, waitingsize, pos, tlId, desId;
		float gain, passenger_factor;
		Sign tl; Drivelane lane; Roaduser ru; ListIterator queue; Node destination;
		
		//Determine wheter it should be random or not
		boolean randomrun = false;
		if (random_number.nextFloat() < random_chance) randomrun = true;
		
		// For all Nodes
		for (int i=0;i<num_nodes;i++) {
			num_dec = tld[i].length;
			// For all Trafficlights
			for(int j=0;j<num_dec;j++) {
				tl = tld[i][j].getTL();
				tlId = tl.getId();
				lane = tld[i][j].getTL().getLane();
				
				waitingsize = lane.getNumRoadusersWaiting();
				queue = lane.getQueue().listIterator();
				gain = 0;
				
				// For each waiting Roaduser
				for(int k=0; k<waitingsize; k++) {
					ru = (Roaduser) queue.next();
					pos = ru.getPosition();
					desId = ru.getDestNode().getId();
					passenger_factor = ru.getNumPassengers();
					
					// Add the pf*(Q([tl,pos,des],red)-Q([tl,pos,des],green))
					gain += passenger_factor * (qTable[tlId][pos][desId][red_index] - qTable[tlId][pos][desId][green_index]);  //red - green
	    		}
				
				// Debug info generator
				if(trackNode!=-1 && i==trackNode) {
					Drivelane currentlane2 = tld[i][j].getTL().getLane();
					boolean[] targets = currentlane2.getTargets();
					System.out.println("node: "+i+" light: "+j+" gain: "+gain+" "+targets[0]+" "+targets[1]+" "+targets[2]+" "+currentlane2.getNumRoadusersWaiting());
				}
				
				// If this is a random run, set all gains randomly
                if(randomrun)
                	gain = random_number.nextFloat();

                if(gain >1000.0 || gain < -1000.0f)
                	System.out.println("Gain might be too high? : "+gain);
	    		tld[i][j].setGain(gain);
	    	}
	    }
	    return tld;
	}

	public void updateRoaduserMove(Roaduser ru, Drivelane prevlane, Sign prevsign, int prevpos, Drivelane dlanenow, Sign signnow, int posnow, PosMov[] posMovs, Drivelane desired)
	{
		// Roaduser has just left the building!
		if(dlanenow == null || signnow == null) {
			return;
		}
		
		//This ordening is important for the execution of the algorithm!
		if(prevsign.getType()==Sign.TRAFFICLIGHT && (signnow.getType()==Sign.TRAFFICLIGHT || signnow.getType()==Sign.NO_SIGN)) {
			int tlId = prevsign.getId();
			int desId = ru.getDestNode().getId();
			recalcP(tlId, prevpos, desId, prevsign.mayDrive(), signnow.getId(), posnow);
			recalcQ(tlId, prevpos, desId, prevsign.mayDrive(), signnow.getId(), posnow, posMovs);
			recalcV(tlId, prevpos, desId);
		}
	}
	
	protected void recalcP(int tlId, int pos, int desId, boolean light, int tlNewId, int posNew)
	{
		// - First create a CountEntry, find if it exists, and if not add it.
		CountEntry thisSituation = new CountEntry(tlId,pos,desId,light,tlNewId,posNew);
		int c_index = count[tlId][pos][desId].indexOf(thisSituation);
		if(c_index >= 0) {
			// Entry found
			thisSituation = (CountEntry) count[tlId][pos][desId].elementAt(c_index);
			thisSituation.incrementValue();
		}
		else {
			// Entry not found
			count[tlId][pos][desId].addElement(thisSituation);
		}
		 
		// We now know how often this exact situation has occurred
		// - Calculate the chance
		long sameSituation = thisSituation.getValue();
		long sameStartSituation = 0;
		
		CountEntry curC;
		int num_c = count[tlId][pos][desId].size();
		for(int i=0;i<num_c;i++) {
			curC = (CountEntry) count[tlId][pos][desId].elementAt(i);
			sameStartSituation	+= curC.sameStartSituation(thisSituation);
		}

		// - Update this chance
		// Calculate the new P(L|(tl,pos,des))
		// P(L|(tl,pos,des))	= P([tl,pos,des],L)/P([tl,pos,des])
		//						= #([tl,pos,des],L)/#([tl,pos,des])
		// Niet duidelijk of dit P([tl,p,d],L,[*,*]) of P([tl,p,d],L,[tl,d]) moet zijn
		// Oftewel, kans op deze transitie of kans om te wachten!
		
		PEntry thisChance = new PEntry(tlId,pos,desId,light,tlNewId,posNew);
		int p_index = pTable[tlId][pos][desId].indexOf(thisChance);
		
		if(p_index >= 0)
			thisChance = (PEntry) pTable[tlId][pos][desId].elementAt(p_index);
		else {
			pTable[tlId][pos][desId].addElement(thisChance);
			p_index = pTable[tlId][pos][desId].indexOf(thisChance);
		}
		
		thisChance.setSameSituation(sameSituation);
		thisChance.setSameStartSituation(sameStartSituation);		 
		
		// - Update rest of the Chance Table
		int num_p = pTable[tlId][pos][desId].size();
		PEntry curP;
		for(int i=0;i<num_p;i++) {
			curP = (PEntry) pTable[tlId][pos][desId].elementAt(i);
			if(curP.sameStartSituation(thisSituation) && i!=p_index)
				curP.addSameStartSituation();
		}
	}

	protected void recalcQ(int tlId, int pos, int desId, boolean light, int tlNewId, int posNew, PosMov[] posMovs)
	{	// Meneer Kaktus zegt: OK!
		// Q([tl,p,d],L)	= Sum(tl', p') [P([tl,p,d],L,[tl',p'])(R([tl,p],[tl',p'])+ yV([tl',p',d]))

		// First gather All tl' and p' in one array
		int num_posmovs	= posMovs.length;
		
		PosMov curPosMov;
		int curPMTlId, curPMPos;
		float R=0, V=0, Q=0;
		
		for(int t=0; t<num_posmovs; t++) {		// For All tl', pos'
			curPosMov = posMovs[t];
			curPMTlId = curPosMov.tlId;
			curPMPos  = curPosMov.pos;

			PEntry P = new PEntry(tlId, pos, desId, light, curPMTlId, curPMPos);
			int p_index = pTable[tlId][pos][desId].indexOf(P);
			
			if(p_index>=0) {
				P = (PEntry) pTable[tlId][pos][desId].elementAt(p_index);
				R = rewardFunction(tlId, pos, curPMTlId, curPMPos);
				V = vTable[curPMTlId][curPMPos][desId];
				Q += P.getChance() *(R + (gamma * V));
			}
			// Else P(..)=0, thus will not add anything in the summation
		}
		qTable[tlId][pos][desId][light?green_index:red_index]=Q;
	}


	protected void recalcV(int tlId, int pos, int desId)
	{	//  V([tl,p,d]) = Sum (L) [P(L|(tl,p,d))Q([tl,p,d],L)]
		float qRed		= qTable[tlId][pos][desId][red_index];
		float qGreen	= qTable[tlId][pos][desId][green_index];
		float[] pGR 	= calcPGR(tlId,pos,desId);
		float pGreen	= pGR[green_index];
		float pRed		= pGR[red_index];

		vTable[tlId][pos][desId] = (pGreen*qGreen) + (pRed*qRed);		
	}

	/*
				==========================================================================
							Additional methods, used by the recalc methods 
				==========================================================================
	*/


	protected float[] calcPGR(int tlId, int pos, int desId) {
		float[] counters = new float[2];
		double countR=0, countG=0;
		
		int psize = pTable[tlId][pos][desId].size()-1;
		for(; psize>=0; psize--) {
			PEntry cur = (PEntry) pTable[tlId][pos][desId].elementAt(psize);
			if(cur.light==green)
				countG += cur.getSameSituation();
			else
				countR += cur.getSameSituation();
		}
		counters[green_index] = (float) (countG/(countG+countR));
		counters[red_index] = (float)(countR/(countG+countR));
		return counters;
	}

	
	protected int rewardFunction(int tlId, int pos, int tlNewId, int posNew) {
		if(tlId!=tlNewId || pos != posNew)
			return 0;
		return 1;
	}
	
	public float getVValue(Sign sign, Node des, int pos) {
		return vTable[sign.getId()][pos][des.getId()];
	}


	public float getColearnValue(Sign now, Sign sign, Node des, int pos) {
		return getVValue(sign,des,pos);
	}
	

	/*
				==========================================================================
					Internal Classes to provide a way to put entries into the tables 
				==========================================================================
	*/

	public class CountEntry implements XMLSerializable
	{
		// CountEntry vars
		int tlId, pos, desId, tlNewId, posNew;
		long value;
		boolean light;
		
		// XML vars
		String parentName="model.tlc";
		
		CountEntry(int _tlId, int _pos, int _desId, boolean _light, int _tlNewId, int _posNew) {
			tlId = _tlId;					// The Sign the RU was at
			pos = _pos;						// The position the RU was at
			desId = _desId;					// The SpecialNode the RU is travelling to
			light = _light;					// The colour of the Sign the RU is at now
			tlNewId= _tlNewId;				// The Sign the RU is at now
			posNew = _posNew;				// The position the RU is on now
			value=1;						// How often this situation has occurred
		}
		
		public CountEntry ()
		{ // Empty constructor for loading
		}
		
		public void incrementValue() {
			value++;
		}
		
		// Returns how often this situation has occurred
		public long getValue() {
			return value;
		}

		public boolean equals(Object other) {
			if(other != null && other instanceof CountEntry)
			{	CountEntry countnew = (CountEntry) other;
				if(countnew.tlId!=tlId) return false;
				if(countnew.pos!=pos) return false;
				if(countnew.desId!=desId) return false;
				if(countnew.light!=light) return false;
				if(countnew.tlNewId!=tlNewId) return false;
				if(countnew.posNew!=posNew) return false;
				return true;
			}
			return false;
		}
		
		// Retuns the count-value if the situations match
		public long sameSituation(CountEntry other) {
			if(equals(other))
				return value;
			else
				return 0;
		}		
		
		// Retuns the count-value if the startingsituations match
		public long sameStartSituation(CountEntry other) {
			if(other.tlId==tlId && other.pos==pos && other.desId==desId && other.light==light)
				return value;
			else
				return 0;
		}
		
		// XMLSerializable implementation of CountEntry
		public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
		{	pos=myElement.getAttribute("pos").getIntValue();
			tlId=myElement.getAttribute("tl-id").getIntValue();
			desId=myElement.getAttribute("des-id").getIntValue();
		   	light=myElement.getAttribute("light").getBoolValue();
		   	tlNewId=myElement.getAttribute("new-tl-id").getIntValue();
			posNew=myElement.getAttribute("new-pos").getIntValue();
			value=myElement.getAttribute("value").getLongValue(); 
		}

		public XMLElement saveSelf () throws XMLCannotSaveException
		{ 	XMLElement result=new XMLElement("count");
			result.addAttribute(new XMLAttribute("pos",pos));
			result.addAttribute(new XMLAttribute("tl-id",tlId));
			result.addAttribute(new	XMLAttribute("des-id",desId));
			result.addAttribute(new XMLAttribute("light",light));
			result.addAttribute(new XMLAttribute("new-tl-id",tlNewId));
			result.addAttribute(new XMLAttribute("new-pos",posNew));
			result.addAttribute(new XMLAttribute("value",value));
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
	}
	
	public class PEntry implements XMLSerializable
	{
		// PEntry vars
		int pos, posNew, tlId, tlNewId, desId;
		double sameStartSituation,sameSituation;
		boolean light;
		
		// XML vars
		String parentName="model.tlc";
		
		PEntry(int _tlId, int _pos, int _desId, boolean _light, int _tlNewId, int _posNew) {
			tlId = _tlId;					// The Sign the RU was at
			pos = _pos;						// The position the RU was at
			desId = _desId;					// The SpecialNode the RU is travelling to
			light = _light;					// The colour of the Sign the RU is at now
			tlNewId= _tlNewId;				// The Sign the RU is at now
			posNew = _posNew;				// The position the RU is on now
			sameStartSituation=0;			// How often this situation has occurred
			sameSituation=0;
		}
		
		public PEntry ()
		{	// Empty constructor for loading
		}
		
		public void addSameStartSituation() {	sameStartSituation++;	}
		public void setSameStartSituation(long s) {	sameStartSituation = s;	}
		
		public void setSameSituation(long s) {	sameSituation = s;	}
		
		public double getSameStartSituation() {	return sameStartSituation;	}
		public double getSameSituation() {	return sameSituation;	}
		
		public double getChance() {	return getSameSituation()/getSameStartSituation();	}
		
		public boolean equals(Object other) {
			if(other != null && other instanceof PEntry) {
				PEntry pnew = (PEntry) other;
				if(pnew.tlId!=tlId) return false;
				if(pnew.pos!=pos) return false;
				if(pnew.desId!=desId) return false;
				if(pnew.light!=light) return false;
				if(pnew.tlNewId!=tlNewId) return false;
				if(pnew.posNew!=posNew) return false;
				return true;
			}
			return false;
		}

		public boolean sameSituation(CountEntry other) {
			return equals(other);
		}

		public boolean sameStartSituation(CountEntry other) {
			if(other.tlId==tlId && other.pos==pos && other.desId==desId && other.light==light)
				return true;
			else
				return false;
		}
		
		// XMLSerializable implementation of PEntry
		public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
		{	pos=myElement.getAttribute("pos").getIntValue();
			tlId=myElement.getAttribute("tl-id").getIntValue();
			desId=myElement.getAttribute("des-id").getIntValue();
		   	light=myElement.getAttribute("light").getBoolValue();
		   	tlNewId=myElement.getAttribute("new-tl-id").getIntValue();
			posNew=myElement.getAttribute("new-pos").getIntValue();
			sameStartSituation=myElement.getAttribute("same-startsituation").getLongValue();
			sameSituation=myElement.getAttribute("same-situation").getLongValue();
		}

		public XMLElement saveSelf () throws XMLCannotSaveException
		{ 	XMLElement result=new XMLElement("pval");
			result.addAttribute(new XMLAttribute("pos",pos));
			result.addAttribute(new XMLAttribute("tl-id",tlId));
			result.addAttribute(new	XMLAttribute("des-id",desId));
			result.addAttribute(new XMLAttribute("light",light));
			result.addAttribute(new XMLAttribute("new-tl-id",tlNewId));
			result.addAttribute(new XMLAttribute("new-pos",posNew));
			result.addAttribute(new XMLAttribute("same-startsituation",sameStartSituation));
			result.addAttribute(new XMLAttribute("same-situation",sameSituation));
	  		return result;
		}
  
		public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
		{ 	// A PEntry has no child objects
		}
		
		public void setParentName (String parentName)
		{	this.parentName=parentName; 
		}
  
		public String getXMLName ()
		{ 	return parentName+".pval";
		}
	}	
	
	public void showSettings(Controller c)
	{
		String[] descs = {"Gamma (discount factor)", "Random decision chance"};
		float[] floats = {gamma, random_chance};
		TLCSettings settings = new TLCSettings(descs, null, floats);
				
		settings = doSettingsDialog(c, settings);
		gamma = settings.floats[0];
		random_chance = settings.floats[1];
	}
	
	// XMLSerializable, SecondStageLoader and InstantiationAssistant implementation
	
	public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
	{	super.load(myElement,loader);
		gamma=myElement.getAttribute("gamma").getFloatValue();
		random_chance=myElement.getAttribute("random-chance").getFloatValue();
		qTable=(float[][][][])XMLArray.loadArray(this,loader);
		vTable=(float[][][])XMLArray.loadArray(this,loader);
		count=(Vector[][][])XMLArray.loadArray(this,loader,this);
		pTable=(Vector[][][])XMLArray.loadArray(this,loader,this);
	}
  
	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
	{	super.saveChilds(saver);
		XMLArray.saveArray(qTable,this,saver,"q-table");
		XMLArray.saveArray(vTable,this,saver,"v-table");
		XMLArray.saveArray(count,this,saver,"counts");
		XMLArray.saveArray(pTable,this,saver,"p-table");
	}
	
	public XMLElement saveSelf () throws XMLCannotSaveException
	{ 	XMLElement result=super.saveSelf();
		result.setName(shortXMLName);
		result.addAttribute(new XMLAttribute ("random-chance",random_chance));
		result.addAttribute(new XMLAttribute ("gamma",gamma));
	  	return result;
	}

	public String getXMLName ()
	{ 	return "model."+shortXMLName;
	}
		
	public boolean canCreateInstance (Class request)
	{ 	System.out.println("Called TC1TLC-opt instantiation assistant ??");
		return CountEntry.class.equals(request) ||
	        	PEntry.class.equals(request);
	}
	
	public Object createInstance (Class request) throws 
	      ClassNotFoundException,InstantiationException,IllegalAccessException
	{ 	System.out.println("Called TC1TLC-opt instantiation assistant");
		if (CountEntry.class.equals(request))
		{ return new CountEntry();
		}
		else if ( PEntry.class.equals(request))
		{ return new PEntry();
		}
		else
		{ throw new ClassNotFoundException
		  ("TC1 IntstantiationAssistant cannot make instances of "+
		   request);
		}
	}
}