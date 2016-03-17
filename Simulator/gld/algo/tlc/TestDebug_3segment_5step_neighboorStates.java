
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
import java.io.IOException;
import java.util.*;
import java.awt.Point;
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
public class TestDebug_3segment_5step_neighboorStates extends TLController {

    public static final String shortXMLName = "TestDebug-3-segment-5-step-neighboorStates";
    int actionCounter;
    Infrastructure infrastructure;
    ArrayList<TuppleStateActionConainer> arrTSAC=new ArrayList<>();
    ArrayList<TuppleStateActionConainer>[] arrMultiTSAC;

    public TestDebug_3segment_5step_neighboorStates(Infrastructure infras) {
        super(infras);
        this.infrastructure=infras;
        actionCounter=0;
        for (int i = 0; i < tld.length; i++) {
            for (int j = 0; j < tld[i].length; j++) {
                System.out.println(j + " " + tld[i][j].getTL().getLane().getLength());
            }
        }
        //lane debugger
        Drivelane[] outLanes = null;
        for (int i = 0; i < tld.length; i++) {
            
            for(int j=0; j<tld[i].length; j++){
                try {
                    System.out.println("Node "+i+", jumlah lane :"+tld[i].length+" jumlah out lanes : "+tld[i][j].getTL().getNode().getOutboundLanes().length);
                    outLanes=getOutLanes(tld[i][j]);
//                    inLanes=tld[i][j].getTL().getNode().getInboundLanes();
                    
//                    for(int k=0; k<outLanes.length; k++){
//                        System.out.println("outLAne ke "+k+" menuju node "+outLanes[k].getNodeLeadsTo().getId());
//                        System.out.println("inLane ke "+k+" menuju node "+inLanes[k].getNodeLeadsTo().getId());
//                    }
                } catch (InfraException ex) {
                    Logger.getLogger(TestDebug_3segment_5step_neighboorStates.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        System.out.println(outLanes.length);
        
        
        //end of lane debugger

    }
    
    //try menggunakan meth....od
    public Drivelane[] getOutLanes(TLDecision tld) throws InfraException{
        return tld.getTL().getNode().getOutboundLanes();
    }
    //end of meth....od
    //the meth.....od workssss
    
    /**
     * This implementation sets the Q-values according to the length of the
     * waiting queue. The longer the queue, the higher the Q-value.
     */
    public TLDecision[][] decideTLs() {

        int num_lanes, num_nodes = tld.length;
        for (int i = 0; i < num_nodes; i++) {
            num_lanes = tld[i].length;
            Drivelane[] outLanes=null;
            TuppleStateActionConainer tSAC=new TuppleStateActionConainer();
            if(i==15){
                for (int j = 0; j < num_lanes; j++) {
                    boolean tempCheck=checkAction(tld[i][j]);
                    if(tempCheck==true){
                        tSAC.setAction(""+j);
                    }
                    
                    try {
                        outLanes=getOutLanes(tld[i][j]);
                    } catch (InfraException ex) {
                        Logger.getLogger(TestDebug_3segment_5step_neighboorStates.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    tSAC.arrState.add(StateSetter(tld[i][j]));
                    tld[i][j].setGain(0);
                    Random rand = new Random();
                    int randomNum = rand.nextInt((10-0)+1)+0;
                    tld[i][j].setGain((float)randomNum);
                }
                for(int j=0; j<outLanes.length; j++){
                    tSAC.arrState.add(StateSetterOutlane(outLanes[j]));
                }
                arrTSAC.add(tSAC);
            }
            
            //gain debugger
            System.out.print("Gain : ");
            for (int j = 0; j < num_lanes; j++) {
                System.out.print(tld[i][j].getGain()+" ");
            }
            System.out.println("");
            //end of gain debugger
        }
        
        try {
            PrintWriter testPrint=new PrintWriter("State-Action Debug_3-segment_5-step_random_neighboorStates.txt");
            for(int i=0; i<arrTSAC.size(); i++){
                System.out.print(arrTSAC.get(i).getAction()+"\t");
                testPrint.write(arrTSAC.get(i).getAction()+"\t");
                for(int j=0; j<arrTSAC.get(i).arrState.size(); j++){
                    testPrint.write(arrTSAC.get(i).arrState.get(j));
                    System.out.print(arrTSAC.get(i).arrState.get(j));
                }
                System.out.println();
                testPrint.println();
            }
            testPrint.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TestDebug_3segment_5step_neighboorStates.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("Cycle = "+(currentCycle-1));
        System.out.println("Action Counter = "+actionCounter);
        actionCounter++;
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
    
    public boolean checkAction(TLDecision tld){
        boolean tempCheck=true;
        if(tld.getTL().getLane().getSign().getState()==true){
            tempCheck=true;
        }
        else tempCheck=false;
        
        return tempCheck;
    }
    
    public String StateSetter(TLDecision tld){
        String state = null;
        double percentage=(double)tld.getTL().getLane().getNumBlocksWaiting()/tld.getTL().getLane().getLength();
        if(percentage<=0.33){
            state="L";
        }
        else if(percentage>0.33&&percentage<=0.67){
            state="M";
        }
        else if(percentage>0.67){
            state="H";
        } 
        return state;
        
    }
    public String StateSetterOutlane(Drivelane outLanes){
        String state = null;
        double percentage=(double)outLanes.getNumBlocksWaiting()/outLanes.getLength();
        if(percentage<=0.33){
            state="L";
        }
        else if(percentage>0.33&&percentage<=0.67){
            state="M";
        }
        else if(percentage>0.67){
            state="H";
        } 
        return state;
    }
    
    public float GainSetter(String state){
        float gain=0;
        if(state.equals("L")){
            gain=1;
        }
        else if(state.equals("M")){
            gain=2;
        }
        else if(state.equals("H")){
            gain=3;
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
        return "testdebug-3-segment-5-step-neighboorStates";
    }
}
