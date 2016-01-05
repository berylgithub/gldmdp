/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MDPLearner;

import java.util.ArrayList;

/**
 *
 * @author Yorozuya
 */
public class TuppleStateActionContainer {
    String action;
    ArrayList<String> arrState=new ArrayList<>();

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public ArrayList<String> getArrState() {
        return arrState;
    }

    public void setArrState(ArrayList<String> arrState) {
        this.arrState = arrState;
    }
}
