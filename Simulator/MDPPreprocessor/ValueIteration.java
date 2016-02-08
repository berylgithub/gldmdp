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
public class ValueIteration {
    StateContainer sCS[];
    double discount, threshold, error;
    
    public ValueIteration() {
    }

    public ValueIteration(StateContainer[] sCS, double discount, double threshold, double error) {
        this.sCS = sCS;
        this.discount = discount;
        this.threshold = threshold;
        this.error = error;
    }
    
    
    public StateContainer[] getsCS() {
        return sCS;
    }

    public void setsCS(StateContainer[] sCS) {
        this.sCS = sCS;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public double getError() {
        return error;
    }

    public void setError(double error) {
        this.error = error;
    }
    
    
    
    //static
    public void setEnvReward(String state, double reward){
        for(int i=0; i<sCS.length; i++){
            if(sCS[i].getState().equals(state)){
                sCS[i].setReward(reward);
            }
        }
    }
    //static

    
    
}
