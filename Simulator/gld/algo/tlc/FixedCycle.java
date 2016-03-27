
/*-----------------------------------------------------------------------
 * Copyright (C) 2001 Green Light District Team, Utrecht University 
 *
 * This program (Green Light District) is free software.
 * You may redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation (version 2 or later).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * See the documentation of Green Light District for further information.
 *------------------------------------------------------------------------*/

package gld.algo.tlc;

import gld.*;
import gld.sim.*;
import gld.algo.tlc.*;
import gld.infra.*;
import gld.utils.*;
import gld.xml.*;
import java.io.IOException;
import java.util.* ;
import java.awt.Point;

/**
 * This controller will switch TrafficLights so that a SignConfig
 * is selected so that the most Roadusers can cross the Node.
 *
 * @author Group Algorithms
 * @version 1.0
 */
public class FixedCycle extends TLController
{	public static final String shortXMLName="Fixed-Policy-Cycle";
	int numCycle;
        int policyNumber;
	/**
	 * The constructor for TL controllers
	 * @param The model being used.
	 */
	public FixedCycle(Infrastructure infras) {
	    super(infras);
            numCycle=infras.getCurCycle(); 
            for(int i=0; i<tld.length; i++){
                for( int j=0; j<tld[i].length; j++){
                    System.out.println(tld[i][j].getTL().getLane().getLength());
                }
            }
            policyNumber=0;
	}
	
	/**
	 * This implementation sets the Q-values according to the length
	 * of the waiting queue. The longer the queue, the higher the Q-value.
	 */	
	public TLDecision[][] decideTLs()
	{
		int num_lanes, num_nodes = tld.length;
                
		if(policyNumber==4){
                    policyNumber=0;
                }
                
		for (int i=0; i < num_nodes; i++) {
                    num_lanes = tld[i].length;
                    for(int j=0; j < num_lanes; j++) {
                        tld[i][j].setGain(0);
                        tld[i][policyNumber].setGain(1);
                    }
                    
		}
                policyNumber++;
                
                //gain debugger
                for(int i=0; i<num_nodes; i++){
                    System.out.print("Gain : ");
                    for(int j=0; j<tld[i].length; j++){
                        System.out.print(tld[i][j].getGain()+" ");
                    }
                    System.out.println("");
                }
                System.out.println("Ppolicy number = "+policyNumber);
                //eo gain debugger
                
		return tld;
	}

	public void updateRoaduserMove(Roaduser _ru, Drivelane _prevlane, Sign _prevsign, int _prevpos, Drivelane _dlanenow, Sign _signnow, int _posnow, PosMov[] posMovs, Drivelane desired)
	{
	    // No needed
            
	}
	
	// Trivial XMLSerializable implementation

	public XMLElement saveSelf () throws XMLCannotSaveException
	{ 	XMLElement result=super.saveSelf();
		result.setName(shortXMLName);
		return result;
	}
  
 	public String getXMLName ()
	{ 	return "Fixed-Policy-Cycle";
	}
}
