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
        trans.setArrTSAC(prep.arrTSAC);
        System.out.println(trans.countStateByAction("LLLL", "HLLL", "0"));
        System.out.println(trans.countState("HHHH"));
    }
    
}
