
/*-----------------------------------------------------------------------
 * Copyright (C) 2001 Green Light District Team, Utrecht University 
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
import gld.algo.dp.*;

import java.io.IOException;
import java.util.*;
import java.awt.Point;

/**
 *
 * This controller will decide it's Q values for the traffic lights according to the traffic situation on
/* the lane connected to the TrafficLight. It will learn how to alter it's outcome by reinforcement learning.
 *
 * @author Arne K & Chaim Z
 * @version 1.0
 */
public class LocalHillTLC extends TLController implements Colearning
{	
	protected Infrastructure infrastructure;
	protected Node[] allnodes;
	protected int num_nodes;
	protected int numSigns;
	protected final static String shortXMLName="tlc-localhill";
	
	protected Random seed;
	protected Vector tld_backup;
	protected int [] v_table;	//signID
	protected final int iteration_length = 1;

	/**
	 * The constructor for TL controllers
	 * @param The model being used.
	 */
 
	public LocalHillTLC( Infrastructure infra) throws InfraException
	{	super(infra);
	}
	
	public void setInfrastructure(Infrastructure infra)
	{	super.setInfrastructure(infra);
		allnodes = infra.getAllNodes(); //Moet Edge zijn eigenlijk, alleen testSimModel knalt er dan op
		num_nodes = allnodes.length;
		try{numSigns = infra.getAllInboundLanes().size();}catch(Exception e){}

		tld_backup = new Vector();

		// Create the array
		v_table = new int [numSigns];
		for(int i=0; i<numSigns; i++) v_table[i]=-1;

		// Create random setting for the D array
		seed=new Random();
		for (int i=0; i < tld.length; i++)
			for (int j=0; j < tld[i].length; j++)
				tld[i][j].setGain(seed.nextFloat());
	}

	
	/**
	* Calculates how every traffic light should be switched
	* Per node, per sign the waiting roadusers are passed and per each roaduser the gain is calculated.
	* @param The TLDecision is a tuple consisting of a traffic light and a reward (Q) value, for it to be green
	* @see gld.algo.tlc.TLDecision
	*/	
	public TLDecision[][] decideTLs() {
		// tld[Node][Sign] the gain per node
		// Perform the local hill climbing algorithm
		int max_points = 0;
		Node max_node;
		Sign[] max_config;
		int steps = iteration_length;
		for(; steps>0; steps--) {
			int num_nodes = allnodes.length;
			for (int i=0; i<num_nodes; i++) {
				if(allnodes[i] instanceof Junction) {
					Sign [][] configs = ((Junction) allnodes[i]).getSignConfigs();
					max_points = 0;
					max_node = allnodes[i];
					max_config = configs[0];

					for(int j=0; j<configs.length; j++) {
						changeTLD(allnodes[i], configs[j]);
						int cPoints = 0;
						try{ cPoints=computePoints(tld);}
						catch(InfraException ie){System.out.println(""+ie);ie.printStackTrace();}
						//System.out.println("Config: "+j+ " points: "+cPoints);
						if (cPoints> max_points) 
						{
							max_node   = allnodes[i];
							max_config = configs[j];
							max_points = cPoints;
						}
						//restoreTLD(allnodes[i]);
					}
					changeTLD(max_node, max_config);
				}
			}
		}
	    return tld;
	}

	protected void reset_v_table() {
		for(int i=0; i<numSigns; i++) v_table[i] = -1;
	}
	
	protected int computePoints( TLDecision[][] tld) throws InfraException
	{
		int Points = 0;
		reset_v_table();
		
		for (int i=0; i<allnodes.length; i++)
		{
			Node currentNode = allnodes[i];
			if (! (currentNode instanceof Junction)) continue;
			Drivelane [] dls=null;
			try{dls= currentNode.getInboundLanes();}catch(Exception e){}
			
			boolean changed;
			do
			{
				changed = false;
				int cnId = currentNode.getId();
				for (int j=0; j<dls.length; j++)
				{
					// If a car is standing at the first place at node,dir
					Sign s = dls[j].getSign();
					if (dls[j].getNumRoadusersWaiting()>0)
					{
						// if the light at node,dir is RED...
						boolean isRed = true;
						for (int k=0; k<tld[currentNode.getId()].length; k++)
							if (tld[currentNode.getId()][k].getTL()==s)	{
								if (tld[currentNode.getId()][k].getGain()==1) isRed=false;
							}
						if (isRed) {
							if (v_table[s.getId()]!=0) {
								v_table[s.getId()]=0;
								changed = true;
							}
						}
						else {
							DrivingPolicy dp = SimModel.getDrivingPolicy();
							Roaduser firstRU = dls[j].getFirstRoaduser();
							Drivelane destLane = dp.getDirection(firstRU, dls[j], currentNode);
							int value = v_table[s.getId()];
							
							if (destLane==null) value = 1; // when it is an end-node
							else if (!destLane.isFull()) value = 1;
							else {
								// When destLane is full
								value = v_table[destLane.getSign().getId()];
							}

							if (v_table[s.getId()]!=value) {
								v_table[s.getId()]=value;
								changed = true;
							}
						}
					}
					else {
						if (v_table[s.getId()]!=0) {
							v_table[s.getId()]=0;
							changed = true;
						}
			  		}
				} 
			}
			while(changed);
		}
		
		for(int i=0; i<numSigns; i++) 
		{
			if(v_table[i] == -1 ) 
			{
				// Find the associated sign
				Sign s = null;
				for (int j=0; j<allnodes.length; j++)
				{
					Drivelane [] dls = allnodes[j].getInboundLanes();
					for (int k=0; k<dls.length; k++)
					{
						if (dls[k].getSign().getId()==i) s=dls[k].getSign();
					}					
				}
				
				Node n1=s.getNode();
				
				if (!(n1 instanceof SpecialNode)) Points++;
			}
				else Points += v_table[i];
		}
		return Points;
	}

	public void updateRoaduserMove(Roaduser ru, Drivelane currentlane, Sign currentsign, int prevpos, Drivelane nextlane, Sign nextsign, int posnow, PosMov[] posMovs, Drivelane desired)
	{
	/* If this road user is on the first position in the road, this one is used for calcing. We want to know his new direction (node, tl)
		if(prevpos == 0 && currentlane != null && nextlane != null) {
			updateNextTable(currentlane.getNodeLeadsTo(), currentsign, nextlane.getNodeLeadsTo(), nextsign);
		}*/
	}

	protected boolean isRed( TLDecision[] tld_node,  int sign_id ) {
		int num_tld_node = tld_node.length;
		Sign sign;
		for(int i=0; i<num_tld_node; i++) {
			sign = tld_node[i].getTL();
			if(sign.getId() == sign_id) return !sign.mayDrive();
		}
		return false;
	}

	protected void changeTLD (Node thisnode, Sign [] config) {
		// Save things we are gonna change, Vector TLDBackup
		
		try {
			Drivelane[] lanes = thisnode.getInboundLanes();
			// clear??
			tld_backup.clear();
			// Error might be here, as you're changing the object, and thus wont have a backup..
			// Not sure how to create a well-retrievable backup.
			/*for(int i=0;i<tld[thisnode.getId()].length;i++) {
				tld_backup.removeElement(tld[thisnode.getId()][i]);
			}*/
			int num_lanes = tld[thisnode.getId()].length;
			for (int j=0; j<num_lanes; j++) {
				tld_backup.addElement(tld[thisnode.getId()][j]);
				if (Arrayutils.findElement(config, tld[thisnode.getId()][j].getTL()) != -1)
					tld[thisnode.getId()][j].setGain(1);
				else
					tld[thisnode.getId()][j].setGain(0);
			}
		}
		catch (Exception e) 
		{
			System.out.println(e+"");
			e.printStackTrace();
		}
		
	}

	protected void restoreTLD (Node thisnode) {
		// Save things we are gonna change, Vector TLDBackup
		int i = thisnode.getId();
		try {
			Drivelane[] lanes = thisnode.getInboundLanes();
			for (int j=0; j<lanes.length; j++) {
				tld[i][j] = (TLDecision) tld_backup.elementAt(j);
			}
		}
		catch (Exception e) {}
	}


// ================================================

	



		





// ================================================

	public float getColearnValue(Sign now, Sign sign, Node des, int pos) {
		return 0;
	}

	
	// XMLSerializable and SecondStageLoader implementation
	
	public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException 
	{	super.load(myElement,loader);
		v_table=(int[])XMLArray.loadArray(this,loader);
		tld_backup=(Vector)(XMLArray.loadArray(this,loader));
	}
		      
	public XMLElement saveSelf () throws XMLCannotSaveException { 	
		XMLElement result=super.saveSelf();		
		result.setName(shortXMLName);
	  	return result;
	}
  
	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException 
	{	super.saveChilds(saver);
		XMLArray.saveArray(v_table,this,saver,"v-table");
		XMLArray.saveArray(tld_backup,this,saver,"tld-backup");
	}

	public String getXMLName () {
		return "model."+shortXMLName;
	}
		
	public void loadSecondStage (Dictionary dictionaries) throws XMLInvalidInputException,XMLTreeException {
		System.out.println("Local Hillclimbing second stage load finished.");			
	}
	
	public void showSettings(Controller c) {
	}


}
