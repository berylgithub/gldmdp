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
        Preprocessor prep=new Preprocessor();
        prep.loadSimulationRecordWNullRemover("State-Action Debug.txt");
    }
    
}
