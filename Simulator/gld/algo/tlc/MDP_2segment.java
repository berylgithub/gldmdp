
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
public class MDP_2segment extends TLController {

    public static final String shortXMLName = "MDP_2segment";
    int numCycle;
    ArrayList<TuppleStateActionConainer> arrTSAC=new ArrayList<>();
    ArrayList<TuppleStateActionConainer>[] arrMultiTSAC;
//    ArrayList<StateSeqContainer> arrSSC=new ArrayList<>();
//    ArrayList<ActionSeqContainer> arrASC=new ArrayList<>();
            /**
             * The constructor for TL controllers
             *
             * @param The model being used.
             */

    public MDP_2segment(Infrastructure infras) {
        super(infras);
        numCycle = infras.getCurCycle();
        
        for (int i = 0; i < tld.length; i++) {
            for (int j = 0; j < tld[i].length; j++) {
                System.out.println(j + " " + tld[i][j].getTL().getLane().getLength());
            }
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
            
            TuppleStateActionConainer tSAC=new TuppleStateActionConainer();
            
            for (int j = 0; j < num_lanes; j++) {
                
                tSAC.arrState.add(StateSetter(tld[i][j]));
                tld[i][j].setGain(GainSetter(StateSetter(tld[i][j])));

                if(tld[i][j].getTL().getLane().getSign().getState()==true){
                    tSAC.setAction(""+j);
                }
                
            }
            arrTSAC.add(tSAC);
        }
//        try {
//            PrintWriter testPrint=new PrintWriter("State-Action Debug.txt");
//            for(int i=0; i<arrTSAC.size(); i++){
//                System.out.print(arrTSAC.get(i).getAction()+"\t");
//                testPrint.write(arrTSAC.get(i).getAction()+"\t");
//                for(int j=0; j<arrTSAC.get(i).arrState.size(); j++){
//                    testPrint.write(arrTSAC.get(i).arrState.get(j));
//                    System.out.print(arrTSAC.get(i).arrState.get(j));
//                }
//                System.out.println();
//                testPrint.println();
//            }
//            testPrint.close();
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(MDP_2segment.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//        System.out.println("");
//
        return tld;

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
        BufferedReader br = new BufferedReader(new java.io.FileReader("ManualTransProb.txt"));
        String line;
        String[] splitLine;
        ArrayList<TransitionProbContainer> tempArrTPC=new ArrayList<>();
        while ((line = br.readLine()) != null) {
            splitLine=line.split("\t");
            if(!splitLine[0].equals("null")){
                TransitionProbContainer tempTPC= new TransitionProbContainer(splitLine[0], splitLine[1], splitLine[3], Double.parseDouble(splitLine[2]));
                tempArrTPC.add(tempTPC);
            }
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
        // No needed

    }

    // Trivial XMLSerializable implementation
    public XMLElement saveSelf() throws XMLCannotSaveException {
        XMLElement result = super.saveSelf();
        result.setName(shortXMLName);
        return result;
    }

    public String getXMLName() {
        return "MDP_2segment";
    }
}
