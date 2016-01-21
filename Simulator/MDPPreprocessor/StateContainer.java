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
public class StateContainer {
    String state, bestAction;
    ArrayList<TransitionProbContainer> arrTransProb=new ArrayList<>();
    double reward;
    
    public StateContainer() {
    }

    public StateContainer(String state) {
        this.state = state;
    }
    
    public StateContainer(String state, String bestAction, double reward) {
        this.state = state;
        this.bestAction = bestAction;
        this.reward = reward;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getBestAction() {
        return bestAction;
    }

    public void setBestAction(String bestAction) {
        this.bestAction = bestAction;
    }

    public ArrayList<TransitionProbContainer> getTransProb() {
        return arrTransProb;
    }

    public void setTransProb(ArrayList<TransitionProbContainer> transProb) {
        this.arrTransProb = transProb;
    }

    public double getReward() {
        return reward;
    }
    
    public void setReward(double reward) {
        this.reward = reward;
    }
    
    
    //method buat semua Action, sementara not dynamic
    public void countThisTransProb(TransitionCounter tC, UniqueStatesContainer uSC){
        ArrayList<TransitionProbContainer> tempArrTP=new ArrayList<>();
        for(int i=0; i<uSC.getStatesString().size(); i++){
            for(int j=0; j<4; j++){
                TransitionProbContainer tempTPObj=new TransitionProbContainer(this.getState(), Integer.toString(j), tC.countTransProb(this.getState(), uSC.getStatesString().get(i), Integer.toString(j)));
                tempArrTP.add(tempTPObj);
            }
        }
        arrTransProb=tempArrTP;
    }
    
    
}
