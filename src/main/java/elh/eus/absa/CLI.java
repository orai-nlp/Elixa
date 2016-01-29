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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

import org.jdom2.JDOMException;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
//import elh.eus.absa.TrainerSVMlight;


/**
 * Main class of elh-eus-absa-atp, the elhuyar absa ATP modules
 * tagger.
 *
 * @author isanvi
 * @version 2014-12-13
 *
 */
public class CLI {
	/**
	 * Get dynamically the version of elh-eus-absa-atp by looking at the MANIFEST
	 * file.
	 */
	private final String version = CLI.class.getPackage().getImplementationVersion();
	/**
	 * Name space of the arguments provided at the CLI.
	 */
	private Namespace parsedArguments = null;
	/**
	 * Argument parser instance.
	 */
	private ArgumentParser argParser = ArgumentParsers.newArgumentParser(
			"elixa-" + version + ".jar").description(
					"elixa-" + version
					+ " is a multilingual ABSA module developed by the Elhuyar Foundation R&D Unit.\n");
	/**
	 * Sub parser instance.
	 */
	private Subparsers subParsers = argParser.addSubparsers().help(
			"sub-command help");
	/**
	 * The parser that manages the tagging sub-command.
	 */
	private Subparser annotateParser;
	/**
	 * The parser that manages the ATP (global polarity) training sub-command.
	 */
	private Subparser trainATPParser;
	/**
	 * The parser that manages the ATP (global polarity) evaluation sub-command.
	 */
	private Subparser evalATPParser;
	/**
	 * The parser that manages the ATP (global polarity) tagging sub-command.
	 */
	private Subparser tagATPParser;
	/**
	 * The parser that manages the ATC (target category classification) training sub-command.
	 */
	private Subparser trainATCParser;
	private Subparser trainATC2Parser;
	
	/**
	 * The parser that manages the slot2 (OTE) tagging sub-command.
	 */
	private Subparser slot2Parser;


	/**
	 * The parser that manages the evaluation sub-command.
	 */
	private Subparser tagSentParser;
	
	
	/**
	 * Parser that manages the polarity tagging and estimation of a text (kaf format for the moment).
	 */
	private Subparser predictParser;
	
	

	/**
	 * Construct a CLI object with the three sub-parsers to manage the command
	 * line parameters.
	 */
	public CLI() {
		annotateParser = subParsers.addParser("tag-ate").help("Tagging CLI");
		loadAnnotateParameters();
		trainATPParser = subParsers.addParser("train-atp").help("ATP training CLI");
		loadATPTrainingParameters();
		evalATPParser = subParsers.addParser("eval-atp").help("ATP evaluation CLI");
		loadATPevalParameters();
		tagATPParser = subParsers.addParser("tag-atp").help("ATP Tagging CLI");
		loadATPtagParameters();
		trainATCParser = subParsers.addParser("train-atc").help("ATC training CLI (single classifier)");
		loadATCTrainingParameters();
		trainATC2Parser = subParsers.addParser("train-atc2").help("ATC Training CLI (E & A classifiers");
		loadATC2TrainingParameters();
		slot2Parser = subParsers.addParser("slot2").help("Semeval 2015 slot2 (ATE) formatting CLI");
		loadslot2Parameters();
		tagSentParser = subParsers.addParser("tagSentences").help("Lemmatization and PoS tagging CLI");
		loadTagSentParameters();
		predictParser = subParsers.addParser("tag-naf").help("Predict polarity of a text");
		loadPredictionParameters();
	}
	
	
	/**
	 * Main entry point of elixa.
	 *
	 * @param args
	 * the arguments passed through the CLI
	 * @throws IOException
	 * exception if input data not available
	 * @throws JDOMException
	 * if problems with the xml formatting of NAF
	 */
	public static void main(final String[] args) throws IOException,
	JDOMException {
		CLI cmdLine = new CLI();
		cmdLine.parseCLI(args);
	}
	
	
	/**
	 * Parse the command interface parameters with the argParser.
	 *
	 * @param args
	 * the arguments passed through the CLI
	 * @throws IOException
	 * exception if problems with the incoming data
	 * @throws JDOMException
	 */
	public final void parseCLI(final String[] args) throws IOException, JDOMException {
		try {
			parsedArguments = argParser.parseArgs(args);
			System.err.println("CLI options: " + parsedArguments);
			if (args[0].equals("tagSentences")) {
				tagSents(System.in);
			} else if (args[0].equals("train-atp") || args[0].equals("train-gp")) {
				trainATP(System.in);
			} else if (args[0].equals("eval-atp") || args[0].equals("eval-gp")) {
				evalATP(System.in);
			} else if (args[0].equals("train-atc")) {
				trainATC(System.in);
			} else if (args[0].equals("train-atc2")) {
				trainATC2(System.in);
			} else if (args[0].equals("tag-atp") || args[0].equals("tag-gp")) {
				tagATP(System.in);
			} else if (args[0].equals("tag-ate")) {
					tagATE(System.in, System.out);					
			} else if (args[0].equals("slot2")) {
				slot2(System.in);
			}else if (args[0].equals("tag-naf")){
				predictPolarity(System.in);
			}
		} catch (ArgumentParserException e) {
			argParser.handleError(e);
			System.out.println("Run java -jar target/elixa-" + version
					+ ".jar (train-atc|train-atp|eval-atp|tag-atp|slot2|tagSentences|tag-ate|train-gp|tag-gp|tag-naf) -help for details");
			System.exit(1);
		}
	}
	
	
	public final void predictPolarity(final InputStream inputStream) throws IOException {

		//String files = parsedArguments.getString("file");
		String lexicon = parsedArguments.getString("lexicon");
		String estimator = parsedArguments.getString("estimator");
		String synset = parsedArguments.getString("synset");
		float threshold = parsedArguments.getFloat("threshold");
		
		//System.out.println("Polarity Predictor: ");
		//BufferedReader freader = new BufferedReader(new FileReader(files));   		
		//String line;
		//while ((line = freader.readLine()) != null) 
		//{
			try {
				KAFDocument naf = KAFDocument.createFromStream(new InputStreamReader(inputStream));
				
				File lexFile = new File(lexicon);
				Evaluator evalDoc = new Evaluator(lexFile, synset, threshold, estimator);
				Map<String, String> results = evalDoc.processKaf(naf, lexFile.getName());
				naf.print();
				//Map<String, Double> results = avg.processCorpus(corpus);
				//System.out.println("eval avg done"+results.toString());
				/*System.out.println("Prediction with avg done: \n"
		    				+ "\tTagged file: "+results.get("taggedFile")+"\n"
		    				+ "\tNumber of words containing sentiment found: "+results.get("sentTermNum")+"\n"
		    				+ "\tPolarity score: "+results.get("avg")
		    				+ "\tPolarity (threshold -> "+results.get("thresh")+"): "+results.get("polarity"));*/
				//FileUtilsElh.prettyPrintSentKaf(results);					
			} catch (Exception e) {
				//System.err.println("predictPolarity: error when processing "+line+" file");
				System.err.println("EliXa::tag-naf: error when processing naf");
				//e.printStackTrace();
			}
		//}
		//freader.close();
	}
	
	private void loadPredictionParameters() {
		/*
		 *  Parameters:
	        - Input File (-f | --file= ): File containing the a list of text files in KAF format whose polarity we want to estimate.    
	        - dict file  (-l | --lexicon= ): path to the polarity lexicon.
	        - Synset polarities (-s | --synset=): default polarities are calculated over lemmas. With this option polarity of synsets is taken into account instead of words. It has two posible values: (first|rank). 'first' uses the sense with the highest confidence value for the lemma. 'rank' uses complete ranking of synsets.
	        - Dictionary weights (-w | --weights): use polarity weights instead of binary polarities (pos/neg). If the dictionary does not provide polarity scores the program defaults to binary polarities.
	        - Threshold (-t | --threshold=) [-1,1]: Threshold which limits positive and negative reviews. Default value is 0.
	        - Polarity score estimator (-e| --estimator) [avg|moh]: average polarity ratio or estimator proposed in (Mohammad et al.,2009 - EMNLP) 

		 * 
		 */		  

		//predictParser.addArgument("-f", "--file")
		//.required(true)
		//.help("Input file to predict the polarity lexicon.\n");

		predictParser.addArgument("-l", "--lexicon")
		.required(true)
		.help("Path to the polarity lexicon file.\n");

		predictParser.addArgument("-s", "--synset")
		.choices("lemma", "first","rank")
		.required(false)
		.setDefault("lemma")
		.help(
				"Default polarities are calculated over lemmas. With this option polarity of synsets is taken into account instead of words. Possible values: (lemma|first|rank). 'first' uses the sense with the highest confidence value for the lemma. 'rank' uses complete ranking of synsets.\n");

		predictParser.addArgument("-w", "--weights")
		.action(Arguments.storeTrue())
		.help(
				"Use polarity weights instead of binary polarities (pos/neg). If the dictionary does not provide polarity scores the program defaults to binary polarities.\n");

		predictParser.addArgument("-t", "--threshold")
		.required(false)
		.setDefault((float)0)
		.help(
				"Threshold which limits positive and negative reviews. Float in the [-1,1] range. Default value is 0.\n");

		predictParser.addArgument("-e", "--estimator")
		.choices("avg", "moh")
		.required(false)
		.setDefault("avg")
		.help(
				"scoring function used for computing the polarity [avg | moh]: \n"
						+ "    - avg: average ratio of the polarity words in the text"
						+ "    - moh: polarity classifier proposed in (Mohammad et al.,2009 - EMNLP). Originally used on the MPQA corpus\n");

	}


	
	public final void tagSents(final InputStream inputStream)
	{
		String posModel = parsedArguments.getString("model");
		String dir = parsedArguments.getString("dir");
		String lang = parsedArguments.getString("language");
		String format = parsedArguments.getString("format");
		boolean print = parsedArguments.getBoolean("print");
		
		CorpusReader reader = new CorpusReader(inputStream, format, lang);
		try {
			String tagDir= dir+File.separator+lang;
			Files.createDirectories(Paths.get(tagDir));
			reader.tagSentences(tagDir, posModel, print);
		} catch (Exception e) {			
			e.printStackTrace();
		} 
		
	}
	
	public final void loadTagSentParameters()
	{
		tagSentParser.addArgument("-m", "--model")
		.required(true)
		.help("Pass the model to do the tagging as a parameter.\n");
		tagSentParser.addArgument("-d", "--dir")
		.required(true)
		.help("directory to store tagged files.\n");
		tagSentParser.addArgument("-f", "--format")
		.setDefault("tabNotagged")
		.choices("tabNotagged", "semeval2015")	
		.help("format of the input corpus.\n");
		tagSentParser.addArgument("-p", "--print")
		.action(Arguments.storeTrue())
		.help("Whether the tagged files should be printed as a corpus.\n");
		tagSentParser.addArgument("-l","--language")
		.setDefault("en")
		.choices("de", "en", "es", "eu", "it", "nl", "fr")
		.help("Choose language; if not provided it defaults to: \n"
				+ "\t- If the input format is NAF, the language value in incoming NAF file."
				+ "\t- en otherwise.\n");
	}
	
	
	/**
	 * Main method to do Aspect Term Extraction tagging.
	 *
	 * @param inputStream
	 * the input stream containing the content to tag, it must be NAF format
	 * @param outputStream
	 * the output stream providing the named entities
	 * @throws IOException
	 * exception if problems in input or output streams
	 */
	public final void tagATE(final InputStream inputStream,
			final OutputStream outputStream) throws IOException, JDOMException {
		BufferedReader breader = new BufferedReader(new InputStreamReader(
				inputStream, "UTF-8"));
		BufferedWriter bwriter = new BufferedWriter(new OutputStreamWriter(
				outputStream, "UTF-8"));
		// read KAF document from inputstream
		KAFDocument naf = KAFDocument.createFromStream(breader);
		// load parameters into a properties
		String model = parsedArguments.getString("model");
		//String outputFormat = parsedArguments.getString("outputFormat");
		String lexer = parsedArguments.getString("lexer");
		String dictTag = parsedArguments.getString("dictTag");
		String dictPath = parsedArguments.getString("dictPath");
		// language parameter
		String lang = null;
		if (parsedArguments.getString("language") != null) {
			lang = parsedArguments.getString("language");
			if (!naf.getLang().equalsIgnoreCase(lang)) {
				System.err
				.println("Language parameter in NAF and CLI do not match!!");
				System.exit(1);
			}
		} else {
			lang = naf.getLang();
		}
		
		naf = NLPpipelineWrapper.ixaPipesNERC(naf, model, lexer, dictTag, dictPath);	
		naf.save("entity-annotated.kaf");
		
		bwriter.close();
		breader.close();
	}
	
	
	/**
	 * Main access to the polarity detection training functionalities.
	 *
	 * @throws IOException
	 * input output exception if problems with corpora
	 */
	public final void trainATP(final InputStream inputStream) throws IOException {
		// load training parameters file
		String paramFile = parsedArguments.getString("params");
		String corpusFormat = parsedArguments.getString("corpusFormat");
		String validation = parsedArguments.getString("validation");
		String lang = parsedArguments.getString("language");
		String classes = parsedArguments.getString("classnum");
		int foldNum = Integer.parseInt(parsedArguments.getString("foldNum"));
		//boolean printPreds = parsedArguments.getBoolean("printPreds");
		
		CorpusReader reader = new CorpusReader(inputStream, corpusFormat, lang);
		System.err.println("trainATP : Corpus read, creating features");
		Features atpTrain = new Features (reader, paramFile, classes);			
		Instances traindata;
		if (corpusFormat.startsWith("tab") && !corpusFormat.equalsIgnoreCase("tabNotagged"))
		{
			traindata = atpTrain.loadInstancesTAB(true, "atp");
		}
		else if (corpusFormat.equalsIgnoreCase("tabNotagged") && lang.equalsIgnoreCase("eu"))
		{
			traindata = atpTrain.loadInstancesConll(true, "atp");			
		}
		else
		{
			traindata = atpTrain.loadInstances(true, "atp");
		}
		
		//setting class attribute (entCat|attCat|entAttCat|polarityCat)
		traindata.setClass(traindata.attribute("polarityCat"));		
		WekaWrapper classify;
		try {
			Properties params = new Properties();			
			params.load(new FileInputStream(paramFile));
			String modelPath = params.getProperty("fVectorDir");
			classify = new WekaWrapper(traindata, true);
			classify.saveModel(modelPath+File.separator+"elixa-atp_"+lang+".model");			
			switch (validation)
			{
			case "cross":
				classify.crossValidate(foldNum); break;				
			case "trainTest":
				classify.trainTest(); break;
			case "both":
				classify.crossValidate(foldNum); classify.trainTest(); break;
			default:
				System.out.println("train-atp: wrong validation option. Model saved but not tested");
			}
		} catch (Exception e) {
			e.printStackTrace();			
		}
	}
	
	/**
	 * Create the main parameters available for training ATP models.
	 */
	private void loadATPTrainingParameters() {
		trainATPParser.addArgument("-p", "--params").required(true)
		.help("Load the training parameters file\n");		
		trainATPParser.addArgument("-cvf", "--foldNum")
		.required(false)
		.setDefault(10)
		.help("Number of folds to run the cross validation on (default is 10).\n");
		trainATPParser.addArgument("-v","--validation")
		.required(false)
		.choices("cross", "trainTest", "both")
		.setDefault("cross")
		.help("Choose the way the trained model will be validated\n"
				+ "\t - cross : 10 fold cross validation.\n"
				+ "\t - trainTest : 90% train / 10% test division.\n"
				+ "\t - both (default): both cross validation and train/test division will be tested.");
		trainATPParser.addArgument("-f","--corpusFormat")
		.required(false)
		.choices("semeval2015", "semeval2014", "tab", "tabglobal", "tabNotagged", "globalNotagged")
		.setDefault("semeval2015")
		.help("Choose format of reference corpus; it defaults to semeval2015 format.\n");
		trainATPParser.addArgument("-cn","--classnum")
		.required(false)
		.choices("3", "3+", "5+", "5", "binary")
		.setDefault("3+none")
		.help("Choose the number of classes the classifier should work on "
				+ "(binary=p|n ; 3=p|n|neu ; 3+=p|n|neu|none ; 5=p|n|neu|p+|n+ ; 5+=p|n|neu|p+|n+|none )"
				+ " it defaults to 3 (p|n|neu).\n");
		trainATPParser.addArgument("-o","--outputpredictions")		
		.action(Arguments.storeTrue())
		.setDefault("false")
		.help("Output predictions or not; output is the corpus annotated with semeval2015 format.\n");
		trainATPParser.addArgument("-l","--language")
		.setDefault("en")
		.choices("de", "en", "es", "eu", "it", "nl", "fr")
		.help("Choose language; if not provided it defaults to: \n"
				+ "\t- If the input format is NAF, the language value in incoming NAF file."
				+ "\t- en otherwise.\n");		
	}
	
	

	/**
	 * Main access to the polarity tagging functionalities. Target based polarity. 
	 *
	 * @throws IOException
	 * input output exception if problems with corpora
	 */
	public final void evalATP(final InputStream inputStream) throws IOException, JDOMException {
		
		String paramFile = parsedArguments.getString("params");
		String corpusFormat = parsedArguments.getString("corpusFormat");
		String model = parsedArguments.getString("model");
		String lang = parsedArguments.getString("language");	
		String classnum = parsedArguments.getString("classnum");
		boolean ruleBased = parsedArguments.getBoolean("ruleBasedClassifier");
		boolean printPreds = parsedArguments.getBoolean("outputPredictions");
		
		//Read corpus sentences
		CorpusReader reader = new CorpusReader(inputStream, corpusFormat, lang);
		
 
		//Rule-based Classifier.
		if (ruleBased) 
		{		
			Properties params = new Properties();
			params.load(new FileInputStream(new File(paramFile)));

			String posModelPath = params.getProperty("pos-model");
			String kafDir = params.getProperty("kafDir");
			
			/* polarity lexicon. Domain specific polarity lexicon is given priority.
			 * If no domain lexicon is found it reverts to general polarity lexicon.
			 * If no general polarity lexicon is found program exits with error message.
			*/
			String lex = params.getProperty("polarLexiconDomain","none");
			if (lex.equalsIgnoreCase("none"))
			{
				lex = params.getProperty("polarLexiconGeneral","none");
				if (lex.equalsIgnoreCase("none"))
				{
					System.err.println("Elixa Error :: Rule-based classifier is selected but no polarity"
							+ " lexicon has been specified. Either specify one or choose ML classifier");
					System.exit(1);
				}
			}			
			File lexFile = new File(lex);			
			Evaluator evalDoc = new Evaluator(lexFile, "lemma");
			
			for (String oId : reader.getOpinions().keySet())
			{
				// sentence posTagging
				String taggedKaf = reader.tagSentenceTab(reader.getOpinion(oId).getsId(), kafDir, posModelPath);
				//process the postagged sentence with the word count based polarity tagger
				Map<String, String> results = evalDoc.polarityScoreTab(taggedKaf, lexFile.getName());				 
				String lblStr = results.get("polarity");
				String actual = "?";
				if (reader.getOpinion(oId).getPolarity() != null)
				{
					actual = reader.getOpinion(oId).getPolarity();
				}
				String rId = reader.getOpinion(oId).getsId().replaceFirst("_g$", "");
				System.out.println(rId+"\t"+actual+"\t"+lblStr+"\t"+reader.getOpinionSentence(oId));
				reader.getOpinion(oId).setPolarity(lblStr);
			}
		}
		//ML Classifier (default)
		else
		{		
			Features atpTest = new Features (reader, paramFile, classnum, model);
			Instances testdata;
			if (corpusFormat.startsWith("tab") && !corpusFormat.equalsIgnoreCase("tabNotagged"))
			{	
				testdata = atpTest.loadInstancesTAB(true, "atp");
			}
			else
			{	
				testdata = atpTest.loadInstances(true, "atp");
			}
			//	setting class attribute (entCat|attCat|entAttCat|polarityCat)
			testdata.setClass(testdata.attribute("polarityCat"));

			WekaWrapper classify;		
			try {
				classify = new WekaWrapper(model);	

				System.err.println("evalAtp : going to test the model");
				//sort according to the instanceId
				//traindata.sort(atpTrain.getAttIndexes().get("instanceId"));
				//Instances testdata = new Instances(traindata);
				//testdata.deleteAttributeAt(0);
				//classify.setTestdata(testdata);
				classify.setTestdata(testdata);
				classify.testModel(model);

				if (printPreds)
				{
					for (String oId : reader.getOpinions().keySet())
					{
						int iId = atpTest.getOpinInst().get(oId);
						Instance i = testdata.get(iId-1);
						double label = classify.getMLclass().classifyInstance(i);
						String lblStr = i.classAttribute().value((int) label);
						String actual = "?";
						if (reader.getOpinion(oId).getPolarity() != null)
						{
							actual = reader.getOpinion(oId).getPolarity();
						}
						String rId = reader.getOpinion(oId).getsId().replaceFirst("_g$", "");
						String oSent = reader.getOpinionSentence(oId);
						if (corpusFormat.startsWith("tab"))
						{
							StringBuilder sb = new StringBuilder();
							for (String kk : oSent.split("\n"))
							{
								sb.append(kk.split("\\t")[0]);
								sb.append(" ");
							}
							oSent=sb.toString(); 
						}
						
						System.out.println(rId+"\t"+actual+"\t"+lblStr+"\t"+oSent+"\t"+reader.getOpinionSentence(oId).replaceAll("\n", " ").replaceAll("\\t",":::"));
						reader.getOpinion(oId).setPolarity(lblStr);
					}
				}
				//reader.print2Semeval2015format(model+"tagATP.xml");
				//reader.print2conll(model+"tagAtp.conll");
			} catch (Exception e) {
				e.printStackTrace();			
			}
		}
	}

	/**
	 * Create the main parameters available for training ATP models.
	 */
	private void loadATPevalParameters() {
		evalATPParser.addArgument("-p", "--params").required(true)
		.help("Load the training parameters file\n");
		evalATPParser.addArgument("-m", "--model")
		.required(true)
		.help("The pretrained model we want to test.\n");
		//evalATPParser.addArgument("-t", "--testset")
		//.required(false)
		//.help("The test corpus to evaluate our model.\n");
		evalATPParser.addArgument("-f","--corpusFormat")
		.required(false)
		.choices("semeval2015", "semeval2014", "tab", "tabglobal", "tabNotagged","globalNotagged")
		.setDefault("semeval2015")
		.help("Choose format of the test corpus; it defaults to semeval2015 format.\n");
		evalATPParser.addArgument("-cn","--classnum")
		.required(false)
		.choices("3", "3+", "5+", "5", "binary")
		.setDefault("3+none")
		.help("Choose the number of classes the classifier should work on "
				+ "(binary=p|n ; 3=p|n|neu ; 3+=p|n|neu|none ; 5=p|n|neu|p+|n+ ; 5+=p|n|neu|p+|n+|none )"
				+ " it defaults to 3 (p|n|neu).\n");
		evalATPParser.addArgument("-r","--ruleBasedClassifier")		
		.action(Arguments.storeTrue())
		.setDefault(false)
		.help("Whether rule based classifier should be used instead of the default ML classifier."
				+ " A polarity lexicon is mandatory if the rule based classifier is used.\n");
		evalATPParser.addArgument("-o","--outputPredictions")		
		.action(Arguments.storeTrue())
		.setDefault(false)
		.help("Output predictions or not; output is the corpus annotated with semeval2015 format.\n");
		evalATPParser.addArgument("-l","--language")
		.setDefault("en")
		.choices("de", "en", "es", "eu", "it", "nl", "fr")
		.help("Choose language; if not provided it defaults to: \n"
				+ "\t- If the input format is NAF, the language value in incoming NAF file."
				+ "\t- en otherwise.\n");		
	}
	
	
	/**
	 * Main access to the polarity tagging functionalities. Target based polarity. 
	 *
	 * @throws IOException
	 * input output exception if problems with corpora
	 * @throws JDOMException 
	 */
	public final void tagATP(final InputStream inputStream) throws IOException, JDOMException {
		// load training parameters file
		String paramFile = parsedArguments.getString("params");
		String corpusFormat = parsedArguments.getString("corpusFormat");
		String model = parsedArguments.getString("model");
		String lang = parsedArguments.getString("language");	
		String classnum = parsedArguments.getString("classnum");
		boolean ruleBased = parsedArguments.getBoolean("ruleBasedClassifier");
		
		//Read corpus sentences
		CorpusReader reader = new CorpusReader(inputStream, corpusFormat, lang);
		
		//Rule-based Classifier.
		if (ruleBased) 
		{		
			Properties params = new Properties();
			params.load(new FileInputStream(new File(paramFile)));

			String posModelPath = params.getProperty("pos-model");
			String kafDir = params.getProperty("kafDir");
			
			/* polarity lexicon. Domain specific polarity lexicon is given priority.
			 * If no domain lexicon is found it reverts to general polarity lexicon.
			 * If no general polarity lexicon is found program exits with error message.
			*/
			String lex = params.getProperty("polarLexiconDomain","none");
			if (lex.equalsIgnoreCase("none"))
			{
				lex = params.getProperty("polarLexiconGeneral","none");
				if (lex.equalsIgnoreCase("none"))
				{
					System.err.println("Elixa Error :: Rule-based classifier is selected but no polarity"
							+ " lexicon has been specified. Either specify one or choose ML classifier");
					System.exit(1);
				}
			}			
			File lexFile = new File(lex);			
			Evaluator evalDoc = new Evaluator(lexFile, "lemma");
			
			for (String oId : reader.getOpinions().keySet())
			{
				// sentence posTagging
				String taggedKaf = reader.tagSentenceTab(reader.getOpinion(oId).getsId(), kafDir, posModelPath);
				//process the postagged sentence with the word count based polarity tagger
				Map<String, String> results = evalDoc.polarityScoreTab(taggedKaf, lexFile.getName());				 
				String lblStr = results.get("polarity");
				String actual = "?";
				if (reader.getOpinion(oId).getPolarity() != null)
				{
					actual = reader.getOpinion(oId).getPolarity();
				}
				String rId = reader.getOpinion(oId).getsId().replaceFirst("_g$", "");
				System.out.println(rId+"\t"+actual+"\t"+lblStr+"\t"+reader.getOpinionSentence(oId));
				reader.getOpinion(oId).setPolarity(lblStr);
			}
		}
		else
		{		
			Features atpTrain = new Features (reader, paramFile, classnum, model);
			Instances traindata;
			if (corpusFormat.startsWith("tab") && !corpusFormat.equalsIgnoreCase("tabNotagged"))
			{
				traindata = atpTrain.loadInstancesTAB(true, "atp");
			}
			else if (lang.equalsIgnoreCase("eu") && (corpusFormat.equalsIgnoreCase("tabNotagged") ||corpusFormat.equalsIgnoreCase("ireom")))
			{
				traindata = atpTrain.loadInstancesConll(true, "atp");			
			}
			else
			{
				traindata = atpTrain.loadInstances(true, "atp");
			}
					
			//	setting class attribute (entCat|attCat|entAttCat|polarityCat)
			traindata.setClass(traindata.attribute("polarityCat"));

			WekaWrapper classify;		
			try {
				classify = new WekaWrapper(model);	

				System.err.println();
				//sort according to the instanceId
				//traindata.sort(atpTrain.getAttIndexes().get("instanceId"));
				//Instances testdata = new Instances(traindata);
				//testdata.deleteAttributeAt(0);
				//classify.setTestdata(testdata);
				classify.setTestdata(traindata);
				classify.loadModel(model);

				for (String oId : reader.getOpinions().keySet())
				{
					int iId = atpTrain.getOpinInst().get(oId);
					Instance i = traindata.get(iId-1);
					double label = classify.getMLclass().classifyInstance(i);
					String lblStr = i.classAttribute().value((int) label);
					String actual = "?";
					if (reader.getOpinion(oId).getPolarity() != null)
					{
						actual = reader.getOpinion(oId).getPolarity();
					}
					String rId = reader.getOpinion(oId).getsId().replaceFirst("_g$", "");
					String oSent = reader.getOpinionSentence(oId);
					if (corpusFormat.startsWith("tab"))
					{
						StringBuilder sb = new StringBuilder();
						for (String kk : oSent.split("\n"))
						{
							sb.append(kk.split("\\t")[0]);
							sb.append(" ");
						}
						oSent=sb.toString(); 
					}
					
					System.out.println(rId+"\t"+actual+"\t"+lblStr+"\t"+oSent+"\t"+reader.getOpinionSentence(oId).replaceAll("\n", " ").replaceAll("\\t",":::"));
					reader.getOpinion(oId).setPolarity(lblStr);
				}

				//reader.print2Semeval2015format(model+"tagATP.xml");
				//reader.print2conll(model+"tagAtp.conll");
			} catch (Exception e) {
				e.printStackTrace();			
			}
		}
	}
	
	
	/**
	 * Create the main parameters available for training ATP models.
	 */
	private void loadATPtagParameters() {
		tagATPParser.addArgument("-p", "--params").required(true)
		.help("Load the training parameters file\n");
		tagATPParser.addArgument("-f","--corpusFormat")
		.required(false)
		.choices("semeval2015", "semeval2014", "tab", "tabglobal", "tabNotagged", "ireom", "globalNotagged")
		.setDefault("semeval2015")
		.help("Choose format of reference corpus; it defaults to semeval2015 format.\n");
		tagATPParser.addArgument("-m","--model")		
		.required(true)
		.help("Pre trained model to classify corpus opinions with. Features are extracted from the model\n");
		tagATPParser.addArgument("-r","--ruleBasedClassifier")		
		.action(Arguments.storeTrue())
		.setDefault(false)
		.help("Whether rule based classifier should be used instead of the default ML classifier."
				+ " A polarity lexicon is mandatory if the rule based classifier is used.\n");
		tagATPParser.addArgument("-cn","--classnum")
		.required(false)
		.choices("3", "3+", "5+", "5", "binary")
		.setDefault("3+none")
		.help("Choose the number of classes the classifier should work on "
				+ "(binary=p|n ; 3=p|n|neu ; 3+=p|n|neu|none ; 5=p|n|neu|p+|n+ ; 5+=p|n|neu|p+|n+|none )"
				+ " it defaults to 3 (p|n|neu).\n");		
		tagATPParser.addArgument("-l","--language")
		.setDefault("en")
		.choices("de", "en", "es", "eu", "it", "nl", "fr")
		.help("Choose language; if not provided it defaults to: \n"
				+ "\t- If the input format is NAF, the language value in incoming NAF file."
				+ "\t- en otherwise.\n");
	}
	
	
	/**
	 * Format ixa-pipes based ATE results to Semeval 2015 format.  
	 * 
	 * @param inputStream
	 * @throws IOException
	 */
	public final void slot2(final InputStream inputStream) throws IOException {
		// load training parameters file
		//String paramFile = parsedArguments.getString("params");
		String corpusFormat = parsedArguments.getString("corpusFormat");
		String naf = parsedArguments.getString("naf");
		String lang = parsedArguments.getString("lang");
		
		CorpusReader reader = new CorpusReader(inputStream, corpusFormat, lang);
		if(! FileUtilsElh.checkFile(naf))			
		{
			System.err.println("Error when trying to read from directory containing de annotations.");
			System.exit(2);
		}
		
		try {
			reader.slot2opinionsFromAnnotations(naf);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	
	/**
	 * Main access to the train-atc functionalities.
	 * Train ATC using a single classifier (one vs. all) for E#A aspect categories.
	 * 
	 * @throws Exception 
	 */
	public final void trainATC(final InputStream inputStream) throws IOException {
		// load training parameters file
		String paramFile = parsedArguments.getString("params");
		String corpusFormat = parsedArguments.getString("corpusFormat");
		//String validation = parsedArguments.getString("validation");
		int foldNum = Integer.parseInt(parsedArguments.getString("foldNum"));
		String lang = parsedArguments.getString("language");
		//boolean printPreds = parsedArguments.getBoolean("printPreds");
		boolean nullSentenceOpinions = parsedArguments.getBoolean("nullSentences");
		//double threshold = 0.2;
		//String modelsPath = "/home/inaki/Proiektuak/BOM/SEMEVAL2015/ovsaModels";
		
		CorpusReader reader = new CorpusReader(inputStream, corpusFormat, nullSentenceOpinions, lang);
		Features atcTrain = new Features (reader, paramFile,"3");	
		Instances traindata = atcTrain.loadInstances(true, "atc");
		
		//setting class attribute (entCat|attCat|entAttCat|polarityCat)
		
		//HashMap<String, Integer> opInst = atcTrain.getOpinInst();
		WekaWrapper classifyEnts;
		WekaWrapper classifyAtts;
		//WekaWrapper onevsall;
		try {
			//train first classifier (entities)
			Instances traindataEnt = new Instances(traindata);
			// IMPORTANT: filter indexes are added 1 because weka remove function counts attributes from 1, 
			traindataEnt.setClassIndex(traindataEnt.attribute("entCat").index());
			classifyEnts = new WekaWrapper(traindataEnt, true);
			String filtRange = String.valueOf(traindata.attribute("attCat").index()+1)+","
					+ String.valueOf(traindata.attribute("entAttCat").index()+1);			
			classifyEnts.filterAttribute(filtRange);
				
			System.out.println("trainATC: entity classifier results -> ");
			classifyEnts.crossValidate(foldNum);
			classifyEnts.saveModel("elixa-atc_ent-"+lang+".model");
			
			//Classifier entityCl = classify.getMLclass();
			
			//train second classifier (attributes)
			Instances traindataAtt = new Instances(traindata);
			traindataAtt.setClassIndex(traindataAtt.attribute("attCat").index());
			classifyAtts = new WekaWrapper(traindataAtt, true);
			filtRange = String.valueOf(traindataAtt.attribute("entAttCat").index()+1);			
			classifyAtts.filterAttribute(filtRange);		
			
			System.out.println("trainATC: attribute classifier results -> ");
			classifyAtts.crossValidate(foldNum);
			classifyAtts.saveModel("elixa-atc_att-"+lang+".model");
			/*
			Instances traindataEntadded = classifyEnts.addClassification(classifyEnts.getMLclass(), traindataEnt);
			//train second classifier (entCat attributes will have the values of the entities always)
			traindataEntadded.setClassIndex(traindataEntadded.attribute("attCat").index());
			WekaWrapper classify2 = new WekaWrapper(traindataEntadded, true);
			System.out.println("trainATC: enhanced attribute classifier results -> ");
			classify2.saveModel("elixa-atc_att_enhanced.model");
			classify2.crossValidate(foldNum);		
			*/
			//classify.printMultilabelPredictions(classify.multiLabelPrediction());		*/	
						
			//reader.print2Semeval2015format(paramFile+"entAttCat.xml");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//traindata.setClass(traindata.attribute("entAttCat"));
		System.err.println("DONE CLI train-atc");				
	}
		
	
	
	/**
	 * Main access to the train-atc functionalities. Train ATC using a double one vs. all classifier
	 * (E and A) for E#A aspect categories
	 * @throws Exception 
	 */
	public final void trainATC2(final InputStream inputStream) throws IOException {
		// load training parameters file
		String paramFile = parsedArguments.getString("params");
		String testFile = parsedArguments.getString("testset");
		String paramFile2 = parsedArguments.getString("params2");
		String corpusFormat = parsedArguments.getString("corpusFormat");
		//String validation = parsedArguments.getString("validation");
		String lang = parsedArguments.getString("language");
		//int foldNum = Integer.parseInt(parsedArguments.getString("foldNum"));
		//boolean printPreds = parsedArguments.getBoolean("printPreds");
		boolean nullSentenceOpinions = parsedArguments.getBoolean("nullSentences");
		boolean onlyTest = parsedArguments.getBoolean("testOnly");
		double threshold = 0.5;
		double threshold2 = 0.5;
		String modelsPath = "/home/inaki/elixa-atp/ovsaModels";
		
		CorpusReader reader = new CorpusReader(inputStream, corpusFormat, nullSentenceOpinions, lang);
		Features atcTrain = new Features (reader, paramFile,"3");		
		Instances traindata = atcTrain.loadInstances(true, "atc");
		
		if (onlyTest)
		{
			if (FileUtilsElh.checkFile(testFile))
			{
				System.err.println("read from test file");
				reader = new CorpusReader(new FileInputStream(new File(testFile)), corpusFormat, nullSentenceOpinions, lang);
				atcTrain.setCorpus(reader);
				traindata = atcTrain.loadInstances(true, "atc");
			}
		}
		
		
		//setting class attribute (entCat|attCat|entAttCat|polarityCat)
		
		//HashMap<String, Integer> opInst = atcTrain.getOpinInst();		
		//WekaWrapper classifyAtts;
		WekaWrapper onevsall;
		try {
			
			//classify.printMultilabelPredictions(classify.multiLabelPrediction());		*/	
			
			//onevsall
			Instances entdata = new Instances(traindata);
			entdata.deleteAttributeAt(entdata.attribute("attCat").index());
			entdata.deleteAttributeAt(entdata.attribute("entAttCat").index());
			entdata.setClassIndex(entdata.attribute("entCat").index());
			onevsall = new WekaWrapper(entdata,true);								
			
			
			if (! onlyTest)
			{
				onevsall.trainOneVsAll(modelsPath, paramFile+"entCat");			
				System.out.println("trainATC: one vs all models ready");
			}
			onevsall.setTestdata(entdata);
			HashMap<Integer, HashMap<String, Double>> ovsaRes = onevsall.predictOneVsAll(modelsPath, paramFile+"entCat");
			System.out.println("trainATC: one vs all predictions ready");
			HashMap<Integer, String> instOps = new HashMap<Integer,String>();
			for (String oId : atcTrain.getOpinInst().keySet())
			{
				instOps.put(atcTrain.getOpinInst().get(oId), oId);
			}
			
			atcTrain = new Features (reader, paramFile2,"3");
			entdata = atcTrain.loadInstances(true, "attTrain2_data");
			entdata.deleteAttributeAt(entdata.attribute("entAttCat").index());
			//entdata.setClassIndex(entdata.attribute("entCat").index());

			Attribute insAtt = entdata.attribute("instanceId");
			double maxInstId = entdata.kthSmallestValue(insAtt, entdata.numDistinctValues(insAtt)-1);
			System.err.println("last instance has index: "+maxInstId);
			for (int ins=0; ins<entdata.numInstances(); ins++)
			{
			    System.err.println("ins"+ins);
				int i = (int)entdata.instance(ins).value(insAtt);
				Instance currentInst = entdata.instance(ins);
				//System.err.println("instance "+i+" oid "+kk.get(i+1)+"kk contains key i?"+kk.containsKey(i));
				String sId = reader.getOpinion(instOps.get(i)).getsId();
				String oId = instOps.get(i);
				reader.removeSentenceOpinions(sId);
				int oSubId =0;				
				for (String cl : ovsaRes.get(i).keySet())
				{				
					//System.err.println("instance: "+i+" class "+cl+" value: "+ovsaRes.get(i).get(cl));
					if  (ovsaRes.get(i).get(cl) > threshold)
					{
						//System.err.println("one got through ! instance "+i+" class "+cl+" value: "+ovsaRes.get(i).get(cl));						
						// for the first one update the instances
						if (oSubId >= 1)
						{
							Instance newIns = new SparseInstance(currentInst);
							newIns.setDataset(entdata);
							entdata.add(newIns);
							newIns.setValue(insAtt, maxInstId+oSubId);
							newIns.setClassValue(cl);
							instOps.put((int)maxInstId+oSubId, oId);
							
						}						
						// if the are more create new instances
						else
						{
							currentInst.setClassValue(cl);
							//create and add opinion to the structure
							//	trgt, offsetFrom, offsetTo, polarity, cat, sId);
							//Opinion op = new Opinion(instOps.get(i)+"_"+oSubId, "", 0, 0, "", cl, sId);
							//reader.addOpinion(op);
						}
						oSubId++;
					}					
				} //finished updating instances data												
			}
			
			entdata.setClass(entdata.attribute("attCat"));
			onevsall = new WekaWrapper(entdata, true);
			
			/**
			 *  Bigarren sailkatzailea
			 * 
			 * */
			if (! onlyTest)
			{				
				onevsall.trainOneVsAll(modelsPath, paramFile+"attCat");			
				System.out.println("trainATC: one vs all attcat models ready");
			}
			
			ovsaRes = onevsall.predictOneVsAll(modelsPath, paramFile+"entAttCat");
			
			
			insAtt = entdata.attribute("instanceId");
			maxInstId = entdata.kthSmallestValue(insAtt, insAtt.numValues());
			System.err.println("last instance has index: "+maxInstId);
			for (int ins=0; ins<entdata.numInstances(); ins++)
			{
				System.err.println("ins: "+ins);
				int i = (int)entdata.instance(ins).value(insAtt);
				Instance currentInst = entdata.instance(ins);
				//System.err.println("instance "+i+" oid "+kk.get(i+1)+"kk contains key i?"+kk.containsKey(i));
				String sId = reader.getOpinion(instOps.get(i)).getsId();
				String oId = instOps.get(i);
				reader.removeSentenceOpinions(sId);
				int oSubId =0;
				for (String cl : ovsaRes.get(i).keySet())
				{				
					//System.err.println("instance: "+i+" class "+cl+" value: "+ovsaRes.get(i).get(cl));
					if  (ovsaRes.get(i).get(cl) > threshold2)
					{
						///System.err.println("instance: "+i+" class "+cl+" value: "+ovsaRes.get(i).get(cl));
						if  (ovsaRes.get(i).get(cl) > threshold)
						{
							//System.err.println("one got through ! instance "+i+" class "+cl+" value: "+ovsaRes.get(i).get(cl));						
							// for the first one update the instances
							if (oSubId >= 1)
							{
								String label = currentInst.stringValue(entdata.attribute("entAtt"))+"#"+cl;							
								//create and add opinion to the structure
								//	trgt, offsetFrom, offsetTo, polarity, cat, sId);							
								Opinion op = new Opinion(oId+"_"+oSubId, "", 0, 0, "", label, sId);
								reader.addOpinion(op);							
							}						
							// if the are more create new instances
							else
							{
								String label = currentInst.stringValue(entdata.attribute("entAtt"))+"#"+cl;							
								//create and add opinion to the structure
								//	trgt, offsetFrom, offsetTo, polarity, cat, sId);
								reader.removeOpinion(oId);
								Opinion op = new Opinion(oId+"_"+oSubId, "", 0, 0, "", label, sId);
								reader.addOpinion(op);
							}
							oSubId++;
						}					
					} //finished updating instances data												
				}
			}			
			reader.print2Semeval2015format(paramFile+"entAttCat.xml");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//traindata.setClass(traindata.attribute("entAttCat"));
		System.err.println("DONE CLI train-atc2 (oneVsAll)");				
	}		
	
	
	
	/**
	 * train ATC using a single classifier (one vs. all) for E#A aspect categories.
	 * 
	 * @param inputStream
	 * @throws IOException
	 */
	public final void trainATCsingleCategory(final InputStream inputStream) throws IOException {
		// load training parameters file
		String paramFile = parsedArguments.getString("params");
		String testFile = parsedArguments.getString("testset");
		String corpusFormat = parsedArguments.getString("corpusFormat");
		//String validation = parsedArguments.getString("validation");
		String lang = parsedArguments.getString("language");
		//int foldNum = Integer.parseInt(parsedArguments.getString("foldNum"));
		//boolean printPreds = parsedArguments.getBoolean("printPreds");
		boolean nullSentenceOpinions = parsedArguments.getBoolean("nullSentences");
		boolean onlyTest = parsedArguments.getBoolean("testOnly");
		double threshold = 0.5;
		
		String modelsPath = "/home/inaki/Proiektuak/BOM/SEMEVAL2015/ovsaModels";
		
		
		CorpusReader reader = new CorpusReader(inputStream, corpusFormat, nullSentenceOpinions, lang);
		Features atcTrain = new Features (reader, paramFile,"3");
		Instances traindata = atcTrain.loadInstances(true, "atc");
		
		if (onlyTest)
		{
			if (FileUtilsElh.checkFile(testFile))
			{
				System.err.println("read from test file");
				reader = new CorpusReader(new FileInputStream(new File(testFile)), corpusFormat, nullSentenceOpinions, lang);
				atcTrain.setCorpus(reader);
				traindata = atcTrain.loadInstances(true, "atc");
			}
		}
		
			
		//setting class attribute (entCat|attCat|entAttCat|polarityCat)
		
		//HashMap<String, Integer> opInst = atcTrain.getOpinInst();
		//WekaWrapper classifyEnts;
		//WekaWrapper classifyAtts;
		WekaWrapper onevsall;
		try {
			
			//classify.printMultilabelPredictions(classify.multiLabelPrediction());		*/	
			
			//onevsall
			//Instances entdata = new Instances(traindata);
			traindata.deleteAttributeAt(traindata.attribute("attCat").index());
			traindata.deleteAttributeAt(traindata.attribute("entCat").index());
			traindata.setClassIndex(traindata.attribute("entAttCat").index());
			onevsall = new WekaWrapper(traindata,true);
			
			if (! onlyTest)
			{
				onevsall.trainOneVsAll(modelsPath, paramFile+"entAttCat");			
				System.out.println("trainATC: one vs all models ready");
			}
			onevsall.setTestdata(traindata);
			HashMap<Integer, HashMap<String, Double>> ovsaRes = onevsall.predictOneVsAll(modelsPath, paramFile+"entAttCat");
			System.out.println("trainATC: one vs all predictions ready");
			HashMap<Integer, String> kk = new HashMap<Integer,String>();
			for (String oId : atcTrain.getOpinInst().keySet())
			{
				kk.put(atcTrain.getOpinInst().get(oId), oId);
			}
			
			Object[] ll = ovsaRes.get(1).keySet().toArray();
			for (Object l : ll)
			{
				System.err.print((String)l+" - ");
			}
			System.err.print("\n");
			
			for (int i : ovsaRes.keySet())
			{
				//System.err.println("instance "+i+" oid "+kk.get(i+1)+"kk contains key i?"+kk.containsKey(i));
				String sId = reader.getOpinion(kk.get(i)).getsId();
				reader.removeSentenceOpinions(sId);
				int oSubId =0;				
				for (String cl : ovsaRes.get(i).keySet())
				{				
					//System.err.println("instance: "+i+" class "+cl+" value: "+ovsaRes.get(i).get(cl));
					if  (ovsaRes.get(i).get(cl) > threshold)
					{
						//System.err.println("one got through ! instance "+i+" class "+cl+" value: "+ovsaRes.get(i).get(cl));
						oSubId++;
						//create and add opinion to the structure
						//trgt, offsetFrom, offsetTo, polarity, cat, sId);
						Opinion op = new Opinion(kk.get(i)+"_"+oSubId, "", 0, 0, "", cl, sId);
						reader.addOpinion(op);
					}					
				}
			}			
			reader.print2Semeval2015format(paramFile+"entAttCat.xml");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//traindata.setClass(traindata.attribute("entAttCat"));
		System.err.println("DONE CLI train-atc2 (oneVsAll)");				
	}
	
	
	
	/**
	 * Main access to the train functionalities.
	 * @throws Exception 
	 */
	public final void tagATC(final InputStream inputStream) throws IOException {
		// load training parameters file
		String paramFile = parsedArguments.getString("params");
		String corpusFormat = parsedArguments.getString("corpusFormat");
		//String validation = parsedArguments.getString("validation");
		String lang = parsedArguments.getString("language");
		int foldNum = Integer.parseInt(parsedArguments.getString("foldNum"));
		//boolean printPreds = parsedArguments.getBoolean("printPreds");
		
		CorpusReader reader = new CorpusReader(inputStream, corpusFormat, lang);
		Features atcTrain = new Features (reader, paramFile,"3");	
		Instances traindata = atcTrain.loadInstances(true, "atc");
		
		//setting class attribute (entCat|attCat|entAttCat|polarityCat)
		
		//HashMap<String, Integer> opInst = atcTrain.getOpinInst();
		WekaWrapper classify;
		try {
			//train first classifier (entities)
			traindata.setClass(traindata.attribute("entCat"));				
			classify = new WekaWrapper(traindata, true);
			classify.crossValidate(foldNum);
			//Classifier entityCl = classify.getMLclass().;
			
			//train second classifier (attributtes)
			traindata.setClass(traindata.attribute("attCat"));				
			classify.setTraindata(traindata);
			classify.crossValidate(foldNum);			
			//Classifier attCl = classify.getMLclass();
			
			classify.printMultilabelPredictions(classify.multiLabelPrediction());			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		traindata.setClass(traindata.attribute("entAttCat"));
		System.err.println("DONE CLI train-atc");				
	}
	
	
	
	/**
	 * Create the available parameters for ATP tagging.
	 */
	private void loadAnnotateParameters() {
		annotateParser.addArgument("-m", "--model")
		.required(true)
		.help("Pass the model to do the tagging as a parameter.\n");
		annotateParser.addArgument("-l","--language")
		.required(false)
		.choices("de", "en", "es", "eu", "it", "nl","fr")
		.help("Choose language; it defaults to the language value in incoming NAF file.\n");
		annotateParser.addArgument("-o","--outputFormat")
		.required(false)
		.choices("semeval2015", "naf")
		.setDefault("semeval2015")
		.help("Choose output format; it defaults to semeval2015.\n");
		annotateParser.addArgument("--dictTag")
		.required(false)
		.choices("tag", "post")
		.setDefault("post")
		.help("Choose to directly tag entities by dictionary look-up; if the 'tag' option is chosen, " +
				"only tags entities found in the dictionary; if 'post' option is chosen, it will " +
				"post-process the results of the statistical model.\n");
		annotateParser.addArgument("--dictPath")
		.required(false)
		.setDefault("")
		.help("Provide the path to the dictionaries for direct dictionary tagging; it ONLY WORKS if --dictTag " +
				"option is activated.\n");
	}
	

	
	/**
	 * Create the main parameters available for tagging slot2 semeval2015.
	 */
	private void loadslot2Parameters() {
		slot2Parser.addArgument("-p", "--params").required(false)
		.help("Load the training parameters file\n");
		slot2Parser.addArgument("-f","--corpusFormat")
		.required(false)
		.choices("semeval2015", "semeval2014", "tab")
		.setDefault("semeval2015")
		.help("Choose format of reference corpus; it defaults to semeval2015 format.\n");
		slot2Parser.addArgument("-n","--naf")
		.required(true)
		.help("tagged naf file path.\n");
		slot2Parser.addArgument("-l","--language")
		.setDefault("en")
		.choices("de", "en", "es", "eu", "it", "nl","fr")
		.help("Choose language; if not provided it defaults to: \n"
				+ "\t- If the input format is NAF, the language value in incoming NAF file."
				+ "\t- en otherwise.\n");
	}
	
	/**
	 * Create the main parameters available for training ATP models.
	 */
	private void loadATCTrainingParameters() {
		trainATCParser.addArgument("-p", "--params").required(true)
		.help("Load the training parameters file\n");
		trainATCParser.addArgument("-t", "--testset")
		.required(false)
		.help("The test or reference corpus.\n");
		trainATCParser.addArgument("-cvf", "--foldNum")
		.required(false)
		.setDefault(10)
		.help("Number of folds to run the cross validation on.\n");
		trainATCParser.addArgument("-v","--validation")
		.required(false)
		.choices("cross", "trainTest", "both")
		.setDefault("both")
		.help("Choose the way the trained model will be validated\n"
				+ "\t - cross : 10 fold cross validation.\n"
				+ "\t - trainTest : 90% train / 10% test division.\n"
				+ "\t - both (default): both cross validation and train/test division will be tested.");
		trainATCParser.addArgument("-f","--corpusFormat")
		.required(false)
		.choices("semeval2015", "semeval2014", "tab")
		.setDefault("semeval2015")
		.help("Choose format of reference corpus; it defaults to semeval2015 format.\n");
		trainATCParser.addArgument("-n","--nullSentences")		
		.action(Arguments.storeTrue())
		.required(false)
		.setDefault(false)
		.help("Whether null examples should be generated from sentences without categories or not.\n");
		trainATCParser.addArgument("-o","--outputpredictions")		
		.action(Arguments.storeTrue())
		.setDefault(false)
		.help("Output predictions or not; output is the corpus annotated with semeval2015 format.\n");
		trainATCParser.addArgument("-l","--language")
		.setDefault("en")
		.choices("de", "en", "es", "eu", "it", "nl","fr")
		.help("Choose language; if not provided it defaults to: \n"
				+ "\t- If the input format is NAF, the language value in incoming NAF file."
				+ "\t- en otherwise.\n");
	}
	
	
	/**
	 * Create the main parameters available for training ATP models.
	 */
	private void loadATC2TrainingParameters() {
		trainATC2Parser.addArgument("-p", "--params").required(true)
		.help("Load the training parameters file\n");
		trainATC2Parser.addArgument("-p2", "--params2").required(false)
		.help("Load the training parameters file\n");
		trainATC2Parser.addArgument("-t", "--testset")
		.required(false)
		.help("The test or reference corpus.\n");
		trainATC2Parser.addArgument("-cvf", "--foldNum")
		.required(false)
		.setDefault(10)
		.help("Number of folds to run the cross validation on.\n");
		trainATC2Parser.addArgument("-v","--validation")
		.required(false)
		.choices("cross", "trainTest", "both")
		.setDefault("both")
		.help("Choose the way the trained model will be validated\n"
				+ "\t - cross : 10 fold cross validation.\n"
				+ "\t - trainTest : 90% train / 10% test division.\n"
				+ "\t - both (default): both cross validation and train/test division will be tested.");
		trainATC2Parser.addArgument("-f","--corpusFormat")
		.required(false)
		.choices("semeval2015", "semeval2014", "tab")
		.setDefault("semeval2015")
		.help("Choose format of reference corpus; it defaults to semeval2015 format.\n");
		trainATC2Parser.addArgument("-n","--nullSentences")		
		.action(Arguments.storeTrue())
		.required(false)
		.setDefault(false)
		.help("Whether null examples should be generated from sentences without categories or not.\n");
		trainATC2Parser.addArgument("-to","--testOnly")		
		.action(Arguments.storeTrue())
		.required(false)
		.setDefault(false)
		.help("Whether only test should be done (assumes models were previously generated).\n");
		trainATC2Parser.addArgument("-o","--outputpredictions")		
		.action(Arguments.storeTrue())
		.setDefault(false)
		.help("Output predictions or not; output is the corpus annotated with semeval2015 format.\n");
		trainATC2Parser.addArgument("-l","--language")
		.setDefault("en")
		.choices("de", "en", "es", "eu", "it", "nl", "fr")
		.help("Choose language; if not provided it defaults to: \n"
				+ "\t- If the input format is NAF, the language value in incoming NAF file."
				+ "\t- en otherwise.\n");
	}
	
	
	

	
}
