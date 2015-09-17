/*
 * Copyright 2014 Elhuyar Fundazioa

This file is part of EliXa.

    EliXa is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    EliXa is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with EliXa.  If not, see <http://www.gnu.org/licenses/>.
 */


package elh.eus.absa;

import ixa.kaflib.KAFDocument;
import ixa.kaflib.Term;
import ixa.kaflib.Term.Sentiment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluator class: this class processes a corpus with a user specified lexicon and algorithm. Parameters: 
 *
 * <ol>
 * <li>Corpus: path to the file containing the corpus.</li>
 * <li>Lexicon: Lexicon to be used for looking up word polarities.</li>
 * <li>Algorithm: which algorithm should be user for evaluating document polarity scores </li>
 * <li>Lemma/sense mode: Whether lemmas or word senses are the basis of the polarity scores </li>
 * </ol>
 *
 *
 * @author isanvi
 * @author ragerri
 * @version 2015-02-14
 */
public class Evaluator {

	//private HashSet<LexiconEntry> lexicon = new HashSet<LexiconEntry> ();
	private Lexicon lexicon;
	private float threshold;
	private Map<String, Float> stats = new HashMap<String, Float>();
	private Map<String, String> kafResults = new HashMap<String, String>();
	private Map<String, String> ref_pols = new HashMap<String, String>();
	private Map<String, Float> ref_polCounts = new HashMap<String, Float>();
	private Map<String, Float> predicted_pols = new HashMap<String, Float>();
	private int synset = 0;	
	private String algorithm = "avg";
	
	
	/**
	 * Constructor: Lexicon object given.
	 * 
	 * @param Lex
	 * @param syn
	 */
	public Evaluator (Lexicon Lex, String syn)
	{
		this(Lex, syn, 0, "avg");
	}

	/**
	 * Constructor: Lexicon object given, threshold given.
	 * 
	 * @param Lex
	 * @param syn
	 * @param thresh
	 */
	public Evaluator (Lexicon Lex, String syn, float thresh)
	{
		this(Lex, syn, thresh, "avg");
	}

	/**
	 * Constructor: Lexicon object given, algorithm given.
	 * 
	 * @param Lex
	 * @param syn
	 * @param algorithm
	 */
	public Evaluator (Lexicon Lex, String syn, String algorithm)
	{
		this(Lex, syn, 0, algorithm);
	}

	/**
	 * Constructor: Lexicon object given.
	 * 
	 * @param Lex
	 * @param syn
	 * @param thresh
	 * @param algorithm
	 */
	public Evaluator (Lexicon Lex, String syn, float thresh, String algorithm)
	{
		this.lexicon = Lex;
		System.out.println("AvgRatioEstimator: lexicon loaded - "+lexicon.size()+" entries");
		this.setThreshold(thresh);
		this.setSynset(syn);	
		this.setAlgorithm(algorithm);
	}

	
	/**
	 * Constructor: Lexicon path given as a string, load lexicon into the lexicon variable.
	 * 
	 * @param Lex
	 * @param syn
	 */
	public Evaluator (File LexPath, String syn)
	{
		this(LexPath, syn, 0, "avg");
	}

	/**
	 * Constructor: Lexicon path given as a string, load lexicon into the lexicon variable.
	 * 
	 * @param Lex
	 * @param syn
	 * @param thresh
	 */
	public Evaluator (File LexPath, String syn, float thresh)
	{
		this(LexPath, syn, thresh, "avg");
	}

	/**
	 * Constructor: Lexicon path given as a string, load lexicon into the lexicon variable.
	 * 
	 * @param Lex
	 * @param syn
	 * @param algorithm
	 */
	public Evaluator (File LexPath, String syn, String algorithm)
	{
		this(LexPath, syn, 0, algorithm);
	}

	
	/**
	 * Constructor: Lexicon path given as a string, load lexicon into the lexicon variable.
	 * 
	 * @param Lex
	 * @param syn
	 * @param thresh
	 * @param algorithm
	 */
	public Evaluator (File LexPath, String syn, float thresh, String algorithm)
	{
		this.lexicon = new Lexicon(LexPath, syn);
		System.out.println("AvgRatioEstimator: lexicon loaded --> "+LexPath+" - "+lexicon.size()+" entries");
		System.err.println("AvgRatioEstimator: lexicon loaded --> "+LexPath+" - "+lexicon.size()+" entries");
		System.err.println("AvgRatioEstimator: lexicon loaded --> "+lexicon.getNumericPolarity("larri")+" proba");
		this.setThreshold(thresh);
		this.setSynset(syn);
		this.setAlgorithm(algorithm);
	}
	
	
	/**
	 * SetSynset function set the lemma/sense mode to be used when loading the lexicon.  
	 * 
	 * @param syn
	 */
	private void setSynset (String syn)
	{
		if (syn.compareTo("lemma") == 0)
			this.synset = 0;
		else if (syn.compareTo("first") == 0 || syn.compareTo("mfs") == 0)
			this.synset = 1;
		else if (syn.compareTo("rank") == 0)
			this.synset = 2;
		else
		{
			System.err.println("AvgRatioEstimator: incorrect sense/lemma option("+syn+"). System defaults to using lemmas\n");
			this.synset = 0;			
		}
		//System.err.println("AvgRatioEstimator: sense/lemma option: "+this.synset+".\n");
	}
	
	/**
	 * SetThreshold: set the minimum polarity score a lexicon entry should have in order 
	 * to be taken into account for processing the corpus. 
	 * 
	 * @param t : float value in [-1,1] for QWN-PPV lexicons. In custom lexicons are used this value
	 * 			   should be in the range of polarity scores in each specific lexicon. 
	 */
	public void setThreshold (float t)
	{
		this.threshold = t;
	}
	
	/**
	 * SetAlgorithm: set the minimum polarity score a lexicon entry should have in order to be included in the lexicon object.  
	 * 
	 * @param a: evaluation method used for computing the polarity scores [avg | moh]: "
	 *				- avg: average ratio of the polarity words in the text"
	 * 				- moh: polarity classifier proposed in (Mohammad et al.,2009 - EMNLP). Originally used on the MPQA corpus
	 */
	private void setAlgorithm (String a)
	{
		this.algorithm = a;
	}

	/**
	 * This is the core of the class, given the path to a corpus process it 
	 * and return the performance results
	 * 
	 * @param corpus: path to the file containing the corpus to be processed.  
	 * @param shouldOptimize: if optimization mode should be entered (find the threshold that maximizes accuracy for the given corpus)
	 * @param useWeights: whether lexicon weights (if existing) should be used when computing polarity or only binary polarities (pos|neg). Default is to use binary polarities.
	 *  
	 */
	public Map<String, Float> processCorpus (String corpus, boolean shouldOptimize, boolean useWeights) 
	{
		
		// clean all previous prediction and references stored in data structures
		this.cleanCorpusData();
				
		float pos = 0;
		float neg = 0;
		float neu = 0;
		int wordCount = 0;
		
		try {
			BufferedReader corpReader = new BufferedReader(new FileReader(corpus));
			String line;
			String docid ="";
			while ((line = corpReader.readLine()) != null) 
			{
				// document start
				Matcher match = Pattern.compile("<doc id=\"([^\"]+)\" pol=\"(neg|pos)\"( score=\"[0-9\\.]*\")?>").matcher(line);
				if (match.find())
				{				
					//store actual polarity.
					docid = match.group(1);
					String pol = match.group(2);
					ref_pols.put(docid, pol);
					//store actual polarity statistics
			        if (! ref_polCounts.containsKey(pol))
			        {
			        	ref_polCounts.put(pol, (float)1); 
			        }
			        else
			        {
			        	float i = ref_polCounts.get(pol)+1;
			        	ref_polCounts.put(pol,i);
			        }

					//initialize polarity word counts
				    pos=0;
				    neg=0;
				    neu=0;
				    wordCount=0;
				}
				// while no document/sentence ending tag comes count polarity words
				else if (! line.matches("^</doc>$"))
				{
					// ignore blank lines 
			        if (line.matches("^\\s*$"))
			        {
			            continue;
			        }
			        //System.err.println("kkkk --- "+line);
			        // Read word form analysis. IMPORTANT: this code is dependent on the output of FreeLing.
			        String[] fields = line.split("\\s+");
			        //fields(string form, String lemma, my $POS, my $POSprob, my $senseInfo) = split /\s+/, $l;
			        // senses come in this format WNsense1:score1/WNsense2:score2/...:... 	
			        String[] senses = {"-"};
			        if (fields.length > 4)
			        {
			        	 senses = fields[4].split("/");
			        }
			        //String form = fields[0];
			        String lemma = fields[1];
			        //String POStag = fields[2];
			        //String POSprob = fields [3];
			        
			        //All tokens are counted for the length of the document (including punctuation marks)
			        wordCount++;
			        
			        String wordPol="none";  
			        List<String> lookwords = new ArrayList<String>();
			        switch (this.synset)
			        {
			        //default is polarity is computed over lemmas
			        case 0: 
			        	lookwords.add(lemma+":1");
			        	break;
			       	// take as correct the first sense (the one with the highest score). 
			        // For the moment the score is normalized to 1 (for comparability with 
			        // previous experiments 2013/11/18. Inaki)
			        case 1:
			        	String sens=senses[0]; 
			        	//sens = sens.replaceFirst(":[^:]+$",""); 			        	
			        	//lookwords.add(sens+":1");
			        	if (!sens.contains(":"))
			        	{	
			        		sens=sens+":1";
			        	}
			        	lookwords.add(sens);
			        	break;
			        // The whole ranking of possible senses returned by FreeLing (UKB) is taken into account.
			        case 2:
			        	lookwords = Arrays.asList(senses);
			        	break;
			        }
			        //System.err.println("AvgEstimator:: "+line+" ------ ");
			        wordPol= sensePolarity(lookwords, useWeights);
			        if (wordPol.compareTo("none") != 0)
			        {
			        	float wscore = Float.parseFloat(wordPol);
			        	if (wscore > 0)  // positive
			        	{
			        		if (useWeights)
			        			pos+=wscore;
			        		else
			        			pos+=1;
			        	}
			        			
			        	else if (wscore < 0) // negative
			        	{	
			        		if (useWeights)
			        			neg+=wscore;
			        		else
			        			neg-=1;
			        	}
			        	else // neutral
			        	{
			        		if (useWeights)
			        			neu+=wscore;
			        		else
			        			neu+=1;
			        	}			        		
			        }
			        
				}
				// document/sentence end. Compute final polarity and store statistics regarding the document.
				else
				{
					computePolarity(docid, pos, neg, wordCount); 
					
					//System.err.println(docid+" - "+avg+" - pos: "+pos+" -neg: "+neg+" - words: "+wordCount);					
				}			    
			}
			corpReader.close();
		} catch (FileNotFoundException e) {
			System.err.println("AvgRatioEstimator: error when loading corpus from file: "+corpus);
			e.printStackTrace();
		} catch (IOException ioe) {
			System.err.println("AvgRatioEstimator: error when reading corpus from file: "+corpus);
			ioe.printStackTrace();
		}	
		
		int docCount=predicted_pols.size();
		System.out.println("AvgRatioEstimator: corpus processed ("+docCount+" elements). Polarity scores ready. \n");

		// compute statistics.
		if (! shouldOptimize)
		{
			computeStatistics(this.threshold);
			// add threshold used to stats
			this.stats.put("thresh", this.threshold);
			
		}
		/*
		 *  Optimization mode: find the threshold that maximizes accuracy over the given corpus
		 *  with the current lexicon. 
		 *  Example: OpFinder_Strong and PangLee_movie_docs = 0.00585, BUT same corpus for Qwnet = 0.0655 
		 */		 
		else
		{
		    		    
		    //min and max values are of the threshold are the min and max scores. If threshold would result on one of those all elements would be classified under the same class.    
			float minValue = Collections.min(predicted_pols.values());
		    float topValue= Collections.max(predicted_pols.values());
		    // interval is 1/1000 portion between the minimum and maximum posible values.
		    float interval=(topValue-minValue)/1000;
		    		    
		    float current = minValue;
		    float optimum = minValue;
		    float maxAcc = 0;
			
		    System.out.println("AvgEstimator: optimization mode entered : "
		    		+minValue+" - "+topValue+" in "+interval+" intervals\n");
			
		    while (current < topValue)
		    {
		        computeStatistics(current);

		        //print STDERR "\r$unekoa - $topValue";
		        if (stats.get("Acc") > maxAcc)
		        {
		            optimum=current;
		            maxAcc=stats.get("Acc");
		        }
		        current+=interval;
		    }
		    System.out.println("\t---- Train Results  - Threshold: "+optimum+" - max Accuracy: "+maxAcc+" ----\n");
		    this.stats.clear();
		    this.stats.put("thresh", optimum);
		    this.stats.put("Acc", maxAcc);
		}
		return this.stats;
	}
	
	/** 
	 * This void cleans current corpus data, in order to process another corpus. This is needed for example, 
	 * to process on a test-set after a development-set has been used to optimize the threshold 
	 */
	private void cleanCorpusData() {
		this.stats.clear();
		this.kafResults.clear();
		this.ref_pols.clear();
		this.ref_polCounts.clear();
		this.predicted_pols.clear();
	}

		
	/**
	 * Bridge function that handles which algorithm should be used when computing polarity 
	 * of a sentence/document. which algorithm shall be used is determined by 'this.algorithm'
	 * 
	 * @param docid
	 * @param pos : float determining the positive score of the sentence/document
	 * @param neg : float determining the negative score of the sentence/document
	 * @param wordCount : number of tokens of the sentence/document.
	 */
	private void computePolarity (String docid, float pos, float neg, float wordCount)
	{
		if (this.algorithm.equals("avg"))
		{
			computeAvgPolarity(pos, neg, wordCount, docid);
			/*System.err.println("predicted polarity for document "+docid+": "+this.predicted_pols.get(docid)
					+" -pos: "+pos+" -neg: "+neg+" -wordCount: "+wordCount);*/
		}
		else if (this.algorithm.equals("moh"))
		{
			computeMohammadPolarity(pos, neg, wordCount, docid);
			//System.err.println("predicted polarity for document "+docid+": "+this.predicted_pols.get(docid)+" -pos: "+pos+" -neg: "+neg+" -wordCount: "+wordCount);
		}
		else
		{
			System.err.println("Unknown polarity estimation algorithm ("+this.algorithm
					+"), evaluation aborted\n");
			System.exit(2);
		}
	}

	
	/**
	 * Computes the Average Ratio of positive and negative words in a document
	 * and stores it in result data structures
	 * 
	 * @param pos : float determining the positive score of the sentence/document 
	 * @param neg : float determining the negative score of the sentence/document 
	 * @param wordCount : number of tokens of the sentence/document.
	 * @param docid : 
	 */
	private void computeAvgPolarity (float pos, float neg, float wordCount, String docid)
	{
		// neg is a negative value, hence we add it to the positivity value.
		float avg = (pos + neg)*(float)1 / wordCount; 
		this.predicted_pols.put(docid, avg);
	}
	
	/**
	 * Computes the Average Ratio of positive and negative words in a document
	 * and stores it in result data structures
	 * 
	 * @param pos : float determining the positive score of the sentence/document 
	 * @param neg : float determining the negative score of the sentence/document 
	 * @param wordCount : number of tokens of the sentence/document.
	 * @param docid : 
	 */
	private void computeMohammadPolarity (float pos, float neg, float wordCount, String docid)
	{
		// evaluation on mpqa as proposed in (Mohammad et al.,2009 - EMNLP)                                                                   
        // if one negative word / synset has been found doc / phrase is negative.                                                         
        if (neg != 0)
        {
        	this.predicted_pols.put(docid, (float) -1);
        }
        // if no negative word / synset has been found and there is at least on poitive words doc / phrase is positive.                   
        else if (pos > 0)
        {
        	this.predicted_pols.put(docid, (float) 1);
        }
        // doc / phrase is undefined                                                                                                      
        else
        {
        	this.predicted_pols.put(docid, (float) 0);
        }

	}

	/**
	 * Compute system performance statistics (Acc, P, R, F) for the given corpus.
	 * 
	 * @param threshold : float value in the [-1,1] range that determines the limit 
	 * between positive and negative reviews. If the a review score equals exactly to the threshold
	 * it is considered undefined (neutral). 
	 */
	private void computeStatistics(float threshold) {
		             
		float okPos=0;
	    float okNeg=0;
	    float predPos=0;
	    float predNeg=0;
	    float undefined=0;
	    
	    //initialize results matrix
	    this.stats.put("Ppos", (float) 0);
	    this.stats.put("Pneg", (float) 0);
	    this.stats.put("Rpos", (float) 0);
	    this.stats.put("Rneg", (float) 0);
	    this.stats.put("Fpos", (float) 0);
	    this.stats.put("Fneg", (float) 0);
	    
	    // compare predictions with the reference
	    for (String id : predicted_pols.keySet())
	    {
	    	// prediction = 0 => undefined / neutral?
	    	if (predicted_pols.get(id) == threshold )
	    	{
	    		undefined++;
	    	}
	    	// predicted as positive
	    	else if (predicted_pols.get(id) > threshold )
	    	{
	    		predPos++;
	    		//correct?
	    		if ( ref_pols.get(id).compareTo("pos") == 0 )
	    		{
	    			okPos++;
	    		}
	    	}
	    	// predicted as negative
	    	else if  (predicted_pols.get(id) < threshold ) 
	    	{
	    		predNeg++;
	    		if ( ref_pols.get(id).compareTo("neg") == 0 )
	    		{
	    			okNeg++;
	    		}
	    	} 
	    }
	    
	    // compute Acc , P, R, F
	    this.stats.put("predPos", (float) predPos);
	    this.stats.put("predNeg", (float) predNeg);
	    this.stats.put("undefined", (float) undefined);

	    //System.err.println(predPos+" pos, "+predNeg+" neg, "+undefined+" undef");
	    // calculate statistics: Accuracy | precision | recall |  f-score
	    int docCount=predicted_pols.size();
	    // ## Accuracy
	    this.stats.put("Acc", (float)((okPos + okNeg) * 1.0 / docCount)) ;   
	    // ## Positive docs' Precision
	    if (predPos > 0)
	    {
	    	this.stats.put("Ppos", (okPos / predPos));
	    }
	    // ## Negative docs' Precision
	    if (predNeg > 0)
	    {
	    	this.stats.put("Pneg", (okNeg / predNeg));
	    }
	    float div;
	    try{
	    	div  = ref_polCounts.get("pos");
	    }catch (NullPointerException npe){
	    	div = 0;
	    } 
	    // ## Positive docs' Recall    
	    if (div > 0)
	    {
	    	this.stats.put("Rpos", (okPos / div)); //#polCount_hash->{"pos"};  
	    }
	    
	    try{
	    	div  = ref_polCounts.get("neg");
	    }catch (NullPointerException npe){
	    	div = 0;
	    }
	    // ## Negative docs' Recall
	    if (div > 0)
	    {
	    	this.stats.put("Rneg", (okNeg / div)); //#polCount_hash->{"pos"};	          
	    }
	    // ## Positive docs' F-score
	    if ((this.stats.get("Ppos") + this.stats.get("Rpos")) > 0)
	    {
	    	this.stats.put("Fpos", (2 * this.stats.get("Ppos") * this.stats.get("Rpos") / (this.stats.get("Ppos") + this.stats.get("Rpos")))); 
	    }
	    // ## Negative docs' F-score
	    if ((this.stats.get("Pneg") + this.stats.get("Rneg")) > 0)
	    {
	    	this.stats.put("Fneg", (2 * this.stats.get("Pneg") * this.stats.get("Rneg") / (this.stats.get("Pneg") + this.stats.get("Rneg")))); 
	    }

	}

	/**
	 *  Compute the polarity of a lemma/sense based on its ranking of senses
	 *  @param senses: list of lemma/senses to look for in the lexicon. 
	 *                It will contain a single element (lemma/first sense cases), 
	 *                except in the "rank" sense case.
	 *  @param useWeights: whether lexicon weights are used or scalar polarities (pos|neg|neu). Default is scalar polarities. 
	 */
	private String sensePolarity (List<String> senses, boolean useWeights)
	{
	    float polarity=0;
	    boolean isFound= false;
	    for (String s : senses)
	    {
	    	// if s=="-" means that there is no sense information at all. Automatically return "none" 
	    	// - is this the best behavior? it would be better to look if the lemma is a modifier?  
	    	if (s.compareTo("-")==0 || s.isEmpty())
	    	{
	    		continue;
	    	}
	    	
	    	String[] fields;
	    	// Next conditions is a temporal solution to urls appearing in the corpus because 
        	// they cause an error with when asking for their polarity
        	// NOTE: if, by any chance, the polarity lexicon contains urls ':' chars should be replaced
        	// by '_' chars in the lexicon as well in order to be matched at this step
	    	if (s.matches(".*://.*"))
        	{
        		fields = s.replaceFirst(":", "_").split(":");        		
        	}
	    	else
	    	{
	    		fields = s.split(":");
	    	}
	    	
	        String sense = fields[0];
	        String scoreStr = "";
	        if (s.startsWith("::"))
	    	{	        	
	        	scoreStr = fields[2];
	    		sense = ":";	
	    	}
		    else
			{
			    scoreStr = fields[1];
			}
	        
	        /*if (s.matches(".*://.*"))
	        {
		    	System.err.println("word to look for: "+sense+" : _"+s+"_ "+scoreStr+" - \n");
	        }*/

	        float senseScore = Float.parseFloat(scoreStr);

	        //System.err.println("word to look for: "+sense+" - "+senseScore+"\n");

	        //look up in the lexicon for the polarity of the words.
	        //if (this.lexicon.getScalarPolarity(sense) != 123456789)
	        if (this.lexicon.getScalarPolarity(sense) < 2)
	        {	        		        	
	        	polarity+=lexicon.getScalarPolarity(sense)*senseScore;
	        	if (useWeights)
	        	{
	        		polarity+=lexicon.getNumericPolarity(sense)*senseScore;
	        	}
	            isFound = true;
	            /*
	            if (polarity > 0)
	            	System.err.println("word found! -"+sense+" - pos - "+polarity);
	            else if (polarity < 0)
	            	System.err.println("word found! -"+sense+" - neg - "+polarity);
	            else
	            	System.err.println("word found! -"+sense+" - neu - "+polarity);
	            */	        
	        }
	    }
	    
	    // return polarity score, main program will interpret the score
	    if (isFound)
	    {
	        return String.valueOf(polarity);
	    }
	    else
	    {
	        return "none";
	    }


	}

	
	
	/**
	 * This function predicts the polarity of a text given in the KAF format. argument is the path to the KAF file 
	 * It saves tagged file to the given path + ".sent" extension.
	 * 
	 * @param fname
	 * @return a map element containing statistics computed for the given text and 
	 *          the path to the annotated file.
	 */
	public Map<String, String> processKaf (String fname, String lexName, boolean save) 
	{
		float score = 0;
		int sentimentTerms = 0;
		try {
			KAFDocument doc = KAFDocument.createFromFile(new File(fname));
			
			KAFDocument.LinguisticProcessor newLp = doc.addLinguisticProcessor("terms", "qwn-ppv-polarity-tagger");
			newLp.setVersion(lexName);			
			newLp.setBeginTimestamp();
			for (Term t : doc.getTerms())
			{				
				String lemma = t.getLemma();			
				
				int pol = lexicon.getScalarPolarity(lemma);				
				if (pol != 123456789)
				{
					Sentiment ts = t.createSentiment();
					switch (pol)
					{
					case 1: ts.setPolarity("positive"); break;
					case -1: ts.setPolarity("negative"); break;
					case 0: ts.setPolarity("neutral"); break;
					case 2: ts.setSentimentModifier("intensifier"); break;
					case 3: ts.setSentimentModifier("weakener"); break;
					case 4: ts.setSentimentModifier("shifter"); break;
					default: 
					}
					
					score+= lexicon.getNumericPolarity(lemma);
					//score+= pol;
					sentimentTerms++;
				}										
			}			
			newLp.setEndTimestamp();
			if (save)
			{
				doc.save(fname+".sent");
				kafResults.put("taggedFile", fname+".sent");
			}
			float avg = score / doc.getTerms().size();			
			kafResults.put("sentTermNum",String.valueOf(sentimentTerms));
			kafResults.put("avg", String.valueOf(avg));
			kafResults.put("thresh", String.valueOf(threshold));
			if (avg > threshold)
			{
				kafResults.put("polarity", "positive");
			}
			else if (avg < threshold)
			{
				kafResults.put("polarity", "negative");
			}
			else
			{
				kafResults.put("polarity", "neutral");
			}									
			
		} catch (FileNotFoundException fe) {
			System.err.println("AvgRatioEstimator: error when loading kaf file: "+fname);
			fe.printStackTrace();
		} catch (IOException ioe) {
			System.err.println("AvgRatioEstimator: error when loading kaf file: "+fname);
			ioe.printStackTrace();
		}							
		
		return this.kafResults;
	}
	
	/**
	 * This function predicts the polarity of a text given in the Tab format. argument is the path to the tab file 
	 * No tagged file is saved
	 * 
	 * @param fname
	 * @return a map element containing statistics computed for the given text and 
	 *          the path to the annotated file.
	 */
	public Map<String, String> polarityScoreTab (String fname, String lexName) 
	{
		float score = 0;
		int sentimentTerms = 0;
		int docSize = 0;
		Map<String, String> results = new HashMap<String, String>();
		try {
			BufferedReader docReader = new BufferedReader(new FileReader(fname));
			String line;
			String docid ="";
			while ((line = docReader.readLine()) != null) 
			{				
				if (line.matches("^\\s*$"))
		        {
		            continue;
		        }
		        //System.err.println("kkkk --- "+line);
		        // Read word form analysis. IMPORTANT: this code is dependent on the output of FreeLing.
		        String[] fields = line.split("\\s+");			
				String lemma = fields[1];

				int pol = lexicon.getScalarPolarity(lemma);				
				if (pol != 123456789)
				{					
					score+= lexicon.getNumericPolarity(lemma);
					//score+= pol;
					sentimentTerms++;
				}	
				docSize++;
			}			
			
			float avg = score / docSize;			
			results.put("sentTermNum",String.valueOf(sentimentTerms));
			results.put("avg", String.valueOf(avg));
			results.put("thresh", String.valueOf(threshold));
			if (avg > threshold)
			{
				results.put("polarity", "positive");
			}
			else if (avg < threshold)
			{
				results.put("polarity", "negative");
			}
			else
			{
				results.put("polarity", "neutral");
			}									
			
		} catch (FileNotFoundException fe) {
			System.err.println("AvgRatioEstimator: error when loading kaf file: "+fname);
			fe.printStackTrace();
		} catch (IOException ioe) {
			System.err.println("AvgRatioEstimator: error when loading kaf file: "+fname);
			ioe.printStackTrace();
		}							
		
		return results;
	}
	
}
