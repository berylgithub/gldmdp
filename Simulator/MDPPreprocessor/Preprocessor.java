/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MDPPreprocessor;

import gld.algo.tlc.MDP_2segment;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author Yorozuya
 */
public class Preprocessor {
    //read file and delete nulls
    ArrayList<TuppleStateActionContainerSingleState> arrTSAC=new ArrayList<>();

    public Preprocessor() {
        
    }

    public void setArrTSAC(ArrayList<TuppleStateActionContainerSingleState> arrTSAC) {
        this.arrTSAC = arrTSAC;
    }

    public ArrayList<TuppleStateActionContainerSingleState> getArrTSAC() {
        return arrTSAC;
    }
    
    
    public void loadSimulationRecordWNullRemover(String url) throws FileNotFoundException, IOException{
        BufferedReader br = new BufferedReader(new java.io.FileReader(url));
        String line;
        String[] splitLine;
        ArrayList<TuppleStateActionContainerSingleState> tempArrTSAC=new ArrayList<>();
        while ((line = br.readLine()) != null) {
            splitLine=line.split("\t");
            if(!splitLine[0].equals("null")){
                TuppleStateActionContainerSingleState tempTSAC = new TuppleStateActionContainerSingleState();
                tempTSAC.setAction(splitLine[0]);
                tempTSAC.setState(splitLine[1]);
                tempArrTSAC.add(tempTSAC);
            }
         }
        setArrTSAC(tempArrTSAC);
    }
    
    //ACTION FIXERRRR
    public void shiftActionMinusOne(){
        ArrayList<TuppleStateActionContainerSingleState> tempArrTSACSS=new ArrayList<>();
        int i=0;
        do{
            TuppleStateActionContainerSingleState tempTSACSS=arrTSAC.get(i);
            tempTSACSS.setAction(arrTSAC.get(i+1).getAction());
            tempArrTSACSS.add(tempTSACSS);
            i++;
        }while(i<arrTSAC.size()-1);
        arrTSAC=tempArrTSACSS;
    }
    //END OF ACTION FIXERRRRR
    
    public void testPrint(){
        for(int i=0; i<arrTSAC.size(); i++){
            System.out.println(arrTSAC.get(i).action+" "+arrTSAC.get(i).state);
        }
    }
}
