
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
 * This controller will switch TrafficLights so that the Trafficlight with the longest queue of 
 * waiting Roadusers will get set to green.
 *
 * @author Group Algorithms
 * @version 1.0
 */
public class LongestQueueTLC extends TLController
{	protected final static String shortXMLName="tlc-longestqueue";
	
	/**
	 * The constructor for TL controllers
	 * @param The model being used.
	 */
	public LongestQueueTLC(Infrastructure i) {
	    super(i);
	}
	
	/**
	 * This implementation sets the Q-values according to the length
	 * of the waiting queue. The longer the queue, the higher the Q-value.
	 */	
	public TLDecision[][] decideTLs()
	{
		int maxLength, num_lanes, num_nodes, maxId, temp_len, ru_pos;
		num_nodes = tld.length;
		
		for (int i=0; i < num_nodes; i++) {
			maxLength = -1;
			maxId = -1;
			num_lanes = tld[i].length;
			for(int j=0; j < num_lanes; j++) {
				temp_len = tld[i][j].getTL().getLane().getNumRoadusersWaiting();
				if (temp_len > maxLength) {
					maxLength = temp_len;
					maxId = j;
				}
				
				tld[i][j].setGain(0);
			}
			if(maxId!=-1)
				tld[i][maxId].setGain(maxLength);
		}
		return tld;
	}

	public void updateRoaduserMove(Roaduser _ru, Drivelane _prevlane, Sign _prevsign, int _prevpos, Drivelane _dlanenow, Sign _signnow, int _posnow, PosMov[] posMovs, Drivelane desired)
	{    // No needed
	}
	
	// XMLSerializable implementation

	public XMLElement saveSelf () throws XMLCannotSaveException
	{ 	XMLElement result=super.saveSelf();
		result.setName(shortXMLName);
		return result;
	}
  
 	public String getXMLName ()
	{ 	return "model."+shortXMLName;
	}

}
