/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Statistics_FileReader;

import MDPPreprocessor.TuppleStateActionContainerSingleState;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 *
 * @author Yorozuya
 */
public class WtContainer {

    ArrayList<String> arrATWT;
    ArrayList<String> arrAJWT;

    public WtContainer() {
        arrATWT = new ArrayList<>();
        arrAJWT = new ArrayList<>();
    }

    public ArrayList<String> getArrATWT() {
        return arrATWT;
    }

    public void setArrATWT(ArrayList<String> arrATWT) {
        this.arrATWT.clear();
        this.arrATWT.addAll(arrATWT);
    }

    public ArrayList<String> getArrAJWT() {
        return arrAJWT;
    }

    public void setArrAJWT(ArrayList<String> arrAJWT) {
        this.arrAJWT.clear();
        this.arrAJWT.addAll(arrAJWT);
    }

    //static function
    public String[] readFile(String url) throws FileNotFoundException, IOException {
        String[] wt = new String[2];
        wt[0] = Files.readAllLines(Paths.get(url)).get(10);
        wt[1] = Files.readAllLines(Paths.get(url)).get(11);
//        System.out.println(wt[0]);
//        System.out.println(wt[1]);
        String[] wtSplit;

        wtSplit = wt[0].split(" ");
        wt[0] = wtSplit[10];

        wtSplit = wt[1].split(" ");
        wt[1] = wtSplit[10];
        if(wt[1].equals("arrived):")){
            String tempwt=Files.readAllLines(Paths.get(url)).get(12);
            wtSplit=tempwt.split(" ");
            wt[1]=wtSplit[10];
        }

//        System.out.println(wt[0]);
//        System.out.println(wt[1]);
        return wt;
    }
    //static function ROFL

    //another statics....... noob
    public void readAllFile(String url, int max) throws FileNotFoundException, IOException {
        String[] tempwt=new String[2];
        ArrayList<String> tempAJWT=new ArrayList<>();
        ArrayList<String> tempATWT=new ArrayList<>();
        for(int i=0; i<max; i++){
            tempwt=readFile(url+i+".dat");
            tempAJWT.add(tempwt[1]);
            tempATWT.add(tempwt[0]);
            System.out.println(tempwt[0]);
            System.out.println(tempwt[1]);
        }
        setArrAJWT(tempAJWT);
        setArrATWT(tempATWT);
    }
    
    public void filePrinter(String url) throws FileNotFoundException{
        PrintWriter printer=new PrintWriter(url);
        printer.println("AJWT\tATWT");
        for(int i=0; i<arrAJWT.size(); i++){
            printer.println(arrAJWT.get(i)+"\t"+arrATWT.get(i));
        }
        printer.close();
    }
}
