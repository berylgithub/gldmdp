
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
 *
 * Changed to use Sarsa(lambda) instead of DP - S. Louring
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
 * @author Arne K, Jilles V and Søren Louring
 * @version 1.2
 */
public class SL6TLC extends TLController implements InstantiationAssistant
{   protected Infrastructure infrastructure;
    protected TrafficLight[][] tls;
    protected Node[] allnodes;
    protected int num_nodes;
    
    protected Vector count; //, p_table;
    protected float [][][][] q_table; //sign, pos, des, color (red=0, green=1)
    protected float [][][][] e_table;
    protected static float gamma=0.95f;             //Discount Factor; used to decrease the influence of previous V values, that's why: 0 < gamma < 1
    protected static float random_chance=0.01f;             //A random gain setting is chosen instead of the on the TLC dictates with this chance
    protected static float alpha  = 0.7f;
    protected static float lambda  = 0.5f;
    protected final static boolean red=false, green=true;
    protected final static int green_index=0, red_index=1;
    protected final static String shortXMLName="tlc-sl6";
    private Random random_number;

    protected float time_step = 0;
    protected Node[] MY_nodes;
    protected int MY_num_specialnodes;
    protected int MY_num_nodes;

    /**
     * The constructor for TL controllers
     * @param The model being used.
     */
 
    public SL6TLC( Infrastructure infra ) throws InfraException
    {   super(infra);
        
        Node[] nodes = infra.getAllNodes(); //Moet Edge zijn eigenlijk, alleen testSimModel knalt er dan op
        MY_nodes = nodes;

        int num_nodes = nodes.length;
        
        count = new Vector();
        
        int numSigns = infra.getAllInboundLanes().size();

        q_table = new float [numSigns+1][][][];
        e_table = new float [numSigns+1][][][]; //For the eligibility traces
        int num_specialnodes = infra.getNumSpecialNodes();
        MY_num_specialnodes = num_specialnodes;
        
        for (int i=0; i<nodes.length; i++)
        {
            Node n = nodes[i];
            Drivelane [] dls = n.getInboundLanes();
            for (int j=0; j<dls.length; j++)
            {
                Drivelane d = dls[j];
                Sign s = d.getSign();
                int id = s.getId();
                int num_pos_on_dl = d.getCompleteLength();
                q_table[id] = new float [num_pos_on_dl][][];
                e_table[id] = new float [num_pos_on_dl][][];
                for (int k=0; k<num_pos_on_dl; k++)
                {
                    q_table[id][k]=new float[num_specialnodes][];
                    e_table[id][k]=new float[num_specialnodes][];
                    for (int l=0; l<q_table[id][k].length;l++)
                    {
                        q_table[id][k][l]=new float [2];
                        q_table[id][k][l][0]=0.0f;
                        q_table[id][k][l][1]=0.0f;

                        e_table[id][k][l]=new float [2];
                        e_table[id][k][l][0]=0.0f;
                        e_table[id][k][l][1]=0.0f;

                    }
                }
            }
        }   
        System.out.println("tl = " + numSigns);
        System.out.println("des = " + num_specialnodes);
        System.out.println("Alpha = " + alpha);
        random_number = new Random();
    }


	public void reset() {
		for(int j=0; j<q_table.length; j++) {
			if (q_table[j] != null) {
				for (int k=0; k<q_table[j].length; k++) { 
					if (q_table[j][k] != null)  {
						for (int l=0; l<q_table[j][k].length;l++) {
							if (q_table[j][k][l] != null) {
								q_table[j][k][l][0] = 0.0f;
								q_table[j][k][l][1] = 0.0f;
								e_table[j][k][l][0] = 0.0f;
								e_table[j][k][l][1] = 0.0f;
	
							}
						}
					}
				}
			}
		}
	System.out.println("E og Q table nulstillet.");
	} //Reset slut


    
    /**
    * Calculates how every traffic light should be switched
    * Per node, per sign the waiting roadusers are passed and per each roaduser the gain is calculated.
    * @param The TLDecision is a tuple consisting of a traffic light and a reward (Q) value, for it to be green
    * @see gld.algo.tlc.TLDecision
    */  
    public TLDecision[][] decideTLs()
    {
        int num_dec;
        int num_tld = tld.length;
        
        //Determine wheter it should be random or not
        boolean do_this_random = false;
        if (random_number.nextFloat() < random_chance) do_this_random = true;
        
        for (int i=0;i<num_tld;i++)  {
            num_dec = tld[i].length;
            for(int j=0;j<num_dec;j++)  {
                Sign currenttl = tld[i][j].getTL();
                float gain=0;
                
                Drivelane currentlane = currenttl.getLane();
                int waitingsize = currentlane.getNumRoadusersWaiting();
                ListIterator queue = currentlane.getQueue().listIterator();
                
                if(!do_this_random)  {
                    for(; waitingsize>0; waitingsize--)
                    {
                        Roaduser ru = (Roaduser) queue.next();
                        int pos = ru.getPosition();
                        Node destination = ru.getDestNode();
                        gain += q_table[currenttl.getId()][pos][destination.getId()][1] - q_table[currenttl.getId()][pos][destination.getId()][0];  //red - green
                    }
                    float q = gain;
                            }               
                            else gain = random_number.nextFloat();

                tld[i][j].setGain(gain);
            }
        }
        return tld;
    }

	public void updateRoaduserMove(
	            Roaduser ru, Drivelane prevlane, Sign prevsign, int prevpos, Drivelane dlanenow, 
	            Sign signnow, int posnow, PosMov[] posMovs, Drivelane desired)
    {
        //When a roaduser leaves the city; this will 
        if(dlanenow == null || signnow == null)
        {
            dlanenow = prevlane;
            signnow = prevsign;
            posnow = -1;
            return; // ?? is recalculation is not necessary ??
        }
        //This ordening is important for the execution of the algorithm!
        
        if(prevsign.getType()==Sign.TRAFFICLIGHT && (signnow.getType()==Sign.TRAFFICLIGHT || signnow.getType()==Sign.NO_SIGN)) {
            Node dest = ru.getDestNode();
            recalcQ(prevsign, prevpos, dest, prevsign.mayDrive(), signnow, posnow, signnow.mayDrive(), posMovs);
        }
    }


    
    protected void recalcQ(Sign tl, int pos, Node destination, boolean light, Sign tl_new, int pos_new, boolean light_new, PosMov[] posMovs)    {
        /*  Recalculate the Q values, only one PEntry has changed, meaning also only 1 QEntry has to change    */

        int R;
        int j;

        float oldQvalue = 0;
        float Qmark = 0;
        float newQvalue = 0;

        float delta = 0;

        float alphadelta;
        float gammalambda;

        R = rewardFunction(tl_new, pos_new, posMovs);
            
        try
        {
            oldQvalue = q_table[tl.getId()][pos][destination.getId()][light?green_index:red_index];
            Qmark = q_table[tl_new.getId()][pos_new][destination.getId()][light_new?green_index:red_index];// Q( [ tl' , p' ] , L')

            delta = R + gamma * Qmark - oldQvalue;
            e_table[tl.getId()][pos][destination.getId()][light?green_index:red_index] = 1;

        }
        catch (Exception e)
        {
            System.out.println("ERROR");
            System.out.println("tl: "+tl.getId());
            System.out.println("pos:"+pos);
            System.out.println("des:"+destination.getId());
        }


        /////////////////////////////////////////////////
        // Run through both tables and update their values


        alphadelta = alpha * delta;
        gammalambda = gamma * lambda;

        j = tl.getId();
        
        if (q_table[j] != null) {
            for (int k=0; k<q_table[j].length; k++) { 
                if (q_table[j][k] != null ) {
                    for (int l=0; l<q_table[j][k].length;l++) {
                        if (q_table[j][k][l] != null) {
                            q_table[j][k][l][0] += alphadelta * e_table[j][k][l][0];
                            q_table[j][k][l][1] += alphadelta * e_table[j][k][l][1];
                            e_table[j][k][l][0] *= gammalambda;
                            e_table[j][k][l][1] *= gammalambda;
                        }
                    }
                }
            }
        }
    }



    /*
                ==========================================================================
                            Additional methods, used by the recalc methods 
                ==========================================================================
    */


    
    protected int rewardFunction(Sign tl_new, int pos_new, PosMov[] posMovs)
    {
        //Ok, the reward function is actually very simple; it searches for the tuple (tl_new, pos_new) in the given set
        int size = posMovs.length;
        for(int i=0; i<size; i++) {
            if(posMovs[i].tlId == tl_new.getId() && posMovs[i].pos == pos_new)
                return 0;
        }
       
        /*int size = possiblelanes.length;
        for(int i=0; i<size; i++)   {
            if( possiblelanes[i].equals(tl_new.getLane()) ) {
                if(ranges[i].x < pos_new)   {
                    if(ranges[i].y > pos_new)   {
                        return 0;
                    }
                }
            }
        }*/
        return 1;
    }
    
    protected Target[] ownedTargets(Sign tl, int pos, Node des, boolean light)
    {
        //This method will determine to which destinations you can go starting at this source represented in this QEntry
        
        CountEntry dummy = new CountEntry(tl, pos, des, light, tl, pos);
        Target[] ownedtargets;
        Vector candidate_targets;
        candidate_targets = new Vector();
        
        //Use the count table to sort this out, we need all Targets from 
        //Only the elements in the count table are used, other  just give a P
        
        Enumeration enum = count.elements();
        while(enum.hasMoreElements()) {
            CountEntry current_entry = (CountEntry) enum.nextElement();
            if(current_entry.sameSource(dummy) != 0) {              
                candidate_targets.addElement(new Target(current_entry.tl_new , current_entry.pos_new));
            }
        }
        ownedtargets = new Target[candidate_targets.size()];
        candidate_targets.copyInto(ownedtargets);
        return ownedtargets;
    }



    /*
                ==========================================================================
                    Internal Classes to provide a way to put entries into the tables 
                ==========================================================================
    */

    public class CountEntry implements XMLSerializable , TwoStageLoader
    {
        Sign tl;
        int pos;
        Node destination;
        boolean light;
        Sign tl_new;
        int pos_new;
        int value;
        TwoStageLoaderData loadData=new TwoStageLoaderData();
        String parentName="model.tlc";
        
        CountEntry(Sign _tl, int _pos, Node _destination, boolean _light, Sign _tl_new, int _pos_new) {
            tl = _tl;
            pos = _pos;
            destination = _destination;
            light = _light;
            tl_new = _tl_new;
            pos_new = _pos_new;
            value=1;
        }
        
        CountEntry ()
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
            {   CountEntry countnew = (CountEntry) other;
                if(!countnew.tl.equals(tl)) return false;
                if(countnew.pos!=pos) return false;
                if(!countnew.destination.equals(destination)) return false;
                if(countnew.light!=light) return false;
                if(!countnew.tl_new.equals(tl_new)) return false;
                if(countnew.pos_new!=pos_new) return false;
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
        
        // XMLSerializable implementation of CountEntry
        
        public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
        {   pos=myElement.getAttribute("pos").getIntValue();
            loadData.oldTlId=myElement.getAttribute("tl-id").getIntValue();
            loadData.destNodeId=myElement.getAttribute("destination").getIntValue();
            light=myElement.getAttribute("light").getBoolValue();
            loadData.newTlId=myElement.getAttribute("newtl-id").getIntValue();
            pos_new=myElement.getAttribute("new-pos").getIntValue();
            value=myElement.getAttribute("value").getIntValue(); 
        }

        public XMLElement saveSelf () throws XMLCannotSaveException
        {   XMLElement result=new XMLElement("count");
            result.addAttribute(new XMLAttribute("tl-id",tl.getId()));
            result.addAttribute(new XMLAttribute("pos",pos));
            result.addAttribute(new XMLAttribute("destination",destination.getId()));
            result.addAttribute(new XMLAttribute("light",light));
            result.addAttribute(new XMLAttribute("newtl-id",tl_new.getId()));
            result.addAttribute(new XMLAttribute("new-pos",pos_new));
            result.addAttribute(new XMLAttribute("value",value));
            return result;
        }
  
        public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
        {   // A count entry has no child objects
        }

        public String getXMLName ()
        {   return parentName+".count";
        }
        
        public void setParentName (String parentName)
        {   this.parentName=parentName; 
        }
                
        // TwoStageLoader implementation of CountEntry

        class TwoStageLoaderData 
        { int oldTlId,newTlId,destNodeId;
        }
        
        public void loadSecondStage (Dictionary dictionaries)
        { Dictionary laneDictionary=(Dictionary)(dictionaries.get("lane")),
                     nodeDictionary=(Dictionary)(dictionaries.get("node"));
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
        
        PEntry ()
        {   // Empty constructor for loading
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
        {   pos=myElement.getAttribute("pos").getIntValue();
            loadData.oldTlId=myElement.getAttribute("tl-id").getIntValue();
            loadData.destNodeId=myElement.getAttribute("destination").getIntValue();
            light=myElement.getAttribute("light").getBoolValue();
            loadData.newTlId=myElement.getAttribute("newtl-id").getIntValue();
            pos_new=myElement.getAttribute("new-pos").getIntValue();
            value=myElement.getAttribute("value").getFloatValue(); 
        }

        public XMLElement saveSelf () throws XMLCannotSaveException
        {   XMLElement result=new XMLElement("pval");
            result.addAttribute(new XMLAttribute("tl-id",tl.getId()));
            result.addAttribute(new XMLAttribute("pos",pos));
            result.addAttribute(new XMLAttribute("destination",destination.getId()));
            result.addAttribute(new XMLAttribute("light",light));
            result.addAttribute(new XMLAttribute("newtl-id",tl_new.getId()));
            result.addAttribute(new XMLAttribute("new-pos",pos_new));
            result.addAttribute(new XMLAttribute("value",value));
            return result;
        }
        
        public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
        {   // A PEntry has no child objects
        }
        
        public void setParentName (String parentName)
        {   this.parentName=parentName; 
        }
  
        public String getXMLName ()
        {   return parentName+".pval";
        }
                
        // TwoStageLoader implementation of PEntry

        class TwoStageLoaderData 
        {   int oldTlId,newTlId,destNodeId;
        }
        
        public void loadSecondStage (Dictionary dictionaries)
        {   Dictionary laneDictionary=(Dictionary)(dictionaries.get("lane")),
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
        
        Target ()
        { // Empty constructor for loading
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
        {   pos=myElement.getAttribute("pos").getIntValue();
           loadData.tlId=myElement.getAttribute("tl-id").getIntValue();
        }
        
        public XMLElement saveSelf () throws XMLCannotSaveException
        {   XMLElement result=new XMLElement("target");
            result.addAttribute(new XMLAttribute("tl-id",tl.getId()));
            result.addAttribute(new XMLAttribute("pos",pos));
            return result;
        }
  
        public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
        {   // A Target has no child objects
        }

        public String getXMLName ()
        {   return parentName+".target";
        }
        
        public void setParentName (String parentName)
        {   this.parentName=parentName;
        }
                
        // TwoStageLoader implementation of Target

        class TwoStageLoaderData 
        {   int tlId;
        }
        
        public void loadSecondStage (Dictionary dictionaries) throws XMLInvalidInputException,XMLTreeException
        {   Dictionary laneDictionary=(Dictionary)(dictionaries.get("lane"));
            tl=((Drivelane)(laneDictionary.get(
                         new Integer(loadData.tlId)))).getSign();
        }
        
    }       
    
    
    public void showSettings(Controller c)
    {
        String[] descs = {"Gamma (discount factor)", "Random decision chance", "Alpha", "Lambda"};
        float[] floats = {gamma, random_chance, alpha, lambda};
        TLCSettings settings = new TLCSettings(descs, null, floats);
                
        settings = doSettingsDialog(c, settings);
//OBS       // Her burde laves et tjek på om 0 < gamma < 1 og det samme med random chance
        gamma = settings.floats[0];
        random_chance = settings.floats[1];
        alpha = settings.floats[2];
        lambda = settings.floats[3];
    }
    
    // XMLSerializable, SecondStageLoader and InstantiationAssistant implementation
    
    public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
    {   super.load(myElement,loader);
        gamma=myElement.getAttribute("gamma").getFloatValue();
        random_chance=myElement.getAttribute("random-chance").getFloatValue();
        q_table=(float[][][][])XMLArray.loadArray(this,loader);
        //v_table=(float[][][])XMLArray.loadArray(this,loader);
        count=(Vector)XMLArray.loadArray(this,loader,this);
        //p_table=(Vector)XMLArray.loadArray(this,loader,this);
    }

    public XMLElement saveSelf () throws XMLCannotSaveException
    {   XMLElement result=super.saveSelf();
        result.setName(shortXMLName);
        result.addAttribute(new XMLAttribute ("random-chance",random_chance));
        result.addAttribute(new XMLAttribute ("gamma",gamma));
        return result;
    }
  
    public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
    {   super.saveChilds(saver);
        XMLArray.saveArray(q_table,this,saver,"q-table");
        //XMLArray.saveArray(v_table,this,saver,"v-table");
        XMLArray.saveArray(count,this,saver,"counts");
        //XMLArray.saveArray(p_table,this,saver,"p-table");
    }

    public String getXMLName ()
    {   return "model."+shortXMLName;
    }
        
    public void loadSecondStage (Dictionary dictionaries) throws XMLInvalidInputException,XMLTreeException
    {   XMLUtils.loadSecondStage(count.elements(),dictionaries);
        //XMLUtils.loadSecondStage(p_table.elements(),dictionaries);
        System.out.println("SL6 second stage load finished.");          
    }
    
    public boolean canCreateInstance (Class request)
    {   return CountEntry.class.equals(request) ||
                PEntry.class.equals(request);
    }
    
    public Object createInstance (Class request) throws 
          ClassNotFoundException,InstantiationException,IllegalAccessException
    {   if (CountEntry.class.equals(request))
        { return new CountEntry();
        }
        else if ( PEntry.class.equals(request))
        { return new PEntry();
        }
        else
        { throw new ClassNotFoundException
          ("SL6 IntstantiationAssistant cannot make instances of "+
           request);
        }
    }   
    
}
