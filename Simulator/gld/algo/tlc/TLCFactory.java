
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
/** This class can be used to create instances of Traffic Light Controllers
  * for a specific infrastructure.
  */

import gld.infra.InfraException;
import gld.infra.Infrastructure;
import gld.utils.StringUtils;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Random;

public class TLCFactory
{
	protected Infrastructure infra;
	protected Random random;

	protected static final int
		RANDOM=0,
 		LONGEST_QUEUE=1,
		MOST_CARS=2,
		BEST_FIRST=3,
		RELATIVE_LONGEST_QUEUE=4,
		RLD=5,
		RLD2=6,
		TC_1OPT=7,
		TC_2OPT=8,
		TC_3OPT=9,
		TC1_B1=10,
		TC2_B1=11,
		TC3_B1=12,
		TC_1_DESTLESS=13,
		TC_2_DESTLESS=14,
		TC_3_WORKINPROGRESS=15,
		ACGJ_1=16,
		ACGJ_2=17,
		ACGJ_3=18,
		ACGJ_3_FV=19,
		ACGJ_4=20,
		ACGJ_5=21,
		LOCAL=22,
		GENNEURAL=23,
		TC_2FINAL=24,
		TC1_FIX=25,
		RLSARSA1=26,
		RLSARSA2=27,
		RLSARSA3=28,
		RLSARSA4=29,
		RLSARSA5=30,
		RLSARSA6=31,
        /////
                FIXEDCYCLE=32,
                PERCENTAGE=33,
                TESTDEBUG=34,
                TESTDEBUG2SEGMENT=35;

	protected static final String[] tlcDescs = {
		"Random",
		"Longest Queue",
		"Most Cars",
		"Best First",
		"Relative Longest Queue",
		"Red Light District",
		"Red Light District 2",
		"TC-1 Optimized 2.0",
		"TC-2 Optimized 2.0 (unfixed?)",
		"TC-3 Optimized 1.0 (unfixed?)",
		"TC-1 Bucket 2.0",
		"TC-2 Bucket 1.0",
		"TC-3 Bucket 1.0",
		"TC-1 Destinationless",
		"TC-2 Destinationless",
		"TC-3 Work In Progress",
		"ACGJ-1",
		"ACGJ-2",
		"ACGJ-3",
		"ACGJ-3 Stupidified",
		"ACGJ-4 : Gain Factoring, 2xDNA",
		"ACGJ-4 : Gain Factoring, 1xDNA",
		"Local Hillclimbing",
	    "GenNeural",
    	"TC-2 Final Version",
    	"TC-1 version (not ok?)",
    	"RL Sarsa 1",
    	"RL Sarsa 2",
    	"RL Sarsa 3",
    	"RL Sarsa 4",
    	"RL Sarsa 5",
    	"RL Sarsa 6",
        
        "FIxed Cycle ",
        "Percentage Queue",
        "Test Debug",
        "Test Debug 2 Segment",
	};

	protected static final String[] xmlNames = {
		RandomTLC.shortXMLName,
		LongestQueueTLC.shortXMLName,
		MostCarsTLC.shortXMLName,
		BestFirstTLC.shortXMLName,
		RelativeLongestQueueTLC.shortXMLName,
		RLDTLC.shortXMLName,
		RLD2TLC.shortXMLName,
		TC1TLCOpt.shortXMLName,
		TC2TLCOpt.shortXMLName,
		TC3Opt.shortXMLName,
		TC1B1.shortXMLName,
		TC2B1.shortXMLName,
		TC3B1.shortXMLName,
		TC1TLCDestless.shortXMLName,
		TC2TLCDestless.shortXMLName,
		TC3TLCWorkInProgress.shortXMLName,
		ACGJ1.shortXMLName,
		ACGJ2.shortXMLName,
		ACGJ3.shortXMLName,
		ACGJ3FixedValue.shortXMLName,
		ACGJ4.shortXMLName,
		ACGJ5.shortXMLName,
		LocalHillTLC.shortXMLName,
    	GenNeuralTLC.shortXMLName,
    	TC2Final.shortXMLName,
    	TC1TLCFix.shortXMLName,
    	SL1TLC.shortXMLName,
    	SL2TLC.shortXMLName,
    	SL3TLC.shortXMLName,
    	SL4TLC.shortXMLName,
    	SL5TLC.shortXMLName,
    	SL6TLC.shortXMLName,
        
        //
        FixedCycle.shortXMLName,
        PercentageQue.shortXMLName,
        TestDebug.shortXMLName,
        TestDebug_2segment.shortXMLName,
	};


	protected static final String[] categoryDescs = {"Simple Maths", "Complex Maths", "Longest Q-variants", "Reinforcement Learning", "RL Sarsa TLCs", "Genetic", "Neural Network", "TA"};
	protected static final int[][] categoryTLCs = {
		{RANDOM, MOST_CARS, RLD, RLD2, },
		{LOCAL, ACGJ_2},
		{LONGEST_QUEUE, RELATIVE_LONGEST_QUEUE, BEST_FIRST},
//		{TC1_FIX, TC_1OPT, TC_2OPT, TC_3OPT, TC1_B1, TC2_B1, TC3_B1, TC_1_DESTLESS, TC_2_DESTLESS, TC_3_WORKINPROGRESS, TC_2FINAL},        
		{TC1_FIX, TC_1OPT, TC_2OPT, TC_3OPT},
		{RLSARSA1,RLSARSA2,RLSARSA3,RLSARSA4,RLSARSA5,RLSARSA6},
		{ACGJ_1, ACGJ_3, ACGJ_3_FV, ACGJ_4, ACGJ_5},
		{GENNEURAL},
                {FIXEDCYCLE, PERCENTAGE, TESTDEBUG, TESTDEBUG2SEGMENT},
                
	};

	/** Makes a new TLCFactory for a specific infrastructure with a new
	  * random number generator.
	  * @param infra The infrastructure
	 */
  	public TLCFactory(Infrastructure infra)
	{ 	this.infra=infra;
		random=new Random();
	}

	/** Makes a new TLCFactory for a specific infrastructure
	  * @param random The random number generator which some algorithms use
	  * @param infra The infrastructure
	 */
  	public TLCFactory(Infrastructure infra, Random random)
  	{ 	this.infra=infra;
		this.random=random;
  	}

	/** Looks up the id of a TLC algorithm by its description
	  * @param algoDesc The description of the algorithm
	  * @returns The id of the algorithm
	  * @throws NoSuchElementException If there is no algorithm with that
	  *        description.
	 */
	public static int getId (String algoDesc)
	{ 	return StringUtils.getIndexObject(tlcDescs,algoDesc);
	}

	/** Returns an array of TLC descriptions */
	public static String[] getTLCDescriptions() { return tlcDescs; }

  	/** Look up the description of a TLC algorithm by its id
	  * @param algoId The id of the algorithm
	  * @returns The description
	  * @throws NoSuchElementException If there is no algorithm with the
	  *	    specified id.
	*/
  	public static String getDescription (int algoId)
  	{ 	return (String)(StringUtils.lookUpNumber(tlcDescs,algoId));
  	}

  	/** Returns an array containing the TLC category descriptions. */
  	public static String[] getCategoryDescs()
	{ 	return categoryDescs;
	}

  	/** Returns an array of TLC numbers for each TLC category. */
  	public static int[][] getCategoryTLCs()
	{ 	return categoryTLCs;
	}

  	/** Returns the total number of TLCs currently available. */
 	public static int getNumberOfTLCs()
	{ 	return tlcDescs.length;
	}

  	/** Gets the number of an algorithm from its XML tag name */
  	public static int getNumberByXMLTagName(String tagName)
  	{ 	return StringUtils.getIndexObject(xmlNames,tagName);
  	}

  	/** Returns an instance of a TLC by its description. */
  	public TLController genTLC (String tlcDesc) throws InfraException
	{	return getInstanceForLoad(getId(tlcDesc));
	}

	public TLController genTLC(int cat, int tlc) throws InfraException
	{
		return getInstanceForLoad(categoryTLCs[cat][tlc]);
	}

  	/** Gets a new instance of an algorithm by its number. This method
    	  * is meant to be used for loading.
   	*/
	public TLController getInstanceForLoad (int algoId) throws InfraException
	{
		switch (algoId) {
			case RANDOM : return (random != null ? new RandomTLC(infra, random): new RandomTLC(infra));
			case LONGEST_QUEUE : return new LongestQueueTLC(infra);
			case MOST_CARS : return new MostCarsTLC(infra);
			case BEST_FIRST : return new BestFirstTLC(infra);
			case RELATIVE_LONGEST_QUEUE : return new RelativeLongestQueueTLC(infra);
			case RLD : return new RLDTLC(infra);
			case RLD2: return new RLD2TLC(infra);
			case TC_1OPT : return new TC1TLCOpt(infra);
			case TC_2OPT : return new TC2TLCOpt(infra);
			case TC_3OPT: return new TC3Opt(infra);
			case TC1_B1: return new TC1B1(infra);
			case TC2_B1: return new TC2B1(infra);
			case TC3_B1: return new TC3B1(infra);
			case TC_1_DESTLESS: return new TC1TLCDestless(infra);
			case TC_2_DESTLESS: return new TC2TLCDestless(infra);
			case TC_3_WORKINPROGRESS: return new TC3TLCWorkInProgress(infra);
			case ACGJ_1 : return new ACGJ1(infra);
			case ACGJ_2 : return new ACGJ2(infra);
			case ACGJ_3 : return new ACGJ3(infra);
			case ACGJ_3_FV : return new ACGJ3FixedValue(infra);
			case ACGJ_4 : return new ACGJ4(infra);
			case ACGJ_5 : return new ACGJ4(infra);
			case LOCAL : return new LocalHillTLC(infra);
			case GENNEURAL : return new GenNeuralTLC(infra);
			case TC_2FINAL: return new TC2Final(infra);
			case TC1_FIX: return new TC1TLCFix(infra);
			case RLSARSA1: return new SL1TLC(infra);
			case RLSARSA2: return new SL2TLC(infra);
			case RLSARSA3: return new SL3TLC(infra);
			case RLSARSA4: return new SL4TLC(infra);
			case RLSARSA5: return new SL5TLC(infra);
			case RLSARSA6: return new SL6TLC(infra);
                            
                        //
                        case FIXEDCYCLE: return new FixedCycle(infra);
                        case PERCENTAGE: return new PercentageQue(infra);
                        case TESTDEBUG: return new TestDebug(infra);
                        case TESTDEBUG2SEGMENT: return new TestDebug_2segment(infra);
                        //
		}
	   	throw new InfraException
    			("The TLCFactory can't make TLC's of type "+algoId);
	}
}