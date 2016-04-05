/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Statistics_FileReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Yorozuya
 */
public class Test_reader {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        WtContainer wtc=new WtContainer();
        try {
            wtc.readFile("D:\\Users\\Yorozuya\\Documents\\Matkul smt 7\\TA2\\gldmdp\\Test results\\statistics - 2junction- MDP_3segment_5step_specNodeType2_4k_car(0,25)_with seed(15032016)_t2_test0.dat");
        } catch (IOException ex) {
            Logger.getLogger(Test_reader.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            wtc.readAllFile("D:\\Users\\Yorozuya\\Documents\\Matkul smt 7\\TA2\\gldmdp\\Test results\\statistics - 2junction- MDP_3segment_5step_specNodeType2_4k_car(0,25)_with seed(15032016)_t2_test", 30);
        } catch (IOException ex) {
            Logger.getLogger(Test_reader.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            wtc.filePrinter("result_MDP.txt");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Test_reader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
