
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
 * This controller will switch TrafficLights so that the Trafficlight 
 * which relatively seen has the most Roadusers waiting will be green.
 *
 * @author Group Algorithms
 * @version 1.0
 */
public class RelativeLongestQueueTLC extends TLController
{	
	protected int num_nodes;
	public static final String shortXMLName="tlc-rlq";
	
	/**
	 * The constructor for TL controllers
	 * @param The model being used.
	 */
	public RelativeLongestQueueTLC(Infrastructure i) {
		super(i);
		num_nodes = tld.length;
	}
	
	public void setInfrastructure(Infrastructure i) 
	{ 	super.setInfrastructure(i); 
		num_nodes = tld.length;
	}
	
	/**
	 * This implementation sets the Q-values according to the length
	 * of the waiting queue. The longer the queue, the higher the Q-value.
	 */	
	public TLDecision[][] decideTLs()
	{
            TLDecision tldec;
            Drivelane lane;
            int num_lanes;

            for (int i=0; i < num_nodes; i++) {
                num_lanes = tld[i].length;
                for(int j=0; j < num_lanes; j++) {
                    tldec = tld[i][j];
                    lane = tldec.getTL().getLane();
                    tldec.setGain(((float)lane.getNumBlocksWaiting())/((float)lane.getLength()));
                }
            }
            return tld;
	}

	public void updateRoaduserMove(Roaduser _ru, Drivelane _prevlane, Sign _prevsign, int _prevpos, Drivelane _dlanenow, Sign _signnow, int _posnow, PosMov[] posMovs, Drivelane desired)
	{    // No needed
	}
	
	// Trivial XMLSerializable implementation

	public XMLElement saveSelf () throws XMLCannotSaveException
	{ 	XMLElement result=super.saveSelf();
		result.setName(shortXMLName);
		return result;
	}
  
 	public String getXMLName ()
	{ 	return "model."+shortXMLName;
	}
}
