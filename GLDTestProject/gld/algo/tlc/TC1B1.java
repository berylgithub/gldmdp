
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
 * Now Bucketed
 * Now Optimized 2.0
 *
 * @author Arne K, Jilles V
 * @version 0.915
 */
public class TC1B1 extends TLController implements Colearning, InstantiationAssistant
{	
	// TLC vars
	protected Infrastructure infrastructure;
	protected TrafficLight[][] tls;
	protected Node[] allnodes;
	protected int num_nodes;
	
	// Bucket vars
	protected float [][] bucket;
	protected static int BUCK = 0,
						 WAIT = 1;	
	
	// TC1 vars
	protected Vector count[][][], p_table[][][];
	protected float [][][][] q_table; //sign, pos, des, color (red=0, green=1)
	protected float [][][]   v_table;
	protected static float gamma=0.90f;				//Discount Factor; used to decrease the influence of previous V values, that's why: 0 < gamma < 1
	protected final static boolean red=false, green=true;
	protected final static int green_index=0, red_index=1;
	protected final static String shortXMLName="tlc-tc1o2";
	protected static float random_chance=0.01f;				//A random gain setting is chosen instead of the on the TLC dictates with this chance
	private Random random_number;
	
	/**
	 * The constructor for TL controllers
	 * @param The model being used.
	 */
 
	public TC1B1( Infrastructure infra ) throws InfraException
	{	super(infra);
	}
	
	public void setInfrastructure(Infrastructure infra) {
		super.setInfrastructure(infra);
		try{
			Node[] nodes = infra.getAllNodes(); 
			num_nodes = nodes.length;

			int numSigns = infra.getAllInboundLanes().size();
			q_table = new float [numSigns][][][];
			v_table = new float [numSigns][][];
			count	= new Vector[numSigns][][];
			p_table = new Vector[numSigns][][];
    
			bucket	= new float[numSigns][2];

			int num_specialnodes = infra.getNumSpecialNodes();
			for (int i=0; i<num_nodes; i++)	{
				Node n = nodes[i];
				Drivelane [] dls = n.getInboundLanes();
				for (int j=0; j<dls.length; j++) {
				    Drivelane d = dls[j];
				    Sign s = d.getSign();
				    int id = s.getId();
				    int num_pos_on_dl = d.getCompleteLength();
    				
				    q_table[id] = new float [num_pos_on_dl][][];
				    v_table[id] = new float [num_pos_on_dl][];
				    count[id] = new Vector[num_pos_on_dl][];
				    p_table[id] = new Vector[num_pos_on_dl][];
    				
				    for (int k=0; k<num_pos_on_dl; k++)	{
					    q_table[id][k]=new float[num_specialnodes][];
					    v_table[id][k]=new float[num_specialnodes];
					    count[id][k] = new Vector[num_specialnodes];
					    p_table[id][k] = new Vector[num_specialnodes];
    					
					    for (int l=0; l<num_specialnodes;l++)	{
						    q_table[id][k][l]	= new float [2];
						    q_table[id][k][l][0]= 0.0f;
						    q_table[id][k][l][1]= 0.0f;
						    v_table[id][k][l]	= 0.0f;
						    count[id][k][l] 	= new Vector();
						    p_table[id][k][l]	= new Vector();
					    }
				    }
				    bucket[id][BUCK]=0;
				    bucket[id][WAIT]=d.getNumRoadusersWaiting();
			    }
		    }		
		}
		catch(Exception e) {}
		System.out.println("TC1TLCOpt2 datastructure created");
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
				gain = bucket[tlId][BUCK];
				
				// For each waiting Roaduser
				for(int k=0; k<waitingsize; k++) {
					ru = (Roaduser) queue.next();
					pos = ru.getPosition();
					desId = ru.getDestNode().getId();
					passenger_factor = ru.getNumPassengers();
					
					// Add the pf*(Q([tl,pos,des],red)-Q([tl,pos,des],green))
					gain += passenger_factor * (q_table[tlId][pos][desId][red_index] - q_table[tlId][pos][desId][green_index]);  //red - green
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

	    		tld[i][j].setGain(gain);
	    		bucket[tlId][BUCK] = gain;
	    		bucket[tlId][WAIT] = lane.getNumRoadusersWaiting();
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
		if(prevsign == signnow && prevpos == posnow) {
			// Previous sign is the same as the current one
			// Previous position is the same as the previous one
			// So, by definition we had to wait this turn. bad.
			
			if(prevsign.getType()==Sign.TRAFFICLIGHT && prevsign.mayDrive() == true &&
			   desired != null && desired.getSign().getType()==Sign.TRAFFICLIGHT &&
			   desired.getNodeLeadsTo().getType()==Node.JUNCTION) {
				siphonGainToOtherBucket(prevlane, desired);
			}
		}
		else if(prevsign != signnow && signnow !=null && prevsign instanceof TrafficLight) {
			// Clearly passed a trafficlight.
			// Lets empty the previous bucket a bit.
			relaxBucket(prevlane.getId(), prevpos);
		}
	}
	
	/**
	 * Empties the 'gain-value' bucket partly, which is being filled when Roadusers are 
	 * waiting/voting for their TrafficLight to be set to green.
	 * 
	 * @param prevlane The Drivelane the Roaduser was on just before.
	 */
	protected void relaxBucket(int laneId, int prevpos) {
		float curWait = bucket[laneId][WAIT];
		float curBuck = bucket[laneId][BUCK];

		if(curWait>0) {
			bucket[laneId][WAIT]--;
			curWait--;
			bucket[laneId][BUCK] = curBuck*(curWait/(curWait+1));
		}
		else {
			// Apparently the Roaduser didnt wait before crossing, like he was on pos1 or 2 and had enough speed.
			bucket[laneId][WAIT] = 0;
			bucket[laneId][BUCK] = 0;
		}
	}
	
	
	/**
		* When a Roaduser may drive according to it's Sign, but cant as the desired Drivelane
		* is full, (some/all) of the current gain-bucket of it's current TrafficLight is
		* siphoned over to the desired Drivelane, making it more probable that that lane will
		* move, making it possible for this Drivelane to move.
		* 
		* @param here the Id of the drivelane the Roaduser is waiting at
		* @param there the Id of the drivelane where the Roaduser wants to go now
		*/
	protected void siphonGainToOtherBucket(Drivelane here, Drivelane there)
	{
		int hereId = here.getId();
		int thereId = there.getId();
		
		float curBuck = bucket[hereId][BUCK];
		float curWait = bucket[hereId][WAIT];
			
		if(curWait>0) {
			if(bucket[thereId][BUCK] < 100000)
				bucket[thereId][BUCK] += curBuck/curWait;	// Aka: make sure that lane gets mmmmooving!
		}
		else {
			int realWait = here.getNumRoadusersWaiting();
			if(realWait>0) {
				bucket[hereId][WAIT] = realWait;
				siphonGainToOtherBucket(here,there);
			}
			else {
				System.out.println("Something weird. at Drivelane "+here.getId());
				bucket[hereId][BUCK] = 0;
				bucket[hereId][WAIT] = 0;
			}
		}
	}
	
/*
 L = kleur licht
 tl = Sign
 p = pos
 d = des 

 TC1Q, eq1:
Q-values:		Q([tl,p,d],L)	= Sum(tl', p') [P([tl,p,d],L,[tl',p'])(R([tl,p],[tl',p'])+ yV([tl',p',d]))
								nec. P,R,V
pos.pos			R([tl,p],[tl',p])					
prb.r/g (tl,pl)	P([tl,p,d],L)			OK, JV'd 
avg wT:			V([tl,p,d]) = Sum(L) [P([tl,p,d],L)Q([tl,p,d],L)]
*/

	
	protected void recalcP(int tlId, int pos, int desId, boolean light, int tlNewId, int posNew)
	{
		/* 
		 * the P-table needs to count values: count([tl,pos,des],L,[tl',pos'])
		 * so we can calculate: P(L|(tl,p,d))
		 * So it holds the chance for a green light given:
		 *	a trafficlight
		 *  the position on that drivelane
		 *	the destination
		 *
		 * As 
		 */
		 
		// Check if this situation has ever appeared before
		CountEntry currentsituation = new CountEntry(tlId, pos, desId, light, tlNewId, posNew);
		int count_index = count[tlId][pos][desId].indexOf(currentsituation);
		if (count_index>=0) {
			// This situation has appeared before, let it know it happened again.
			CountEntry temp = (CountEntry) count[tlId][pos][desId].elementAt(count_index);
			temp.incrementValue();
			currentsituation = temp;
		}
		else {
			// This situation has not appeared before, add it to the table
			count[tlId][pos][desId].add(currentsituation);
		}
		
		// Calculate the new P(L|(tl,pos,des))
		// P(L|(tl,pos,des))	= P([tl,pos,des],L)/P([tl,pos,des])
		//						= #([tl,pos,des],L)/#([tl,pos,des])
		
		int sameStartSituation	= 0;								// Will hold a count of all the occurances that a RU was on the same startingposition
		int thisSituation		= currentsituation.getValue();		// Will hold a count of all the occurances that a RU was on the same startingposition, and the light had the same colour
		
		/* Calculate sameStart and sameStartAndLight */
		Enumeration enum = count[tlId][pos][desId].elements();
		while(enum.hasMoreElements()) {
			CountEntry enumelem = (CountEntry) enum.nextElement();
			sameStartSituation += enumelem.sameStartSituation(currentsituation);
		}

		// Put this chance P(L|(tl,pos,des,L,tl',pos') in the table
		PEntry currentchance = new PEntry(tlId, pos, desId, light, tlNewId, posNew);
		currentchance.setSameSituation(thisSituation);
		currentchance.setSameStartSituation(sameStartSituation);
		
		int p_index = p_table[tlId][pos][desId].indexOf(currentchance);
		if(p_index>=0) p_table[tlId][pos][desId].setElementAt(currentchance, p_index);
		else {
			p_table[tlId][pos][desId].add(currentchance);
			p_index = p_table[tlId][pos][desId].indexOf(currentchance);
		}

		int size = p_table[tlId][pos][desId].size()-1;
		for(; size>=0; size--) {
			PEntry P = (PEntry) p_table[tlId][pos][desId].elementAt(size);
			if(P.sameStartSituation(currentsituation) && size!=p_index) {
				P.addSameStartSituation();
			}
		}
	}

	protected void recalcQ(int tlId, int pos, int desId, boolean light, int tlNewId, int posNew, PosMov[] posMovs)
	{
		/*	Recalculate the Q values, only one PEntry has changed, meaning also only 1 QEntry has to change	
		 *
		 * Q([tl,p,d],L)	= Sum(tl', p') [P([tl,p,d],L,[tl',p'])(R([tl,p],[tl',p'])+ yV([tl',p',d]))
		 */

		/*
		 * For All tl' and p'
		 *   P*(R+(y*V))
		 */

		// First gather All tl' and p' in one array
		int num_posmovs	= posMovs.length;
		
		PosMov curPosMov;
		int curPMTlId, curPMPos, R;
		float V=0,newQvalue=0;
		
		for(int ti=0; ti<num_posmovs; ti++) {		// For All tl', pos'
			curPosMov = posMovs[ti];
			curPMTlId = curPosMov.tlId;
			curPMPos  = curPosMov.pos;
			
			// See if this situation ever occured
			PEntry P = new PEntry(tlId, pos, desId, light, curPMTlId, curPMPos);
			int p_index = p_table[tlId][pos][desId].indexOf(P);
			
			if(p_index>=0) {	// There exists a situation in the P-table like the current one
				P = (PEntry) p_table[tlId][pos][desId].elementAt(p_index);
				R = rewardFunction(tlId, pos, curPMTlId, curPMPos);			// Calculate the Rewardvalue, see if this option is in the possiblelanes&ranges
				V = v_table[curPMTlId][curPMPos][desId];
				
				newQvalue += P.getChance() *(R + (gamma * V));
			}
			// Else P(..)=0, thus will not add anything in the summation
		}
		q_table[tlId][pos][desId][light?green_index:red_index]=newQvalue;
	}


	protected void recalcV(int tlId, int pos, int desId)
	{
		/*
		 *  V([tl,p,d]) = Sum (L) [P(L|(tl,p,d))Q([tl,p,d],L)]
		 */
		
		// VCalculations vars		
		float redVValue, greenVValue, newVValue;
		// Chances for Green and Red, and fetchArray
		float pGreen, pRed, pGR[];
		// Q Values for Red and Green
		float qRed = q_table[tlId][pos][desId][red_index];
		float qGreen = q_table[tlId][pos][desId][green_index];
		
		// Get the Chances
		
		//pGR = calcPGR(tlId,pos,desId);
		pGR 	= oldCalcPGR(tlId,pos,desId);
		pGreen	= pGR[green_index];
		pRed	= pGR[red_index];

		// Calculate the new V value
		//greenVValue = pGreen*qGreen;
		greenVValue = (pGreen/(pGreen+pRed))*qGreen;
		//redVValue	= pRed*qRed;
		redVValue	= (pRed/(pGreen+pRed))*qRed;
		newVValue = greenVValue + redVValue;
		//System.out.println("pG:"+pGreen+" qG:"+qGreen+" pR:"+pRed+" qR:"+qRed+" nV:"+newVValue);
		v_table[tlId][pos][desId]=newVValue;
	}

	/*
				==========================================================================
							Additional methods, used by the recalc methods 
				==========================================================================
	*/


	protected float[] oldCalcPGR(int tlId, int pos, int desId)
	{
		// Old implementation
		float[] counters = new float[2];
		int red_counter = 0;
		int green_counter = 0;
		
		//Calcs the number of entries in the table matching the given characteristics, and returns the count
		int psize = p_table[tlId][pos][desId].size()-1;
		for(; psize>=0; psize--) {
			PEntry candidate = (PEntry) p_table[tlId][pos][desId].elementAt(psize);
			if(candidate.light==green)
				green_counter++;
			else
				red_counter++;
		}
		counters[green_index] = green_counter;
		counters[red_index] = red_counter;
		return counters;
	}

	
	protected int rewardFunction(int tlId, int pos, int tlNewId, int posNew) {
		if(tlId!=tlNewId || pos != posNew)
			return 0;
		return 1;
	}
	
	public float getVValue(Sign sign, Node des, int pos) {
		return v_table[sign.getId()][pos][des.getId()];
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
		int tlId, pos, desId, tlNewId, posNew, value;
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
		public int getValue() {
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
		
		// Retuns the count-value if the startingsituations match
		public int sameStartSituation(CountEntry other) {
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
			value=myElement.getAttribute("value").getIntValue(); 
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
		float sameStartSituation,sameSituation;
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
		public void setSameStartSituation(int s) {	sameStartSituation = s;	}
		
		public void setSameSituation(int s) {	sameSituation = s;	}
		
		public float getSameStartSituation() {	return sameStartSituation;	}
		public float getSameSituation() {	return sameSituation;	}
		
		public float getChance() {	return getSameSituation()/getSameStartSituation();	}
		
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
			sameStartSituation=myElement.getAttribute("same-startsituation").getIntValue();
			sameSituation=myElement.getAttribute("same-situation").getIntValue();
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
	
	protected class Target implements XMLSerializable
	{
		int tlId, pos;
		String parentName="model.tlc";
		
		Target(int _tlId, int _pos) {
			tlId = _tlId;
			pos = _pos;
		}
		
		Target ()
		{ // Empty constructor for loading
		}
		
		public int getTLId() {
			return tlId;
		}
		
		public int getPos() {
			return pos;
		}

		public boolean equals(Object other) {
			if(other != null && other instanceof Target) {
				Target qnew = (Target) other;
				if(qnew.tlId!=tlId) return false;
				if(qnew.pos!=pos) return false;
				return true;
			}
			return false;
		}
		
		// XMLSerializable implementation of Target
		
		public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
		{	pos=myElement.getAttribute("pos").getIntValue();
			tlId=myElement.getAttribute("tl-id").getIntValue();
		}
		
		public XMLElement saveSelf () throws XMLCannotSaveException
		{ 	XMLElement result=new XMLElement("target");
			result.addAttribute(new XMLAttribute("pos",pos));
			result.addAttribute(new XMLAttribute("tl-id",tlId));
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
		bucket=(float[][])XMLArray.loadArray(this,loader);
		q_table=(float[][][][])XMLArray.loadArray(this,loader);
		v_table=(float[][][])XMLArray.loadArray(this,loader);
		count=(Vector[][][])XMLArray.loadArray(this,loader,this);
		p_table=(Vector[][][])XMLArray.loadArray(this,loader,this);
	}
  
	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
	{	super.saveChilds(saver);
		XMLArray.saveArray(bucket,this,saver,"bucket");
		XMLArray.saveArray(q_table,this,saver,"q-table");
		XMLArray.saveArray(v_table,this,saver,"v-table");
		XMLArray.saveArray(count,this,saver,"counts");
		XMLArray.saveArray(p_table,this,saver,"p-table");
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
