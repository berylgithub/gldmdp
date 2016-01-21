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
    //the most importand variable in the whole package screen
    ArrayList<TuppleStateActionContainerSingleState> arrTSAC=new ArrayList<>();
    //OR IN OTHER WORDS THIS IS THE FRIGGIN ENVIRONMENNTTT
    
    public TransitionCounter() {
    }

    public ArrayList<TuppleStateActionContainerSingleState> getArrTSAC() {
        return arrTSAC;
    }

    public void setArrTSAC(ArrayList<TuppleStateActionContainerSingleState> arrTSAC) {
        this.arrTSAC = arrTSAC;
    }
    
    public int countActionByState(String startState, String action){
        int count=0;
        for(int i=0; i<arrTSAC.size(); i++){
            if(arrTSAC.get(i).getState().equals(startState)&&arrTSAC.get(i).getAction().equals(action)){
                count++;
            }
        }
        return count;
    }
    
    public int countNextStateByAction(String startState, String nextState, String action){
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
    
    
    public int countState(String state){//lol wtf is this function for
        int count=0;
        for(int i=0; i<arrTSAC.size(); i++){
            if(arrTSAC.get(i).getState().equals(state)){
                count++;
            }
        }
        return count;
    }
    
    public double countTransProb(String startState, String nextState, String action){
        return (double)countNextStateByAction(startState, nextState, action)/countActionByState(startState, action);
    }
}
