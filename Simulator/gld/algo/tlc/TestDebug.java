
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
public class TestDebug extends TLController {

    public static final String shortXMLName = "TestDebug";
    int numCycle;
    ArrayList<TuppleStateActionConainer> arrTSAC=new ArrayList<>();
    ArrayList<StateSeqContainer> arrSSC=new ArrayList<>();
    ArrayList<ActionSeqContainer> arrASC=new ArrayList<>();
            /**
             * The constructor for TL controllers
             *
             * @param The model being used.
             */

    public TestDebug(Infrastructure infras) {
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
            
            StateSeqContainer sSC=new StateSeqContainer();
            ActionSeqContainer aSC=new ActionSeqContainer();
            
            for (int j = 0; j < num_lanes; j++) {
                
                tSAC.arrState.add(StateSetter(tld[i][j]));
                
                sSC.arrState.add(StateSetter(tld[i][j]));
                if(tld[i][j].getTL().getLane().getSign().getState()==true){
                    aSC.setAction("hijau||"+j);
                }
                
                if(tld[i][j].getTL().getLane().getSign().getState()==true){
                    tSAC.setAction("hijau||"+j);
                }
                
            }
            arrTSAC.add(tSAC);
            
            arrSSC.add(sSC);
            arrASC.add(aSC);
            
        }//WAT WHY IT DOESNT CHANGE
        try {
            //debugger
//            PrintWriter printActionSeq=new PrintWriter("Action debug.txt");
//            PrintWriter printStateSeq=new PrintWriter("State debug.txt");
            PrintWriter testPrint=new PrintWriter("State-Action Debug.txt");
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
//            for(int i=0; i<arrASC.size(); i++){
//                printActionSeq.println(arrASC.get(i).getAction());
//            }
//            for(int i=0; i<arrSSC.size(); i++){
//                for(int j=0; j<arrSSC.get(i).arrState.size(); j++){
//                    printStateSeq.print(arrSSC.get(i).arrState.get(j));
//                }
//                printStateSeq.println();
//            }
//            printActionSeq.close();
//            printStateSeq.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TestDebug.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("");

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
    
    public String StateSetter(TLDecision tld){
        String state = null;
        double percentage=(double)tld.getTL().getLane().getNumBlocksWaiting()/tld.getTL().getLane().getLength();
        if(percentage<=0.3){
            state="L";
        }
        else if((percentage<=0.6)&&(percentage>0.3)){
            state="M";
        }
        else if(percentage>0.6){
            state="H";
        }
        return state;
        
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
        return "testdebug";
    }
}
