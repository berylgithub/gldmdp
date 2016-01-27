/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MDPPreprocessor;

import java.io.IOException;

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
        
        //create States from environments
        StateContainer[] sCS=new StateContainer[uSC.getStatesString().size()];
        for(int i=0; i<uSC.getStatesString().size(); i++){
            sCS[i]=new StateContainer(uSC.getStatesString().get(i));
            sCS[i].countThisTransProb(tSCount, uSC);
            System.out.println(sCS[i].getState());
        }
        
    }
    
}
