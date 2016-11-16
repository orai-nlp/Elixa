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

import elh.eus.absa.CorpusReader;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Term;
import ixa.kaflib.WF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.ArffSaver;

import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jdom2.JDOMException;



/**
 * @author isanvi
 * 
 */
public class Features {
	private CorpusReader corpus;
	private Properties params = new Properties();	
	private Instances traindata;
	
	// Create unigram base numeric attributes   
	private ArrayList<Attribute> atts = new ArrayList<Attribute>();
	
	//structure to control attribute indexes
	private HashMap<String,Integer> attIndexes = new HashMap<String,Integer>();
	
	//structure to control instance ids wrt opinions/sentences
	private HashMap<String,Integer> opInst = new HashMap<String,Integer>();
	
	//structure to control instance ids wrt opinions/sentences
	private HashMap<String, HashMap<String,Integer>> attributeSets = new HashMap<String, HashMap<String,Integer>>();
	
	//structure to store word form ngram attributes
	private HashMap<String, Integer> wfNgrams = new HashMap<String,Integer>(); 

	//structure to store lemma ngram attributes
	private HashMap<String, Integer> lemmaNgrams = new HashMap<String,Integer>(); 

	//structure to store POS ngram attributes
	private HashMap<String, Integer> POSNgrams = new HashMap<String,Integer>(); 
	
	private List<String> ClassificationClasses = Arrays.asList("dummy","positive","negative","neutral");  

	//structure to control general polarity lexicon
	private Lexicon polarLexiconGen;
	//structure to control domain polarity lexicon
	private Lexicon polarLexiconDom;
	
	// PoS tagger object. Defined here for optimizing posTagger resource loading times
	private eus.ixa.ixa.pipe.pos.Annotate postagger;
	
	//MicroText Normalization object
	private MicroTextNormalizer MicrotxtNormalizer; 	
	
	// feature number
	private int featNum;
	
	/**
	 *  Constructor
	 * @param InputStream ins : InputStream containing a corpus to process. 
	 * @param String paramFile : Path to the file containing the feature configuration file 
	 *                            (which features should be used)
	 */
	public Features (InputStream ins, String lang, String format, String paramFile, String classes)
	{
		this(new CorpusReader(ins, format, lang), paramFile, classes);
	}
	

	/**
	 *  Constructor
	 * @param Corpus reader creader : An already existing corpus reader object. 
	 * @param String paramFile : Path to the file containing the feature configuration file 
	 *                            (which features should be used)
	 */
	public Features (CorpusReader creader, String paramFile, String classes)
	{
		//System.err.println("Features: constructor call");	
		this.corpus = creader;
		this.featNum = 0;
		setClasses(classes);
		File pfile = new File(paramFile);
		if (FileUtilsElh.checkFile(pfile))
		{
			try {
				params.load(new FileInputStream(pfile));
				String norm = params.getProperty("normalization", "none") ;
				//preprocess
				if (norm.matches("(?i)(all|noHashtag)"))
				{
					MicrotxtNormalizer = new MicroTextNormalizer(corpus.getLang());
					MicrotxtNormalizer.setEmodict(this.getClass().getClassLoader().getResourceAsStream("emoticons.lex"));
				}
				else if (norm.compareTo("none")!=0)
				{
					 MicrotxtNormalizer =  new MicroTextNormalizer(corpus.getLang());
				}
				//System.err.println("Features: initiate feature extraction from corpus");	
				createFeatureSet();
			} catch (IOException e) {
				System.err.println("Features: error when loading training parameter properties");				
				e.printStackTrace();
			}
		}
		else 
		{
			System.err.println("Features: given parameter file ("+paramFile+") is not a valid file.");
			System.exit(1);
		}
	}
	
	
	/**
	 *  Constructor
	 * @param Corpus reader creader : An already existing corpus reader object. 
	 * @param String paramFile : Path to the file containing the feature configuration file 
	 *                            (which features should be used)
	 */
	public Features (CorpusReader creader, String paramFile, String classes, String modelPath)
	{
		this.corpus = creader;
		this.featNum = 0;
		setClasses(classes);
		File pfile = new File(paramFile);
		if (FileUtilsElh.checkFile(pfile))
		{
			try {
				params.load(new FileInputStream(pfile));
				String norm = params.getProperty("normalization", "none") ;
				//preprocess
				if (norm.matches("(?i)(all|noHashtag)"))
				{
					MicrotxtNormalizer = new MicroTextNormalizer(corpus.getLang());
					MicrotxtNormalizer.setEmodict(this.getClass().getClassLoader().getResourceAsStream("emoticons.lex"));					 
				}
				else if (norm.compareTo("none")!=0)
				{
					MicrotxtNormalizer = new MicroTextNormalizer(corpus.getLang());
				}
				
				if (FileUtilsElh.checkFile(modelPath))
				{
					createFeatureSetFromModel(modelPath);
				}
				else
				{
					System.err.println("Features: initiate feature extraction from corpus");	
					createFeatureSet();
				}
			} catch (IOException e) {
				System.err.println("Features: error when loading training parameter properties");				
				e.printStackTrace();
			}
		}
		else 
		{
			System.err.println("Features: given parameter file ("+paramFile+") is not a valid file.");
			System.exit(1);
		}
	}
	

	/**
	 * @return HashMap<String, Integer> containing the relation between the original opinions and 
	 *          their generated attribute vector instances
	 */
	public HashMap<String, Integer> getOpinInst() {
		return opInst;
	}

	/**
	 * @return Instances object containing the attribute vectors of the given corpus
	 * 
	 */
	public Instances getTraindata() {
		return traindata;
	}

	/**
	 * Set the number of classification classes the classifier should be trained on. Depending on the number 
	 * of classes selected the annotation
	 *  
	 * @param classes: String option for the number of classes to be learnt by the classifier. Options are:
	 *    (binary=p|n ; 3=p|n|neu ; 3+=p|n|neu|none ; 5+=p|n|neu|p+|n+ ; 5+=p|n|neu|p+|n+|none)"
				+ " it defaults to 3 (p|n|neu).\n");
	 */
	private void setClasses(String classes)
	{
		switch (classes)
		{
		case "binary": 
			this.ClassificationClasses = Arrays.asList("dummy","positive","negative"); 
			break; 
		case "3": 
			this.ClassificationClasses = Arrays.asList("dummy","positive","negative","neutral"); 
			break; 
		case "3+": 
			this.ClassificationClasses = Arrays.asList("dummy","positive","negative","neutral","none"); 
			break;
		case "5" : 
			this.ClassificationClasses = Arrays.asList("dummy","positive","negative","neutral","positive+","negative+"); 
			break;
		case "5+" : 
			this.ClassificationClasses = Arrays.asList("dummy","positive","negative","neutral","positive+","negative+","none"); 
			break;
		}		
	}
	
	
	/**
	 * @return HashMap<String, Integer> containing the name of the attributes and their indexes 
	 * 			in the attribute vectors
	 */
	private HashMap<String, Integer> getAttIndexes() {
		return attIndexes;
	}

	/**
	 * Creates a feature set from a previously saved model. This allows to load previously saved feature sets. 
	 * 
	 * @param model string: path to the serialized model containing header information
	 * @throws IOException 
	 */
	private void createFeatureSetFromModel (String model) throws IOException
	{
		try
		{
			WekaWrapper ww = new WekaWrapper(model);
			Instances header = ww.loadHeader(model);
			
			int attNum = header.numAttributes();
			for (int i= 0; i<attNum; i++) 
			{
				Attribute att = header.attribute(i);
				String name = att.name();
				if (att.isNumeric())
				{
					addNumericFeature(name);
					//System.out.println("numeric feature: "+name);
				}
				else if (att.isNominal())
				{
					//System.out.println("nominal feature: "+name+" - "+att.toString());
					ArrayList<String> vals = new ArrayList<String>();
					Enumeration<Object> e = att.enumerateValues();
					while (e.hasMoreElements())
					{
						vals.add(e.nextElement().toString());
					}
					addNominalFeature(name, vals);						
				}				
			}
			
			//General polarity lexicon
			if (header.attribute("polLexGen_posScore")!=null)
			{
				this.polarLexiconGen = new Lexicon(new File(params.getProperty("polarLexiconGeneral")),"lemma");
				System.err.println("Features : createFeatureSet() - General polarity lexicon loaded -> "
						+params.getProperty("polarLexiconGeneral")
						+" ("+this.polarLexiconGen.size()+" entries)");
				System.out.println("Features : createFeatureSet() - General polarity lexicon loaded -> "
						+params.getProperty("polarLexiconGeneral")
						+" ("+this.polarLexiconGen.size()+" entries)");
			}
			
			//Domain polarity lexicon
			if (header.attribute("polLexDom_posScore")!=null)
			{
				//this.polarLexiconDom = loadPolarityLexiconFromFile(params.getProperty("polarLexiconDomain"), "polLexDom_");
				this.polarLexiconDom = new Lexicon(new File(params.getProperty("polarLexiconDomain")),"lemma");
				System.err.println("Features : createFeatureSet() - Domain polarity lexicon loaded -> "
						+params.getProperty("polarLexiconDomain")
						+" ("+this.polarLexiconDom.size()+" entries)");
				System.out.println("Features : createFeatureSet() - Domain polarity lexicon loaded -> "
						+params.getProperty("polarLexiconDomain")
						+" ("+this.polarLexiconDom.size()+" entries)");	
			}
			
			
			
			
			// Load clark cluster category info from files
			loadClusterFeatures("clark");
			
			// Load brown cluster category info from files
			loadClusterFeatures("brown");

			// Load word2vec cluster category info from files
			loadClusterFeatures("word2vec");


			
		} catch (Exception e)
		{
			System.err.println("Features::createFeatureSetFromFile -> error when loading model header");
			e.printStackTrace();
		}			
		
	}
	
	/**
	 * create feature set starting from the training corpus provided.
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private void createFeatureSet () throws IOException
	{	
		// create a Id attribute to link the instances to a certain opinion. Note that this attribute won't be
		// used for classifying. It is used only for linking the instances with their corresponding opinions
		addNumericFeature("instanceId");
				
		//naf paths for the tagged files		
		String nafDir = params.getProperty("kafDir");
		// create pos tagging dir if not exists
		Files.createDirectories(Paths.get(nafDir));
		
		
		// dummy variable to debug the feature loading
		int featPos = this.featNum;
		
		//word form and lemma ngram minimum frequencies.
		int wfMinFreq=1;
		int lemmaMinFreq=1;
		
		// in case pos tags are used and we want to filter lemmas according to their pos 
		List<String> discardPos = new ArrayList<String>();
		
		if (params.containsKey("posFilter"))
		{
			String[] posTofilter = params.getProperty("posFilter").split(",");
			for (int i=0; i<posTofilter.length;i++)
			{
				discardPos.add(posTofilter[i]); 
			}
		}

		Set<String> corpSentenceIds = corpus.getSentences().keySet();
		
		//Corpus tagging, if the corpus is not in conll tabulated format
        if (corpus.getFormat().equalsIgnoreCase("tabNotagged") || !corpus.getFormat().startsWith("tab"))
        {
        	if ((params.containsKey("lemmaNgrams") || (params.containsKey("pos") && !params.getProperty("pos").equalsIgnoreCase("0")))
        			&& (corpus.getLang().compareToIgnoreCase("eu")!=0))
        	{
        		Properties posProp = NLPpipelineWrapper.setPostaggerProperties( params.getProperty("pos-model"), params.getProperty("lemma-model"),
        				corpus.getLang(), "bin", "false");					
        		postagger = new eus.ixa.ixa.pipe.pos.Annotate(posProp);						
        	}

        	
        	for (String key : corpSentenceIds)
        	{
        		String currentSent = corpus.getSentence(key);		
        		if ((params.containsKey("wfngrams") || params.containsKey("lemmaNgrams")) &&
        				(! params.getProperty("normalization", "none").equalsIgnoreCase("noEmot")))
        		{
        			currentSent = normalize(currentSent, params.getProperty("normalization", "none"));
        		}

        		String nafPath = nafDir+File.separator+key.replace(':', '_');	

        		try {
        			String taggedPath = NLPpipelineWrapper.tagSentence(currentSent, nafPath, corpus.getLang(),  params.getProperty("pos-model"), params.getProperty("lemma-model"), postagger);
        		} catch (JDOMException e) {
        			System.err.println("Features::createFeatureSet -> NAF error when tagging sentence");
        			e.printStackTrace();
        		}
        		//System.err.println("Features::createFeatureSet -> corpus normalization step done");						
        	}
        }
		// word form ngram features
		if (params.containsKey("wfngrams"))
		{	
			// Min frequency for word form ngrams
			if (params.containsKey("wfMinFreq"))
			{
				try {
					wfMinFreq = Integer.parseInt(params.getProperty("wfMinFreq"));
				}
				catch (NumberFormatException nfe){
					System.err.println("Features::createFeatureSet() - provided word form minimum frequency "
							+ "is not an integer. Default value 1 will be used");
				}				
			}			
			
			File test = new File(params.getProperty("wfngrams"));
			// If the word form ngram list is stored in a file.
			if (test.isFile())
			{
				loadAttributeListFromFile(test,"wf");
			}
			// If the corpus is in conll tabulated format
			else if (corpus.getFormat().startsWith("tab") && !corpus.getFormat().equalsIgnoreCase("tabNotagged"))
			{
				// N-gram Feature vector : extracted from sentences
				int success = extractNgramsTAB(Integer.valueOf(params.getProperty("wfngrams")), "wf", discardPos, true);
				addNumericFeatureSet("", wfNgrams, wfMinFreq);
			}
			// Otherwise  use previously tagged files with ixa-pipes 
			else
			{
				int wfNgramsLength=Integer.valueOf(params.getProperty("wfngrams"));
				System.err.println("Features::createFeatureSet -> word from ngram extraction ("+wfNgramsLength+")-grams)...");
				for (String key : corpSentenceIds)
				{
					String nafPath = nafDir+File.separator+key.replace(':', '_')+".kaf";
					//eu tagged files are conll format
					if (corpus.getLang().equalsIgnoreCase("eu"))
					{
						int success = extractNgramsTABString(new FileInputStream(new File(nafPath)), wfNgramsLength, "wf", discardPos, true);
					}
					else
					{
						KAFDocument naf = KAFDocument.createFromFile(new File(nafPath));
						// N-gram Feature vector : extracted from sentences
						int success = extractWfNgramsKAF(Integer.valueOf(params.getProperty("wfngrams")), naf, true);			
					}	
				}
				addNumericFeatureSet("", wfNgrams, wfMinFreq);
			}

			System.err.println("Features : createFeatureSet() - unigram features -> "+(this.featNum-featPos));
			System.out.println("Features : createFeatureSet() - unigram features -> "+(this.featNum-featPos));
		}
		
		// lemma ngram features
		if (params.containsKey("lemmaNgrams"))
		{	
			// Min frequency for word form ngrams
			if (params.containsKey("lemmaMinFreq"))
			{
				try {
					lemmaMinFreq = Integer.parseInt(params.getProperty("lemmaMinFreq"));
				}
				catch (NumberFormatException nfe){
					System.err.println("Features::createFeatureSet() - provided lemma minimum frequency "
							+ "is not an integer. Default value 1 will be used");
				}				
			}
			
			featPos = this.featNum;
			File test = new File(params.getProperty("lemmaNgrams"));
			// If N-grams are stored in a file
			if (test.isFile())
			{
				loadAttributeListFromFile(test,"lemmaNgrams");
			}
			// If the corpus is in conll tabulated format
			else if (corpus.getFormat().startsWith("tab") && !corpus.getFormat().equalsIgnoreCase("tabNotagged"))
			{
				// N-gram Feature vector : extracted from sentences
				int success = extractNgramsTAB(Integer.valueOf(params.getProperty("lemmaNgrams")), "lemma", discardPos, true);
				addNumericFeatureSet("", lemmaNgrams, lemmaMinFreq);
			}
			// Otherwise  use previously tagged files with ixa-pipes 
			else
			{
				int lemmaNgramsLength=Integer.valueOf(params.getProperty("lemmaNgrams"));
				System.err.println("Features::createFeatureSet -> lemma ngram extraction ("+lemmaNgramsLength+"-grams)...");
				for (String key : corpSentenceIds)
				{
					String nafPath = nafDir+File.separator+key.replace(':', '_')+".kaf";
					//eu tagged files are conll format
					if (corpus.getLang().equalsIgnoreCase("eu"))
					{
						int success = extractNgramsTABString(new FileInputStream(new File(nafPath)), lemmaNgramsLength, "lemma", discardPos, true);
					}
					else
					{
						KAFDocument naf = KAFDocument.createFromFile(new File(nafPath));
						// N-gram Feature vector : extracted from sentences
						int success = extractLemmaNgrams(Integer.valueOf(params.getProperty("lemmaNgrams")), naf, discardPos, true);
					}
				} 			
				addNumericFeatureSet("", lemmaNgrams, lemmaMinFreq);					
			}
			System.out.println("Features : createFeatureSet() - lemma ngram features -> "+(this.featNum-featPos));
			System.err.println("Features : createFeatureSet() - lemma ngram features -> "+(this.featNum-featPos));
		}

		// pos tag features
		if (params.containsKey("pos") && !params.getProperty("pos").equalsIgnoreCase("0"))
		{	
			featPos = this.featNum;
			File test = new File(params.getProperty("pos"));
			String conll ="";
			// if POS ngrams are stored in a file
			if (test.isFile())
			{
				loadAttributeListFromFile(test,"POS_");
			}
			// If the corpus is in conll tabulated format
			else if (corpus.getFormat().startsWith("tab") && !corpus.getFormat().equalsIgnoreCase("tabNotagged"))
			{
				// N-gram Feature vector : extracted from sentences
				int success = extractNgramsTAB(Integer.valueOf(params.getProperty("pos")), "pos", discardPos, true);
				addNumericFeatureSet("", POSNgrams, 1);
			}
			// Otherwise  use previously tagged files with ixa-pipes 
			else
			{
				int posNgramLength= Integer.valueOf(params.getProperty("pos"));
				System.err.println("Features::createFeatureSet -> pos ngram extraction ("+posNgramLength+"-grams)...");
				for (String key : corpSentenceIds)
				{
					String nafPath = nafDir+File.separator+key.replace(':', '_')+".kaf";
					//eu tagged files are conll format
					if (corpus.getLang().equalsIgnoreCase("eu"))
					{
						int success = extractNgramsTABString(new FileInputStream(new File(nafPath)), posNgramLength, "pos", discardPos, true);
					}
					else
					{
						KAFDocument naf = KAFDocument.createFromFile(new File(nafPath));
						// N-gram Feature vector : extracted from sentences
						int success = extractPosNgrams(Integer.valueOf(params.getProperty("pos")), naf, discardPos, true);
					}
					
				} 							
				addNumericFeatureSet("", POSNgrams, 1);
			}
			System.out.println("Features : createFeatureSet() - pos tag features -> "+(this.featNum-featPos));
			System.err.println("Features : createFeatureSet() - pos tag features -> "+(this.featNum-featPos));
		}	
		
		
		// Load clark cluster category info from files
		loadClusterFeatures("clark");
		
		// Load brown cluster category info from files
		loadClusterFeatures("brown");

		// Load word2vec cluster category info from files
		loadClusterFeatures("word2vec");

		// add sentence length as feature
		if (! params.getProperty("sentenceLength", "no").equalsIgnoreCase("no"))
		{
			addNumericFeature("sentenceLength");
		}
		
		// add sentence length as feature
		if (! params.getProperty("upperCaseRatio","no").equalsIgnoreCase("no"))
		{
			addNumericFeature("upperCaseRatio");
		}
		
		// Category vector extracted from training set opinions
		TreeSet<String>[] categoryInfo = new TreeSet[3];
		if (! params.getProperty("categories", "no").equalsIgnoreCase("no"))
		{
			categoryInfo = extractCategories();
		}
		
		// Two separated features characterize the category infor: entity (E) and attribute (A)
		if (params.getProperty("categories", "no").equalsIgnoreCase("E&A"))
		{
			// Declare Nominal attribute for entity category	
			ArrayList<String> entVal = new ArrayList<String>(categoryInfo[0]);
			entVal.add(0, "dummy");  //needed because of weka's sparse data format problems...
			addNominalFeature("entCat", entVal);

			// Declare Nominal attribute for entity category	
			ArrayList<String> attVal = new ArrayList<String>(categoryInfo[1]);
			attVal.add(0, "dummy");  //needed because of weka's sparse data format problems...
			addNominalFeature("attCat", attVal);

			// Declare Nominal attribute for category as a whole
			/*ArrayList<String> entAttVal = new ArrayList<String>(categoryInfo[2]);
			entAttVal.add(0, "dummy");  //needed because of weka's sparse data format problems...
			addNominalFeature("entAttCat", entAttVal);*/
		}		
		// Category as a whole
		else if (params.getProperty("categories","no").equalsIgnoreCase("E#A"))
		{

			// Declare Nominal attribute for category as a whole
			ArrayList<String> entAttVal = new ArrayList<String>(categoryInfo[2]);
			entAttVal.add(0, "dummy");  //needed because of weka's sparse data format problems...
			addNominalFeature("entAttCat", entAttVal);
		}

		/** 
		 * Look at the polarity lexicons 
		 * */
		//General domain polarity lexicon
		if (params.containsKey("polarLexiconGeneral") && FileUtilsElh.checkFile(params.getProperty("polarLexiconGeneral")))
		{
			//this.polarLexiconGen = loadPolarityLexiconFromFile(params.getProperty("polarLexiconGeneral"), "polLexGen_");
			this.polarLexiconGen = new Lexicon(new File(params.getProperty("polarLexiconGeneral")),"lemma");
			
			System.err.println("Features : createFeatureSet() - General polarity lexicon loaded -> "
							+params.getProperty("polarLexiconGeneral")
							+" ("+this.polarLexiconGen.size()+" entries)");
			System.out.println("Features : createFeatureSet() - General polarity lexicon loaded -> "
							+params.getProperty("polarLexiconGeneral")
							+" ("+this.polarLexiconGen.size()+" entries)");
			
			if (params.containsKey("polNgrams") && !params.getProperty("polNgrams").equalsIgnoreCase("no"))
			{
				for (String s : this.polarLexiconGen.getEntrySet())
				{
					addNumericFeature("polgen_"+s);
				}
				System.err.println("Features : createFeatureSet() - General polarity lexicon lemmas loaded. -> "+this.polarLexiconGen.size());
				System.out.println("Features : createFeatureSet() - General polarity lexicon lemmas loaded. -> "+this.polarLexiconGen.size());		

			}
			
			//add features to feature map:  two features, positive|negative scores 
			addNumericFeature("polLexGen_posScore");
			addNumericFeature("polLexGen_negScore");
		}
		
		//Domain polarity lexicon
		if (params.containsKey("polarLexiconDomain") && FileUtilsElh.checkFile(params.getProperty("polarLexiconDomain")))
		{
			//this.polarLexiconDom = loadPolarityLexiconFromFile(params.getProperty("polarLexiconDomain"), "polLexDom_");
			this.polarLexiconDom = new Lexicon(new File(params.getProperty("polarLexiconDomain")),"lemma");
			System.err.println("Features : createFeatureSet() - Domain polarity lexicon loaded -> "
							+params.getProperty("polarLexiconDomain")
							+" ("+this.polarLexiconDom.size()+" entries)");
			System.out.println("Features : createFeatureSet() - Domain polarity lexicon loaded -> "
					+params.getProperty("polarLexiconDomain")
					+" ("+this.polarLexiconDom.size()+" entries)");		
			
			if (params.containsKey("polNgrams") && !params.getProperty("polNgrams").equalsIgnoreCase("no"))
			{
				for (String s : this.polarLexiconDom.getEntrySet())
				{
					addNumericFeature("poldom_"+s);
				}
				System.err.println("Features : createFeatureSet() - Domain polarity lexicon lemmas loaded -> "+this.polarLexiconDom.size());
				System.out.println("Features : createFeatureSet() - Domain polarity lexicon lemmas loaded -> "+this.polarLexiconDom.size());		

			}
			
			//add features to feature map:  two features, positive|negative scores 
			addNumericFeature("polLexDom_posScore");
			addNumericFeature("polLexDom_negScore");
		}
		
		
		// if polarity is activated in the parameter file look for
		if (params.containsKey("polarity") && params.getProperty("polarity").equalsIgnoreCase("yes"))
		{
			// Declare the class attribute along with its values			
			addNominalFeature("polarityCat", this.ClassificationClasses);			
		}
	}
	
		
	private void loadClusterFeatures (String clname) throws IOException{
		// Load clark cluster category info from files
		HashMap<String, Integer> clMap = new HashMap<String, Integer>();
		if (params.containsKey("clark"))
		{
			int featPos = this.featNum;
			clMap = loadAttributeMapFromFile(params.getProperty(clname), clname+"ClId_");
			if (clMap.isEmpty())
			{
				params.remove(clname);
			}
			else
			{
				attributeSets.put(clname+"Cl", clMap);
			}

			System.err.println("Features : loadClusterFeatures() - "+clname+" cluster features -> "+(this.featNum-featPos));
			System.out.println("Features : loadClusterFeatures() -  "+clname+" cluster features -> "+(this.featNum-featPos));
		}
	}
	
	/**
	 *   Function fills the attribute vectors for the instances existing in the corpus given. 
	 *   Attribute vectors contain the features loaded by the creatFeatureSet() function.
	 * 
	 * @param boolean save : whether the Instances file should be saved to an arff file or not.
	 * @return Weka Instances object containing the attribute vectors filled with the features specified
	 * 			in the parameter file.
	 */
	public Instances loadInstances (boolean save, String prefix) throws IOException
	{
		String savePath = params.getProperty("fVectorDir")+File.separator+"arff"+File.separator+"train_"+prefix;
		HashMap<String, Opinion> trainExamples = corpus.getOpinions();
				
		int trainExamplesNum = trainExamples.size();

		int bowWin=0;
		if (params.containsKey("window"))
		{
			bowWin = Integer.parseInt(params.getProperty("window"));
			savePath = savePath+"_w"+bowWin;		
		}
		
		//Properties posProp = new Properties();
		//eus.ixa.ixa.pipe.pos.Annotate postagger = new eus.ixa.ixa.pipe.pos.Annotate(posProp);		
		if (params.containsKey("lemmaNgrams"))
		{
			Properties posProp = NLPpipelineWrapper.setPostaggerProperties( params.getProperty("pos-model"), params.getProperty("lemma-model"),
					corpus.getLang(), "bin", "false");
			
			postagger = new eus.ixa.ixa.pipe.pos.Annotate(posProp);						
		}
		
		//System.out.println("train examples: "+trainExamplesNum);
		//Create the Weka object for the training set
        Instances rsltdata = new Instances("train", atts, trainExamplesNum);
        
        // setting class attribute (last attribute in train data.
        //traindata.setClassIndex(traindata.numAttributes() - 1);
		
		System.err.println("Features: loadInstances() - featNum: "+this.featNum+" - trainset attrib num -> "+rsltdata.numAttributes()+" - ");
		System.out.println("Features: loadInstances() - featNum: "+this.featNum+" - trainset attrib num -> "+rsltdata.numAttributes()+" - ");
		
		int instId = 1;
		// fill the vectors for each training example
		for (String oId : trainExamples.keySet())
		{									
			//System.err.println("sentence: "+ corpus.getOpinionSentence(o.getId()));
			
			//value vector
			double[] values = new double[featNum];
			
			// first element is the instanceId			
			values[rsltdata.attribute("instanceId").index()] = instId;  
			
						
			// string normalization (emoticons, twitter grammar,...)
			String opNormalized = corpus.getOpinionSentence(oId);
			
			// compute uppercase ratio before normalization (if needed)		
			double upRatio =0.0;
			if (params.getProperty("upperCaseRatio", "no").equalsIgnoreCase("yes"))
			{
				String upper = opNormalized.replaceAll("[\\p{Ll}]", "");
				upRatio = (double)upper.length() / (double)opNormalized.length();
				values[rsltdata.attribute("upperCaseRation").index()] = upRatio;
			}
			
			// string normalization (emoticons, twitter grammar,...)
			if ((params.containsKey("wfngrams") || params.containsKey("lemmaNgrams")) &&
						(! params.getProperty("normalization", "none").equalsIgnoreCase("noEmot")))
			{
				opNormalized = normalize(opNormalized, params.getProperty("normalization", "none"));
			}
			
			
			//process the current instance with the NLP pipeline in order to get token and lemma|pos features
			KAFDocument nafinst = new KAFDocument("","");
			String nafname = trainExamples.get(oId).getsId().replace(':', '_');
			String nafDir = params.getProperty("kafDir");
			String nafPath = nafDir+File.separator+nafname+".kaf";			
			//counter for opinion sentence token number. Used for computing relative values of the features
			int tokNum=1;
			try {
				if (params.containsKey("lemmaNgrams")) //(lemmaNgrams != null) && (!lemmaNgrams.isEmpty()))
				{						
					if (FileUtilsElh.checkFile(nafPath))
					{
						nafinst = KAFDocument.createFromFile(new File(nafPath));						
					}
					else
					{
						nafinst = NLPpipelineWrapper.ixaPipesTokPos(opNormalized, corpus.getLang(),  params.getProperty("pos-model"), params.getProperty("lemma-model"), postagger);
						Files.createDirectories(Paths.get(nafDir));
						nafinst.save(nafPath);						
					}
					tokNum = nafinst.getWFs().size();
					//System.err.println("Features::loadInstances - postagging opinion sentence ("+oId+") - "+corpus.getOpinionSentence(oId));
				}
				else
				{
					if (FileUtilsElh.checkFile(nafPath))
					{
						nafinst = KAFDocument.createFromFile(new File(nafPath));
					}
					else
					{
						nafinst = NLPpipelineWrapper.ixaPipesTok(opNormalized,corpus.getLang());						
					}
					tokNum = nafinst.getWFs().size();
					//System.err.println("Features::loadInstances - tokenizing opinion sentence ("+oId+") - "+corpus.getOpinionSentence(oId));

				}
			} catch (IOException | JDOMException e) {
				System.err.println("Features::loadInstances() - error when NLP processing the instance "+instId+"|"+oId+
						") for filling the attribute vector");
				e.printStackTrace();
				System.exit(5);
			}
			
			LinkedList<String> ngrams = new LinkedList<String>();
			int ngramDim;
			try {
				ngramDim = Integer.valueOf(params.getProperty("wfngrams"));			
			} catch (Exception e){
				ngramDim = 0;
			}
			
			boolean polNgrams=false;
			if (params.containsKey("polNgrams"))
			{
				polNgrams=params.getProperty("polNgrams").equalsIgnoreCase("yes");
			}
			
			
			List<WF> window = nafinst.getWFs();
			Integer end = corpus.getOpinion(oId).getTo();
			// apply window if window active (>0) and if the target is not null (to=0)
			if ((bowWin > 0) && (end > 0))
			{
				Integer start = corpus.getOpinion(oId).getFrom();
				Integer to = window.size();
				Integer from = 0;		
				end++;				
				for (int i=0; i<window.size(); i++)
				{	
					WF wf = window.get(i);								
					if ((wf.getOffset() == start) && (i >= bowWin))
					{
						from = i-bowWin;
					}
					else if (wf.getOffset() >= end)
					{	
						if (i+bowWin < window.size())
						{
							to = i+bowWin;
						}
						break;
					}					
				}
				window = window.subList(from, to);
				//System.out.println("startTgt: "+start+" - from: "+from+" | endTrgt:"+(end-1)+" - to:"+to);
			}
			
			//System.out.println("Sentence: "+corpus.getOpinionSentence(oId)+" - target: "+corpus.getOpinion(oId).getTarget()+
			//		"\n window: from-> "+window.get(0).getForm()+" to-> "+window.get(window.size()-1)+" .\n");

			
			List<String> windowWFIds = new ArrayList<String>();

			// word form ngram related features
			for (WF wf : window)
			{	
				windowWFIds.add(wf.getId());
				
				String wfStr = wf.getForm();				
				if (params.containsKey("wfngrams") && ngramDim > 0)
				{
					if (! savePath.contains("_wf"+ngramDim))
					{
						savePath = savePath+"_wf"+ngramDim;
					}
					//if the current word form is in the ngram list activate the feature in the vector
					if (ngrams.size() >= ngramDim)
					{
						ngrams.removeFirst();
					}
					ngrams.add(wfStr);

					// add ngrams to the feature vector
					checkNgramFeatures(ngrams, values, "wf", 1, false); //toknum
				   
				}
				// Clark cluster info corresponding to the current word form
				if (params.containsKey("clark") && attributeSets.get("ClarkCl").containsKey(wfStr))
				{
					if (! savePath.contains("_cl"))
					{
						savePath = savePath+"_cl";
					}
					values[rsltdata.attribute("ClarkClId_"+attributeSets.get("ClarkCl").get(wfStr)).index()]++;					
				}
				
				// Clark cluster info corresponding to the current word form
				if (params.containsKey("brown") && attributeSets.get("BrownCl").containsKey(wfStr))
				{
					if (! savePath.contains("_br"))
					{
						savePath = savePath+"_br";
					}
					values[rsltdata.attribute("BrownClId_"+attributeSets.get("BrownCl").get(wfStr)).index()]++;
				}
				
				// Clark cluster info corresponding to the current word form
				if (params.containsKey("word2vec") && attributeSets.get("w2vCl").containsKey(wfStr))
				{
					if (! savePath.contains("_w2v"))
					{
						savePath = savePath+"_w2v";
					}
					values[rsltdata.attribute("w2vClId_"+attributeSets.get("w2vCl").get(wfStr)).index()]++;
				}

			}
			
			//empty ngram list and add remaining ngrams to the feature list
			checkNgramFeatures(ngrams, values, "wf", 1, true); //toknum
			
			// PoS tagger related attributes: lemmas and pos tags
			if (params.containsKey("lemmaNgrams") || 
					(params.containsKey("pos") && !params.getProperty("pos").equalsIgnoreCase("0")) ||
					params.containsKey("polarLexiconGeneral") ||
					params.containsKey("polarLexiconDomain"))
			{
				ngrams = new LinkedList<String>();
				if (params.containsKey("lemmaNgrams")&& (!params.getProperty("lemmaNgrams").equalsIgnoreCase("0")))
				{
					ngramDim = Integer.valueOf(params.getProperty("lemmaNgrams"));
				}
				else
				{
					ngramDim = 3;
				}
				LinkedList<String> posNgrams = new LinkedList<String>();
				int posNgramDim =0;
				if (params.containsKey("pos"))
				{
					posNgramDim = Integer.valueOf(params.getProperty("pos"));
				}
										
				for (Term t : nafinst.getTermsFromWFs(windowWFIds))
				{	
					//lemmas // && (!params.getProperty("lemmaNgrams").equalsIgnoreCase("0"))
					if ((params.containsKey("lemmaNgrams")) || params.containsKey("polarLexiconGeneral") || params.containsKey("polarLexiconDomain"))
					{
						if (! savePath.contains("_l"+ngramDim))
						{
							savePath = savePath+"_l"+ngramDim;
						}

						String lemma = t.getLemma();
						
						if (ngrams.size() >= ngramDim)
						{
							ngrams.removeFirst();
						}
						ngrams.add(lemma);
				        		
						// add ngrams to the feature vector
						for (int i=0;i<ngrams.size();i++)
						{
							String ng = featureFromArray(ngrams.subList(0, i+1), "lemma");
							//if the current lemma is in the ngram list activate the feature in the vector
							if (params.containsKey("lemmaNgrams") && (!params.getProperty("lemmaNgrams").equalsIgnoreCase("0")))
							{
								Attribute ngAtt = rsltdata.attribute(ng);
								if (ngAtt != null)
								{
									addNumericToFeatureVector (ng, values, 1);	//tokNum							
								}
							}
							
							ng = featureFromArray(ngrams.subList(0, i+1), "");
							if (params.containsKey("polarLexiconGeneral") || params.containsKey("polarLexiconDomain"))
							{
								checkPolarityLexicons(ng, values, tokNum, polNgrams);
							} //end polarity ngram checker
						} //end ngram checking				        						
					}
					//pos tags
					if (params.containsKey("pos") && !params.getProperty("pos").equalsIgnoreCase("0"))
					{
						if (! savePath.contains("_p"))
						{
							savePath = savePath+"_p";
						}
						
						if (posNgrams.size() >= posNgramDim)
						{
							posNgrams.removeFirst();
						}
						posNgrams.add(t.getPos());

						// add ngrams to the feature vector
						checkNgramFeatures(posNgrams, values, "pos", 1, false);
					}										
				} //endFor
				
				//empty ngram list and add remaining ngrams to the feature list
				while (!ngrams.isEmpty())
				{
					String ng = featureFromArray(ngrams, "lemma");
					
					//if the current lemma is in the ngram list activate the feature in the vector
					if (rsltdata.attribute(ng) != null)
					{
						addNumericToFeatureVector (ng, values, 1); //tokNum
					}
					
					// polarity lexicons
					if (params.containsKey("polarLexiconGeneral") || params.containsKey("polarLexiconDomain"))
					{
						checkPolarityLexicons(ng, values, tokNum, polNgrams);
					} //end polarity ngram checker

					ngrams.removeFirst();
				}
				
				//empty pos ngram list and add remaining pos ngrams to the feature list
				checkNgramFeatures(posNgrams, values, "pos", 1, true);
				
			}						
			
			// add sentence length as a feature
			if (params.containsKey("sentenceLength") && (! params.getProperty("sentenceLength").equalsIgnoreCase("no")))
			{				
				values[rsltdata.attribute("sentenceLength").index()]=tokNum;
			}
			
			//create object for the current instance and associate it with the current train dataset.			
			Instance inst = new SparseInstance(1.0, values);
			inst.setDataset(rsltdata);
			
			// add category attributte values
			String cat = trainExamples.get(oId).getCategory();
		
			if (params.containsKey("categories") && params.getProperty("categories").compareTo("E&A")==0)
			{
				if (cat.compareTo("NULL")==0)
				{
					inst.setValue(rsltdata.attribute("entCat").index(), cat);
					inst.setValue(rsltdata.attribute("attCat").index(), cat);	
				}
				else
				{
					String[] splitCat = cat.split("#");
					inst.setValue(rsltdata.attribute("entCat").index(), splitCat[0]);
					inst.setValue(rsltdata.attribute("attCat").index(), splitCat[1]);
				}
				
				//inst.setValue(attIndexes.get("entAttCat"), cat);
			}
			else if (params.containsKey("categories") && params.getProperty("categories").compareTo("E#A")==0)
			{
				inst.setValue(rsltdata.attribute("entAttCat").index(), cat);
			}
			
			
			if (params.containsKey("polarity") && params.getProperty("polarity").compareTo("yes")==0)
			{
				// add class value as a double (Weka stores all values as doubles )
				String pol = normalizePolarity(trainExamples.get(oId).getPolarity());
				//System.err.println("Features::loadInstances - pol "+pol+" for oid "+oId+" - text:"+corpus.getOpinionSentence(oId));
				if (pol != null && !pol.isEmpty())
				{
					//System.err.println("polarity: _"+pol+"_");
					inst.setValue(rsltdata.attribute("polarityCat"), pol);
				}
				else
				{
					inst.setMissing(rsltdata.attribute("polarityCat"));
				}
			}
			
			//add instance to train data
			rsltdata.add(inst);
						
			//store opinion Id and instance Id
			this.opInst.put(oId, instId);
			instId++;
		}

		System.err.println("Features : loadInstances() - training data ready total number of examples -> "+trainExamplesNum+
				" - "+rsltdata.numInstances());
		
		if (save)
		{
			try {		
				savePath = savePath+".arff";
				System.err.println("arff written to: "+savePath);
				ArffSaver saver = new ArffSaver();
		
				saver.setInstances(rsltdata);
		
				saver.setFile(new File(savePath));					
				saver.writeBatch();			 
			} catch (IOException e1) {			
				e1.printStackTrace();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return rsltdata;
	}
	
	
	
	/**
	 *   Function fills the attribute vectors for the instances existing in the Conll tabulated formatted corpus given. 
	 *   Attribute vectors contain the features loaded by the creatFeatureSet() function.
	 * 
	 * @param boolean save : whether the Instances file should be saved to an arff file or not.
	 * @return Weka Instances object containing the attribute vectors filled with the features specified
	 * 			in the parameter file.
	 */
	public Instances loadInstancesTAB (boolean save, String prefix)
	{
		String savePath = params.getProperty("fVectorDir")+File.separator+"arff"+File.separator+"train_"+prefix;
		HashMap<String, Opinion> trainExamples = corpus.getOpinions();
				
		int trainExamplesNum = trainExamples.size();

		int bowWin=0;
		if (params.containsKey("window"))
		{
			bowWin = Integer.parseInt(params.getProperty("window"));
			savePath = savePath+"_w"+bowWin;		
		}
		
		//System.out.println("train examples: "+trainExamplesNum);
		//Create the Weka object for the training set
        Instances rsltdata = new Instances("train", atts, trainExamplesNum);
        
        // setting class attribute (last attribute in train data.
        //traindata.setClassIndex(traindata.numAttributes() - 1);
		
		System.err.println("Features: loadInstancesTAB() - featNum: "+this.featNum+" - trainset attrib num -> "+rsltdata.numAttributes()+" - ");
		System.out.println("Features: loadInstancesTAB() - featNum: "+this.featNum+" - trainset attrib num -> "+rsltdata.numAttributes()+" - ");
				
		int instId = 1;
		// fill the vectors for each training example
		for (String oId : trainExamples.keySet())
		{									
			//System.err.println("sentence: "+ corpus.getOpinionSentence(o.getId()));
			
			//value vector
			double[] values = new double[featNum];
			
			// first element is the instanceId			
			values[rsltdata.attribute("instanceId").index()] = instId;  
			
			
			
			LinkedList<String> ngrams = new LinkedList<String>();
			int ngramDim;
			try {
				ngramDim = Integer.valueOf(params.getProperty("wfngrams"));			
			} catch (Exception e){
				ngramDim = 0;
			}
			
			boolean polNgrams=false;
			if (params.containsKey("polNgrams"))
			{
				polNgrams=params.getProperty("polNgrams").equalsIgnoreCase("yes");
			}
			
			
			String[] noWindow = corpus.getOpinionSentence(oId).split("\n");
			
			//counter for opinion sentence token number. Used for computing relative values of the features
			int tokNum=noWindow.length;
			
			List<String> window = Arrays.asList(noWindow); 
			Integer end = corpus.getOpinion(oId).getTo();		
			// apply window if window active (>0) and if the target is not null (to=0)
			if ((bowWin > 0) && (end > 0))
			{
				Integer start = corpus.getOpinion(oId).getFrom();
				Integer from = start - bowWin;
				if (from < 0)
				{
					from = 0;
				}
				Integer to = end+bowWin;
				if (to > noWindow.length-1)
				{
					to = noWindow.length-1;
				}
				window = Arrays.asList(Arrays.copyOfRange(noWindow, from, to));
			}
			
			//System.out.println("Sentence: "+corpus.getOpinionSentence(oId)+" - target: "+corpus.getOpinion(oId).getTarget()+
			//		"\n window: from-> "+window.get(0).getForm()+" to-> "+window.get(window.size()-1)+" .\n");
			
			//System.err.println(Arrays.toString(window.toArray()));
			
			// word form ngram related features
			for (String wf : window)
			{					
				String[] fields = wf.split("\t"); 
				String wfStr = normalize(fields[0], params.getProperty("normalization", "none"));
				// blank line means we found a sentence end. Empty n-gram list and reiniciate.  
				if (wf.equals(""))
				{
					// add ngrams to the feature vector
					checkNgramFeatures(ngrams, values, "", 1, true); //toknum
					
					// since wf is empty no need to check for clusters and other features.
					continue;
				}
				
				
				if (params.containsKey("wfngrams") && ngramDim > 0)
				{
					if (! savePath.contains("_wf"+ngramDim))
					{
						savePath = savePath+"_wf"+ngramDim;
					}
					//if the current word form is in the ngram list activate the feature in the vector
					if (ngrams.size() >= ngramDim)
					{
						ngrams.removeFirst();
					}
					ngrams.add(wfStr);

					// add ngrams to the feature vector
					checkNgramFeatures(ngrams, values, "", 1, false); //toknum
				}
				// Clark cluster info corresponding to the current word form
				if (params.containsKey("clark") && attributeSets.get("ClarkCl").containsKey(wfStr))
				{
					if (! savePath.contains("_cl"))
					{
						savePath = savePath+"_cl";
					}
					values[rsltdata.attribute("ClarkClId_"+attributeSets.get("ClarkCl").get(wfStr)).index()]++;					
				}
				
				// Clark cluster info corresponding to the current word form
				if (params.containsKey("brown") && attributeSets.get("BrownCl").containsKey(wfStr))
				{
					if (! savePath.contains("_br"))
					{
						savePath = savePath+"_br";
					}
					values[rsltdata.attribute("BrownClId_"+attributeSets.get("BrownCl").get(wfStr)).index()]++;
				}
				
				// Clark cluster info corresponding to the current word form
				if (params.containsKey("word2vec") && attributeSets.get("w2vCl").containsKey(wfStr))
				{
					if (! savePath.contains("_w2v"))
					{
						savePath = savePath+"_w2v";
					}
					values[rsltdata.attribute("w2vClId_"+attributeSets.get("w2vCl").get(wfStr)).index()]++;
				}

			}
			
			//empty ngram list and add remaining ngrams to the feature list
			checkNgramFeatures(ngrams, values, "", 1, true); //toknum
			
			// PoS tagger related attributes: lemmas and pos tags
			if (params.containsKey("lemmaNgrams") || 
					(params.containsKey("pos") && !params.getProperty("pos").equalsIgnoreCase("0")) ||
					params.containsKey("polarLexiconGeneral") ||
					params.containsKey("polarLexiconDomain"))
			{
				ngrams = new LinkedList<String>();
				if (params.containsKey("lemmaNgrams")&& (!params.getProperty("lemmaNgrams").equalsIgnoreCase("0")))
				{
					ngramDim = Integer.valueOf(params.getProperty("lemmaNgrams"));
				}
				else
				{
					ngramDim = 3;
				}
				LinkedList<String> posNgrams = new LinkedList<String>();
				int posNgramDim =0;
				if (params.containsKey("pos"))
				{
					posNgramDim = Integer.valueOf(params.getProperty("pos"));
				}
								
				for (String t : window)
				{						
					//lemmas // && (!params.getProperty("lemmaNgrams").equalsIgnoreCase("0"))
					if ((params.containsKey("lemmaNgrams")) || params.containsKey("polarLexiconGeneral") || params.containsKey("polarLexiconDomain"))
					{
						if (! savePath.contains("_l"+ngramDim))
						{
							savePath = savePath+"_l"+ngramDim;
						}
						
						//blank line means we found a sentence end. Empty n-gram list and reiniciate.
						if (t.equals(""))
						{
							// check both lemma n-grams and polarity lexicons, and add values to the feature vector
							checkNgramsAndPolarLexicons(ngrams, values, "lemma", 1,tokNum, true, polNgrams); //toknum
														
							// since t is empty no need to check for clusters and other features.
							continue;
						}
						
						String[] fields = t.split("\t");
						if (fields.length < 2)
						{
							continue;
						}
						String lemma = normalize(fields[1], params.getProperty("normalization", "none"));
						
						
						if (ngrams.size() >= ngramDim)
						{
							ngrams.removeFirst();
						}
						ngrams.add(lemma);
				       
						// check both lemma n-grams and polarity lexicons, and add values to the feature vector
						checkNgramsAndPolarLexicons(ngrams, values, "lemma", 1,tokNum, false, polNgrams);

					}
					
					//pos tags
					if (params.containsKey("pos") && !params.getProperty("pos").equalsIgnoreCase("0"))
					{
						if (! savePath.contains("_p"))
						{
							savePath = savePath+"_p";
						}
						
						if (posNgrams.size() >= posNgramDim)
						{
							posNgrams.removeFirst();
						}
						
						String[] fields = t.split("\t");
						if (fields.length < 3)
						{
							continue;
						}
						String pos = fields[2];
						
						
						posNgrams.add(pos);
						
						// add ngrams to the feature vector
						checkNgramFeatures(posNgrams, values, "pos", 1, false);
					}										
				} //endFor
				
				//empty ngram list and add remaining ngrams to the feature list
				// check both lemma n-grams and polarity lexicons, and add values to the feature vector
				checkNgramsAndPolarLexicons(ngrams, values,"", 1,tokNum, true, polNgrams);
				
				//empty pos ngram list and add remaining pos ngrams to the feature list
				checkNgramFeatures(posNgrams, values, "pos", 1, true);
			
			}						
			
			// add sentence length as a feature
			if (params.containsKey("sentenceLength") && (! params.getProperty("sentenceLength").equalsIgnoreCase("no")))
			{				
				values[rsltdata.attribute("sentenceLength").index()]=tokNum;
			}
			
			// compute uppercase ratio before normalization (if needed)		
			//double upRatio =0.0;
			//if (params.getProperty("upperCaseRatio", "no").equalsIgnoreCase("yes"))
			//{
			//	String upper = opNormalized.replaceAll("[a-z]", "");
			//	upRatio = (double)upper.length() / (double)opNormalized.length();
			//	values[rsltdata.attribute("upperCaseRation").index()] = upRatio;
			//}
			
			
			
			
			
			//create object for the current instance and associate it with the current train dataset.			
			Instance inst = new SparseInstance(1.0, values);
			inst.setDataset(rsltdata);
			
			// add category attributte values
			String cat = trainExamples.get(oId).getCategory();
		
			if (params.containsKey("categories") && params.getProperty("categories").compareTo("E&A")==0)
			{
				if (cat.compareTo("NULL")==0)
				{
					inst.setValue(rsltdata.attribute("entCat").index(), cat);
					inst.setValue(rsltdata.attribute("attCat").index(), cat);	
				}
				else
				{
					String[] splitCat = cat.split("#");
					inst.setValue(rsltdata.attribute("entCat").index(), splitCat[0]);
					inst.setValue(rsltdata.attribute("attCat").index(), splitCat[1]);
				}
				
				//inst.setValue(attIndexes.get("entAttCat"), cat);
			}
			else if (params.containsKey("categories") && params.getProperty("categories").compareTo("E#A")==0)
			{
				inst.setValue(rsltdata.attribute("entAttCat").index(), cat);
			}
			
			
			if (params.containsKey("polarity") && params.getProperty("polarity").compareTo("yes")==0)
			{
				// add class value as a double (Weka stores all values as doubles )
				String pol = normalizePolarity(trainExamples.get(oId).getPolarity());
				if (pol != null && !pol.isEmpty())
				{
					inst.setValue(rsltdata.attribute("polarityCat"), pol);
				}
				else
				{
					//System.err.println("polarity: _"+pol+"_");
					inst.setMissing(rsltdata.attribute("polarityCat"));
				}
			}
			
			//add instance to train data
			rsltdata.add(inst);
						
			//store opinion Id and instance Id
			this.opInst.put(oId, instId);
			instId++;
		}

		System.err.println("Features : loadInstancesTAB() - training data ready total number of examples -> "+trainExamplesNum+
				" - "+rsltdata.numInstances());
		
		if (save)
		{
			try {		
				savePath = savePath+".arff";
				System.err.println("arff written to: "+savePath);
				ArffSaver saver = new ArffSaver();
		
				saver.setInstances(rsltdata);
		
				saver.setFile(new File(savePath));					
				saver.writeBatch();			 
			} catch (IOException e1) {			
				e1.printStackTrace();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		
		return rsltdata;
	}
	

	/**
	 *   Function fills the attribute vectors for the instances existing in the Conll tabulated formatted corpus given. 
	 *   Attribute vectors contain the features loaded by the creatFeatureSet() function.
	 * 
	 * @param boolean save : whether the Instances file should be saved to an arff file or not.
	 * @return Weka Instances object containing the attribute vectors filled with the features specified
	 * 			in the parameter file.
	 */
	public Instances loadInstancesConll (boolean save, String prefix)
	{
		String savePath = params.getProperty("fVectorDir")+File.separator+"arff"+File.separator+"train_"+prefix;
		HashMap<String, Opinion> trainExamples = corpus.getOpinions();
			
		String nafdir = params.getProperty("kafDir");
		int trainExamplesNum = trainExamples.size();

		int bowWin=0;
		if (params.containsKey("window"))
		{
			bowWin = Integer.parseInt(params.getProperty("window"));
			savePath = savePath+"_w"+bowWin;		
		}
		
		//System.out.println("train examples: "+trainExamplesNum);
		//Create the Weka object for the training set
        Instances rsltdata = new Instances("train", atts, trainExamplesNum);
        
        // setting class attribute (last attribute in train data.
        //traindata.setClassIndex(traindata.numAttributes() - 1);
		
		System.err.println("Features: loadInstancesConll() - featNum: "+this.featNum+" - trainset attrib num -> "+rsltdata.numAttributes()+" - ");
		System.out.println("Features: loadInstancesConll() - featNum: "+this.featNum+" - trainset attrib num -> "+rsltdata.numAttributes()+" - ");
				
		int instId = 1;
		// fill the vectors for each training example
		for (String oId : trainExamples.keySet())
		{									
			//System.err.println("sentence: "+ corpus.getOpinionSentence(o.getId()));
			
			//value vector
			double[] values = new double[featNum];
			
			// first element is the instanceId			
			values[rsltdata.attribute("instanceId").index()] = instId;  
			
			
			
			LinkedList<String> ngrams = new LinkedList<String>();
			int ngramDim;
			try {
				ngramDim = Integer.valueOf(params.getProperty("wfngrams"));			
			} catch (Exception e){
				ngramDim = 0;
			}
			
			boolean polNgrams=false;
			if (params.containsKey("polNgrams"))
			{
				polNgrams=params.getProperty("polNgrams").equalsIgnoreCase("yes");
			}
			
			String nafPath = nafdir+File.separator+trainExamples.get(oId).getsId().replace(':', '_');
			String taggedFile ="";
			try {
				if (!FileUtilsElh.checkFile(nafPath+".kaf"))
				{	
        			nafPath = NLPpipelineWrapper.tagSentence(corpus.getOpinionSentence(oId), nafPath, corpus.getLang(),  params.getProperty("pos-model"), params.getProperty("lemma-model"), postagger);
				}
				else
				{
					nafPath =nafPath+".kaf";
				}
				InputStream reader = new FileInputStream(new File(nafPath));
				taggedFile = IOUtils.toString(reader); 
				reader.close();				
			} catch (IOException | JDOMException fe) {
				// TODO Auto-generated catch block
				fe.printStackTrace();
			} 
			
			String[] noWindow = taggedFile.split("\n");
			
			//counter for opinion sentence token number. Used for computing relative values of the features
			int tokNum=noWindow.length;
			
			//System.err.println("Features::loadInstancesConll - tagged File read lines:"+tokNum);
			
			List<String> window = Arrays.asList(noWindow); 
			Integer end = corpus.getOpinion(oId).getTo();		
			// apply window if window active (>0) and if the target is not null (to=0)
			if ((bowWin > 0) && (end > 0))
			{
				Integer start = corpus.getOpinion(oId).getFrom();
				Integer from = start - bowWin;
				if (from < 0)
				{
					from = 0;
				}
				Integer to = end+bowWin;
				if (to > noWindow.length-1)
				{
					to = noWindow.length-1;
				}
				window = Arrays.asList(Arrays.copyOfRange(noWindow, from, to));
			}
			
			//System.out.println("Sentence: "+corpus.getOpinionSentence(oId)+" - target: "+corpus.getOpinion(oId).getTarget()+
			//		"\n window: from-> "+window.get(0).getForm()+" to-> "+window.get(window.size()-1)+" .\n");
			
			//System.err.println(Arrays.toString(window.toArray()));
			
			// word form ngram related features
			for (String wf : window)
			{					
				String[] fields = wf.split("\\s"); 
				String wfStr = normalize(fields[0], params.getProperty("normalization", "none"));
				// blank line means we found a sentence end. Empty n-gram list and reiniciate.  
				if (wf.equals(""))
				{
					// add ngrams to the feature vector
					checkNgramFeatures(ngrams, values, "", 1, true); //toknum
					
					// since wf is empty no need to check for clusters and other features.
					continue;
				}
				
				
				if (params.containsKey("wfngrams") && ngramDim > 0)
				{
					if (! savePath.contains("_wf"+ngramDim))
					{
						savePath = savePath+"_wf"+ngramDim;
					}
					//if the current word form is in the ngram list activate the feature in the vector
					if (ngrams.size() >= ngramDim)
					{
						ngrams.removeFirst();
					}
					ngrams.add(wfStr);

					// add ngrams to the feature vector
					checkNgramFeatures(ngrams, values, "", 1, false); //toknum
				}
				// Clark cluster info corresponding to the current word form
				if (params.containsKey("clark") && attributeSets.get("ClarkCl").containsKey(wfStr))
				{
					if (! savePath.contains("_cl"))
					{
						savePath = savePath+"_cl";
					}
					values[rsltdata.attribute("ClarkClId_"+attributeSets.get("ClarkCl").get(wfStr)).index()]++;					
				}
				
				// Clark cluster info corresponding to the current word form
				if (params.containsKey("brown") && attributeSets.get("BrownCl").containsKey(wfStr))
				{
					if (! savePath.contains("_br"))
					{
						savePath = savePath+"_br";
					}
					values[rsltdata.attribute("BrownClId_"+attributeSets.get("BrownCl").get(wfStr)).index()]++;
				}
				
				// Clark cluster info corresponding to the current word form
				if (params.containsKey("word2vec") && attributeSets.get("w2vCl").containsKey(wfStr))
				{
					if (! savePath.contains("_w2v"))
					{
						savePath = savePath+"_w2v";
					}
					values[rsltdata.attribute("w2vClId_"+attributeSets.get("w2vCl").get(wfStr)).index()]++;
				}

			}
			
			//empty ngram list and add remaining ngrams to the feature list
			checkNgramFeatures(ngrams, values, "", 1, true); //toknum
			
			// PoS tagger related attributes: lemmas and pos tags
			if (params.containsKey("lemmaNgrams") || 
					(params.containsKey("pos") && !params.getProperty("pos").equalsIgnoreCase("0")) ||
					params.containsKey("polarLexiconGeneral") ||
					params.containsKey("polarLexiconDomain"))
			{
				ngrams = new LinkedList<String>();
				if (params.containsKey("lemmaNgrams")&& (!params.getProperty("lemmaNgrams").equalsIgnoreCase("0")))
				{
					ngramDim = Integer.valueOf(params.getProperty("lemmaNgrams"));
				}
				else
				{
					ngramDim = 3;
				}
				LinkedList<String> posNgrams = new LinkedList<String>();
				int posNgramDim =0;
				if (params.containsKey("pos"))
				{
					posNgramDim = Integer.valueOf(params.getProperty("pos"));
				}
								
				for (String t : window)
				{						
					//lemmas // && (!params.getProperty("lemmaNgrams").equalsIgnoreCase("0"))
					if ((params.containsKey("lemmaNgrams")) || params.containsKey("polarLexiconGeneral") || params.containsKey("polarLexiconDomain"))
					{
						if (! savePath.contains("_l"+ngramDim))
						{
							savePath = savePath+"_l"+ngramDim;
						}
						
						//blank line means we found a sentence end. Empty n-gram list and reiniciate.
						if (t.equals(""))
						{
							// check both lemma n-grams and polarity lexicons, and add values to the feature vector
							checkNgramsAndPolarLexicons(ngrams, values, "lemma", 1,tokNum, true, polNgrams); //toknum
														
							// since t is empty no need to check for clusters and other features.
							continue;
						}
						
						String[] fields = t.split("\\s");
						if (fields.length < 2)
						{
							continue;
						}
						String lemma = normalize(fields[1], params.getProperty("normalization", "none"));
						
						
						if (ngrams.size() >= ngramDim)
						{
							ngrams.removeFirst();
						}
						ngrams.add(lemma);
				       
						// check both lemma n-grams and polarity lexicons, and add values to the feature vector
						checkNgramsAndPolarLexicons(ngrams, values, "lemma", 1,tokNum, false, polNgrams);

					}
					
					//pos tags
					if (params.containsKey("pos") && !params.getProperty("pos").equalsIgnoreCase("0"))
					{
						if (! savePath.contains("_p"))
						{
							savePath = savePath+"_p";
						}
						
						if (posNgrams.size() >= posNgramDim)
						{
							posNgrams.removeFirst();
						}
						
						String[] fields = t.split("\\s");
						if (fields.length < 3)
						{
							continue;
						}
						String pos = fields[2];
						
						
						posNgrams.add(pos);
						
						// add ngrams to the feature vector
						checkNgramFeatures(posNgrams, values, "pos", 1, false);
					}										
				} //endFor
				
				//empty ngram list and add remaining ngrams to the feature list
				// check both lemma n-grams and polarity lexicons, and add values to the feature vector
				checkNgramsAndPolarLexicons(ngrams, values,"", 1,tokNum, true, polNgrams);
				
				//empty pos ngram list and add remaining pos ngrams to the feature list
				checkNgramFeatures(posNgrams, values, "pos", 1, true);
			
			}						
			
			// add sentence length as a feature
			if (params.containsKey("sentenceLength") && (! params.getProperty("sentenceLength").equalsIgnoreCase("no")))
			{				
				values[rsltdata.attribute("sentenceLength").index()]=tokNum;
			}
			
			// compute uppercase ratio before normalization (if needed)		
			//double upRatio =0.0;
			//if (params.getProperty("upperCaseRatio", "no").equalsIgnoreCase("yes"))
			//{
			//	String upper = opNormalized.replaceAll("[a-z]", "");
			//	upRatio = (double)upper.length() / (double)opNormalized.length();
			//	values[rsltdata.attribute("upperCaseRation").index()] = upRatio;
			//}
			
			
			
			
			
			//create object for the current instance and associate it with the current train dataset.			
			Instance inst = new SparseInstance(1.0, values);
			inst.setDataset(rsltdata);
			
			// add category attributte values
			String cat = trainExamples.get(oId).getCategory();
		
			if (params.containsKey("categories") && params.getProperty("categories").compareTo("E&A")==0)
			{
				if (cat.compareTo("NULL")==0)
				{
					inst.setValue(rsltdata.attribute("entCat").index(), cat);
					inst.setValue(rsltdata.attribute("attCat").index(), cat);	
				}
				else
				{
					String[] splitCat = cat.split("#");
					inst.setValue(rsltdata.attribute("entCat").index(), splitCat[0]);
					inst.setValue(rsltdata.attribute("attCat").index(), splitCat[1]);
				}
				
				//inst.setValue(attIndexes.get("entAttCat"), cat);
			}
			else if (params.containsKey("categories") && params.getProperty("categories").compareTo("E#A")==0)
			{
				inst.setValue(rsltdata.attribute("entAttCat").index(), cat);
			}
			
			
			if (params.containsKey("polarity") && params.getProperty("polarity").compareTo("yes")==0)
			{
				// add class value as a double (Weka stores all values as doubles )
				String pol = normalizePolarity(trainExamples.get(oId).getPolarity());
				if (pol != null && !pol.isEmpty())
				{
					inst.setValue(rsltdata.attribute("polarityCat"), pol);
				}
				else
				{
					//System.err.println("polarity: _"+pol+"_");
					inst.setMissing(rsltdata.attribute("polarityCat"));
				}
			}
			
			//add instance to train data
			rsltdata.add(inst);
						
			//store opinion Id and instance Id
			this.opInst.put(oId, instId);
			instId++;
		}

		System.err.println("Features : loadInstancesConll() - training data ready total number of examples -> "+trainExamplesNum+
				" - "+rsltdata.numInstances());
		
		if (save)
		{
			try {		
				savePath = savePath+".arff";
				System.err.println("arff written to: "+savePath);
				ArffSaver saver = new ArffSaver();
		
				saver.setInstances(rsltdata);
		
				saver.setFile(new File(savePath));					
				saver.writeBatch();			 
			} catch (IOException e1) {			
				e1.printStackTrace();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		
		return rsltdata;
	}
	
	
	/**
	 * normalizePolarity maps polarity categories to the categories defined in this.classificationClasses;
	 *  
	 * @param polarity
	 * @return String :  normalized polarity string
	 */
	private String normalizePolarity(String polarity) {
		
		if (polarity==null)
		{
			return null;
		}
		// normalize all 
		polarity = polarity.replaceFirst("^(?i)pos(\\+)?$", "positive$1");
		polarity = polarity.replaceFirst("^(?i)neg(\\+)?$", "negative$1");
		polarity = polarity.replaceFirst("^(?i)p(\\+)?$", "positive$1");
		polarity = polarity.replaceFirst("^(?i)n(\\+)?$", "negative$1");
		polarity = polarity.replaceFirst("^(?i)neu$", "neutral");
		polarity = polarity.replaceFirst("^\\+$", "positive");
		polarity = polarity.replaceFirst("\\-$", "negative");
		polarity = polarity.replaceFirst("^\\=$", "neutral");
		polarity = polarity.replaceFirst("^(?i)none$", "none");
		
		switch (this.ClassificationClasses.size())
		{
		// binary (dummy,p,n) - 'none' and 'neutral' examples are discarded.
		case 3: 			
			polarity = polarity.replaceFirst("^neutral$", "");
			polarity = polarity.replaceFirst("^none$", "");
			polarity = polarity.replaceFirst("\\+$", "");
			break;
		// 3 classes (dummy,p,n,neu) - 'none' examples are treated as neutral ones.				
		case 4:			 
			polarity = polarity.replaceFirst("^none$", "neutral");
			polarity = polarity.replaceFirst("\\+$", "");
			break;
		// 3+ classes (dummy,p,n,neu,none)				
		case 5:
			polarity = polarity.replaceFirst("\\+$", "");
			break;
		// 5 classes (dummy,p,n,neu,p+,n+) 			
		case 6: 
			// none examples are treated as neutral ones. 
			polarity = polarity.replaceFirst("^none$", "neutral");
			break;
		// 5+ classes (dummy,p,n,neu,p+,n+,none) nothing to do.				
		case 7:
			break;				
		}
		if (polarity.equalsIgnoreCase(""))
		{
			return null;
		}
		else
		{
			return polarity;
		}
	}
	
	
	/**
	 * @param lemma
	 * @return TreeSet<String> containing the unigrams extracted from the opinions. No NLP chain is used.
	 *  
	 * @deprecated use {@extractWfNgrams(int length, KAFDocument kaf)} instead.  
	 */
	@Deprecated
	public TreeSet<String> extract1gramsOldNoNLPchain(String lemma)
	{
		TreeSet<String> result = new TreeSet<String>();
        System.err.println("unigram extraction: _"+lemma+"_");
        // Word form unigrams are required
        if (lemma.equalsIgnoreCase("wform"))
        {
        	for (String sent : corpus.getSentences().values()) 
        	{ 
        		String[] split = sent.split(" ");
				for (String w : split)
				{
					String w_nopunct = w.replaceAll("[^\\p{L}\\p{M}\\p{Nd}]", "");
					result.add(w_nopunct);
				}
			}
        }                
        return result;
	}

	
	/**
	 *  Extract n-grams up to a certain length from an Conll tabulated format corpus.
	 * 
	 * @param int length : which 'n' use for 'n-grams' 
	 * @param string type (wf|lemma|pos): what type of ngrams we want to extract.
	 * @param boolean save : safe ngrams to file or not. 
	 * @return TreeSet<String> return word form ngrams of length length
	 */
	private int extractNgramsTAB(int length, String type, List<String> discardPos, boolean save)
	{		
        //System.err.println("ngram extraction Tab: _"+length+"_"+type);
        if (length == 0)
        {
        	return 0;
        }
        
        for (String sent : corpus.getSentences().keySet())
        {
            //System.err.println("ngram extraction, corpus sentences: "+corpus.getSentences().get(sent));        	
        	String[] tokens = corpus.getSentences().get(sent).split("\n");
        	LinkedList<String> ngrams = new LinkedList<String>();
        	for (String row : tokens)
        	{
        		String ngram = "";
        		String[] fields = row.split("\t");
        		String pos = "";        		
        		switch (type)
        		{
        		case "wf": ngram = fields[0]; break;
        		case "lemma": 
        				if (fields.length>1){ngram = fields[1];} 
        				if (fields.length>2){pos=fields[2];} 
        				break;
        		case "pos": 
        				if (fields.length>2){
        					ngram = fields[2];
        					switch (ngram.length())
        					{
        					case 0: ngram = "-"; break;
   							case 1: ngram = ngram.substring(0,1); break;
   							default: ngram = ngram.substring(0,2); break;
        					}
        				}
        		}

        		//if the is a blank line we assume sentence has ended and we empty and re-initialize the n-gram list 
        		if (ngram.equals(""))
        		{
        			//empty n-gram list and add remaining n-grams to the feature list
        			while (!ngrams.isEmpty())
        			{
        				String ng = featureFromArray(ngrams, type);
        				addNgram (type, ng);        				
        				ngrams.removeFirst();
        			}        
        			continue;
        		}
        		
        		if (ngrams.size() >= length)
        		{
        			ngrams.removeFirst();
        		}
        		
        		//if no alphanumeric char is present discard the element as invalid ngram. Or if it has a PoS tag that
        		//should be discarded
        		String lCurrent = ngram;
        		if ((!discardPos.contains(pos)) && (!ngram.matches("^[^\\p{L}\\p{M}\\p{Nd}\\p{InEmoticons}]+$")) && (lCurrent.length()>1))
        		{
        			//standarize numeric values to NUMNUM lemma value
        			//ngram.replaceFirst("^[0-9]$", "NUMNUM");
        			if (!type.equalsIgnoreCase("pos"))
        			{
        				ngrams.add(normalize(ngram, params.getProperty("normalization", "none")));
        			}
        			else
        			{
        				ngrams.add(ngram);
        			}
        		}  
        		//certain punctuation marks are allowed as lemmas
        		else if ((lCurrent.length()<2) && (lCurrent.matches("[,;.?!]")))
        		{        		
        			ngrams.add(lCurrent);
        		}        		
        		
        		// add ngrams to the feature list
        		for (int i=0;i<ngrams.size();i++)
        		{
        			String ng = featureFromArray(ngrams.subList(0, i+1), type);    				
    				addNgram (type, ng);        				
        		}
        	}
        	//empty ngram list and add remaining ngrams to the feature list
        	while (!ngrams.isEmpty())
        	{
        		String ng = featureFromArray(ngrams, type);
				addNgram (type, ng);        				
				ngrams.removeFirst();
        	}        
        }
        return 1;
	}
	
	
	/**
	 *  Extract n-grams up to a certain length from an Conll tabulated format string.
	 * 
	 * @param String input : input tagged conll string 
	 * @param int length : which 'n' use for 'n-grams' 
	 * @param string type (wf|lemma|pos): what type of ngrams we want to extract.
	 * @param boolean save : safe ngrams to file or not. 
	 * @return int success: return 1 if the process ended correctly
	 */
	private int extractNgramsTABString(InputStream input, int length, String type, List<String> discardPos, boolean save)
	{		
		//System.err.println("ngram extraction Tab: _"+length+"_"+type);
		if (length == 0)
		{
			return 0;
		}

		//System.err.println("ngram extraction, corpus sentences: "+corpus.getSentences().get(sent));        			
		//String[] tokens = input.split("\n");
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		LinkedList<String> ngrams = new LinkedList<String>();
		String line;
		try {
			while ((line=reader.readLine()) != null)
			{
				String ngram = "";
				String[] fields = line.split("\\s");
				String pos = "";        		
				switch (type)
				{
				case "wf": ngram = fields[0]; break;
				case "lemma": 
					if (fields.length>1){ngram = fields[1];} 
					if (fields.length>2){pos=fields[2];} 
					break;
				case "pos": 
					if (fields.length>2){
						ngram = fields[2];
						switch (ngram.length())
						{
						case 0: ngram = "-"; break;
						case 1: ngram = ngram.substring(0,1); break;
						default: ngram = ngram.substring(0,2); break;
						}
					}
				}

				//if the is a blank line we assume sentence has ended and we empty and re-initialize the n-gram list 
				if (ngram.equals(""))
				{
					//empty n-gram list and add remaining n-grams to the feature list
					while (!ngrams.isEmpty())
					{
						String ng = featureFromArray(ngrams, type);
						addNgram (type, ng);        				
						ngrams.removeFirst();
					}        
					continue;
				}

				if (ngrams.size() >= length)
				{
					ngrams.removeFirst();
				}

				//if no alphanumeric char is present discard the element as invalid ngram. Or if it has a PoS tag that
				//should be discarded
				String lCurrent = ngram;
				if ((!discardPos.contains(pos)) && (!ngram.matches("^[^\\p{L}\\p{M}\\p{Nd}\\p{InEmoticons}]+$")) && (lCurrent.length()>1))
				{
					//standarize numeric values to NUMNUM lemma value
					//ngram.replaceFirst("^[0-9]$", "NUMNUM");
					if (!type.equalsIgnoreCase("pos"))
					{
						ngrams.add(normalize(ngram, params.getProperty("normalization", "none")));
					}
					else
					{
						ngrams.add(ngram);
					}
				}  
				//certain punctuation marks are allowed as lemmas
				else if ((lCurrent.length()<2) && (lCurrent.matches("[,;.?!]")))
				{        		
					ngrams.add(lCurrent);
				}        		

				// add ngrams to the feature list
				for (int i=0;i<ngrams.size();i++)
				{
					String ng = featureFromArray(ngrams.subList(0, i+1), type);    				
					addNgram (type, ng);        				
				}
			}
		} catch (IOException e) {
			System.err.println("EliXa::Features::extractNgramsTABString - WARNING: Error reading tagged file, "
					+ "ngram extraction may be only partial\n");			
		}
		
		//empty ngram list and add remaining ngrams to the feature list
		while (!ngrams.isEmpty())
		{
			String ng = featureFromArray(ngrams, type);
			addNgram (type, ng);        				
			ngrams.removeFirst();
		}        

		return 1;
	}
		
	
	/**
	 *  Extract word form n-grams up to a certain length from a kaf/naf file
	 * 
	 * @param int length : which 'n' use for 'n-grams' 
	 * @param KAFDocument kafDoc : postagged kaf document to extract ngrams from.
	 * @param boolean save : safe ngrams to file or not. 
	 * @return TreeSet<String> return word form ngrams of length length
	 */
	private int extractWfNgramsKAF(int length, KAFDocument kafDoc, boolean save)
	{
        //System.err.println("ngram extraction: _"+length+"_");
        if (length == 0)
        {
        	return 0;
        }

        for (List<WF> sent : kafDoc.getSentences()) 
        { 
        	LinkedList<String> ngrams = new LinkedList<String>();
        	for (WF wf : sent)
        	{
        		if (ngrams.size() >= length)
        		{
        			ngrams.removeFirst();
        		}
        		ngrams.add(wf.getForm());
        		//ngrams.add(normalize(wf.getForm(), params.getProperty("normalization", "none")));
        		
        		// add ngrams to the feature list
        		for (int i=0;i<ngrams.size();i++)
        		{
        			String ng = featureFromArray(ngrams.subList(0, i+1), "wf");
        			addNgram ("wf", ng);  
        		}
        	}
        	//empty ngram list and add remaining ngrams to the feature list
        	while (!ngrams.isEmpty())
        	{
        		String ng = featureFromArray(ngrams, "wf");
        		addNgram ("wf", ng);  
        		ngrams.removeFirst();
        	}
        }        
        return 1;
	}

	
	/**
	 *     Lemma ngram extraction from a kaf document
	 * 
	 * @param int length : which 'n' use for 'n-grams' 
	 * @param KAFDocument kafDoc : postagged kaf document to extract ngrams from.
	 * @param boolean save : safe ngrams to file or not. 
	 * @return TreeSet<String> return lemma ngrams of length length
	 */
	private int extractLemmaNgrams(int length, KAFDocument kafDoc, List<String> discardPos, boolean save)
	{		
        //System.err.println("lemma ngram extraction: _"+length+"_");
        if (length == 0)
        {
        	return 0;
        }

        int sentNum = kafDoc.getSentences().size();
        for (int s=0; s<sentNum;s++) 
        { 
        	LinkedList<String> ngrams = new LinkedList<String>();
        	for (Term term : kafDoc.getTermsBySent(s))
        	{
        		if (ngrams.size() >= length)
        		{
        			ngrams.removeFirst();
        		}
        		
        		//if no alphanumeric char is present discard the element as invalid ngram. Or if it has a PoS tag that
        		//should be discarded        		
        		String lCurrent = term.getLemma();
        		if ((! discardPos.contains(term.getPos())) && (!lCurrent.matches("[^\\p{L}\\p{M}\\p{Nd}\\p{InEmoticons}]+")) && (lCurrent.length()>1))
        		{
        			ngrams.add(lCurrent);
        			//ngrams.add(normalize(term.getLemma(), params.getProperty("normalization", "none")));
        		}       		        		 
        		//certain punctuation marks and emoticons are allowed as lemmas
        		else if ((lCurrent.length()<=2) && (lCurrent.matches("[,;.?!]")))
        		{
        			ngrams.add(lCurrent);
        		}
        		
        		// add ngrams to the feature list
        		for (int i=0;i<ngrams.size();i++)
        		{        			
        			String ng = featureFromArray(ngrams.subList(0, i+1), "lemma");
        			addNgram ("lemma", ng);  
        		}
        	}
        	//empty ngram list and add remaining ngrams to the feature list
        	while (!ngrams.isEmpty())
        	{
        		String ng = featureFromArray(ngrams, "lemma");
        		addNgram ("lemma", ng);
        		ngrams.removeFirst();
        	}
        }        
        return 1;
	}
	

	
	/**
	 *     POS ngram extraction from a kaf document
	 * 
	 * @param int length : which 'n' use for 'n-grams' 
	 * @param KAFDocument kafDoc : postagged kaf document to extract ngrams from.
	 * @param boolean save : safe ngrams to file or not. 
	 * @return TreeSet<String> return lemma ngrams of length length
	 */
	public int extractPosNgrams(int length, KAFDocument kafDoc, List<String> discardPos, boolean save)
	{
		//System.err.println("POS ngram extraction: _"+length+"_");
        if (length == 0)
        {
        	return 0;
        }

        int sentNum = kafDoc.getSentences().size();
        for (int s=0; s<sentNum;s++) 
        { 
        	LinkedList<String> ngrams = new LinkedList<String>();
        	for (Term term : kafDoc.getTermsBySent(s))
        	{
        		if (ngrams.size() >= length)
        		{
        			ngrams.removeFirst();
        		}
        		
        		if (! discardPos.contains(term.getPos()))
        		{
        			ngrams.add(term.getPos());
        		}   
        		// add ngrams to the feature list
        		for (int i=0;i<ngrams.size();i++)
        		{
        			String ng = featureFromArray(ngrams.subList(0, i+1), "pos");
        			addNgram ("pos", ng);
        		}
        	}
        	//empty ngram list and add remaining ngrams to the feature list
        	while (!ngrams.isEmpty())
        	{
        		String ng = featureFromArray(ngrams, "pos");
        		addNgram ("pos", ng);
				ngrams.removeFirst();
        	}
        }        
        return 1;
	}
	
	
	/**
	 *  Help function to add one ngram and its frequence (current+1) to the corresponding structure depending 
	 *  on the ngram type.
	 * 
	 * @param type (wf|lemma|pos)
	 * @param ngram
	 */
	private void addNgram (String type, String ngram)
	{
		int freq = 0;        					
		switch (type)
		{
		case "wf": 
			freq = 0;			
			if (wfNgrams.containsKey(ngram))
			{
				freq = wfNgrams.get(ngram);
			}
			this.wfNgrams.put(ngram, freq+1); 
			break;
		case "lemma": 
			freq = 0;
			if (lemmaNgrams.containsKey(ngram))
			{
				freq = lemmaNgrams.get(ngram);
			}
			this.lemmaNgrams.put(ngram, freq+1); 
			break;

		case "pos": 
			freq = 0;
			if (POSNgrams.containsKey(ngram))
			{	
				freq = POSNgrams.get(ngram);
			}
			this.POSNgrams.put(ngram, freq+1); 
			break;
		case "default": System.err.println("Features::addNgram - wrong type, no ngram added.");
		}
	}
	
	
	/**
	 *     POS tags ngram extraction from a kaf document
	 * 
	 * @param int length : which 'n' use for 'n-grams' 
	 * @param KAFDocument kafDoc : postagged kaf document to extract ngrams from.
	 * @param boolean save : safe ngrams to file or not. 
	 * @return TreeSet<String> return lemma ngrams of length length
	 */
	public TreeSet<String> extractPOStags(KAFDocument kafDoc, boolean save)
	{
		TreeSet<String> result = new TreeSet<String>();
		for (Term term : kafDoc.getTerms())
		{
			String pos = "POS_"+term.getPos();
			// for good measure, test that the lemma is not already included as a feature
			if (! getAttIndexes().containsKey(pos))
			{
				addNumericFeature(pos);
				result.add(pos);
			}

		}
        return result;
	}
	
	
	/**
	 *  Extract category information. 
	 *  If the format "category#subcategory" is used in the annotation both 3 infos are extracted:
	 *      - category ; subcategory ; category#subcategory
	 *  otherwise only the annotation is considered as a single category.  
	 * 
	 * @return TreeSet<String>[3] structure with 3 lists, one containing E categories, other containing A
	 * 			attributes, and the other containing E#A pairs. Those opinion containing "NULL" values are will have
	 * 			"NULL#NULL" value in the third case.
	 */
	@SuppressWarnings("unchecked")
	public TreeSet<String>[] extractCategories()
	{
		
		TreeSet<String>[] result = new TreeSet[3];
		result[0] = new TreeSet<String>();
		result[1] = new TreeSet<String>();
		result[2] = new TreeSet<String>();
		
		for (Opinion op : corpus.getOpinions().values()) 
		{ 
			String entAtt = op.getCategory();
			try {
				String[] split = entAtt.split("#");
				result[0].add(split[0]);
				result[1].add(split[1]);
			} catch (NullPointerException|IndexOutOfBoundsException npe)
			{
				result[0].add("NULL");
				result[1].add("NULL");									
			}
			result[2].add(entAtt);
		}
		return result;
	}
	
	
	/**
	 * Function reads an attribute map from a file (mainly word cluster files) and adds the 
	 * 
	 * @param fname : path to the file containing the feature information
	 * @param attName : prefix for the feature name in the feature vector
	 * @return HashMap<String,Integer> contains the elements and their respective attribute values, 
	 * 			in order to later fill the vectors.
	 * 
	 * @throws IOException if the given file give reading problems.
	 */
	private HashMap<String, Integer> loadAttributeMapFromFile(String fname, String attName) 
			throws IOException
	{
		HashMap<String, Integer> result = new HashMap<String, Integer>();
		TreeSet<Integer> valueSet = new TreeSet<Integer>();
		
		if (FileUtilsElh.checkFile(fname))
		{
			BufferedReader breader = new BufferedReader(new FileReader(fname));
			String line;
			while ((line = breader.readLine()) != null) 
			{
				if (line.startsWith("#") || line.matches("^\\s*$"))
				{
					continue;
				}
				String[] fields = line.split(" ");
				Integer attValue;
				try {
					attValue = Integer.valueOf(fields[1]);
				} catch (NumberFormatException nfe){
					attValue = Integer.parseInt(fields[1],2);
				}
				result.put(fields[0], attValue);
				valueSet.add(attValue);				
			}
			breader.close();
			//add features to feature map
			addNumericFeatureSet(attName, valueSet);
		}			

		return result;
	}

	
	/**
	 * Function reads an attribute map from a file (mainly word cluster files) and adds the 
	 * 
	 * @param fname : path to the file containing the feature information
	 * @param attName : prefix for the feature name in the feature vector
	 * @return HashMap<String,HashMap<String, Double>> contains the elements and their respective attribute values, 
	 * 			in order to later fill the vectors.
	 * 
	 * @throws IOException if the given file give reading problems.
	 */
	@Deprecated
	private HashMap<String, HashMap<String, Double>> loadPolarityLexiconFromFile(String fname, String attName) 
			throws IOException
	{
		HashMap<String, HashMap<String, Double>> result = new HashMap<String, HashMap<String, Double>>();		
		
		if (FileUtilsElh.checkFile(fname))
		{
			BufferedReader breader = new BufferedReader(new FileReader(fname));
			String line;
			while ((line = breader.readLine()) != null) 
			{
				if (line.startsWith("#") || line.matches("^\\s*$"))
				{
					continue;
				}
				HashMap<String, Double> values = new HashMap<String, Double>();
				String[] fields = line.split("\t");
				double pos = 0.0;
				double neg = 0.0;
				switch (fields.length)
				{
				// not valid entry
				case 1: break; 
				// single score representation check for modifiers and shifters as well
				case 2: 					
					if (fields[1].matches("(?i:pos.*)"))
					{						
						pos = 1.0;
					} //|| Double.valueOf(fields[2])>0
					else if (fields[1].matches("(?i:neg.*)"))
					{
						neg = 1.0;
					}
					else
					{
						try {
							double sc = Double.valueOf(fields[1]);
							if (sc > 0)
							{
								pos = sc;
							}
							else
							{
								neg = sc;
							}
						} catch (NumberFormatException nfe) {
							System.err.println("Warning, lexicon entry with strange format, it maybe a modifier/shifter :"+fields[0]+" -- "+fields[1]);
						}
					}
					break;
				case 3:
					pos = Double.valueOf(fields[1]);
					neg = Double.valueOf(fields[2]);
					break;
				default: System.err.println("format error in the polarity lexicon\n"); break;	
				}
				// if both positive and negative scores are 0 we consider the word should not be in the lexicon.
				if (pos > 0 || neg > 0)
				{
					values.put("pos", pos);
					values.put("neg", neg);
				
					result.put(fields[0],values);
				}				
			}
			breader.close();

			//add features to feature map:  two features, positive|negative scores 
			addNumericFeature(attName+"posScore");
			addNumericFeature(attName+"negScore");
		}			

		return result;
	}
	
	
	/**
	 * Function reads an attribute list from a file (ngram/word list files) and adds the elements to the 
	 * attribute list 
	 * 
	 * @param fname : path to the file containing the feature information
	 * @param attName : prefix for the feature name in the feature vector
	 * @return HashMap<String,Integer> contains the elements and their respective attribute values, 
	 * 			in order to later fill the vectors.
	 * 
	 * @throws IOException if the given file give reading problems.
	 */
	private TreeSet<String> loadAttributeListFromFile(File fname, String attName) 
			throws IOException
	{		
		TreeSet<String> valueSet = new TreeSet<String>();
		
		if (FileUtilsElh.checkFile(fname))
		{
			BufferedReader breader = new BufferedReader(new FileReader(fname));
			String line;
			while ((line = breader.readLine()) != null) 
			{
				// # starting lines are ignored, considered comments. Blank lines are ignored as well
				if (line.startsWith("#") || line.matches("^\\s*$"))
				{
					continue;
				}
				// for good measure, test that the lemma is not already included as a feature
				else if (! getAttIndexes().containsKey(line))
				{
					addNumericFeature(line);
					valueSet.add(line);
				}
			}
			breader.close();			
		}			
		return valueSet;
	}
	
	/**
	 * addNumericFeature adds a numeric feature to the feature vector of the classifier
	 * @param feat
	 */
	private void addNumericFeature(String feat) {
				
		this.atts.add(new Attribute(feat, this.featNum));		
		this.attIndexes.put(feat,this.featNum);
		this.featNum++;	
	}

	/**
	 * featureFromArray converts a list of  ngram/words into a numeric feature to the feature vector of the classifier
	 * @param feat
	 * @param prefix
	 */
	private String featureFromArray(List<String> feat, String prefix) {
				
		Object[] currentNgram = feat.toArray();
		String ng = Arrays.asList(currentNgram).toString().replaceAll("(^\\[|\\]$)", "").replace(", ", "_").toLowerCase(); //.toLowerCase()
		// feature prefix
		switch (prefix)
		{
		case "wf": ng = "WF_"+ng; break;
		case "lemma": ng = "LEM_"+ng; break;                				
		case "pos":	ng = "POS_"+ng; break;
		case "" : break;
		default: ng = prefix+"_"+ng; break;
		}		
		return ng;
	}
	
	
	/**
	 * addNumericFeatureSet adds a set numeric features to the feature vector of the classifier
	 * 
	 * @param feat
	 * @param String prefix : prefix appended to each of the values to build the feature name 
	 * 							e.g. "attId_"+13 = "attId_13"  
	 */
	private void addNumericFeatureSet(String prefix, TreeSet<Integer> featSet) {
		
		for (Integer i : featSet)
		{
			String attName  = prefix+i.toString();			
			this.atts.add(new Attribute(attName, this.featNum));
			this.attIndexes.put(attName, this.featNum);
			this.featNum++;
		}
	}

	/**
	 * addNumericFeatureSet adds a set numeric features to the feature vector of the classifier
	 * 
	 * @param feat
	 * @param String prefix : prefix appended to each of the values to build the feature name 
	 * 							e.g. "attId_"+13 = "attId_13"  
	 */
	private void addNumericFeatureSet(String prefix, HashMap<String,Integer> featSet, int threshold) {
		
		System.err.println("Features::addNumericFeatureSet - threshold: "+threshold);
		for (String s : featSet.keySet())
		{
			if (featSet.get(s) >= threshold)
			{
				String attName  = prefix+s;			
				this.atts.add(new Attribute(attName, this.featNum));
				this.attIndexes.put(attName, this.featNum);
				this.featNum++;
			}
			/*else 
			{
				System.err.println("discarded ngram, freq="+featSet.get(s));
			}*/
		}
	}
	
	
	/**
	 * addNominalFeature adds a nominal feature (feature with values in a range) 
	 * to the feature vector of the classifier
	 * 
	 * @param feat
	 * @param featValues
	 */
	private void addNominalFeature(String feat, List<String> featValues) {
		
		this.atts.add(new Attribute(feat, featValues));
		this.attIndexes.put(feat,this.featNum);
		this.featNum++;	
	}

	/**
	 *  Adds frequency attribute +1 to an attribute in the given feature vector
	 * 
	 * @param String att attribute name to add a value to.
	 * @param double[] fVector feature vector where the value should be added
	 * 
	 */
	private void addNumericToFeatureVector (String att, double[] fVector, int sentTokNum)
	{
		if (attIndexes.containsKey(att))
		{
			int current_ind = attIndexes.get(att);
			//if the current word form is in the ngram list activate the feature in the vector 
			fVector[current_ind]=fVector[current_ind]+(1/(double)sentTokNum);
			//fVector[current_ind]++;
		}	
	}
	
	/**
	 *  Adds frequency attribute +1 (or +(1/tokenNum) to an attribute in the given feature vector
	 * 
	 * @param Attribute att to add a value to.
	 * @param double[] fVector feature vector where the value should be added
	 * 
	 */
	private void addNumericToFeatureVector (Attribute att, double[] fVector, int sentTokNum)
	{
		if (! att.equals(null))
		{
			int current_ind = att.index();
			//update feature value in the feature vector 
			fVector[current_ind]=fVector[current_ind]+(1/(double)sentTokNum);		
		}	
	}


	/**
	 * Given a window check if the ngrams inside (all of them) are present in the feature set, and if so, 
	 * update the feature vector accordingly
	 * 
	 * @param ngrams
	 * @param prefix String : possible prefix used to differentiate ngram groups in the attribute set.
	 * @param double[] fVector : feature vector for the corresponding instance
	 * @param int tokens : number of tokens in the sentence (in case we want to add not a frequency value
	 * but a normalized value)
	 * 
	 */
	private void checkNgramFeatures (LinkedList<String> ngrams, double[] fVector, String prefix, int tokens, boolean empty)
	{
		//System.err.println("features::checkNgramFeatures ->"+Arrays.asList(ngrams).toString());
		
		// if empty is active means that we are checking the end of the sentence and 
		// the ngram list must be emptied 
		if (empty)
		{
			while (!ngrams.isEmpty())
			{
				String ng = featureFromArray(ngrams, prefix);
				//add occurrence to feature vector (the functions checks if the given ngram feature exists).
				addNumericToFeatureVector (ng, fVector, tokens); //tokNum
				
				ngrams.removeFirst();
			}
		}
		// if empty is false search for all ngrams in the window
		else
		{
			// add ngrams to the feature list
			for (int i=0;i<ngrams.size();i++)
			{
				String ng = featureFromArray(ngrams.subList(0, i+1), prefix);
				// add occurrence to feature vector (the functions checks if the given ngram feature exists). 
				addNumericToFeatureVector(ng, fVector, tokens);//tokNum
			}
		}
	}
		
	
	/**
	 *  Check if the given word/lemma/ngram exists in the general or domain polarity lexicons, and if yes
	 *  updates the corresponding attributes in the feature vector
	 * 
	 * @param String wrd :  word/lemma/ngram to look for in the polarity lexicons
	 * @param double[] fVector : feature vector that should be updated
	 * 
	 */
	private void checkPolarityLexicons(String wrd, double[] fVector, int tokNum, boolean ngrams)
	{
		// fill vector with general polarity scores
		if ((polarLexiconGen != null) && (polarLexiconGen.size()>0)) //(!polarLexiconGen.isEmpty()))
		{
			if (polarLexiconGen.isInLexicon(wrd))
			{
				//fVector[getAttIndexes().get("polLexGen_posScore")]+=(polarLexiconGen.get(wrd).get("pos")/(double)tokNum);
				//fVector[getAttIndexes().get("polLexGen_negScore")]+=(polarLexiconGen.get(wrd).get("neg")/(double)tokNum);
				fVector[getAttIndexes().get("polLexGen_posScore")]+=(polarLexiconGen.getPolarity(wrd).getPositiveScore()/(double)tokNum);
				fVector[getAttIndexes().get("polLexGen_negScore")]+=(polarLexiconGen.getPolarity(wrd).getNegativeScore()/(double)tokNum);

				if (ngrams)
				{
					fVector[getAttIndexes().get("polgen_"+wrd)]++;
				}
			}
		}
		
		// fill vector with domain polarity scores
		if ((polarLexiconDom != null) && (polarLexiconDom.size()>0)) //(!polarLexiconDom.isEmpty()))
		{
			if (polarLexiconDom.isInLexicon(wrd))
			{
				//fVector[getAttIndexes().get("polLexDom_posScore")]+=(polarLexiconDom.get(wrd).get("pos")/(double)tokNum);
				//fVector[getAttIndexes().get("polLexDom_negScore")]+=(polarLexiconDom.get(wrd).get("neg")/(double)tokNum);
				fVector[getAttIndexes().get("polLexDom_posScore")]+=(polarLexiconDom.getPolarity(wrd).getPositiveScore()/(double)tokNum);
				fVector[getAttIndexes().get("polLexDom_negScore")]+=(polarLexiconDom.getPolarity(wrd).getNegativeScore()/(double)tokNum);

				if (ngrams)
				{
					fVector[getAttIndexes().get("poldom_"+wrd)]++;
				}

			}
		}
		
	}

	/**
	 * Check if the given word/lemma/ngram exists both in the ngram list and in the general or domain polarity
	 * lexicons, and if yes updates the corresponding attributes in the feature vector
	 * 
	 * @param ngrams
	 * @param fVector
	 * @param prefix
	 * @param toknumNgram
	 * @param toknumPol
	 * @param empty
	 * @param ngram
	 */
	private void checkNgramsAndPolarLexicons(LinkedList<String> ngrams, double[] fVector, String prefix, int toknumNgram, int toknumPol, boolean empty, boolean ngram)
	{
		//System.err.println(Arrays.asList(ngrams).toString());
		// if empty is active means that we are checking the end of the sentence and 
		// the ngram list must be emptied 
		if (empty)
		{
			// add ngrams to the feature vector
			while (! ngrams.isEmpty())
			{
				String ng = featureFromArray(ngrams, prefix);
				//if the current lemma is in the ngram list activate the feature in the vector
				if (params.containsKey("lemmaNgrams") && (!params.getProperty("lemmaNgrams").equalsIgnoreCase("0")))
				{
					// add occurrence to feature vector (the functions checks if the given ngram feature exists).
					addNumericToFeatureVector (ng, fVector, toknumNgram);	//tokNum
				}
				
				ng = featureFromArray(ngrams, "");
				if (params.containsKey("polarLexiconGeneral") || params.containsKey("polarLexiconDomain"))
				{
					checkPolarityLexicons(ng, fVector, toknumPol, ngram);
				} //end polarity ngram checker

				ngrams.removeFirst();

			} //end ngram checking
		}
		// if empty is false search for all ngrams in the window
		else
		{
			// add ngrams to the feature vector
			for (int i=0;i<ngrams.size();i++)
			{
				String ng = featureFromArray(ngrams.subList(0, i+1), prefix);
				//if the current lemma is in the ngram list activate the feature in the vector
				if (params.containsKey("lemmaNgrams") && (!params.getProperty("lemmaNgrams").equalsIgnoreCase("0")))
				{
					// add occurrence to feature vector (the functions checks if the given ngram feature exists).
					addNumericToFeatureVector (ng, fVector, toknumNgram);	//tokNum												
				}
				
				ng = featureFromArray(ngrams.subList(0, i+1), "");
				if (params.containsKey("polarLexiconGeneral") || params.containsKey("polarLexiconDomain"))
				{
					checkPolarityLexicons(ng, fVector, toknumPol, ngram);
				} //end polarity ngram checker
			} //end ngram checking				        		
		}
	}
	
	
	public void setCorpus(CorpusReader corp)
	{
		this.corpus = corp;
	}
	
	
	/**
	 *  Normalize input String (urls -> URL) 
	 * 
	 * @param input : input string to normalize
	 * @returns String
	 */
	private String normalize (String input, String normOpt){			
		
		//URL normalization
		if (normOpt.equalsIgnoreCase("all"))
		{
			return MicrotxtNormalizer.normalizeSentence(input, true, true, true);
		}	
		else if (normOpt.equalsIgnoreCase("noHashtag")) 
		{
			return MicrotxtNormalizer.normalizeSentence(input, true, true, false);
		}
		else if (normOpt.equalsIgnoreCase("noHashEmo")) 
		{
			return MicrotxtNormalizer.normalizeSentence(input, true, true, false);
		}
		else if (normOpt.equalsIgnoreCase("noEmot")) 
		{
			return MicrotxtNormalizer.normalizeSentence(input, true, true, true);
		}	
		else if (normOpt.equalsIgnoreCase("url")) 
		{
			return MicrotxtNormalizer.normalizeSentence(input, true, false, false);
		}		
		else if (normOpt.equalsIgnoreCase("minimum")) 
		{
			return MicrotxtNormalizer.normalizeSentence(input, false, false, true);
		}		
		else if (normOpt.equalsIgnoreCase("old")) 
		{
			return MicrotxtNormalizer.normalizeSentence(input, true, false, true);
		}
		else {
			return input;
		}
	}	
	
}
