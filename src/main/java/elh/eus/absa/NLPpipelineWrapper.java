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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.jdom2.JDOMException;

/**
 * 
 * @author isanvi
 *
 */
public final class NLPpipelineWrapper {
	
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
	public static KAFDocument ixaPipesPos(KAFDocument tokenizedKaf, String posModelPath) throws IOException, JDOMException
	{
		
		KAFDocument.LinguisticProcessor posLp = tokenizedKaf.addLinguisticProcessor(
				"terms", "ixa-pipe-pos-"+FileUtilsElh.fileName(posModelPath), "v1.naf" + "-" + "elixa");			
		//pos tagger parameters
		if (! FileUtilsElh.checkFile(posModelPath))
		{
			System.err.println("NLPpipelineWrapper::ixaPipesPos() - provided pos model path is problematic, "
					+ "probably pos tagging will end up badly...");
		}
		Properties posProp = setPostaggerProperties(posModelPath,
				tokenizedKaf.getLang(), "3", "bin", "true");
		//pos tagger call
		eus.ixa.ixa.pipe.pos.Annotate postagger = new eus.ixa.ixa.pipe.pos.Annotate(posProp);
		posLp.setBeginTimestamp();		
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
	public static String ixaPipesPosConll(KAFDocument tokenizedKaf, String posModelPath) throws IOException, JDOMException
	{
		
		//KAFDocument.LinguisticProcessor posLp = tokenizedKaf.addLinguisticProcessor(
		//		"terms", "ixa-pipe-pos-"+FileUtilsElh.fileName(posModelPath), "v1.naf" + "-" + "elixa");			
		//pos tagger parameters
		if (! FileUtilsElh.checkFile(posModelPath))
		{
			System.err.println("NLPpipelineWrapper::ixaPipesPos() - provided pos model path is problematic, "
					+ "probably pos tagging will end up badly...");
		}
		Properties posProp = setPostaggerProperties(posModelPath,
				tokenizedKaf.getLang(), "3", "bin", "true");
		//pos tagger call
		eus.ixa.ixa.pipe.pos.Annotate postagger = new eus.ixa.ixa.pipe.pos.Annotate(posProp);
		//posLp.setBeginTimestamp();		
		return (postagger.annotatePOSToCoNLL(tokenizedKaf));
		//posLp.setEndTimestamp();

		//System.err.println("NLPpipelineWrapper::ixaPipesPos - pos tagging ready");

		//return tokenizedKaf;		

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
	public static KAFDocument ixaPipesTokPos(String text, String lang, String posModelPath) throws IOException, JDOMException
	{
		return ixaPipesPos(ixaPipesTok(text, lang), posModelPath);
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
	public static String ixaPipesTokPosConll(String text, String lang, String posModelPath) throws IOException, JDOMException
	{
		return ixaPipesPosConll(ixaPipesTok(text, lang), posModelPath);
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
	 * @param language (ISO-639 code) 
	 * @param beamSize
	 * @param lemmatize
	 * @param multiwords
	 * 
	 * @return Properties props
	 * 
	 */
	public static Properties setPostaggerProperties(String model, String language, String beamSize, String lemmatize, String multiwords) {
		Properties props = new Properties();
		props.setProperty("model", model);
		props.setProperty("language", language);
		props.setProperty("beamSize", beamSize);
		props.setProperty("lemmatize", lemmatize);
		//this is a work around for ixa-pipes, because it only allows multiword matching for es and gl.
		if (!language.matches("(gl|es)")){
			multiwords = "false";
		}			
		props.setProperty("multiwords", multiwords);
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
		annotateProperties.setProperty("model", model);
		annotateProperties.setProperty("language", language);
		annotateProperties.setProperty("ruleBasedOption", lexer);
		annotateProperties.setProperty("dictTag", dictTag);
		annotateProperties.setProperty("dictPath", dictPath);
		return annotateProperties;
	}

	public static int eustaggerCall(String taggerCommand, String string, String fname) {
		
		try {
			File temp = new File(fname);
			//System.err.println("eustaggerCall: created temp file: "+temp.getAbsolutePath());
			BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
			bw.write(string+"\n");
			bw.close();
						
			String[] command = {taggerCommand,temp.getName()};
			System.err.println("Eustagger agindua: "+Arrays.toString(command));
				
			ProcessBuilder eustBuilder = new ProcessBuilder()
			.command(command);	
			eustBuilder.directory(new File(temp.getParent()));
			//.redirectErrorStream(true);
			Process eustagger = eustBuilder.start();	
			int success = eustagger.waitFor();			
			//System.err.println("eustagger succesful? "+success);
			if (success != 0)
			{
				System.err.println("eustaggerCall: eustagger error");
			}
			else
			{				
				String tagged = fname+".kaf";
				BufferedReader reader = new BufferedReader(new InputStreamReader(eustagger.getInputStream()));
				//new Eustagger_lite outputs to stdout. Also called ixa-pipe-pos-eu
				if (taggerCommand.contains("eustagger") || taggerCommand.contains("ixa-pipe"))
				{
					Files.copy(eustagger.getInputStream(), Paths.get(tagged));
				}
				// old eustagger (euslem)
				else
				{
					FileUtilsElh.renameFile(temp.getAbsolutePath()+".etiketatua3",tagged);
				}
			}
			//
			// delete all temporal files used in the process.
			temp.delete();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		
		return 0;
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
	public static String tagSentence(String input, String savePathNoExt, String lang, String posModel, eus.ixa.ixa.pipe.pos.Annotate postagger) throws IOException, JDOMException
	{				
		KAFDocument kafinst = new KAFDocument("","");
					
		if (FileUtilsElh.checkFile(savePathNoExt+".kaf"))
		{
			//System.err.println("NLPpipelineWrapper::tagSentence : file already there:"+savePathNoExt+".kaf");
			return savePathNoExt+".kaf";
		}
		// if language is basque 'posModel' argument is used to pass the path to the basque morphological analyzer eustagger 
		else if (lang.compareToIgnoreCase("eu")==0)
		{
			int ok =eustaggerCall(posModel, input, savePathNoExt);
		}
		else
		{
			kafinst = ixaPipesTokPos(input, lang, posModel, postagger);
			kafinst.save(savePathNoExt+".kaf");										
		}
		return savePathNoExt+".kaf";
	}
	
	
}
