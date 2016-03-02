
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

import MDPPreprocessor.TransitionProbContainer;
import gld.*;
import gld.sim.*;
import gld.algo.tlc.*;
import gld.infra.*;
import gld.utils.*;
import gld.xml.*;
import java.io.IOException;
import java.util.*;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This controller will switch TrafficLights so that a SignConfig is selected so
 * that the most Roadusers can cross the Node.
 *
 * @author Group Algorithms
 * @version 1.0
 */
public class MDP_2segment_5step_multinodes extends TLController {

    public static final String shortXMLName = "MDP_2segment_5step_multinodes";
    
    ArrayList<TuppleStateActionConainer> arrTSAC=new ArrayList<>();
    ArrayList<TuppleStateActionConainer>[] arrMultiTSAC;
    
    ArrayList<StateBestActionContainer> arrSBAC=new ArrayList<>();
//    ArrayList<StateSeqContainer> arrSSC=new ArrayList<>();
//    ArrayList<ActionSeqContainer> arrASC=new ArrayList<>();
            /**
             * The constructor for TL controllers
             *
             * @param The model being used.
             */

    public MDP_2segment_5step_multinodes(Infrastructure infras) throws IOException {
        super(infras);
        for (int i = 0; i < tld.length; i++) {
            for (int j = 0; j < tld[i].length; j++) {
                System.out.println(j + " " + tld[i][j].getTL().getLane().getLength());
            }
        }
        arrSBAC=loadActionFile("Value Iteration Result_5-step_random_2.txt");
        //test load array of result
        for(int i=0; i<arrSBAC.size(); i++){
            System.out.println(arrSBAC.get(i).getState()+" "+arrSBAC.get(i).getAction());
        }
        
    }

   

    

    
    /**
     * This implementation sets the Q-values according to the length of the
     * waiting queue. The longer the queue, the higher the Q-value.
     */
    public TLDecision[][] decideTLs() {
        int num_lanes, num_nodes = tld.length;
        for (int i = 0; i < num_nodes; i++) {
            num_lanes = tld[i].length;
            
            
            String tempState="";
            for (int j = 0; j < num_lanes; j++) {
                if(tld[i][j].getTL().getNode().getNumSigns()!=0){
                    tempState=tempState+StateSetter(tld[i][j]);
                    
                }
            }
//            for(int j=0; j<num_lanes; j++){
//                if(tld[i][j].getTL().getNode().getNumSigns()!=0){
//                    tld[i][manualPolicyApplier(tempState)].setGain(2);
////                    tld[i][0].setGain(2);
//                }
//            }
//            System.out.println(manualPolicyApplier(tempState)+" "+tempState);
            
            
            
            
            
            
            //POLICY APPLIER
            for(int k=0; k<arrSBAC.size(); k++){
                int laneNumber=0;
                if(tempState.equals(arrSBAC.get(k).getState())){
                    laneNumber=Integer.parseInt(arrSBAC.get(k).getAction());
                    for(int j=0; j<num_lanes; j++){
                        tld[i][j].setGain(0);
                    }
                    tld[i][laneNumber].setGain(15);
                    System.out.println(arrSBAC.get(k).getState()+" "+laneNumber+" "+currentCycle);
                }
            }
            
            for(int k=0; k<num_lanes; k++){
                if(tld[i][k].getTL().getState()==true){
                    System.out.println("Green = "+k);
                    
                }
            }
            for(int k=0; k<num_lanes; k++){
                System.out.print(tld[i][k].getGain()+" ");
            }
            System.out.println("");
            //END OF POLICY APPLIER
            
            //Manual Policy Applier
            
            
            //end of manual policy applier
            
            //STUCK HANDLER, dunno dude this algorithm cause the traffic to stuck at one point of time stepa (maybe coz of the action)
//            for(int j=0; j<tld[i].length; j++){
//                if((double)tld[i][j].getTL().getLane().getNumBlocksWaiting()/tld[i][j].getTL().getLane().getLength()>0.8){
//                    tld[i][j].setGain(5);
//                }
//            }
            //end of stuck handler
        }
        //empty the temp Var<---nevermind LOL
        //System.out.println(this.currentCycle);
        return tld;

    }
    
    public int manualPolicyApplier(String state){
        int action=0;
        switch(state){
            case "LLLL": action=3; break;
            case "HLLL": action=2; break; 
            case "LLLH": action=0; break;
            case "HHLL": action=2; break;
            case "LHLH": action=2; break;
            case "LLHL": action=3; break;
            case "LHLL": action=3; break;
            case "HLHH": action=3; break;
            case "LLHH": action=3; break;
            case "LHHL": action=1; break;
            case "HHLH": action=0; break;
            case "HLLH": action=2; break;
            case "HHHL": action=3; break;
            case "HLHL": action=0; break;
            case "HHHH": action=0; break;
            case "LHHH": action=2; break;
        }
        return action;
    }
    
    public int setAction(TLDecision tld){
        int tempAction=0;
        
        return tempAction;
    }
    
    public class StateSeqContainer{
        ArrayList<String> arrState=new ArrayList();
    }
    public class ActionSeqContainer{
        String action;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
        
    }
    
    public class TuppleStateActionConainer {
        String action;
        ArrayList<String> arrState=new ArrayList<>();

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public ArrayList<String> getArrState() {
            return arrState;
        }

        public void setArrState(ArrayList<String> arrState) {
            this.arrState = arrState;
        }
        
        
    }
    
    public class StateBestActionContainer{
        String state, action;

        public StateBestActionContainer() {
        }

        public StateBestActionContainer(String state, String action) {
            this.state = state;
            this.action = action;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
        
    }
    public ArrayList<StateBestActionContainer> loadActionFile(String url) throws FileNotFoundException, IOException{
        ArrayList<StateBestActionContainer> tempArrSBAC=new ArrayList<>();
        BufferedReader br = new BufferedReader(new java.io.FileReader(url));
        String line;
        String[] splitLine;
        while ((line = br.readLine()) != null) {
            splitLine=line.split("\t");
            StateBestActionContainer tempSBAC=new StateBestActionContainer(splitLine[0], splitLine[2]);
            tempArrSBAC.add(tempSBAC);
        }
        return tempArrSBAC;
    }
    
    public String StateSetter(TLDecision tld){
        String state = null;
        double percentage=(double)tld.getTL().getLane().getNumBlocksWaiting()/tld.getTL().getLane().getLength();
        if(percentage<=0.5){
            state="L";
        }
        else if(percentage>0.5){
            state="H";
        }
        return state;
        
    }
    
    public float GainSetter(String state){
        float gain=0;
        if(state.equals("L")){
            gain=1;
        }
        else if(state.equals("H")){
            gain=2;
        }
        return gain;
    }
    
    
    public void updateRoaduserMove(Roaduser _ru, Drivelane _prevlane, Sign _prevsign, int _prevpos, Drivelane _dlanenow, Sign _signnow, int _posnow, PosMov[] posMovs, Drivelane desired) {
        // No needed <---YEP U'RRE RIGHT

    }

    // Trivial XMLSerializable implementation
    public XMLElement saveSelf() throws XMLCannotSaveException {
        XMLElement result = super.saveSelf();
        result.setName(shortXMLName);
        return result;
    }

    public String getXMLName() {
        return "MDP_2segment_5step_multinodes";
    }
}
