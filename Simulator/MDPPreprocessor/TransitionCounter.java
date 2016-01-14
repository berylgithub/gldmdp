/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MDPPreprocessor;

import java.util.ArrayList;

/**
 *
 * @author Yorozuya
 */
public class TransitionCounter {
    ArrayList<TuppleStateActionContainerSingleState> arrTSAC=new ArrayList<>();

    public TransitionCounter() {
    }

    public ArrayList<TuppleStateActionContainerSingleState> getArrTSAC() {
        return arrTSAC;
    }

    public void setArrTSAC(ArrayList<TuppleStateActionContainerSingleState> arrTSAC) {
        this.arrTSAC = arrTSAC;
    }

    public int countStateByAction(String startState, String nextState, String action){
        int count=0;
        for(int i=0; i<arrTSAC.size(); i++){
            if(arrTSAC.get(i).getState().equals(startState)&&arrTSAC.get(i).getAction().equals(action)){
                String tempNextState=arrTSAC.get(i+1).getState();
                if(tempNextState.equals(nextState)){
                    count++;
                }
            }
        }
        return count;
    }
    
    public int countState(String state){
        int count=0;
        for(int i=0; i<arrTSAC.size(); i++){
            if(arrTSAC.get(i).getState().equals(state)){
                count++;
            }
        }
        return count;
    }
    
    
}
