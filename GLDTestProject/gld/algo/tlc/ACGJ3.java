
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
import java.util.Random;
import java.util.*;
import java.awt.Point;
  
public class ACGJ3 extends TLController implements InstantiationAssistant,XMLSerializable
{
	/** A constant for the number of steps an individual may run */
	protected static int NUM_STEPS = 400;
	/** A constant for the chance that genes cross over */
	protected static float CROSSOVER_CHANCE = 0.1f;
	/** A constant for the chance that mutation occurs */
	protected static float MUTATION_CHANCE = 0.001f;	
	/** A constant for the number of individuals */
	protected static int NUMBER_INDIVIDUALS = 50;
	/** Used constants for array addressing */
	protected static int BUCK = 3,
	                     WAIT = 2,
	                     DECF = 1,
	                     RUWF = 0;	                     
	
	/** counter for the number of steps the simulation has ran yet */
	protected int num_step;
	/** counter for how many roadusers had to wait a turn */
	protected int num_wait;
	/** counter for the number of roadusers that did move */
	protected int num_move;
	/** counter for the number of roadusers that did move */
	protected int num_nodes;
	/** the current ACGJ3Individual that is running  */
	protected ACGJ3Individual ind;
	/** The Population of ACGJ3Individuals */
	protected ACGJ3Population pop;
	/** The pseudo-random number generator for generating the chances that certain events will occur */
	protected Random random;
	
	// Stuff for our XML parser
	protected final static String shortXMLName="tlc-acgj3";
	protected InstantiationAssistant assistant=this;
	
	/**
	 * Creates a new ACGJ3 Algorithm.
	 * This TLC-algorithm is using genetic techniques to find an optimum in calculating the
	 * gains for each trafficlight. 
	 * Till date it hasnt functioned that well. Rather poorly, to just say it.
	 * 
	 * @param i The infrastructure this algorithm will have to operate on
	 */
	public ACGJ3(Infrastructure i) 
	{	random = new Random();
		setInfrastructure(i);
	}
	
	/**
	 * Changes the Infrastructure this algorithm is working on
	 * 
	 * @param i The new infrastructure for which the algorithm has to be set up
	 */
	public void setInfrastructure(Infrastructure _i)
	{	super.setInfrastructure(_i);
		infra = _i;
		num_nodes = tld.length;
		pop = new ACGJ3Population(_i, NUMBER_INDIVIDUALS, 100,CROSSOVER_CHANCE,MUTATION_CHANCE);
		ind = pop.getIndividual();
		num_wait = 0;
		num_step = 0;
	}
	
	/**
	 * Calculates how every traffic light should be switched
	 * @return Returns a double array of TLDecision's. These are tuples of a TrafficLight and the gain-value of when it's set to green.
	 * @see gld.algo.tlc.TLDecision
	 */	
	public TLDecision[][] decideTLs()
	{
		if(num_step==NUM_STEPS) {
			pop.getNextIndividual(num_wait,num_move);
			System.out.println("Next Individual gotten, previous:(wait,move) ("+num_wait+","+num_move+")");
			num_wait = 0;
			num_step = 0;
			num_move = 0;
		}
		for(int i=0;i<num_nodes;i++) {
			int num_tl = tld[i].length;
			for(int j=0;j<num_tl;j++) {
				float q = ind.getQValue(i,j);
	    		if(trackNode!=-1)
				if(i==trackNode) {
					Drivelane currentlane = tld[i][j].getTL().getLane();
					boolean[] targets = currentlane.getTargets();
					System.out.println("node: "+i+" light: "+j+" gain: "+q+" "+targets[0]+" "+targets[1]+" "+targets[2]+" "+currentlane.getNumRoadusersWaiting());
				}
				if(q==Float.NaN) {
					System.out.println("NaN!");
				}
				else if(q==Float.POSITIVE_INFINITY) {
					System.out.println("Too big. I think.");
				}
				else if(q>=100000)
					System.out.println("Too big.");

				tld[i][j].setGain(q);
			}
		}
		
		//System.out.println("DoingACGJ3: num_wait:"+num_wait+" num-move:"+num_move);
		num_step++;
		return tld;
	}

	/** Resets the algorithm */
	public void reset() {
		ind.reset();
	}

	/**
	 * Provides the TLC-algorithm with information about a roaduser that has had it's go in the moveAllRoaduser-cycle.
	 * From this information it can be distilled whether a roaduser has moved or had to wait.
	 * 
	 * @param _ru
	 * @param _prevlane
	 * @param _prevsign
	 * @param _prevpos
	 * @param _dlanenow
	 * @param _signnow
	 * @param _posnow
	 * @param _possiblelanes
	 * @param _ranges
	 */
	public void updateRoaduserMove(Roaduser _ru, Drivelane _prevlane, Sign _prevsign, int _prevpos, Drivelane _dlanenow, Sign _signnow, int _posnow, PosMov[] posMovs, Drivelane _desired)
	{
		if(_prevsign == _signnow && _prevpos == _posnow) {
			// Previous sign is the same as the current one
			// Previous position is the same as the previous one
			// So, by definition we had to wait this turn. bad.
			//System.out.println("ACGJ3.updateRoaduserMove(..) Roaduser waiting.");
			num_wait += _ru.getSpeed();
			
			if(_prevsign.getType()==Sign.TRAFFICLIGHT && _prevsign.mayDrive() == true &&
			   _desired != null && _desired.getSign().getType()==Sign.TRAFFICLIGHT &&
			   _desired.getNodeLeadsTo().getType()==Node.JUNCTION) 
			{
				ind.siphonToOtherBucket((TrafficLight) _signnow, (TrafficLight) _desired.getSign());
			}
		}
		else if(_prevsign != _signnow && _signnow !=null && _prevsign instanceof TrafficLight) {
			//clearly passed a trafficlight.
			//lets empty the bucket value a bit.
			//System.out.println("ACGJ3.updateRoaduserMove(..) Roaduser crossed Node.");
			ind.relaxBucket(_prevsign.getNode().getId(),(TrafficLight)_prevsign);
			num_move += _ru.getSpeed();
		}
		else {
			// Roaduser did move
			if(_prevsign != _signnow) {
				// Passed a Sign, thus moved max.
				//System.out.println("ACGJ3.updateRoaduserMove(..) Roaduser crossed.");
				num_move += _ru.getSpeed();
			}
			else {
				// Didnt pass a Sign, so might've moved lessThanMax
				int old_move = num_move;
				num_move += (_prevpos - _posnow);
				num_wait += _ru.getSpeed() - (_prevpos - _posnow);
				//System.out.println("ACGJ3.updateRoaduserMove(..) Roaduser moved some. num_move("+num_move+","+old_move+")");
			}
		}
	}
	
	protected class ACGJ3Population implements XMLSerializable
	{

		/**
		 * the chance that mutation occurs when evolving. Set via Constructor of ACGJ3Population
		 */
		protected float mutate;

		/**
		 * the chance that crossover occurs at evolving.
		 */
		protected float crossover;

		/**
		 * the current number of individuals in this population
		 */
		protected int num_ind;

		/**
		 * the total amount of generations this population should evolve over
		 */
		protected int num_gen;

		/**
		 * the current Individual that's showing off it's coolness
		 */
		protected int this_ind;

		/**
		 * a counter for the current generation of Individuals
		 */
		protected int this_gen;
		protected ACGJ3Individual[] inds;
		
		/**
		 * Creates a new population of ACGJ3Individuals
		 * 
		 * @param infra The Infrastructure the population will run on
		 * @param _num_ind the number of individuals in the population
		 * @param _num_gen the number of generations there should be evolved over
		 * @param cross the chance that crossover occurs at reproduction
		 * @param mut the chance that mutation occurs at reproduction
		 */
		protected ACGJ3Population(Infrastructure infra, int _num_ind, int _num_gen, float cross, float mut)
		{
			mutate = mut;
			crossover = cross;
			num_ind = _num_ind;
			num_gen = _num_gen;
			inds = new ACGJ3Individual[_num_ind];
			for(int i=0;i<_num_ind;i++) {
				inds[i] = new ACGJ3Individual(infra);
			}
			this_ind = 0;
			this_gen = 0;
		}
		
		protected ACGJ3Population()
		{ 	// Empty constructor for loading
		}
		
		/**
		 * @return the number of the current generation of individuals
		 */
		protected int getCurrentGenerationNum() {
			return this_gen;
		}
		
		/**
		 * @return the current individual
		 */
		protected ACGJ3Individual getIndividual() {
			return inds[this_ind];
		}
		
		/**
		 * @param perf The performance of the current Individual
		 * @return the next ACGJ3Individual. Either one of this generation,
		 * or if all individuals in this generation have been used,
		 * it evolves into a new generation and returns a new Individual from that generation.
		 */
		protected ACGJ3Individual getNextIndividual(int wait, int move) {
			inds[this_ind].setWait(wait);
			inds[this_ind].setMove(move);
			
			this_ind++;
			if(this_ind>=num_ind) {
				evolve();
				this_ind = 0;
			}
			return inds[this_ind];
		}
		
		/**
		 * BubbleSorts an array of ACGJ3Individuals on the parameter of performance
		 * 
		 * @param ar the array to be sorted
		 * @return the sorted array
		 */
		protected ACGJ3Individual[] sortIndsArr(ACGJ3Individual[] ar)
		{
			int num_ar = ar.length;
			for(int j=0;j<num_ar-1;j++) {
				for(int i=0;i<num_ar-1-j;i++) {
					if(ar[i].getFitness()<ar[i+1].getFitness()) {
						ACGJ3Individual temp = ar[i+1];
						ar[i+1] = ar[i];
						ar[i] = temp;
					}
					
				}
			}
			return ar;
		}
		
		/**
		 * Calculates the fitness of this Individual
		 * 
		 * @param ind the individual of which the fitness has to be calculated
		 * @return the fitness of this individual
		 */
		protected float calcFitness(ACGJ3Individual ind) {
			float w = (float) ind.getWait();
			float g = (float) ind.getMove();
			return g/(w+g);
		}
		
		/**
		 * Mates two ACGJ3Individuals creating two new ACGJ3Individuals
		 * 
		 * @param ma The mamma-ACGJ3Individual
		 * @param pa The pappa-ACGJ3Individual
		 * @return an array of length 2 of two newborn ACGJ3Individuals
		 */
		protected ACGJ3Individual[] mate(ACGJ3Individual ma, ACGJ3Individual pa) 
		{
			System.out.println("Mating ("+ma.getWait()+","+ma.getMove()+") and ("+pa.getWait()+","+pa.getMove()+")");
			
			byte[] ma1 = ma.getReproGenes(crossover,mutate);
			byte[] pa1 = pa.getReproGenes(crossover,mutate);
			byte[] ma2 = ma.getReproGenes(crossover,mutate);
			byte[] pa2 = pa.getReproGenes(crossover,mutate);
			
			ACGJ3Individual[] kids = {new ACGJ3Individual(ma1,pa1),new ACGJ3Individual(ma2,pa2)};
			return kids;
		}
		
		/**
		 * Evolves the current generation of ACGJ3Individuals in the ACGJ3Population into a new one.
		 */
		protected void evolve()
		{
			if(this_gen<num_gen) {
				// Create a new generation.
				float[] fitnesses = new float[num_ind];
				for(int i=0;i<num_ind;i++) {
					inds[i].setFitness(calcFitness(inds[i]));
				}
				
				float totfit=0;
				float avgfit=Float.MIN_VALUE;
				float maxfit=-1;
				float totwait=0;
				float avgwait=0;
				float totmove=0;
				float avgmove=0;
				
				for(int i=0;i<num_ind;i++) {
					float fit = inds[i].getFitness();
					totfit+=fit;
					if(fit>maxfit)
						maxfit = fit;
					int wait = inds[i].getWait();
					int move = inds[i].getMove();
					totwait+=wait;
					totmove+=move;
				}
				avgwait = totwait/num_ind;
				avgmove = totmove/num_ind;
				avgfit = totfit/num_ind;
				
				System.out.println("Previous gen stats ("+avgwait+","+avgmove+")"+" (afit,mxfit):("+avgfit+","+maxfit+")");
				System.out.println("Evolving...");
				
				inds = sortIndsArr(inds);
				
				ACGJ3Individual i1 = inds[0];
				ACGJ3Individual i2 = inds[1];
				ACGJ3Individual i3 = inds[2];
				
				int normal_mating = (int) Math.floor(num_ind*0.8);
				int succession_ma = (int) Math.ceil(num_ind*0.2);
				if(normal_mating%2!=0) {
					normal_mating--;
					succession_ma++;
				}
				
				ACGJ3Individual[] newInds = new ACGJ3Individual[num_ind];
				
				for(int i=0;i<normal_mating;i+=2) {
					ACGJ3Individual[] kids = mate(inds[i],inds[i+1]);
					newInds[i] = kids[0];
					newInds[i+1] = kids[1];
				}
				// Weird, I know. But I like it :)
				for(int i=normal_mating;i<num_ind;i++) {
					newInds[i] = inds[i-normal_mating];
				}
				
				inds = newInds;								
				this_gen++;
			}
			else {
				System.out.println("Done with evolving");
				// set the best individual as the ruling champ?
				// do nothing
				return;
			}				
		}
		
		// XMLSerializable implementation of ACGJ3Population
		
		public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
		{	mutate=myElement.getAttribute("mutate").getFloatValue();
			crossover=myElement.getAttribute("crossover").getFloatValue();
			num_ind=myElement.getAttribute("num-ind").getIntValue();
			num_gen=myElement.getAttribute("num-gen").getIntValue();
			this_ind=myElement.getAttribute("this-ind").getIntValue();
			this_gen=myElement.getAttribute("this-gen").getIntValue();
			inds=(ACGJ3Individual[])XMLArray.loadArray(this,loader,assistant);
		}

		public XMLElement saveSelf () throws XMLCannotSaveException
		{ 	XMLElement result=new XMLElement("population");
			result.addAttribute(new XMLAttribute("mutate",mutate));
			result.addAttribute(new XMLAttribute("crossover",crossover));
			result.addAttribute(new XMLAttribute("num-ind",num_ind));
			result.addAttribute(new XMLAttribute("num-gen",num_gen));
			result.addAttribute(new XMLAttribute("this-ind",this_ind));
			result.addAttribute(new XMLAttribute("this-gen",this_gen));
	  		return result;
		}
  
		public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
		{	XMLArray.saveArray(inds,this,saver,"individuals");
		}

		public String getXMLName ()
		{ 	return "model.tlc.population";
		}
		
		public void setParentName (String parentName_) throws XMLTreeException
		{ 	throw new XMLTreeException
				("Operation not supported. ACGJ3Population has a fixed parentname");
		}		
	}
	
	protected class ACGJ3Individual implements XMLSerializable
	{
		/**
		 * The Chromosomes as gotten from mama
		 */
		protected byte[] bytema;
		protected byte[] bytepa;

		/**
		 * The array of float values that describe this ACGJ3Individual. Hence the name of the array.
		 * Encoded are for every TrafficLight in the Infrastructure:
		 * a float value 'weight per waiting roaduser'
		 * a float value 'degrading/increasement weight per Roaduser waiting further up the Queue'
		 * a float value 'gain-bucket' in which all the weights of waiting Roadusers are collected
		 * a float value 'number of Roadusers waiting' which is needed to make decreasement of the gain-bucket possible
		 */
		protected float[][][] me;
		protected int wait;
		protected int move;
		protected float fitness;
		
		//XML Parser stuff
		protected String myParentName="model.tlc.population";
		
		/** Creates a new ACGJ3Individual, providing the Infrastructure it should run on */
		protected ACGJ3Individual(Infrastructure infra)
		{
			bytema = new byte[2*infra.getNumNodes()];
			bytepa = new byte[2*infra.getNumNodes()];
			random.nextBytes(bytema);
			random.nextBytes(bytepa);
			createMe();
			wait = 0;
			move = 0;
		}
		
		protected void createMe() {
			me = new float[infra.getNumNodes()][][];
			for(int i=0;i<infra.getNumNodes();i++) {
				me[i] = new float[tld[i].length][];
				for(int j=0;j<tld[i].length;j++) {
					//Qweight per waiting roaduser, degradingfunction factor, build-up Q
					me[i][j] = new float[4];
					float m0 = bytema[i*2]+128f;
					float p0 = bytepa[i*2]+128f;
					float m1 = bytema[(i*2)+1]+128f;
					float p1 = bytepa[(i*2)+1]+128f;
					
					me[i][j][RUWF] = (m0+p0)/256f;		// (((512)/2)==256)/64==4 -> between 0 and 4) weight per waiting roaduser
					me[i][j][DECF] = (m1+p1)/256f;	// (between 0 and 2) degrading factor
					me[i][j][WAIT] = 0;			// num_waiting
					me[i][j][BUCK] = 0;			// build up Q, aka bucket
				}
			}
		}
		
		/** Creates a new ACGJ3Individual, providing the reproduction genes from daddy and mummy */		
		protected ACGJ3Individual(byte[] ma, byte[] pa)
		{
			bytema = ma;
			bytepa = pa;
			createMe();
			wait = 0;
			move = 0;
		}
		
		/** Constructor for loading */
		protected ACGJ3Individual ()
		{	
		}

		/** Resets the buckets and waiting values for this Individual */
		protected void reset() {
			for(int i=0;i<me.length;i++) {
				for(int j=0;j<me[i].length;j++) {
					me[i][j][WAIT] = 0;
					me[i][j][BUCK] = 0;
				}
			}		
		}
		
		/** Returns the genes of this ACGJ3Individual */
		protected byte[][] getGenes() {
			byte[][] ret = {bytema,bytepa};
			return ret;
		}
		
		/** Returns some reproduction genes from this ACGJ3Individual. */
		protected byte[] getReproGenes(float cross, float mutate) {
			byte[] ar1,ar2;
			if(random.nextFloat() > 0.5f) {
				ar1 = bytema;
				ar2 = bytepa;
			}
			else {
				ar1 = bytepa;
				ar2 = bytema;
			}
			
			/* Crossover? */
			int num_genes = ar1.length;
			for(int i=0;i<num_genes;i++) {
				if(random.nextFloat() < cross) {
					int pos = (int) Math.floor(random.nextFloat()*8);
					int temp1 = (int) Math.pow(2,pos)-1;
					int temp2 = ar1[i]&temp1;
					int temp3 = ar2[i]&temp1;
					ar1[i] &= temp2;
					ar1[i] += temp3;
				}						
			}
			
			/* Mutation? */		
			for(int i=0;i<num_genes;i++) {
				for(int j=0;j<8;j++) {
					if(random.nextFloat()<mutate) {
						ar1[i] ^= (byte) Math.pow(2,j);;
					}
				}
			}	
			return ar1;
		}

		/**
		 * Returns the fitness of the current Individual, as was set at iterating through this generation
		 */
		/* Returns the Fitness of this individual as calculated elsewhere */
		protected float getFitness() {
			return fitness;
		}
		
		/**
		 * Sets the fitness of this Individual
		 * 
		 * @param fit
		 */
		/* Sets the Fitness of this individual */		
		protected void setFitness(float fit) {
			fitness = fit;
		}
		
		/**
		 * Sets the number of cars that this Individual caused to wait
		 * 
		 * @param _wait
		 */
		/* Sets the number of Roadusers this Individual made waiting */		
		protected void setWait(int _wait) {
			wait = _wait;
		}
		
		/**
		 * returns the number of Roadusers this Individual caused to wait
		 */
		/* Returns the number of Roadusers I caused to wait */
		protected int getWait() {
			return wait;
		}
		
		/**
		 * Sets the amount of Roadusers that did move during this Individual's lifespan
		 * 
		 * @param _move
		 */
		/* Sets the number of Roadusers this Individual made moving */
		protected void setMove(int _move) {
			move = _move;
		}
		
		/**
		 * Returns the amount of Roadusers that did move during this Individuals lifespan
		 */
		/* Returns the number of Roadusers I caused to move */
		protected int getMove() {
			return move;
		}
		
		/**
		 * Calculates the current gain of the given TrafficLight
		 * 
		 * @param node the Id of the Node this TrafficLight belongs to
		 * @param tl The position of the TrafficLight in the TLDecision[][]
		 * @return the gain-value of when this TrafficLight is set to green
		 */
		/* Returns the gain for the provided TrafficLight to be set to Green */
		protected float getQValue(int node, int tl) {
			int num_waiting = tld[node][tl].getTL().getLane().getNumRoadusersWaiting();
			me[node][tl][2] = num_waiting;
			
			float oldQ = me[node][tl][BUCK];
			float newQ = 0;
			float ruW = me[node][tl][RUWF];
			float dFS = me[node][tl][DECF];
			float dec = 1;
			for(int i=0;i<num_waiting;i++) {
				newQ += ruW*dec;
				dec *= dFS;
			}
			if((oldQ+newQ)>=100000)
				me[node][tl][BUCK] = 10000;
			else
				me[node][tl][BUCK] = oldQ + newQ;
			
			return newQ + oldQ;
		}
		
		/**
		 * Empties the 'gain-value' bucket partly, which is being filled when Roadusers are 
		 * waiting/voting for their TrafficLight to be set to green.
		 * 
		 * @param node The Id of the Node of which a bucket has to be emptied a bit
		 * @param tl The TrafficLight of which a bucket has to be partly emptied in the TLDecision[][]
		 */
		/* Empties the build-up Gain as a roaduser left the scene */
		protected void relaxBucket(int node, TrafficLight tl) {
			int tli = -1;
			int num_tls = me[node].length;
			for(int i=0;i<num_tls;i++)
				if(tld[node][i].getTL()==tl) {
					tli = i;
					break;
				}
			float curWait = me[node][tli][WAIT];
			float curBuck = me[node][tli][BUCK];
			
			if(curWait>0) {
				me[node][tli][BUCK] = curBuck*((curWait-1)/curWait);
				me[node][tli][WAIT]--;
			}
			else {
				me[node][tli][BUCK] = 0;
				me[node][tli][WAIT] = 0;
			}
		}
		
		/**
		 * When a Roaduser may drive according to it's Sign, but cant as the desired Drivelane
		 * is full, (some/all) of the current gain-bucket of it's current TrafficLight is
		 * siphoned over to the desired Drivelane, making it more probable that that lane will
		 * move, making it possible for this Drivelane to move.
		 * 
		 * @param node the Id of the Node at which the Roaduser is waiting
		 * @param sign The TrafficLight the Roaduser is waiting for
		 * @param des The TrafficLight that controls the traffic on the desired Drivelane
		 */
		protected void siphonToOtherBucket(TrafficLight here, TrafficLight there)
		{
			int hereId = here.getId();
			int thereId = there.getId();
			
			int hereNodeId = here.getNode().getId();
			int thereNodeId = there.getNode().getId();
			
			int hereRelId = -1;
			int thereRelId = -1;
			
			TLDecision[] heretlds = tld[hereNodeId];
			int num_heretlds = heretlds.length;
			for(int i=0;i<num_heretlds;i++) {
				if(heretlds[i].getTL()==here)
					hereRelId = i;
			}
			
			TLDecision[] theretlds = tld[thereNodeId];
			int num_theretlds = theretlds.length;
			for(int j=0;j<num_theretlds;j++) {
				//System.out.println("Checking: "+there+"=="+tld[thereNodeId][j].getTL());
				if(theretlds[j].getTL()==there)
					thereRelId = j;
			}
			
			float buck = me[hereNodeId][hereRelId][BUCK];
			float curWait = me[hereNodeId][hereRelId][WAIT];
			if(curWait>0) {
				if(me[thereNodeId][thereRelId][BUCK] < 100000)
					me[thereNodeId][thereRelId][BUCK] += buck/curWait;			 // Aka: make sure that lane gets mmmmooving!
			}
			else {
				//System.out.println("Something weird.");
				me[hereNodeId][hereRelId][BUCK] = 0;
				me[hereNodeId][hereRelId][WAIT] = 0;
			}
		}
		
		// XMLSerializable implementation of ACGJ3Individual
		
		public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
		{	wait=myElement.getAttribute("wait").getIntValue();
			move=myElement.getAttribute("move").getIntValue();
			fitness=myElement.getAttribute("fitness").getFloatValue();
			bytepa=(byte[])XMLArray.loadArray(this,loader);
			bytema=(byte[])XMLArray.loadArray(this,loader);
			me=(float[][][])XMLArray.loadArray(this,loader);
		}

		public XMLElement saveSelf () throws XMLCannotSaveException
		{ 	XMLElement result=new XMLElement("individual");
			result.addAttribute(new XMLAttribute("wait",wait));
			result.addAttribute(new XMLAttribute("move",move));
			result.addAttribute(new XMLAttribute("fitness",fitness));
			return result;
		}
  
		public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
		{ 	XMLArray.saveArray(bytepa,this,saver,"pa");
			XMLArray.saveArray(bytema,this,saver,"ma");
			XMLArray.saveArray(me,this,saver,"me");
		}

		public String getXMLName ()
		{ 	return myParentName+".individual";
		}
		
		public void setParentName (String parentName) throws XMLTreeException
		{ 	myParentName=parentName;
		}
	}
	
	public void showSettings(Controller c)
	{
		String[] descs = {"# of steps an individual may run", "Number of Individuals in Population", "Crossover chance", "Mutation chance"};
		int[] ints = {NUM_STEPS, NUMBER_INDIVIDUALS};
		float[] floats = {CROSSOVER_CHANCE, MUTATION_CHANCE};
		TLCSettings settings = new TLCSettings(descs, ints, floats);
		
		settings = doSettingsDialog(c, settings);

		NUM_STEPS = settings.ints[0];
		NUMBER_INDIVIDUALS = settings.ints[1];
		CROSSOVER_CHANCE = settings.floats[0];
		MUTATION_CHANCE = settings.floats[1];
		setInfrastructure(infra);
	}
	
	// XMLSerializable implementation
	
	public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
	{	super.load(myElement,loader);
		NUM_STEPS=myElement.getAttribute("s-num-steps").getIntValue();
		CROSSOVER_CHANCE=myElement.getAttribute("co-prob").getFloatValue();
		MUTATION_CHANCE=myElement.getAttribute("mut-prob").getFloatValue();				
		NUMBER_INDIVIDUALS=myElement.getAttribute("num-ind").getIntValue();
		num_step=myElement.getAttribute("o-num-steps").getIntValue();
		num_wait=myElement.getAttribute("num-wait").getIntValue();
		num_move=myElement.getAttribute("num-move").getIntValue();
		System.out.println("LoadingACGJ3: num_wait:"+num_wait+" num-move:"+num_move);
		pop=new ACGJ3Population();
		loader.load(this,pop);
		ind=pop.inds[myElement.getAttribute("ind-index").getIntValue()];
	}

	public XMLElement saveSelf () throws XMLCannotSaveException
	{ 	XMLElement result=super.saveSelf();
		result.setName(shortXMLName);	
		result.addAttribute(new XMLAttribute("s-num-steps",NUM_STEPS));
		result.addAttribute(new XMLAttribute("co-prob",CROSSOVER_CHANCE));
		result.addAttribute(new XMLAttribute("mut-prob",MUTATION_CHANCE));
		result.addAttribute(new XMLAttribute("num-ind",NUMBER_INDIVIDUALS));
		result.addAttribute(new XMLAttribute("o-num-steps",num_step));
		result.addAttribute(new XMLAttribute("num-wait",num_wait));
		result.addAttribute(new XMLAttribute("num-move",num_move));
		System.out.println("SavingACGJ3: num_wait:"+num_wait+" num-move:"+num_move);
		result.addAttribute(new XMLAttribute("ind-index",
					StringUtils.getIndexObject(pop.inds,ind)));
	  	return result;
	}
  
	public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
	{ 	super.saveChilds(saver);
		saver.saveObject(pop);
	}

	public String getXMLName ()
	{ 	return "model."+shortXMLName;
	}
	
	// InstantiationAssistant implementation
	
	public Object createInstance (Class request) throws 
	      ClassNotFoundException,InstantiationException,IllegalAccessException
	{ 	if (ACGJ3Population.class.equals(request))
		{  return new ACGJ3Population();
		}
 		else if (ACGJ3Individual.class.equals(request))
		{  return new ACGJ3Individual();
		}		
		else
		{ throw new ClassNotFoundException
		  ("ACGJ3 InstantiationAssistant cannot make instances of "+
		   request);
		}
	}	
	
	public boolean canCreateInstance (Class request)
	{ 	return ACGJ3Population.class.equals(request) ||
	        	 ACGJ3Individual.class.equals(request);
	}
}