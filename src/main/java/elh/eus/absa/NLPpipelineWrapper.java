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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.jdom2.JDOMException;

/**
 * 
 * @author isanvi
 *
 */
public final class NLPpipelineWrapper {
	
	
	private static final String modelDir = "morph-models-1.5.0";
	private static final Properties defaultModels = new Properties();
	static {
		try {
			System.err.println(modelDir+File.separator+"morph-models-1.5.0.txt"); 
			defaultModels.load(NLPpipelineWrapper.class.getClassLoader().getResourceAsStream(modelDir+File.separator+"morph-models-1.5.0.txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	/**
	 * Processes a given string with the Ixa-pipe tokenizer.
	 * 
	 * @param String text : input text
	 * @return KAFDocument : tokenized input text in kaf format
	 * 
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static KAFDocument ixaPipesTok(String text, String lang, String savePath) throws IOException, JDOMException
	{
		// Regex added to correct ixa-pipes treatment of punctuation marks : 
		// <wf id="w19" sent="1" para="1" offset="76" length="4">!!??</wf>
		// <term id="t19" type="open" lemma="!!??" pos="N" morphofeat="NCMC000">
		text = text.replaceAll("([!¡?¿])", "$1 ");	 //\p{Po} gets too many punctuation marks.
		
		
		//kaf document to store tokenized text
		KAFDocument kaf = new KAFDocument(lang, "v1.naf");
		KAFDocument.LinguisticProcessor newLp = kaf.addLinguisticProcessor("text", "ixa-pipe-tok-"+lang, 
				"v1.naf" + "-" + "elixa");
		newLp.setBeginTimestamp();
		// objects needed to call the tokenizer
		BufferedReader breader = new BufferedReader(new StringReader(text));
		Properties tokProp = setTokenizerProperties(lang, "default", "no", "no");		
		
		// tokenizer call
		eus.ixa.ixa.pipe.tok.Annotate tokenizer = new eus.ixa.ixa.pipe.tok.Annotate(breader,tokProp);
		tokenizer.tokenizeToKAF(kaf);
		newLp.setEndTimestamp();
		
		breader.close();

		System.err.println("NLPpipelineWrapper::ixaPipesTok - tokenizing ready");
								
		kaf.save(savePath);	
		
		return kaf;		
	}
	
	/**
	 * Processes a given string with the Ixa-pipe tokenizer.
	 * 
	 * @param String text : input text
	 * @return KAFDocument : tokenized input text in kaf format
	 * 
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static KAFDocument ixaPipesTok(String text, String lang) throws IOException, JDOMException
	{
		// Regex added to correct ixa-pipes treatment of punctuation marks : 
		// <wf id="w19" sent="1" para="1" offset="76" length="4">!!??</wf>
		// <term id="t19" type="open" lemma="!!??" pos="N" morphofeat="NCMC000">
		text = text.replaceAll("([!¡?¿])", "$1 ");	 //\p{Po} gets too many punctuation marks.
		
		
		//kaf document to store tokenized text
		KAFDocument kaf = new KAFDocument(lang, "v1.naf");
		KAFDocument.LinguisticProcessor newLp = kaf.addLinguisticProcessor("text", "ixa-pipe-tok-"+lang, 
				"v1.naf" + "-" + "elixa");
		newLp.setBeginTimestamp();
		// objects needed to call the tokenizer
		BufferedReader breader = new BufferedReader(new StringReader(text));
		Properties tokProp = setTokenizerProperties(lang, "default", "no", "no");		
		
		// tokenizer call
		eus.ixa.ixa.pipe.tok.Annotate tokenizer = new eus.ixa.ixa.pipe.tok.Annotate(breader,tokProp);
		tokenizer.tokenizeToKAF(kaf);
		newLp.setEndTimestamp();
		
		breader.close();

		System.err.println("NLPpipelineWrapper::ixaPipesTok - tokenizing ready");
								
		return kaf;		
	}
	
	/**
	 * Processes a given string with the Ixa-pipe PoS tagger.
	 * 
	 * @param KAFDocument tokenizedKaf: tokenized input text in KAF format
	 * @param String posModelPath : path to the pos tagger model
	 * 
	 * @return KAFDocument : PoStagged input text in KAF format
	 * 
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static KAFDocument ixaPipesPos(KAFDocument tokenizedKaf, String posModelPath, String lemmaModelPath) throws IOException, JDOMException
	{
		
		KAFDocument.LinguisticProcessor posLp = tokenizedKaf.addLinguisticProcessor(
				"terms", "ixa-pipe-pos-"+FileUtilsElh.fileName(posModelPath), "v1.naf" + "-" + "elixa");			
		//pos tagger parameters
		if (! FileUtilsElh.checkFile(posModelPath))
		{
			System.err.println("NLPpipelineWrapper::ixaPipesPos() - provided pos model path is problematic, "
					+ "probably pos tagging will end up badly...");
		}
		if (! FileUtilsElh.checkFile(lemmaModelPath))
		{
			System.err.println("NLPpipelineWrapper::ixaPipesPos() - provided lemma model path is problematic, "
					+ "probably pos tagging will end up badly...");
		}
		Properties posProp = setPostaggerProperties(posModelPath,lemmaModelPath,tokenizedKaf.getLang(), "false", "false");
		//pos tagger call
		eus.ixa.ixa.pipe.pos.Annotate postagger = new eus.ixa.ixa.pipe.pos.Annotate(posProp);
		posLp.setBeginTimestamp();		
		//System.err.println(postagger.annotatePOSToCoNLL(tokenizedKaf));
		postagger.annotatePOSToKAF(tokenizedKaf);
		posLp.setEndTimestamp();

		System.err.println("NLPpipelineWrapper::ixaPipesPos - pos tagging ready");

		return tokenizedKaf;		
	}

	/**
	 * Processes a given string with the Ixa-pipe PoS tagger.
	 * 
	 * @param KAFDocument tokenizedKaf: tokenized input text in KAF format
	 * @param String posModelPath : path to the pos tagger model
	 * 
	 * @return KAFDocument : PoStagged input text in KAF format
	 * 
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static String ixaPipesPosConll(KAFDocument tokenizedKaf, String posModelPath,  String lemmaModelPath) throws IOException, JDOMException
	{
		
		//KAFDocument.LinguisticProcessor posLp = tokenizedKaf.addLinguisticProcessor(
		//		"terms", "ixa-pipe-pos-"+FileUtilsElh.fileName(posModelPath), "v1.naf" + "-" + "elixa");			
		//pos tagger parameters
		if (! FileUtilsElh.checkFile(posModelPath))
		{
			System.err.println("NLPpipelineWrapper::ixaPipesPos() - provided pos model path is problematic, "
					+ "probably pos tagging will end up badly...");
		}
		if (! FileUtilsElh.checkFile(lemmaModelPath))
		{
			System.err.println("NLPpipelineWrapper::ixaPipesPos() - provided lemma model path is problematic, "
					+ "probably pos tagging will end up badly...");
		}
		Properties posProp = setPostaggerProperties(posModelPath,lemmaModelPath,tokenizedKaf.getLang(), "false", "false");
		//pos tagger call
		eus.ixa.ixa.pipe.pos.Annotate postagger = new eus.ixa.ixa.pipe.pos.Annotate(posProp);		
		return (postagger.annotatePOSToCoNLL(tokenizedKaf));
		
		//System.err.println("NLPpipelineWrapper::ixaPipesPos - pos tagging ready");
	}
	
	/**
	 * Processes a given string with the Ixa-pipe PoS tagger.
	 * 
	 * @param KAFDocument tokenizedKaf: tokenized input text in KAF format
	 * @param String posModelPath : path to the pos tagger model
	 * 
	 * @return KAFDocument : PoStagged input text in KAF format
	 * 
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static KAFDocument ixaPipesPos(KAFDocument tokenizedKaf, String posModelPath, eus.ixa.ixa.pipe.pos.Annotate postagger) throws IOException, JDOMException
	{
		
		KAFDocument.LinguisticProcessor posLp = tokenizedKaf.addLinguisticProcessor(
				"terms", "ixa-pipe-pos-"+FileUtilsElh.fileName(posModelPath), "v1.naf" + "-" + "elixa");			
		posLp.setBeginTimestamp();			
		postagger.annotatePOSToKAF(tokenizedKaf);
		posLp.setEndTimestamp();

		System.err.println("NLPpipelineWrapper::ixaPipesPos - pos tagging ready");

		return tokenizedKaf;		
	}
	
	/**
	 * Processes a given string with the Ixa-pipe NERC tagger.
	 * 
	 * @param KAFDocument tokenizedKaf: tokenized input text in KAF format
	 * @param String nercModelPath : path to the NERC tagger model
	 * 
	 * @return KAFDocument : NERC tagged input text in KAF format
	 * 
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static KAFDocument ixaPipesNERC(KAFDocument tokenizedKaf, String nercModelPath, String lexer, String dictTag, String dictPath) throws IOException, JDOMException
	{
		
		KAFDocument.LinguisticProcessor nercLp = tokenizedKaf.addLinguisticProcessor(
				"entities", "ixa-pipe-pos-"+FileUtilsElh.fileName(nercModelPath), "v1.naf" + "-" + "elixa");			
		//pos tagger parameters
		if (! FileUtilsElh.checkFile(nercModelPath))
		{
			System.err.println("NLPpipelineWrapper : ixaPipesPos() - provided pos model path is problematic, "
					+ "probably pos tagging will end up badly...");
		}
		Properties nercProp = setIxaPipesNERCProperties(nercModelPath,
				tokenizedKaf.getLang(), lexer, dictTag, dictPath);
		//pos tagger call
		eus.ixa.ixa.pipe.nerc.Annotate nerctagger = new eus.ixa.ixa.pipe.nerc.Annotate(nercProp);
		nercLp.setBeginTimestamp();		
		nerctagger.annotateNEsToKAF(tokenizedKaf);
		nercLp.setEndTimestamp();
		
		return tokenizedKaf;		

		//System.err.println("pos tagging amaituta");
	}
	
	/**
	 * Processes a given string with the Ixa-pipe NERC tagger.
	 * 
	 * @param KAFDocument tokenizedKaf: tokenized input text in KAF format
	 * @param String nercModelPath : path to the NERC tagger model
	 * 
	 * @return KAFDocument : NERC tagged input text in KAF format
	 * 
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static KAFDocument ixaPipesNERC(KAFDocument tokenizedKaf, String nercModelPath, eus.ixa.ixa.pipe.nerc.Annotate nerctagger) throws IOException, JDOMException
	{
		
		KAFDocument.LinguisticProcessor nercLp = tokenizedKaf.addLinguisticProcessor(
				"entities", "ixa-pipe-pos-"+FileUtilsElh.fileName(nercModelPath), "v1.naf" + "-" + "elixa");			
		
		//NERC tagger call
		nercLp.setBeginTimestamp();		
		nerctagger.annotateNEsToKAF(tokenizedKaf);
		nercLp.setEndTimestamp();
		
		return tokenizedKaf;		

		//System.err.println("pos tagging amaituta");
	}
	
	/**
	 * Tokenizes and PoS tags a given string with Ixa-pipes.
	 * 
	 * @param String text : input text
	 * @param String lang : input text language (ISO-639 code) 
	 * @return KAFDocument : PoStagged input text in KAF format
	 * 
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static KAFDocument ixaPipesTokPos(String text, String lang, String posModelPath, String lemmatizerModelPath) throws IOException, JDOMException
	{
		return ixaPipesPos(ixaPipesTok(text, lang), posModelPath, lemmatizerModelPath);
	}
	
	/**
	 * Tokenizes and PoS tags a given string with Ixa-pipes.
	 * 
	 * @param String text : input text
	 * @param String lang : input text language (ISO-639 code) 
	 * @return KAFDocument : PoStagged input text in KAF format
	 * 
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static String ixaPipesTokPosConll(String text, String lang, String posModelPath, String lemmatizerModelPath) throws IOException, JDOMException
	{
		return ixaPipesPosConll(ixaPipesTok(text, lang), posModelPath, lemmatizerModelPath);
	}
	
	/**
	 * Tokenizes and PoS tags a given string with Ixa-pipes.
	 * 
	 * @param String text : input text
	 * @param String lang : input text language (ISO-639 code) 
	 * @return KAFDocument : PoStagged input text in KAF format
	 * 
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static KAFDocument ixaPipesTokPos(String text, String lang, String posModelPath, eus.ixa.ixa.pipe.pos.Annotate postagger) throws IOException, JDOMException
	{
		return ixaPipesPos(ixaPipesTok(text, lang), posModelPath, postagger);
	}	
	
	/**
	 * 
	 * Set properties for the Ixa-pipe-tok tokenizer module
	 * 
	 * @param language (ISO-639 code) 
	 * @param normalize
	 * @param untokenizable
	 * @param hardParagraph
	 * 
	 * @return Properties props
	 * 
	 */
	private static Properties setTokenizerProperties(String language, String normalize, String untokenizable, String hardParagraph) {
		Properties props = new Properties();
		props.setProperty("language", language);
	    props.setProperty("normalize", normalize);
	    props.setProperty("untokenizable", untokenizable);
	    props.setProperty("hardParagraph", hardParagraph);
	    return props;
	}
	/**
	 * 
	 * Set properties for the Ixa-pipe-pos tagger module
	 * 
	 * @param model
	 * @param lemmaModel
	 * @param language (ISO-639 code) 
	 * @param multiwords
	 * @param dictag
	 * 
	 * @return Properties props
	 * 
	 */
	public static Properties setPostaggerProperties(String model, String lemmaModel, String language, String multiwords, String dictag) {
		Properties props = new Properties();
		props.setProperty("model", getXPipeResource(model, language, "pos"));
		props.setProperty("lemmatizerModel", getXPipeResource(lemmaModel, language, "lemma"));
		props.setProperty("language", language);
		//props.setProperty("beamSize", beamSize); @deprecated 
		//this is a work around for ixa-pipes, because it only allows multiword matching for es and gl.
		if (!language.matches("(gl|es)")){
			multiwords = "false";
		}			
		props.setProperty("multiwords", multiwords);
		
		props.setProperty("dictag", dictag);
		
		return props;
	}
	
	/**
	 * Set a Properties object with the CLI parameters for annotation.
	 * @param model the model parameter
	 * @param language language parameter
	 * @param lexer rule based parameter
	 * @param dictTag directly tag from a dictionary
	 * @param dictPath directory to the dictionaries
	 * @return the properties object
	 */
	public static Properties setIxaPipesNERCProperties(String model, String language, String lexer, String dictTag, String dictPath) {
		Properties annotateProperties = new Properties();
		annotateProperties.setProperty("model", getXPipeResource(model, language, "nerc"));
		annotateProperties.setProperty("language", language);
		annotateProperties.setProperty("ruleBasedOption", lexer);
		annotateProperties.setProperty("dictTag", dictTag);
		annotateProperties.setProperty("dictPath", dictPath);
		return annotateProperties;
	}

	public static int eustaggerCall(String taggerCommand, String string, String fname) {
		
		
		try {
			if (taggerCommand.contains("ixa-pipe-pos-eu")) 
			{
				String[] command = { taggerCommand };
				//System.err.println("ixa-pipe-pos-eu agindua: " + Arrays.toString(command));

				ProcessBuilder eustBuilder = new ProcessBuilder().command(command);				
				Process eustagger = eustBuilder.start();
				
				
				OutputStreamWriter bw = new OutputStreamWriter(eustagger.getOutputStream());
				bw.write(string + "\n");
				bw.close();
				
				int success = eustagger.waitFor();
				
				// System.err.println("eustagger succesful? "+success);
				if (success != 0) {
					System.err.println("eustaggerCall: ixa-pipe-pos-eu error: "+fname);
					return 0;
				} else {
					String tagged = fname + ".kaf";
					BufferedReader reader = new BufferedReader(new InputStreamReader(eustagger.getInputStream()));
					
					Files.copy(eustagger.getInputStream(), Paths.get(tagged));
					return 1;
				}
			}
			else {

				File temp = new File(fname);
				// System.err.println("eustaggerCall: created temp file:
				// "+temp.getAbsolutePath());
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp), "UTF8"));
				bw.write(string + "\n");
				bw.close();

				String[] command = { taggerCommand, temp.getName() };
				System.err.println("Eustagger agindua: " + Arrays.toString(command));

				ProcessBuilder eustBuilder = new ProcessBuilder().command(command);
				eustBuilder.directory(new File(temp.getParent()));
				// .redirectErrorStream(true);
				Process eustagger = eustBuilder.start();
				int success = eustagger.waitFor();
				// System.err.println("eustagger succesful? "+success);
				if (success != 0) {
					System.err.println("eustaggerCall: eustagger error");
					return 0;
				} else {
					String tagged = fname + ".kaf";
					BufferedReader reader = new BufferedReader(new InputStreamReader(eustagger.getInputStream()));
					// new Eustagger_lite outputs to stdout. Also called
					// ixa-pipe-pos-eu
					if (taggerCommand.contains("eustagger") || taggerCommand.contains("ixa-pipe-pos-eu")) {
						Files.copy(eustagger.getInputStream(), Paths.get(tagged));
					}
					// old eustagger (euslem)
					else {
						FileUtilsElh.renameFile(temp.getAbsolutePath() + ".etiketatua3", tagged);
					}
				}
				//
				// delete all temporal files used in the process.
				temp.delete();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
		
		return 1;
	}
	
	
	/**
	 *  Process linguistically input sentence with ixa-pipes (tokenization and PoS tagging).
	 *  A tagged file is generated for each sentence in the corpus and stored in the directory
	 *  given as argument. Sentence Ids are used as file names. If a tagged file already exists 
	 *  that sentence is not tagged 
	 * 
	 * @param nafdir : path to the directory were tagged files should be stored
	 * @param posModel : model to be used by the PoS tagger
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static int tagSentence(String input, String savePathNoExt, String lang, String posModel, String lemmaModel, eus.ixa.ixa.pipe.pos.Annotate postagger) throws IOException, JDOMException
	{				
		KAFDocument kafinst = new KAFDocument("","");
			
		//System.err.println(posModel);
		if (FileUtilsElh.checkFile(savePathNoExt+".kaf"))
		{
			//System.err.println("NLPpipelineWrapper::tagSentence : file already there:"+savePathNoExt+".kaf");
			return 1;
		}
		
		/* if language is basque 'posModel' argument can be used to pass the path to the 
		 * basque morphological analyzer eustagger. If the path does not contain "eustagger" or "euslem" 
		 * (usual executable names for the tagger) it defaults to ixa-pipes.
		 * */
		else if (Pattern.compile("(eustagger|euslem|ixa-pipe-pos-eu)", Pattern.CASE_INSENSITIVE).matcher(posModel).find())
		{
			int ok =eustaggerCall(posModel, input, savePathNoExt);
			return ok;
		}
		else
		{
			kafinst = ixaPipesTokPos(input, lang, posModel, postagger);
			kafinst.save(savePathNoExt+".kaf");										
		}
		return 1;
	}
	
	/**
	 * 
	 *  Function to get the resource path to pass it to Ixa-pipes. Needed to pass the default lemma and 
	 *  pos models. In cases where specific models are used instead of the defaults, 
	 *  this function returns the same input. No IO problems are handled here. 
	 * @param model
	 * @param lang [en|es|eu|fr]
	 * @param type [pos|lemma|nerc]
	 * @return
	 */
	private static String getXPipeResource(String model, String lang, String type){
		String rsrcStr = "";
		if (model.equalsIgnoreCase("default"))
		{
			String rsrcPath = defaultModels.getProperty(lang+"-"+type);
			InputStream rsrc = NLPpipelineWrapper.class.getClassLoader().getResourceAsStream((modelDir+File.separator+lang+File.separator+rsrcPath));
			try {
				File tempModelFile = File.createTempFile("Ellixa-posModel", Long.toString(System.nanoTime()));
				tempModelFile.deleteOnExit();
				System.err.println(lang+"-"+type+" --> "+rsrcPath+" -- "+rsrc+" --- "+tempModelFile.getAbsolutePath());
				FileUtils.copyInputStreamToFile(rsrc, tempModelFile);
				return tempModelFile.getAbsolutePath();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return model;
			}
		}
		else
		{
			return model;
		}
	}
	
}
