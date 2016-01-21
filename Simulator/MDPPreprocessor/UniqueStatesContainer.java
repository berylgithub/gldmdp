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

/**
 *
 * @author Yorozuya
 */
public class UniqueStatesContainer {
    ArrayList<String> statesString=new ArrayList();

    public UniqueStatesContainer() {
    }
    
    public void setUniqueStatesStringFromEnvironment(ArrayList<TuppleStateActionContainerSingleState> arrTSAC){
        ArrayList<String> tempStatesString=new ArrayList<>();
        for(int i=0; i<arrTSAC.size(); i++){
            if(!tempStatesString.contains(arrTSAC.get(i).getState())){
                tempStatesString.add(arrTSAC.get(i).getState());
            }
        }
        statesString=tempStatesString;
    }
    public void setUniqueStatesStringFromFile(String url) throws FileNotFoundException, IOException{
        BufferedReader br = new BufferedReader(new java.io.FileReader(url));
        ArrayList<String> tempStatesString=new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            tempStatesString.add(line);
        }
        statesString=tempStatesString;
    }
    
    public ArrayList<String> getStatesString(){
        return statesString;
    }
}
