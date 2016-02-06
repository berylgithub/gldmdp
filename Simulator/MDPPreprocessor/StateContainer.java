/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MDPPreprocessor;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
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
            for(int j=0; j<4; j++){ //number of action = 4 (4 road per junction)
                TransitionProbContainer tempTPObj=new TransitionProbContainer(this.getState(), Integer.toString(j), uSC.getStatesString().get(i), tC.countTransProb(this.getState(), uSC.getStatesString().get(i), Integer.toString(j)));
                tempArrTP.add(tempTPObj);
            }
        }
        arrTransProb=tempArrTP;
    }
    
    public void printTransProb(PrintWriter printer) throws FileNotFoundException{
        for(int i=0; i<arrTransProb.size(); i++){
            printer.println(this.getState()+"\t"+this.getTransProb().get(i).getAction()+"\t"+this.getTransProb().get(i).getTransProb()+"\t"+this.getTransProb().get(i).getNextState());
        }
        printer.println();
    }
    
    
}
