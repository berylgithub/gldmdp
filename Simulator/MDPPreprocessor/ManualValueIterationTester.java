/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MDPPreprocessor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author Yorozuya
 */
public class ManualValueIterationTester {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        // TODO code application logic here
        //should've made TEST PACKAGE :P, this structure is sooo shiiieeett *clq
        StateContainer sCS[]=new StateContainer[5];
        for(int i=0; i<sCS.length; i++){
            sCS[i]=new StateContainer();
        }
        
        sCS[0].setState("A");
        sCS[1].setState("B");
        sCS[2].setState("C");
        sCS[3].setState("D");
        sCS[4].setState("E");
        
        //loadfile manual
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
        
        //test loader
        for(int i=0; i<tempArrTPC.size(); i++){
            System.out.println(tempArrTPC.get(i).getState()+"\t"+tempArrTPC.get(i).getAction()+"\t"+tempArrTPC.get(i).getTransProb()+"\t"+tempArrTPC.get(i).getNextState());
        }
        
        //set info on States
        for(int i=0; i<tempArrTPC.size(); i++){
            if(tempArrTPC.get(i).getState().equals("A")){
                sCS[0].getTransProb().add(tempArrTPC.get(i));
            }
            else if(tempArrTPC.get(i).getState().equals("B")){
                sCS[1].getTransProb().add(tempArrTPC.get(i));
            }
            else if(tempArrTPC.get(i).getState().equals("C")){
                sCS[2].getTransProb().add(tempArrTPC.get(i));
            }
            else if(tempArrTPC.get(i).getState().equals("D")){
                sCS[3].getTransProb().add(tempArrTPC.get(i));
            }
            else if(tempArrTPC.get(i).getState().equals("E")){
                sCS[4].getTransProb().add(tempArrTPC.get(i));
            }
        }
        
        System.out.println(sCS[0].getState()+"\t"+sCS[0].getTransProb().get(1).getState()+"\t"+sCS[0].getTransProb().get(1).getAction()+"\t"+sCS[0].getTransProb().get(1).getNextState()+"\t"+sCS[0].getTransProb().get(1).getTransProb());
        
        
        //valueiteration+set params
        ValueIteration Vi=new ValueIteration();
        Vi.setsCS(sCS);
        Vi.setDelta(0); //shoukd be set to zero
        Vi.setDiscount(0.9);
        Vi.setError(0.01);
        Vi.setThreshold(); //should be done LAST after all other params are aset
        
        System.out.println(Vi.getThreshold());
        
        //set environment reward
        Vi.setEnvReward("A", 0);
        Vi.setEnvReward("B", 0);
        Vi.setEnvReward("C", 0);
        Vi.setEnvReward("D", 0);
        Vi.setEnvReward("E", 4);
        
        
        //lest's do it
        Vi.IterateValue();
        
        //check results
        for(int i=0; i<sCS.length; i++){
            System.out.println(sCS[i].getState()+"\t"+sCS[i].getUtility()+"\t"+sCS[i].getBestAction());
        }
        
        //YESSS VALUE ITERATION IS SUCCESSFUL
    }

}
