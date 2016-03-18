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
public class Debugger {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        // TODO code application logic here
        Preprocessor prep=new Preprocessor();
        prep.loadSimulationRecordWNullRemover("State-Action Debug.txt");
        prep.testPrint();
        
        //testing transition counter
        TransitionCounter trans=new TransitionCounter();
        trans.setArrTSAC(prep.getArrTSAC());
        System.out.println(trans.countNextStateByAction("LLLL", "LLLL", "0"));
        System.out.println(trans.countActionByState("LLLL", "0"));
        //good
        
        //test transition probability
        System.out.println(trans.countTransProb("LLLL", "LLLL", "0"));
        
        //test unique states container
        UniqueStatesContainer uSC=new UniqueStatesContainer();
        uSC.setUniqueStatesStringFromEnvironment(prep.getArrTSAC());
        System.out.println("from environment, total states = "+uSC.getStatesString().size());
        uSC.setUniqueStatesStringFromFile("State_2segment.txt");
        System.out.println("from file = "+uSC.getStatesString().size());
        
        //test count transProb in 1 state against all state and actions
        //LMAO I DONT EVEN REMEMBER THE CLASS SCHEMES
        StateContainer sC0=new StateContainer("LLLL");
        sC0.countThisTransProb(trans, uSC);
        System.out.println("P(LLLL, 0, LLLL) = "+sC0.getTransProb().get(0).getTransProb());
        //SUCCESS
        
        //test autoreward
        
    }
    
}
