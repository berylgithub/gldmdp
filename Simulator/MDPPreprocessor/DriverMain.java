/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MDPPreprocessor;

import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * @author Yorozuya
 */
public class DriverMain {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        // TODO code application logic here
        
        //initialize Unique States and Transition Counter from Environments
        Preprocessor prep=new Preprocessor();
        prep.loadSimulationRecordWNullRemover("State-Action Debug.txt");
        UniqueStatesContainer uSC=new UniqueStatesContainer();
        uSC.setUniqueStatesStringFromEnvironment(prep.getArrTSAC());
        TransitionCounter tSCount=new TransitionCounter();
        tSCount.setArrTSAC(prep.getArrTSAC());
        
        //create States from environments + NaN remover
        PrintWriter printer=new PrintWriter("Transition Probability.txt");
        StateContainer[] sCS=new StateContainer[uSC.getStatesString().size()];
        for(int i=0; i<uSC.getStatesString().size(); i++){
            sCS[i]=new StateContainer(uSC.getStatesString().get(i));
            sCS[i].countThisTransProb(tSCount, uSC);
            //NaN remover, NaN=0, coz if it is left it will destroy the equation
            for(int j=0; j<sCS[i].getTransProb().size(); j++){
                if(Double.isNaN(sCS[i].getTransProb().get(j).getTransProb())){
                    sCS[i].getTransProb().get(j).setTransProb(0);
                }
            }
            //
            sCS[i].printTransProb(printer);
            System.out.println(sCS[i].getState());
        }
        printer.close();
        
        //Value Iteration !!!
        ValueIteration Vi=new ValueIteration();
        Vi.setsCS(sCS);
        Vi.setDelta(0);
        Vi.setDiscount(0.9);
        Vi.setError(0.01);
        Vi.setThreshold();
        
        //set Environment
        Vi.setEnvReward("LLLL", 100);
        Vi.setEnvReward("LLLH", 1);
        Vi.setEnvReward("LLHL", 1);
        Vi.setEnvReward("LHLL", 1);
        Vi.setEnvReward("HLLL", 1);
        Vi.setEnvReward("HHLL", -20);
        Vi.setEnvReward("LHHL", -20);
        Vi.setEnvReward("LLHH", -20);
        Vi.setEnvReward("HLLH", -20);
        Vi.setEnvReward("HHHL", -40);
        Vi.setEnvReward("HHLH", -40);
        Vi.setEnvReward("HLHH", -40);
        Vi.setEnvReward("LHHH", -40);
        Vi.setEnvReward("HHHH", -100);
        
        //LETS' DO IT !!!!!!!!!
        Vi.IterateValue();
        
        //check results
        for(int i=0; i<sCS.length; i++){
            System.out.println(sCS[i].getState()+"\t"+sCS[i].getUtility()+"\t"+sCS[i].getBestAction());
        }
        
        PrintWriter printer2=new PrintWriter("Value Iteration Result.txt");
        printer2.println("State\tState's Utility\tBest Action");
        for(int i=0; i<sCS.length; i++){
            printer2.println(sCS[i].getState()+"\t"+sCS[i].getUtility()+"\t"+sCS[i].getBestAction());
        }
        printer2.close();
    }
    
}
