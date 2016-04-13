/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MDPPreprocessor;

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author Yorozuya
 */
public class ValueIteration {

    StateContainer sCS[];
    double discount, error, delta, threshold; //delta must be zero for the first iteration

    public ValueIteration() {
    }

    public ValueIteration(StateContainer[] sCS, double discount, double error, double delta) {
        this.sCS = sCS;
        this.discount = discount;
        this.error = error;
        this.delta = delta;
    }

    public double getDelta() {
        return delta;
    }

    public void setDelta(double delta) {
        this.delta = delta;
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

    public void setThreshold() {
        this.threshold = (error * (1 - discount)) / discount;
    }

    public double getError() {
        return error;
    }

    public void setError(double error) {
        this.error = error;
    }

    //static
    public void setEnvReward(String state, double reward) {
        for (int i = 0; i < sCS.length; i++) {
            if (sCS[i].getState().equals(state)) {
                sCS[i].setReward(reward);
            }
        }
    }
    //static

    public void setAutoRewardByState() {
        for (int i = 0; i < sCS.length; i++) {
            sCS[i].autoSetRewardByState();
        }
    }

    public void setAutoRewardByState5Segment() {
        for (int i = 0; i < sCS.length; i++) {
            sCS[i].autoSetRewardByState5Segment();
        }
    }

    public StateContainer searchState(String state) {
        StateContainer tempSC = null;
        for (int i = 0; i < sCS.length; i++) {
            if (sCS[i].getState().equals(state)) {
                tempSC = sCS[i];
            }
        }
        return tempSC;
    }

    public double calUtilityPerStateAction(StateContainer sC, String action) {
        double tempUtil = 0;
        StateContainer tempSC = null;
        for (int i = 0; i < sC.arrTransProb.size(); i++) {
            if (sC.arrTransProb.get(i).getAction().equals(action)) {
                tempSC = searchState(sC.arrTransProb.get(i).getNextState());
                //debugger
                //System.out.println(sC.getState()+" "+tempSC.getState());
                //
                tempUtil = tempUtil + (sC.arrTransProb.get(i).getTransProb() * tempSC.getUtility()); //searchState can ONLY be done in environment, so i can't place this mthod at stateContainer :p, man im confused
                //debugger
                //System.out.println(tempUtil);
                //
            }
        }
        return tempUtil;
    }

    //static
    public double calUtilityPerState(StateContainer sC) {
        double tempStateUtil = 0, tempUtil = 0;
        ArrayList<Double> arrTempUtil = new ArrayList();
        for (int i = 0; i < 4; i++) {//4 action, 0-3
            tempUtil = calUtilityPerStateAction(sC, Integer.toString(i));
            arrTempUtil.add(tempUtil);
        }
        //sort ascend, max(Mij*V(j))
        Collections.sort(arrTempUtil);

        //immmediately set best action
        for (int i = 0; i < 4; i++) {//4 action, 0-3
            tempUtil = calUtilityPerStateAction(sC, Integer.toString(i));
            if (tempUtil == arrTempUtil.get(arrTempUtil.size() - 1)) {
                sC.setBestAction(Integer.toString(i));
            }
        }

        //R[i]+max(Mij*V(j))
        double maxVal = arrTempUtil.get(arrTempUtil.size() - 1);
        tempStateUtil = sC.getReward() + (discount * maxVal);
        return tempStateUtil;
    }
    //static

    //main method to do valueIteration
    public void IterateValue() {
        int n = 0;
        do {
            delta = 0;
            System.out.println("Iteration-" + n);
            double tempUtilState = 0;
            for (int i = 0; i < sCS.length; i++) {
                tempUtilState = calUtilityPerState(sCS[i]);
                if (Math.sqrt(Math.pow((tempUtilState - sCS[i].getUtility()), 2)) > delta) {
                    delta = Math.sqrt(Math.pow((tempUtilState - sCS[i].getUtility()), 2));
                }
                sCS[i].setUtility(tempUtilState);
                //
                //debugger
                System.out.println(sCS[i].getState() + "\t" + sCS[i].getUtility());
                System.out.println("delta : " + delta);
                //
            }
            //debugger
            System.out.println("final delta this iteration : " + delta);
            System.out.println("threshold : " + threshold);
            System.out.println("");
            n++;
        } while (delta > threshold); //until delta < threshold(error)

    }

    public void IterateValue2(int iteration) {
        //debugger
        for (int n = 0; n < iteration; n++) {
            delta = 0;
            System.out.println("Iteration-" + n);
            double tempUtilState = 0;
            for (int i = 0; i < sCS.length; i++) {
                tempUtilState = calUtilityPerState(sCS[i]);
                if (Math.sqrt(Math.pow((tempUtilState - sCS[i].getUtility()), 2)) > delta) {
                    delta = Math.sqrt(Math.pow((tempUtilState - sCS[i].getUtility()), 2));
                }
                sCS[i].setUtility(tempUtilState);
                //
                //debugger
                System.out.println(sCS[i].getState() + "\t" + sCS[i].getUtility());
                System.out.println("delta : " + delta);
                //
            }
            //debugger
            System.out.println("final delta this iteration : " + delta);
            System.out.println("threshold : " + threshold);
            System.out.println("");
            //
        }
    }
}
