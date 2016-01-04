
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
  
public class ACGJ5 extends TLController implements InstantiationAssistant,XMLSerializable
{
	/** A constant for the number of steps an individual may run */
	protected static int NUM_STEPS = 500;
	/** A constant for the chance that genes cross over */
	protected static float CROSSOVER_CHANCE = 0.1f;
	/** A constant for the chance that mutation occurs */
	protected static float MUTATION_CHANCE = 0.001f;
	protected static float ELITISM_FACTOR = 0.1f;
	/** A constant for the number of individuals */
	protected static int NUMBER_INDIVIDUALS = 20;
	/** A constant for the number of generations */
	protected static int NUMBER_GENERATIONS = 100;
	/** counter for the number of steps the simulation has ran yet */
	protected int num_step;
	/** counter for how many roadusers had to wait a turn */
	protected int num_wait;
	/** counter for the number of roadusers that did move */
	protected int num_move;
	/** counter for the number of roadusers that did move */
	protected int num_nodes;
	/** the current TLController being used to determine gains */
	protected TLController tlc;
	/** the current Individual that is running  */
	protected Individual ind;
	/** The Population of Individuals */
	protected Population pop;
	/** The pseudo-random number generator for generating the chances that certain events will occur */
	protected Random random;
	
	// Stuff for our XML parser
	protected final static String shortXMLName="tlc-acgj5";
	protected InstantiationAssistant assistant=this;
	
	/**
	 * Creates a new ACGJ3 Algorithm.
	 * This TLC-algorithm is using genetic techniques to find an optimum in calculating the
	 * gains for each trafficlight. 
	 * Till date it hasnt functioned that well. Rather poorly, to just say it.
	 * 
	 * @param i The infrastructure this algorithm will have to operate on
	 */
	public ACGJ5(Infrastructure i)
	{	random = new Random();
		tlc = new BestFirstTLC(i);
		setInfrastructure(i);
	}
	
	/**
	 * Changes the Infrastructure this algorithm is working on
	 * 
	 * @param i The new infrastructure for which the algorithm has to be set up
	 */
	public void setInfrastructure(Infrastructure _i)
	{	super.setInfrastructure(_i);
		tlc.setInfrastructure(_i);
		infra = _i;
		num_nodes = tld.length;
		pop = new Population(_i);
		ind = pop.getIndividual();
		num_wait = 0;
		num_move = 0;
		num_step = 0;
	}
	
	/**
	 * Calculates how every traffic light should be switched
	 * @return Returns a double array of TLDecision's. These are tuples of a TrafficLight and the gain-value of when it's set to green.
	 * @see gld.algo.tlc.TLDecision
	 */	
	public TLDecision[][] decideTLs()
	{	if(num_step==NUM_STEPS) {
			pop.getNextIndividual(num_wait,num_move);
			System.out.println("New Individual gotten, previous: (wait,move) ("+num_wait+","+num_move+")");
			num_wait = 0;
			num_step = 0;
			num_move = 0;
		}
		
		tld = tlc.decideTLs();
		for(int i=0;i<num_nodes;i++) {
			int num_tl = tld[i].length;
			for(int j=0;j<num_tl;j++) {
				float gain = (ind.getFactor(i,j))*tld[i][j].getGain();
	    		if(trackNode!=-1)
					if(i==trackNode) {
						Drivelane currentlane = tld[i][j].getTL().getLane();
						boolean[] targets = currentlane.getTargets();
						System.out.println("N:"+i+" L:"+j+" G:"+gain+" <:"+targets[0]+" |:"+targets[1]+" >:"+targets[2]+" W:"+currentlane.getNumRoadusersWaiting());
					}
				tld[i][j].setGain(gain);
			}
		}
		
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
			num_wait += _ru.getSpeed();
		}
		else if(_prevsign != _signnow && _signnow !=null && _prevsign instanceof TrafficLight) {
			//clearly passed a trafficlight.
			num_move += _ru.getSpeed();
		}
		else {
			// Roaduser did move
			if(_prevsign != _signnow) {
				// Passed a Sign, thus moved max.
				num_move += _ru.getSpeed();
			}
			else {
				// Didnt pass a Sign, so might've moved lessThanMax
				num_move += (_prevpos - _posnow);
				num_wait += _ru.getSpeed() - (_prevpos - _posnow);
			}
		}
	}
	
	protected class Population implements XMLSerializable
	{
		/** the ID of the current Individual that's showing off it's coolness */
		protected int this_ind;
		/** a counter for the current generation of Individuals */
		protected int this_gen;
		/** The Individuals in this population */
		protected Individual[] inds;
		
		/**
		 * Creates a new population of Individuals
		 * 
		 * @param infra The Infrastructure the population will run on
		 * @param _num_ind the number of individuals in the population
		 * @param _num_gen the number of generations there should be evolved over
		 * @param cross the chance that crossover occurs at reproduction
		 * @param mut the chance that mutation occurs at reproduction
		 */
		protected Population(Infrastructure infra)
		{	inds = new Individual[NUMBER_INDIVIDUALS];
			for(int i=0;i<NUMBER_INDIVIDUALS;i++)
				inds[i] = new Individual(infra);
			this_ind = 0;
			this_gen = 0;
		}
		
		protected Population()
		{ 	// Empty constructor for loading
		}
		
		/** @return the number of the current generation of individuals */
		protected int getCurrentGenerationNum() {	return this_gen;		}
		/** @return the current individual */
		protected Individual getIndividual()	{	return inds[this_ind];	}
		
		/**
		 * @param perf The performance of the current Individual
		 * @return the next ACGJ3Individual. Either one of this generation,
		 * or if all individuals in this generation have been used,
		 * it evolves into a new generation and returns a new Individual from that generation.
		 */
		protected Individual getNextIndividual(int wait, int move) {
			inds[this_ind].setWait(wait);
			inds[this_ind].setMove(move);
			
			this_ind++;
			if(this_ind>=NUMBER_INDIVIDUALS) {
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
		protected Individual[] sortIndsArr(Individual[] ar)
		{	int num_ar = ar.length;
			Individual temp;
			for(int j=0;j<num_ar-1;j++)
				for(int i=0;i<num_ar-1-j;i++)
					if(ar[i].getFitness()<ar[i+1].getFitness()) {
						temp	= ar[i+1];
						ar[i+1]	= ar[i];
						ar[i]	= temp;
					}
			return ar;
		}
		
		/**
		 * Calculates the fitness of this Individual
		 * 
		 * @param ind the individual of which the fitness has to be calculated
		 * @return the fitness of this individual
		 */
		protected float calcFitness(Individual ind) {
			double w = (double) ind.getWait();
			double g = (double) ind.getMove();
			return (float) (g/(w+g));
		}
		
		/**
		 * Mates two Individuals creating two new Individuals
		 * 
		 * @param ma The mamma-Individual
		 * @param pa The pappa-Individual
		 * @return an array of length 2 of two newborn Individuals
		 */
		protected Individual[] mate(Individual ma, Individual pa) 
		{	float[] ch1 = getNewGenes(ma,pa);
			float[] ch2 = getNewGenes(pa,ma);
			
			Individual[] kids = {new Individual(ch1),new Individual(ch2)};
			return kids;
		}
		
		/**
		 * Evolves the current generation of Individuals in the Population into a new one.
		 */
		protected void evolve()
		{	if(this_gen < NUMBER_GENERATIONS) {
				// Create a new generation.
				float avg_fit=Float.MIN_VALUE, max_fit=Float.MIN_VALUE, sum_fit=0;
				float tot_wait=0, avg_wait=0, tot_move=0, avg_move=0;

				for(int i=0;i<NUMBER_INDIVIDUALS;i++) {
					float fit = calcFitness(inds[i]);
					inds[i].setFitness(fit);
					sum_fit	 += fit;
					max_fit   = fit>max_fit ? fit : max_fit;
					tot_wait += inds[i].getWait();
					tot_move += inds[i].getMove();
				}
				
				avg_wait = tot_wait/NUMBER_INDIVIDUALS;
				avg_move = tot_move/NUMBER_INDIVIDUALS;
				avg_fit  = sum_fit/NUMBER_INDIVIDUALS;
				
				System.out.println("Stats of prev. gen: (a-wait,a-move)=("+avg_wait+","+avg_move+")"+" (a-fit,mx-fit)=("+avg_fit+","+max_fit+")");
				System.out.println("Evolving...");
				
				// Sorts the current Individual-array on fitness, descending
				inds = sortIndsArr(inds);
				
				int num_mating = (int) Math.floor(NUMBER_INDIVIDUALS*(1-ELITISM_FACTOR));
				int num_elitism = (int) Math.ceil(NUMBER_INDIVIDUALS*ELITISM_FACTOR);
				if(num_mating%2!=0) {
					num_mating--;
					num_elitism++;
				}
				
				Individual[] newInds = new Individual[NUMBER_INDIVIDUALS];
				
				// Tournament selection
				for(int i=0;i<num_mating;i+=2) {
					Individual mamma=null, pappa=null;
					float chn_mum = random.nextFloat();
					float chn_pap = random.nextFloat();
					float fit_mum, sum_fit2=0, sum_fit3=0;
					int index_mum = -1;
					
					for(int j=0;j<NUMBER_INDIVIDUALS;j++) {
						sum_fit2 += (inds[j].getFitness()/sum_fit);
						if(chn_mum <= sum_fit2) {
							mamma = inds[j];
							index_mum = j;
							fit_mum = mamma.getFitness();
							sum_fit2 = sum_fit-fit_mum;
							break;
						}
					}
					
					for(int j=0;j<NUMBER_INDIVIDUALS;j++) {
						if(j!=index_mum) {
							sum_fit3 += (inds[j].getFitness()/sum_fit2);
							if(chn_pap <= sum_fit3) {
								pappa = inds[j];
								break;
							}
						}
					}
					//System.out.println("Mating...: "+mamma.getFitness()+","+pappa.getFitness());
					Individual[] kids = mate(mamma, pappa);
					newInds[i]	= kids[0];
					newInds[i+1]= kids[1];
				}
				
				// Elitism
				for(int i=0;i<num_elitism;i++) {
					newInds[i+num_mating] = inds[i];
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

		/** Returns some reproduction genes from this ACGJ3Individual. */
		protected float[] getNewGenes(Individual ma, Individual pa) {
			float[] ma_genes = ma.getGenes();
			float[] pa_genes = pa.getGenes();
			int num_genes = ma_genes.length;
			float[] new_genes = new float[num_genes];
			
			for(int i=0;i<num_genes;i++)
				new_genes[i] = ma_genes[i];
			
			/* Crossover? */
			int l_bound = 0, r_bound = num_genes-1;
			if(random.nextFloat() < CROSSOVER_CHANCE) {
				// We gonna do CrossssOver!
				while(l_bound < r_bound) {
					float diff = r_bound - l_bound;
					diff = diff/2;
					int dif = (int) Math.ceil(diff);
					if(random.nextBoolean())
						r_bound = r_bound - dif;
					else
						l_bound = l_bound + dif;
				}
				for(int i=l_bound;i<num_genes-1;i++)
					new_genes[i] = pa_genes[i];
			}
			
			/* Mutation? */
			for(int i=0;i<num_genes;i++)
				for(int j=0;j<8;j++)
					if(random.nextFloat()<MUTATION_CHANCE)
						new_genes[i] = random.nextFloat();
			return new_genes;
		}
		
		// XMLSerializable implementation of ACGJ3Population
		public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
		{	this_ind=myElement.getAttribute("this-ind").getIntValue();
			this_gen=myElement.getAttribute("this-gen").getIntValue();
			inds=(Individual[])XMLArray.loadArray(this,loader,assistant);
		}

		public XMLElement saveSelf () throws XMLCannotSaveException
		{ 	XMLElement result=new XMLElement("population");
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
	
	protected class Individual implements XMLSerializable
	{	protected float[] genes;
		protected float[][] me;
		protected float fitness;
		protected int wait;
		protected int move;

		//XML Parser stuff
		protected String myParentName="model.tlc.population";
		
		/** Creates a new Individual, providing the Infrastructure it should run on */
		protected Individual(Infrastructure infra)
		{	int num_genes = 0;
			for(int i=0;i<num_nodes;i++) {
				num_genes += tld[i].length;
			}
			
			genes = new float[num_genes];
			for(int i=0;i<num_genes;i++)
				genes[i] = random.nextFloat();
				
			createMe();
			wait = 0;
			move = 0;
		}
		
		/** Creates a new Individual, providing the reproduction genes from daddy and mummy */		
		protected Individual(float[] _genes)
		{	genes = _genes;
			createMe();
			wait = 0;
			move = 0;
		}

		/** Constructor for loading */
		protected Individual ()
		{
		}

		protected void createMe() {
			me = new float[num_nodes][];
			int subtot = 0;
			for(int i=0;i<num_nodes;i++) {
				me[i] = new float[tld[i].length];
				for(int j=0;j<tld[i].length;j++) {
					me[i][j] = genes[subtot];
					subtot++;
				}
			}
		}

		/** Resets the buckets and waiting values for this Individual */
		protected void reset() {
		}
		
		/** Returns the genes of this Individual */
		protected float[] getGenes() {
			return genes;
		}

		/* Returns the Fitness of this individual as calculated elsewhere */
		protected float getFitness()			{	return fitness;		}
		/* Sets the Fitness of this individual */
		protected void setFitness(float fit)	{	fitness = fit;		}
		/* Sets the number of Roadusers this Individual made waiting */		
		protected void setWait(int _wait)		{	wait = _wait;		}
		/* Returns the number of Roadusers I caused to wait */
		protected int getWait() 				{	return wait;		}
		/* Sets the number of Roadusers this Individual made moving */
		protected void setMove(int _move)		{	move = _move;		}
		/* Returns the number of Roadusers I caused to move */
		protected int getMove() 				{	return move;		}
		
		/**
		 * Calculates the gain-factor for the given TrafficLight
		 * 
		 * @param node the Id of the Node this TrafficLight belongs to
		 * @param tl The position of the TrafficLight in the TLDecision[][]
		 * @return the factor that should be used to calculate the real gain-value
		 */
		protected float getFactor(int node, int tl) {
			return me[node][tl];
		}
		
		
		// XMLSerializable implementation of ACGJ3Individual
		public void load (XMLElement myElement,XMLLoader loader) throws XMLTreeException,IOException,XMLInvalidInputException
		{	wait=myElement.getAttribute("wait").getIntValue();
			move=myElement.getAttribute("move").getIntValue();
			fitness=myElement.getAttribute("fitness").getFloatValue();
			genes=(float[])XMLArray.loadArray(this,loader);
			me=(float[][])XMLArray.loadArray(this,loader);
		}

		public XMLElement saveSelf () throws XMLCannotSaveException
		{ 	XMLElement result=new XMLElement("individual");
			result.addAttribute(new XMLAttribute("wait",wait));
			result.addAttribute(new XMLAttribute("move",move));
			result.addAttribute(new XMLAttribute("fitness",fitness));
			return result;
		}
  
		public void saveChilds (XMLSaver saver) throws XMLTreeException,IOException,XMLCannotSaveException
		{ 	XMLArray.saveArray(genes,this,saver,"genes");
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
		pop=new Population();
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
	{ 	if (Population.class.equals(request))
		{  return new Population();
		}
 		else if (Individual.class.equals(request))
		{  return new Individual();
		}		
		else
		{ throw new ClassNotFoundException
		  ("ACGJ5 InstantiationAssistant cannot make instances of "+
		   request);
		}
	}	
	
	public boolean canCreateInstance (Class request)
	{ 	return Population.class.equals(request) ||
	        	 Individual.class.equals(request);
	}
}