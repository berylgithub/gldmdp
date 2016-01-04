
/*-----------------------------------------------------------------------
 * Copyright (C) 2001 Green Light District Team, Utrecht University 
 * Copyright of the TC2 algorithm (C) Marco Wiering, Utrecht University
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
 *
 * Now Working Rightly!
 *
 * @author Jilles V and Arne K
 * @version 1.0
 */
public class TC2Final extends TLController implements Colearning,InstantiationAssistant
{	
	// TLC vars
	protected Infrastructure infrastructure;
	protected TrafficLight[][] tls;
	protected Node[] allnodes;
	protected int num_nodes;

	// TC2 vars
	protected Vector [][][] count, intercount, pTable, pKtlTable;		// SituationCount, Chance for situtation for SignId, Position, DestinationNodeId
	protected float [][][][] qTable;						// Punishment for SignId, Position, DestinationNodeId, LightColor
	protected float [][][]   vTable;						// Average wait for SignId, Position, DestinationNodeId
	protected static float gamma=0.95f;						// Discount Factor; used to decrease the influence of previous V values, that's why: 0 < gamma < 1
	protected final static boolean red=false, green=true;
	protected final static int green_index=0, red_index=1;
	public final static String shortXMLName="tlc-tc2final";
	protected static float random_chance=0.01f;				//A random gain setting is chosen instead of the on the TLC dictates with this chance
	private Random random_number;


	/**
	 * The constructor for TL controllers
	 * @param The infrastructure being used.
	 */
	public TC2Final( Infrastructure infra ) throws InfraException
	{	super(infra);
	}
	
	public void setInfrastructure(Infrastructure infra)
	{	super.setInfrastructure(infra);
		try{
			Node[] nodes = infra.getAllNodes(); 
			num_nodes = nodes.length;

			int numSigns = infra.getAllInboundLanes().size();
			qTable = new float [numSigns][][][];
			vTable = new float [numSigns][][];
			count	= new Vector[numSigns][][];
      intercount	= new Vector[numSigns][][];
			pTable = new Vector[numSigns][][];
			pKtlTable = new Vector[numSigns][][];
    
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
            intercount[id] = new Vector[num_pos_on_dl][];
					pTable[id] = new Vector[num_pos_on_dl][];
					pKtlTable[id] = new Vector[num_pos_on_dl][];
    				
				    for (int k=0; k<num_pos_on_dl; k++)	{
					    qTable[id][k]=new float[num_specialnodes][];
					    vTable[id][k]=new float[num_specialnodes];
					    count[id][k] = new Vector[num_specialnodes];
              intercount[id][k] = new Vector[num_specialnodes];
					    pTable[id][k] = new Vector[num_specialnodes];
						pKtlTable[id][k] = new Vector[num_specialnodes];
    					
					    for (int l=0; l<num_specialnodes;l++)	{
						    qTable[id][k][l]	= new float [2];
						    qTable[id][k][l][0] = 0.0f;
						    qTable[id][k][l][1] = 0.0f;
						    vTable[id][k][l]	= 0.0f;
						    count[id][k][l] 	= new Vector();
                intercount[id][k][l] 	= new Vector();
						    pTable[id][k][l] = new Vector();
							pKtlTable[id][k][l] = new Vector();
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
	public TLDecision[][] decideTLs()
	{
		/* gain = 0
		 * For each TL
		 *  For each Roaduser waiting
		 *   gain = gain + pf*(Q([tl,pos,des],red) - Q([tl,pos,des],green))
		 */
		 
		int num_dec, waitingsize, pos, tlId, desId;
		float gain, passenger_factor;
		Sign tl;
		Drivelane lane;
		Roaduser ru;
		ListIterator queue;
		Node destination;
	    
		//Determine wheter it should be random or not
		boolean randomrun = false;
		if (random_number.nextFloat() < random_chance) randomrun = true;
	    
		for (int i=0;i<num_nodes;i++) {
			num_dec = tld[i].length;
			for(int j=0;j<num_dec;j++) {
				tl = tld[i][j].getTL();
				tlId = tl.getId();
				lane = tld[i][j].getTL().getLane();
				
				waitingsize = lane.getNumRoadusersWaiting();
				queue = lane.getQueue().listIterator();
				gain = 0;

				for(; waitingsize>0; waitingsize--) {
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

                if(gain >50.0 || gain < -50.0f)
                	System.out.println("Gain might be too high? : "+gain);
	    		tld[i][j].setGain(gain);
	    	}
	    }
	    return tld;
	}

	public void updateRoaduserMove(Roaduser ru, Drivelane prevlane, Sign prevsign, int prevpos, Drivelane dlanenow, Sign signnow, int posnow, PosMov[] posMovs, Drivelane desired)
	{
		//When a roaduser leaves the city; this will 
		if(dlanenow == null || signnow == null) {
			return;
		}

		//This ordening is important for the execution of the algorithm!
		int Ktl = dlanenow.getNumRoadusersWaiting();
    boolean isWaiting = dlanenow.updateWaitingPosition(ru);

		if(prevsign.getType()==Sign.TRAFFICLIGHT && (signnow.getType()==Sign.TRAFFICLIGHT || signnow.getType()==Sign.NO_SIGN)) {
			int tlId = prevsign.getId();
			int desId = ru.getDestNode().getId();
			recalcP(tlId, prevpos, desId, prevsign.mayDrive(), signnow.getId(), posnow, Ktl);
			recalcQ(tlId, prevpos, desId, prevsign.mayDrive(), signnow, posnow, posMovs, Ktl, ru, isWaiting);
			recalcV(tlId, prevpos, desId);
		}
	}
	
	protected void recalcP(int tlId, int pos, int desId, boolean light, int tlNewId, int posNew, int Ktl)
	{	// Meneer Kaktus zegt: OK!
		// Meneer Kaktus zegt: PEntries nu ook updated
		// Recalc the chances
		
		// - First create a CountEntry, find if it exists, and if not add it.
		CountEntry thisSituation = new CountEntry(tlId,pos,desId,light,tlNewId,posNew,Ktl);
		int c_index = count[tlId][pos][desId].indexOf(thisSituation);

		if(c_index >= 0) {
			// Entry found
			thisSituation = (CountEntry) count[tlId][pos][desId].elementAt(c_index);
			thisSituation.incrementValue();
		}
		else {
			// Entry not found
			count[tlId][pos][desId].addElement(thisSituation);
			c_index = count[tlId][pos][desId].indexOf(thisSituation);
		}

		// We now know how often this exact situation has occurred
		// - Calculate the chance
		long sameSituationKtl = 0;
		long sameStartSituationKtl = 0;
		long sameSituation = 0;
		long sameStartSituation = 0;
		
		CountEntry curC;
		int num_c = count[tlId][pos][desId].size();
		for(int i=0;i<num_c;i++) {
			curC = (CountEntry) count[tlId][pos][desId].elementAt(i);
			sameSituationKtl		+= curC.sameSituationWithKtl(thisSituation);
			sameStartSituationKtl	+= curC.sameStartSituationWithKtl(thisSituation);
			sameSituation			+= curC.sameSituationDiffKtl(thisSituation);
			sameStartSituation		+= curC.sameStartSituationDiffKtl(thisSituation);
		}

		// - Update this chance in the PKtlTable
		PKtlEntry thisChanceKtl = new PKtlEntry(tlId,pos,desId,light,tlNewId,posNew,Ktl);
		int pKtl_index = pKtlTable[tlId][pos][desId].indexOf(thisChanceKtl);
		
		if(pKtl_index >= 0)
			thisChanceKtl = (PKtlEntry) pKtlTable[tlId][pos][desId].elementAt(pKtl_index);
		else {
			pKtlTable[tlId][pos][desId].addElement(thisChanceKtl);
			pKtl_index = pKtlTable[tlId][pos][desId].indexOf(thisChanceKtl);
		}
		
		thisChanceKtl.setSameSituation(sameSituationKtl);
		thisChanceKtl.setSameStartSituation(sameStartSituationKtl);
		
		// - Update rest of the PKtl Table
		int num_pKtl = pKtlTable[tlId][pos][desId].size();
		PKtlEntry curPKtl;
		for(int i=0;i<num_pKtl;i++) {
			curPKtl = (PKtlEntry) pKtlTable[tlId][pos][desId].elementAt(i);
			if(curPKtl.sameStartSituationWithKtl(thisSituation) && i!=pKtl_index)
				curPKtl.addSameStartSituation();
		}
	}

	protected void recalcQ(int tlId, int pos, int desId, boolean light, Sign newSign, int posNew, PosMov[] posMovs, int Ktl, Roaduser ru, boolean isWaiting)

	{	// Meneer Kaktus zegt: OK!
		// Q([tl,p,d],L)	 = Sum (tl',p') [P([tl,p,d],*,L,[tl',p'])*(R([tl,p],[tl',p'])+yV([tl',p',d]))
		// Q([tl,p,d],green) = Sum (tl',p') [P([tl,p,d],K,green,[tl',p'])*(R([tl,p],[tl',p'])+yV([tl',p',d]))]

		// First gather All tl' and p' in one array
        int tlNewId = newSign.getId();
		int num_posmovs		= posMovs.length;

		PosMov curPosMov;
		int curPMTlId, curPMPos;
		float R=0, V=0, Q=0, P=0;

        /* Nu Foutieve Code Alert!!!!!!!
        // Zou het toch wel moeten zijn he!
        if((isWaiting || posNew == 0) && ru.getLastWaitPointTl() != null) {     // Deze optie als hij een kruispunt over is en nu moet wachten, of nu op positie 0 is
        // !!!!!!!!!  Eigenlijk ook check op huidige Tl != lastwaitTl
        int tlwaitId = ru.getLastWaitPointTl().getId();
        int poswait = ru.getLastWaitPointPos();
        // update the intercount table
  		CountEntry thisSituation = new CountEntry(tlwaitId,poswait,desId,light,tlNewId,posNew,Ktl);
		  int c_index = intercount[tlwaitId][poswait][desId].indexOf(thisSituation);

  		if(c_index >= 0) {
  			thisSituation = (CountEntry) intercount[tlwaitId][poswait][desId].elementAt(c_index);
  			thisSituation.incrementValue();
	  	}
  		else intercount[tlwaitId][poswait][desId].addElement(thisSituation);

      //For each create sums for all posnew-length positions
      int length = (newSign.getLane()).getLength();
      long [] samesituation;
      samesituation = new long [length - posNew];

   		int num_c = intercount[tlwaitId][poswait][desId].size();
		  CountEntry curC;
      long sameStartSituationKtl = 0;

  		for(int i=0;i<num_c;i++) {
  			curC = (CountEntry) intercount[tlwaitId][poswait][desId].elementAt(i);
  			sameStartSituationKtl	+= curC.sameStartSituationWithKtl(thisSituation);
        if((curC.posNew - posNew) >= 0 && curC.tlNewId==tlNewId) samesituation[curC.posNew - posNew] += curC.sameStartSituationWithKtl(thisSituation);
	  	}

			for(int t=posNew;t<length;t++) {
        if (sameStartSituationKtl != 0) P = (float)( (double) samesituation[t-posNew] / (double) sameStartSituationKtl );
        else P=0;
//        System.out.println("P: "+P+" teller: "+samesituation[t-posNew]+" noemer: "+ sameStartSituationKtl);
//				R = calcReward(tlId,pos,tlNewId,t);
        R = 0;
				V = vTable[tlNewId][t][desId];
				Q += P * (R + (gamma * V));
			}
		}

		else {
			// The light was either on red, or there is no way we can cross the node.
			for(int t=0;t<num_posmovs;t++) {
				// For each tl',p' 
				curPosMov = posMovs[t];
				curPMTlId = curPosMov.tlId;
				curPMPos  = curPosMov.pos;
				
				PKtlEntry PKtl = new PKtlEntry(tlId,pos,desId,light,curPMTlId,curPMPos,-1);
				
				long noem = 0;
				long deel = 0;

				CountEntry C = new CountEntry(tlId,pos,desId,light,curPMTlId,curPMPos,-1);
				int numC = count[tlId][pos][desId].size()-1;
				for(;numC>=0;numC--) {
					CountEntry curC = (CountEntry) count[tlId][pos][desId].elementAt(numC);
					noem += curC.sameSituationDiffKtl(C);
					deel += curC.sameStartSituationDiffKtl(C);					
				}
				
				PKtl.setSameSituation(noem);
				PKtl.setSameStartSituation(deel);
				
				R = calcReward(tlId,pos,curPMTlId,curPMPos);
				V = vTable[curPMTlId][curPMPos][desId];
				//System.out.println("TC1: Q:"+Q+" += "+PKtl.getChance()+ " * ("+R+"+("+gamma+"*"+V+"))");
				Q += PKtl.getChance() * (R + (gamma * V));
			}
		}
        */

		qTable[tlId][pos][desId][light?green_index:red_index]=Q;
	}

	protected void recalcV(int tlId, int pos, int desId)
	{	// Meneer Kaktus is tevree!

		// Dit moet gemiddelde *wachttijd* zijn dus:
		// V = pGreen*qGreen + pRed*qRed
		//	where
		//	pGreen	= P([tl,p,d],green,*,[*,*])
		//	pRed	= P([tl,p,d],red,*,[*,*])
		//	qGreen	= Q([tl,p,d],green)
		//	qRed	= Q([tl,p,d],red)

		float qRed		= qTable[tlId][pos][desId][red_index];
		float qGreen	= qTable[tlId][pos][desId][green_index];
		float[] pGR 	= calcPGR(tlId,pos,desId);

		vTable[tlId][pos][desId] = (pGR[green_index]*qGreen) + (pGR[red_index]*qRed);		
	}

	/*
				==========================================================================
							Additional methods, used by the recalc methods 
				==========================================================================
	*/

	protected float[] calcPGR(int tlId, int pos, int desId) {
		float[] counters = new float[2];
		
		int pKtlsize = pKtlTable[tlId][pos][desId].size()-1;
		long pKtlGC2=0,pKtlRC2=0;
		for(;pKtlsize>=0;pKtlsize--) {
			PKtlEntry cur = (PKtlEntry) pKtlTable[tlId][pos][desId].elementAt(pKtlsize);
			if(cur.light==green)
				pKtlGC2 += cur.getSameSituation();
			else
				pKtlRC2 += cur.getSameSituation();
		}
		
		counters[green_index] = (float) (((double)pKtlGC2)/((double)(pKtlGC2+pKtlRC2)));
		counters[red_index] = (float) (((double)pKtlRC2)/((double)(pKtlGC2+pKtlRC2)));
		
		return counters;
	}
	
	protected int calcReward(int tlId, int pos, int tlNewId, int posNew) {
		if(pos==posNew && tlId==tlNewId)
			return 1;
		else
			return 0;
	}
	
	protected boolean calcPossibleCross(int tlId, PosMov[] posMovs) {
		for(int i=0;i<posMovs.length;i++) {
			if(posMovs[i].tlId!=tlId)
				return true;
		}
		return false;
	}
	
	public float getVValue(Sign sign, Node des, int pos)	{
		return vTable[sign.getId()][pos][des.getId()];
	}


	public float getColearnValue(Sign sign_new, Sign sign, Node destination, int pos)	{
		int Ktl = sign.getLane().getNumRoadusersWaiting();
	
		// Calculate the colearning value
		float newCovalue=0;
		int size = sign.getLane().getCompleteLength()-1;

		for(; size>=0; size--) {
			float V;
			PKtlEntry P = new PKtlEntry(sign.getId(), 0, destination.getId(), green, sign_new.getId(), size, Ktl);
			int p_index = pKtlTable[sign.getId()][0][destination.getId()].indexOf(P);
			
			if(p_index>=0) {
				P = (PKtlEntry) pKtlTable[sign.getId()][0][destination.getId()].elementAt(p_index);
				V = vTable[sign.getId()][size][destination.getId()];
				newCovalue += P.getChance() * V;
			}
		}
		return newCovalue;
	}
	

	/*
				==========================================================================
					Internal Classes to provide a way to put entries into the tables 
				==========================================================================
	*/

	public class CountEntry implements XMLSerializable
	{
		// CountEntry vars
		int tlId, pos, desId, tlNewId, posNew, Ktl;
		long value;
		boolean light;
		
		// XML vars
		String parentName="model.tlc";

		CountEntry(int _tlId, int _pos, int _desId, boolean _light, int _tlNewId, int _posNew, int _Ktl) {		
			tlId = _tlId;					// The Sign the RU was at
			pos = _pos;						// The position the RU was at
			desId = _desId;					// The SpecialNode the RU is travelling to
			light = _light;					// The colour of the Sign the RU is at now
			tlNewId= _tlNewId;				// The Sign the RU is at now
			posNew = _posNew;				// The position the RU is on now
			Ktl = _Ktl;
			value=1;						// How often this situation has occurred
		}
		
		CountEntry ()
		{ // Empty constructor for loading
		}
		
		public void incrementValue() {
			value++;
		}
		
		public long getValue() {
			return value;
		}

		public boolean equals(Object other) {
			if(other != null && other instanceof CountEntry) {
				CountEntry countnew = (CountEntry) other;
				if(countnew.tlId!=tlId) return false;
				if(countnew.pos!=pos) return false;
				if(countnew.desId!=desId) return false;
				if(countnew.light!=light) return false;
				if(countnew.tlNewId!=tlNewId) return false;
				if(countnew.posNew!=posNew) return false;
				if(countnew.Ktl!=Ktl) return false;
				return true;
			}
			return false;
		}

		// Retuns the count-value if the startingsituations match
		public long sameStartSituationDiffKtl(CountEntry other) {
			if(other.tlId==tlId && other.pos==pos && other.desId==desId && other.light==light)
				return value;
			else
				return 0;
		}

		public long sameStartSituationWithKtl(CountEntry other) {
			if(other.tlId==tlId && other.pos==pos && other.desId==desId && other.light==light && other.Ktl==Ktl)
				return value;
			else
				return 0;
		}

		public long sameSituationDiffKtl(CountEntry other) {
			if(other.tlId==tlId && other.pos==pos && other.light==light && other.desId==desId && other.tlNewId==tlNewId && other.posNew==posNew)
				return value;
			else
				return 0;
		}
		
		public long sameSituationWithKtl(CountEntry other) {
			if(equals(other))
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
			Ktl=myElement.getAttribute("ktl").getIntValue();
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
			result.addAttribute(new XMLAttribute("ktl",Ktl));
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
	
	public class PKtlEntry implements XMLSerializable
	{	// PEntry vars
		int pos, posNew, tlId, tlNewId, desId, Ktl;
		long sameStartSituation,sameSituation;
		boolean light;
		// XML vars
		String parentName="model.tlc";
		
		PKtlEntry(int _tlId, int _pos, int _desId, boolean _light, int _tlNewId, int _posNew, int _Ktl) {
			tlId = _tlId;					// The Sign the RU was at
			pos = _pos;						// The position the RU was at
			desId = _desId;					// The SpecialNode the RU is travelling to
			light = _light;					// The colour of the Sign the RU is at now
			tlNewId= _tlNewId;				// The Sign the RU is at now
			posNew = _posNew;				// The position the RU is on now
			Ktl = _Ktl;
			sameStartSituation=0;			// How often this situation has occurred
			sameSituation=0;
		}
		
		public PKtlEntry ()	{
			// Empty constructor for loading
		}
		
		public void addSameStartSituation() 		{	sameStartSituation++;		}
		public void setSameStartSituation(long s)	{	sameStartSituation = s;		}
		public void setSameSituation(long s)		{	sameSituation = s;			}
		public float getSameStartSituation()		{	return sameStartSituation;	}
		public float getSameSituation() 			{	return sameSituation;		}
		
		public float getChance()	{
			if(getSameStartSituation()==0)
				return 0;
			else
				return (float) ((float)getSameSituation())/((float)getSameStartSituation());
		}
		

		public boolean sameStartSituationDiffKtl(CountEntry other) {
			if(other.tlId==tlId && other.pos==pos && other.desId==desId && other.light==light)
				return true;
			else
				return false;
		}

		public boolean sameStartSituationWithKtl(CountEntry other) {
			if(other.tlId==tlId && other.pos==pos && other.desId==desId && other.light==light && other.Ktl==Ktl)
				return true;
			else
				return false;
		}
		
		public boolean sameSituationDiffKtl(PKtlEntry other) {
			if(other.tlId==tlId && other.pos==pos && other.desId==desId && other.light==light && other.tlNewId==tlNewId && other.posNew==posNew)
				return true;
			else
				return false;
		}

		public boolean equals(Object other) {
			if(other != null && other instanceof PKtlEntry) {
				PKtlEntry pnew = (PKtlEntry) other;
				if(pnew.tlId!=tlId) return false;
				if(pnew.pos!=pos) return false;
				if(pnew.desId!=desId) return false;
				if(pnew.light!=light) return false;
				if(pnew.tlNewId!=tlNewId) return false;
				if(pnew.posNew!=posNew) return false;
				if(pnew.Ktl!=Ktl) return false;
				return true;
			}
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
			Ktl=myElement.getAttribute("ktl").getIntValue();
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
			result.addAttribute(new XMLAttribute("ktl",Ktl));
	  		return result;
		}
  
		public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
		{ 	// A PEntry has no child objects
		}
		
		public void setParentName (String parentName)	{	this.parentName=parentName;		}
		public String getXMLName ()						{	return parentName+".pktlval";	}
	}	

	public void showSettings(Controller c)	{
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
		qTable=(float[][][][])XMLArray.loadArray(this,loader);
		vTable=(float[][][])XMLArray.loadArray(this,loader);
		gamma=myElement.getAttribute("gamma").getFloatValue();
		random_chance=myElement.getAttribute("random-chance").getFloatValue();
		count=(Vector[][][])XMLArray.loadArray(this,loader,this);
		pKtlTable=(Vector[][][])XMLArray.loadArray(this,loader,this);
	}
		      
	public XMLElement saveSelf () throws XMLCannotSaveException
	{ 	XMLElement result=super.saveSelf();
		result.setName(shortXMLName);
		result.addAttribute(new XMLAttribute("random-chance",random_chance));
		result.addAttribute(new XMLAttribute("gamma",gamma));
	  	return result;
	}
  
	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
	{	super.saveChilds(saver);
		XMLArray.saveArray(qTable,this,saver,"q-table");
		XMLArray.saveArray(vTable,this,saver,"v-table");
		XMLArray.saveArray(count,this,saver,"count");
		XMLArray.saveArray(pKtlTable,this,saver,"pKtlTable");
	}

	public String getXMLName ()
	{ 	return "model."+shortXMLName;
	}
		
	public void loadSecondStage (Dictionary dictionaries) throws XMLInvalidInputException,XMLTreeException
	{	}
	
	public boolean canCreateInstance (Class request)
	{ 	return CountEntry.class.equals(request) ||
			PKtlEntry.class.equals(request);
	}
	
	public Object createInstance (Class request) throws
	      ClassNotFoundException,InstantiationException,IllegalAccessException
	{ 	if (CountEntry.class.equals(request))
		{  return new CountEntry();
		}
		else if ( PKtlEntry.class.equals(request))
		{ return new PKtlEntry();
		}
		else
		{ throw new ClassNotFoundException
		  ("TC2 IntstantiationAssistant cannot make instances of "+
		   request);
		}
	}
}





