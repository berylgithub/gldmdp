/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MDPPreprocessor;

/**
 *
 * @author Yorozuya
 */
public class TransitionProbContainer {
    String state, action;
    double transProb;

    public TransitionProbContainer() {
    }

    
    public TransitionProbContainer(String state, String action, double transProb) {
        this.state = state;
        this.action = action;
        this.transProb = transProb;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public double getTransProb() {
        return transProb;
    }

    public void setTransProb(double transProb) {
        this.transProb = transProb;
    }
    
}
