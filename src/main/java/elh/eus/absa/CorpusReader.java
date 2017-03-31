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

import ixa.kaflib.Entity;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.WF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;



/**
 * @author isanvi
 *
 */
public class CorpusReader {

	// polarities for each opinion are stored here.
	private HashMap<String, String> sentences = new HashMap<String, String>();
	private HashMap<String, List<String>> sentOps = new HashMap<String, List<String>>();
	private HashMap<String, Opinion> opinions = new HashMap<String, Opinion>();
	private HashMap<String, List<String>> revSents = new HashMap<String, List<String>>();
	//corpus language	
	private String lang;
	//corpus format	(semeval2014|semeval2015|tab|tabglobal)
	private String format;
	
	//pattern to match eustagger executable
	private Pattern eustagger = Pattern.compile("(eustagger|euslem|ixa-pipe-pos-eu)",Pattern.CASE_INSENSITIVE);

	
	/**
	 * Constructor. 
	 * 
	 * @param InputStream in: corpus 
	 * @param String format : format of the corpus (semeval2014|semeval2015|tab|tabglobal)
	 * @param String lang : language of the corpus (ISO 639 code)
	 */
	public CorpusReader (InputStream in, String format, String lang)
	{
		this(in, format, false, lang);
	}
	
	/**
	 * Constructor. 
	 * 
	 * @param InputStream in: corpus 
	 * @param String format : format of the corpus (semeval2014|semeval2015|tab|tabglobal)
	 * @param boolean nullSentOps: whether null opinions should be created for sentence with no opinion
	 *                              (only used for semeval-absa 2015 formatted corpora)
	 * @param String lang : language of the corpus (ISO 639 code)
	 */
	public CorpusReader (InputStream in, String format, boolean nullSentOps, String lang)
	{
		if (format.compareTo("semeval2015")==0)
		{
			extractOpinionsAbsaSemEval2015(in, nullSentOps);			
		}
		else if (format.compareTo("semeval2014")==0)
		{
			extractOpinionsAbsaSemEval2014(in);			
		}
		else if (format.compareTo("tab")==0)
		{
			extractOpinionsTabText(in);
		}
		else if (format.compareTo("tabglobal")==0)
		{
			try {
				extractGlobalPolarityTabText(in);
			} catch (IOException e) {
				System.err.println("IO error when reading corpus file");
			}
		}
		else if (format.compareTo("globalNotagged")==0)
		{
			try {
				extractGlobalPolarityText(in);
			} catch (IOException e) {
				System.err.println("IO error when reading corpus file");
			}
		}
		else if (format.compareTo("ireom")==0)
		{
			try {
				readIreomSentencesToTag(in);
			} catch (IOException e) {
				System.err.println("IO error when reading corpus file");
			}
		}
		else if (format.compareTo("tabNotagged")==0)
		{
			try {
				readTabNotaggedCorpus(in);
			} catch (IOException e) {
				System.err.println("IO error when reading corpus file");
			}
		}
		else
		{
			System.err.println("Corpus couldn't be read");
			System.exit(-5);
		}
		
		setLang(lang);	
		setFormat(format);
	}
	
	/**
	 * @return the language of the corpus
	 */
	public String getLang() {
		return lang;
	}
	
	/**
	 * Set the language of the corpus
	 * 
	 * @param lang string (ISO 639 code)
	 */
	private void setLang(String lang) {
		this.lang = lang;
	}
	
	/**
	 * @param format string (semeval2014|semeval2015|tab|tabglobal)
	 * @return the format of the corpus
	 */
	public String getFormat() {
		return format;
	}
	
	/**
	 * Set the format of the corpus
	 * 
	 * @param format string (semeval2014|semeval2015|tab|tabglobal) 
	 */
	private void setFormat(String format) {
		this.format = format;
	}
	
	/**
	 * @return the sentOps
	 */
	public HashMap<String, List<String>> getSentOps() {
		return sentOps;
	}

	/**
	 * @return the opinions
	 */
	public HashMap<String, Opinion> getOpinions() {
		return opinions;
	}

	/**
	 * 
	 * @param oId
	 * @return a certain opinion in the corpus give its id
	 */
	public Opinion getOpinion(String oId) {
		return getOpinions().get(oId);
	}
	
	/**
	 * 
	 * @param String sId - sentence Id. It has the Id in the "id" attribute (semeval2015 format 'reviewId:sentenceId').
	 *                                   If no "id" attribute exist for sentences "s[0-9]+" format is adopted
	 * @return List<Opinion> a list contain all the opinions annotated in the sentence
	 */
	public List<Opinion> getSentenceOpinions(String sId) {
		List<Opinion> result = new ArrayList<Opinion>();
		if (getSentOps().containsKey(sId))
		{
			for (String id : getSentOps().get(sId) )
			{	
				result.add(opinions.get(id));
			}
		}
		return result;
	}
	
	/**
	 * Sets an opinion set as the opinions of the class
	 * 
	 * @param HashMap<String, Opinion> : HashMap with opinion an their respectice ids.
	 */
	public void setOpinions(HashMap<String, Opinion> opinions) {
		this.opinions = opinions;
	}

	/**
	 * Adds an opinion to the opinions set
	 * 
	 * @param Opinion op : opinion to add. 
	 */
	public void addOpinion(Opinion op) {
		getOpinions().put(op.getId(), op);
		addOpinionToSentence(op);
	}
	
	/**
	 * Removes an opinion from the opinions set
	 * 
	 * @param Opinion op : opinion to remove. 
	 */
	public void removeOpinion(String oid) {
		String sId = getOpinion(oid).getsId();
		getSentOps().get(sId).remove(oid);
		//getOpinions().remove(oid);
		
	}

	/**
	 * Add opinion to sentence
	 * 
	 * @param Opinion op : opinion to add. 
	 *         Sentence to which the opinion belongs is specified within the opinion object. 
	 */
	public void addOpinionToSentence(Opinion op) {
		String oid = op.getId();
		String sid = op.getsId();
		//System.err.println("--"+oid+" "+sid+"--");
		if (getSentOps().containsKey(sid))
		{
			getSentOps().get(sid).add(oid);
		}
		else
		{
			List<String> ops = new ArrayList<String>();
			ops.add(oid);
			getSentOps().put(sid, ops);
		}
	}

	/**
	 * Remove opinions from a certain sentence
	 * 
	 * @param String sId: Id of the sentence to remove opinions from.
	 */
	public void removeSentenceOpinions(String sId) {
		
		//List<String> ops =  getSentOps().get(sId);
		//System.err.println("--"+oid+" "+sid+"--");
		//for (String oid : ops)
		//{			
		//	removeOpinion(oid);
		//}
		sentOps.put(sId, new ArrayList<String>());
	}

	/**
	 * Remove a sentence an its opinions from the corpus. 
	 * WARNING: use this function carefully, as the sentence is not deleted from the review sentence list.
	 * 
	 * @param String sId: Id of the sentence to remove.
	 */
	public void removeSentence(String sId) {
		
		removeSentenceOpinions(sId);
		sentences.remove(sId);
	}
	
	/**
	 * @return the sentences
	 */
	public HashMap<String, String> getSentences() {
		return sentences;
	}

	/**
	 * Returns a string containing all sentences in the corpus as a sentence. 
	 * - A blank line is introduced between sentences.
	 * - If a sentence does not finish with a punctuation mark ([!?.]) a punct '.' is added at the end of a sentence.
	 * 
	 * @return String
	 * 
	 */
	public String getAllSentencesAsString() {
		//create string with the corpus sentences.
		StringBuilder toTag = new StringBuilder();
		for (String sent : getSentences().values()) 
		{
			toTag.append(sent);
			String lineEnd = "\n\n";
			if (! sent.matches("[!?.]$"))
			{
				lineEnd = ".\n\n";
			}
			toTag.append(lineEnd);		
		}
		return toTag.toString();
	}
	
	/**
	 * @return the reviews and their corresponding sentences.
	 */
	public HashMap<String, List<String>> getReviews() {
		return revSents;
	}
	
	/**
	 * 
	 * @param oId - opinion Id in "o[0-9]+" format
	 * @return String the text of the sentence containing the opinion with the given identifier
	 * 
	 */
	public String getOpinionSentence(String oId) 
	{ 
		String s = opinions.get(oId).getsId();
		return sentences.get(s);
	}

	
	/**
	 * @param sentences the sentences to set
	 */
	public void setSentences(HashMap<String, String> sentences) {
		this.sentences = sentences;
	}

	/**
	 * @param sentences the sentences to set
	 */
	public void addSentence(String id, String text) {
		this.sentences.put(id, StringEscapeUtils.unescapeHtml4(StringEscapeUtils.unescapeJava(text)));
	}
	
	/**
	 * @param sentences the sentences to set
	 */
	public String getSentence(String id) {
		return this.sentences.get(id);
	}

	/**
	 * @param String rId : review Id to add
	 * @param String sId : sentence id to add
	 * 
	 */
	private void addRevSent(String rId, String sId) {
		if (!this.revSents.containsKey(rId))
		{
			this.revSents.put(rId, new ArrayList<String>());
		}
		this.revSents.get(rId).add(sId);
	}
	
	private void extractOpinionsAbsaSemEval2014(InputStream fileName) {
		SAXBuilder sax = new SAXBuilder();
		XPathFactory xFactory = XPathFactory.instance();
		try {
			Document doc = sax.build(fileName);
			XPathExpression<Element> expr = xFactory.compile("//sentence",
					Filters.element());
			List<Element> sentences = expr.evaluate(doc);
			Integer sId = 0; //sentence id
			Integer oId = 0; //opinion id			
			for (Element sent : sentences) {
				sId++;
				StringBuilder sb = new StringBuilder();
				String sentString = sent.getChildText("text");
				sb = sb.append(sentString);
				Element aspectTerms = sent.getChild("aspectTerms");
				if (aspectTerms != null) {
					List<Element> aspectTermList = aspectTerms.getChildren();
					for (Element aspectElem : aspectTermList) 
					{
						oId++;
						String trgt = aspectElem.getAttributeValue("target");
						Integer offsetFrom = Integer.parseInt(aspectElem.getAttributeValue("from"));
						Integer offsetTo = Integer.parseInt(aspectElem.getAttributeValue("to"));
						String polarity = aspectElem.getAttributeValue("polarity");
						//String cat = aspectElem.getAttributeValue("category");
						
						//create and add opinion to the structure
						Opinion op = new Opinion("o"+oId, trgt, offsetFrom, offsetTo, polarity, null, "s"+sId);
						this.addOpinion(op);
					}
					
					//System.out.println(sb.toString());
				}
			}
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Read semeval-absa 2015 shared task formatted corpus and extract opinions.
	 * 
	 * @param InputStream fileName: corpus 
	 * @param boolean nullSentOps: whether null opinions should be created for sentence with no opinion
	 *                              (only used for semeval-absa 2015 formatted corpora)
	 * 
	 */
	private void extractOpinionsAbsaSemEval2015(InputStream fileName, boolean nullSentenceOps) {		
		SAXBuilder sax = new SAXBuilder();
		XPathFactory xFactory = XPathFactory.instance();
		try {
			Document doc = sax.build(fileName);
			XPathExpression<Element> expr = xFactory.compile("//sentence",
					Filters.element());
			List<Element> sentences = expr.evaluate(doc);
			String rId = "";
			String sId = ""; //sentence id
			Integer oId = 0; //opinion id
			for (Element sent : sentences) {				
				sId = sent.getAttributeValue("id");
				rId = sId.replaceAll(":[0-9]+$", "");
				
				if (rId.equalsIgnoreCase(sId))
				{
					rId = sId.replaceAll("#[0-9]+$", "");
				}
				
				//store the sentence and the corresponding review
				addRevSent(rId,sId);
				StringBuilder sb = new StringBuilder();
				String sentString = sent.getChildText("text");
				//add sentence to the reader object
				this.addSentence(sId, sentString);

				sb = sb.append(sentString);
				Element opinions = sent.getChild("Opinions");
				if (opinions != null) {
					List<Element> opinionList = opinions.getChildren();
					//there is no opinions
					if (opinionList.isEmpty())
					{
						//System.err.println("kkkkk");
						//create sentence at list, even if it has no opinion elements
						sId = sent.getAttributeValue("id");
						addRevSent(rId,sId);
						String sentStr = sent.getChildText("text");
						//add sentence to the reader object
						this.addSentence(sId, sentStr);
						if (nullSentenceOps)
						{
							oId++;
							//create and add opinion to the structure
							Opinion op = new Opinion("o"+oId, "NULL", 0, 0, "NULL", "NULL", sId);
							this.addOpinion(op);
						}
					}
					
					
					
					for (Element opElem : opinionList) 
					{
						oId++;
						String trgt = opElem.getAttributeValue("target");
						Integer offsetFrom= 0;
						Integer offsetTo = 0;
						try {
							offsetFrom = Integer.parseInt(opElem.getAttributeValue("from"));
							offsetTo = Integer.parseInt(opElem.getAttributeValue("to"));

						} catch (NumberFormatException ne)
						{
						}
						String polarity = opElem.getAttributeValue("polarity");
						String cat = opElem.getAttributeValue("category");
						
						//create and add opinion to the structure
						Opinion op = new Opinion("o"+oId, trgt, offsetFrom, offsetTo, polarity, cat, sId);
						this.addOpinion(op);
						
						//debugging
						sb.append("\n\t> "+"o"+oId+" "+trgt+" "+offsetFrom+"-"+offsetTo+" "+polarity+" "+cat);
					}
					//System.out.println(sb.toString());
				}
				else
				{
				    //System.err.println("kkkkk");
					//create sentence at list, even if it has no opinion elements
					sId = sent.getAttributeValue("id");
					addRevSent(rId,sId);
					String sentStr = sent.getChildText("text");
					//add sentence to the reader object
					this.addSentence(sId, sentStr);
					if (nullSentenceOps)
					{
						oId++;
						//create and add opinion to the structure
						Opinion op = new Opinion("o"+oId, "NULL", 0, 0, "NULL", "NULL", sId);
						this.addOpinion(op);
					}
				}
			}
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}
	}		

	/**
	 * 	Extract sentence texts from tabulated format. The function assumes the text is PoS tagged in
	 *  Conll tabulated format.
	 * 
	 * @param fileName string: input corpus file path
	 */
	private void extractOpinionsTabText(InputStream fileName) {
		SAXBuilder sax = new SAXBuilder();
		XPathFactory xFactory = XPathFactory.instance();
		try {
			Document doc = sax.build(fileName);
			XPathExpression<Element> expr = xFactory.compile("//sentence",
					Filters.element());
			List<Element> sentences = expr.evaluate(doc);
			String rId = "";
			String sId = ""; //sentence id
			Integer oId = 0; //opinion id
			for (Element sent : sentences) {
				sId = sent.getAttributeValue("id");
				rId = sId;
				oId++;
				
				/*store the sentence and the corresponding review
				 * (in this case this info is redundant, because a whole review is represented by a sentence)  
				 */
				addRevSent(rId,sId);
				//StringBuilder sb = new StringBuilder();
				String sentString = sent.getChildText("text");
				//add sentence to the reader object
				this.addSentence(sId, sentString);				
				
				String polarity = sent.getAttributeValue("polarity");
				if (polarity == null)
				{
					System.err.println("no polarity annotation for review "+rId+"."
							+ " Review won't be taken into account");
					continue;
				}
				
				String trgt = "";
				String cat = "global";
				Integer offsetFrom= 0;
				Integer offsetTo = 0;
				
				//create and add opinion to the structure
				Opinion op = new Opinion("o"+oId, trgt, offsetFrom, offsetTo, polarity, cat, sId);
				this.addOpinion(op);
				
				//debugging
				//sb.append("\n\t> "+"o"+oId+" "+trgt+" "+offsetFrom+"-"+offsetTo+" "+polarity+" "+cat);
			}
			//System.out.println(sentString);			
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 	Extract documents from a corpus tagged with the global polarity of the text and create opinions
	 *  from them. The function assumes the text is PoS tagged in Conll tabulated format.
	 *  
	 *  *NOTE: in this case we treat whole documents as sentences.
	 * 
	 * @param fileName string: input corpus file path
	 * @throws IOException 
	 */
	private void extractGlobalPolarityTabText(InputStream fileName) throws IOException {
		BufferedReader creader = new BufferedReader(new InputStreamReader(fileName));   		
		String line;
		String rId = "";
		String sId = ""; //sentence id
		Integer oId = 0; //opinion id
		String polarity = null;
		StringBuilder sentString = new StringBuilder();
		while ((line = creader.readLine()) != null) 
		{
			Pattern p = Pattern.compile("^<doc id=\"([^\"]+)\" (pol|polarity)=\"([^\"]+)\"( score=\"([^\"]+)\")?>$");
			Matcher m = p.matcher(line);
			if (m.matches())
			{
				rId = m.group(1);
				sId = rId+"_g";
				oId++;				
				polarity = m.group(3);				
				//System.err.print("\rReview num "+oId+" read");
				continue;
			}
			//Text unit end, store the document
			else if (line.matches("</doc>"))
			{
				/*store the sentence and the corresponding review
				 * (in this case this info is redundant, because a whole review is represented by a sentence)  
				 */
				addRevSent(rId,sId);
				//add sentence to the reader object
				this.addSentence(sId, sentString.toString());				
				sentString= new StringBuilder();
				
				if (polarity == null)
				{
					System.err.println("no polarity annotation for review "+rId+"."
							+ " Review won't be taken into account, but it will be used for feature extraction"
							+ "(n-grams) if it is specified so.");
					continue;
				}
				
				String trgt = "";
				String cat = "global";
				Integer offsetFrom= 0;
				Integer offsetTo = 0;
				
				//create and add opinion to the structure
				Opinion op = new Opinion("o"+oId, trgt, offsetFrom, offsetTo, polarity, cat, sId);
				this.addOpinion(op);
				
				//debugging
				//sb.append("\n\t> "+"o"+oId+" "+trgt+" "+offsetFrom+"-"+offsetTo+" "+polarity+" "+cat);		
			}	
			// normal annotated line add
			else
			{
				String[] fields = line.split("\\s+");
				if (fields.length >= 3)				
				{
					line = Arrays.toString(Arrays.copyOfRange(fields,0,3)).replace(", ", "\t").replaceAll("[\\[\\]]", "");					
				}
				//System.err.println("length: "+fields.length+" - "+line);
				sentString.append(line+"\n");
			}
		}// reader
		System.err.println("CorpusReader::extractGlobalPolarityTabText -> "+oId+" reviews read.");
	}
	
	/**
	 * 	Extract documents from a corpus tagged with the global polarity of the text and create opinions
	 *  from them. 
	 *  
	 *  *NOTE: in this case we treat whole documents as sentences.
	 * 
	 * @param fileName string: input corpus file path
	 * @throws IOException 
	 */
	private void extractGlobalPolarityText(InputStream fileName) throws IOException {
		BufferedReader creader = new BufferedReader(new InputStreamReader(fileName));   		
		String line;
		String rId = "";
		String sId = ""; //sentence id
		Integer oId = 0; //opinion id
		String polarity = null;
		StringBuilder sentString = new StringBuilder();
		while ((line = creader.readLine()) != null) 
		{
			Pattern p = Pattern.compile("^<doc id=\"([^\"]+)\" (pol|polarity)=\"([^\"]+)\"( score=\"([^\"]+)\")?>$");
			Matcher m = p.matcher(line);
			if (m.matches())
			{
				rId = m.group(1);
				sId = rId+"_g";
				oId++;				
				polarity = m.group(3);				
				//System.err.print("\rReview num "+oId+" read");
				continue;
			}
			//Text unit end, store the document
			else if (line.matches("</doc>"))
			{
				/*store the sentence and the corresponding review
				 * (in this case this info is redundant, because a whole review is represented by a sentence)  
				 */
				addRevSent(rId,sId);
				//add sentence to the reader object
				this.addSentence(sId, sentString.toString());				
				sentString= new StringBuilder();
				
				if (polarity == null)
				{
					System.err.println("no polarity annotation for review "+rId+"."
							+ " Review won't be taken into account, but it will be used for feature extraction"
							+ "(n-grams) if it is specified so.");
					continue;
				}
				
				String trgt = "";
				String cat = "global";
				Integer offsetFrom= 0;
				Integer offsetTo = 0;
				
				//create and add opinion to the structure
				Opinion op = new Opinion("o"+oId, trgt, offsetFrom, offsetTo, polarity, cat, sId);
				this.addOpinion(op);
				
				//debugging
				//sb.append("\n\t> "+"o"+oId+" "+trgt+" "+offsetFrom+"-"+offsetTo+" "+polarity+" "+cat);		
			}	
			// normal annotated line add
			else
			{
				//System.err.println("length: "+fields.length+" - "+line);
				sentString.append(line+"\n");
			}
		}// reader
		System.err.println("CorpusReader::extractGlobalPolarityText -> "+oId+" reviews read.");
	}
	
	
	/**
	 * 	Extract documents from a corpus tagged with the global polarity of the text and create opinions
	 *  from them. The function assumes the text is PoS tagged in Conll tabulated format.
	 *  
	 *  *NOTE: in this case we treat whole documents as sentences.
	 * 
	 * @param fileName string: input corpus file path
	 * @throws IOException 
	 */
	private void readIreomSentencesToTag(InputStream fileName) throws IOException {
		BufferedReader creader = new BufferedReader(new InputStreamReader(fileName));   		
		String line;
		String rId = "";
		String sId = ""; //sentence id
		Integer oId = 0; //opinion id
		String polarity = null;
		
		while ((line = creader.readLine()) != null) 
		{
			StringBuilder sentString = new StringBuilder();
			String[] fields = line.split("\\t");
			
			if (fields.length != 2)
			{
				System.err.println("CorpusReader::readIreomSentencesToTag : bad sentence format,"
						+ "sentence won't be annotated.");
				continue;
			}
			//first field is the Id of the sentence to tag
			rId = fields[0];
			sId = rId+"_g";
			oId++;				
			
			//second field is the text of the sentence to tag
			/*store the sentence and the corresponding review
			 * (in this case this info is redundant, because a whole review is represented by a sentence)  
			 */
			addRevSent(rId,sId);
			sentString.append(fields[1]);
			//add sentence to the reader object
			this.addSentence(sId, sentString.toString());							

			//System.err.print("\rReview num "+oId+" read");
				
			String trgt = "";
			String cat = "global";
			Integer offsetFrom= 0;
			Integer offsetTo = 0;
			
			
			//create and add opinion to the structure
			Opinion op = new Opinion("o"+oId, trgt, offsetFrom, offsetTo, polarity, cat, sId);
			this.addOpinion(op);
				
				//debugging
				//sb.append("\n\t> "+"o"+oId+" "+trgt+" "+offsetFrom+"-"+offsetTo+" "+polarity+" "+cat);				
		}// reader
		System.err.println("CorpusReader::readIreomSentencesToTag -> "+oId+" reviews read.");
	}	
	
	
	/**
	 * 	Extract documents from a corpus tagged with the global polarity of the text and create opinions
	 *  from them. The function assumes the text is PoS tagged in Conll tabulated format.
	 *  
	 *  *NOTE: in this case we treat whole documents as sentences.
	 * 
	 * @param fileName string: input corpus file path
	 * @throws IOException 
	 */
	private void readTabNotaggedCorpus(InputStream fileName) throws IOException {
		BufferedReader creader = new BufferedReader(new InputStreamReader(fileName));   		
		String line;
		String rId = "";
		String sId = ""; //sentence id
		Integer oId = 0; //opinion id
		String polarity = null;
		
		while ((line = creader.readLine()) != null) 
		{
			StringBuilder sentString = new StringBuilder();
			String[] fields = line.split("\\t");
			
			if (fields.length < 3)
			{
				System.err.println("CorpusReader::readTabNotaggedCorpus : bad sentence format, format must be:\n"
						+ "\t\"id<tab>polarity<tab>text[<tab>addittionalfields]\"\t("+fields[0]+") "
						+ "sentence won't be annotated.");
				continue;
			}
			//first field is the Id of the sentence to tag
			rId = fields[0];
			sId = rId+"_g";
			oId++;				
			
			//second field is the polarity of the sentence
			polarity = fields[1];
			
			//third field is the text of the sentence to tag
			/*store the sentence and the corresponding review
			 * (in this case this info is redundant, because a whole review is represented by a sentence)  
			 */
			addRevSent(rId,sId);
			sentString.append(fields[2]);
			//add sentence to the reader object
			this.addSentence(sId, sentString.toString());							

			//System.err.print("\rReview num "+oId+" read");
				
			String trgt = "";
			String cat = "global";
			Integer offsetFrom= 0;
			Integer offsetTo = 0;
			
			
			//create and add opinion to the structure
			Opinion op = new Opinion("o"+oId, trgt, offsetFrom, offsetTo, polarity, cat, sId);
			this.addOpinion(op);
				
				//debugging
				//sb.append("\n\t> "+"o"+oId+" "+trgt+" "+offsetFrom+"-"+offsetTo+" "+polarity+" "+cat);				
		}// reader
		System.err.println("CorpusReader::readTabNotagged -> "+oId+" reviews read.");
	}	
	
	/**
	 * print annotations in Semeval-absa 2015 format
	 *
	 * @param savePath string : path for the file to save the data 
	 * @throws ParserConfigurationException
	 */
	public void print2Semeval2015format(String savePath) throws ParserConfigurationException 
	{
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();			
		
		// root elements
		org.w3c.dom.Document doc = docBuilder.newDocument();
		org.w3c.dom.Element rootElement = doc.createElement("Reviews");
		doc.appendChild(rootElement);
		
		for (String rev : getReviews().keySet())
		{
			// review elements
			org.w3c.dom.Element review = doc.createElement("Review");
			rootElement.appendChild(review);
		 
			// set id attribute to sentence element
			review.setAttribute("rid", rev);

			// Sentences element
			org.w3c.dom.Element sentences = doc.createElement("sentences");
			review.appendChild(sentences);
			
			List<String> processed = new ArrayList<String>();
			
			for (String sent : this.revSents.get(rev))
			{
				if (processed.contains(sent))
				{
					continue;
				}
				else
				{
					processed.add(sent);
				}
				//System.err.println("creating elements for sentence "+sent);
				
				// sentence elements
				org.w3c.dom.Element sentence = doc.createElement("sentence");
				sentences.appendChild(sentence);
			 
				// set attribute to sentence element					
				sentence.setAttribute("id",sent);
				
				// text element of the current sentence
				org.w3c.dom.Element text = doc.createElement("text");				
				sentence.appendChild(text);
				text.setTextContent(getSentences().get(sent));
				
				
				// Opinions element
				org.w3c.dom.Element opinions = doc.createElement("Opinions");
				sentence.appendChild(opinions);
				
				for (Opinion op : getSentenceOpinions(sent))
				{
					if (op.getCategory().equalsIgnoreCase("NULL"))
					{
						continue;
					}
					// opinion elements
					org.w3c.dom.Element opinion = doc.createElement("Opinion");
					opinions.appendChild(opinion);
					
					// set attributes to the opinion element					
					opinion.setAttribute("target", op.getTarget());
					opinion.setAttribute("category", op.getCategory());					
					opinion.setAttribute("polarity", op.getPolarity());
					opinion.setAttribute("from",op.getFrom().toString());
					opinion.setAttribute("to",op.getTo().toString());															
				}
			}
		}
		
		
		// write the content into xml file
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		    
		    
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(savePath));
			
			// Output to console for testing
			//StreamResult result = new StreamResult(System.out);
	 
			transformer.transform(source, result);
	 
			System.err.println("File saved to run.xml");

		} catch (TransformerException e) {
			System.err.println("CorpusReader: error when trying to print generated xml result file.");
			e.printStackTrace();
		} 
	}

	/**
	 * print annotations in conll format 2015 format
	 *
	 * @param savePath string : path for the file to save the data 
	 * @throws ParserConfigurationException
	 * @throws FileNotFoundException 
	 */
	public void print2conll(String savePath) throws FileNotFoundException 
	{
		PrintWriter output = new PrintWriter(savePath);
		
		for (String opkey : getOpinions().keySet())
		{
			String pol = getOpinions().get(opkey).getPolarity();
			String sId = getOpinions().get(opkey).getsId();
			String text = getSentences().get(sId);
			//String toprint = "<doc id=\""+sId+"\" polarity=\""+pol+"\">\n"+text+"\n</doc>\n";
			String toprint = sId+"\t"+pol+"\n";
			output.print(toprint);
		}
		output.close();
	}
	
	/**
	 * Read NAF file containing ATE annotations in the entity layer and print them in Semeval-absa 2015 format
	 * 
	 * @param naf
	 * @throws ParserConfigurationException
	 * @throws Exception
	 */
	public void slot2opinionsFromAnnotations(String naf) throws ParserConfigurationException, Exception 
	{		
		int oId = 0;
		KAFDocument kaf = KAFDocument.createFromFile(new File(naf));
			
		for (Entity e : kaf.getEntities())
		{
			oId++;
			//create and add opinion to the structure
			String polarity ="";
			String cat = "";
			String trgt = e.getStr();
			int offsetFrom = e.getTerms().get(0).getWFs().get(0).getOffset();
			List<WF> entWFs = e.getTerms().get(e.getTerms().size()-1).getWFs();
			int offsetTo = entWFs.get(entWFs.size()-1).getOffset()+entWFs.get(entWFs.size()-1).getLength();
			String sId = e.getTerms().get(0).getWFs().get(0).getXpath();
			Opinion op = new Opinion("o"+oId, trgt, offsetFrom, offsetTo, polarity, cat, sId);
			this.addOpinion(op);
		}				
		print2Semeval2015format("EliXa_Arun.xml");
	}
	
	
	/**
	 *  Process linguistically input sentences with ixa-pipes (tokenization and PoS tagging).
	 *  A tagged file is generated for each sentence in the corpus and stored in the directory
	 *  given as argument. Sentence Ids are used as file names. If a tagged file already exists 
	 *  that sentence is not tagged 
	 * 
	 * @param nafdir : path to the directory were tagged files should be stored
	 * @param posModel : model to be used by the PoS tagger
	 * @throws IOException
	 * @throws JDOMException
	 */
	public void tagSentences(String nafdir, String posModel, String lemmaModel, boolean print) throws IOException, JDOMException
	{				
		KAFDocument nafinst = new KAFDocument("","");
		for (String sId : getSentences().keySet())
		{
			String nafname = sId.replace(':', '_');
			String nafPath = nafdir+File.separator+nafname+".kaf";			
			if (FileUtilsElh.checkFile(nafPath))
			{
				System.err.println("CorpusReader::tagSentence : file already there:"+nafPath);
			}
			/* if language is basque 'posModel' argument can be used to pass the path to the 
			 * basque morphological analyzer eustagger. If the path does not contain "eustagger" or "euslem" 
			 * (usual executable names for the tagger) it defaults to ixa-pipes.
			 * */ 
			else if (eustagger.matcher(posModel).find())
			{
				int ok =NLPpipelineWrapper.eustaggerCall(posModel, getSentences().get(sId), nafdir+File.separator+nafname);				
			}
			else
			{
				nafinst = NLPpipelineWrapper.ixaPipesTokPos(getSentences().get(sId), lang, posModel, lemmaModel);
				//System.err.println(nafinst.toString());
				//nafinst.print();
				//System.err.println("corpusReader::tagSentences naf printed.	");
				nafinst.save(nafPath);										
			}
			
			if (print)
			{
				String toprint = "<doc id=\""+sId+"\" polarity=\""+getSentenceOpinions(sId).get(0).getPolarity()+"\">";
				System.out.println(toprint);
				System.out.println(IOUtils.toString(new FileInputStream(new File(nafPath))));
				System.out.println("</doc>");
			}
		}		
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
	public String tagSentence(String sId, String nafdir, String posModel, String lemmatizerModel) throws IOException, JDOMException
	{				
		KAFDocument kafinst = new KAFDocument("","");
		
		String kafname = sId.replace(':', '_');
		String kafPath = nafdir+File.separator+kafname+".kaf";			
		if (FileUtilsElh.checkFile(kafPath))
		{
			System.err.println("CorpusReader::tagSentence : file already there:"+kafPath);
		}
		/* if language is basque 'posModel' argument can be used to pass the path to the 
		 * basque morphological analyzer eustagger. If the path does not contain "eustagger" or "euslem" 
		 * (usual executable names for the tagger) it defaults to ixa-pipes.
		 * */ 
		else if (eustagger.matcher(posModel).find())
		{
			int ok =NLPpipelineWrapper.eustaggerCall(posModel, getSentences().get(sId), nafdir+File.separator+kafname);
		}
		else
		{
			kafinst = NLPpipelineWrapper.ixaPipesTokPos(getSentences().get(sId), lang, posModel, lemmatizerModel);
			kafinst.save(kafPath);										
		}
		return kafPath;
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
	public String tagSentenceTab(String sId, String nafdir, String posModel, String lemmatizerModel) throws IOException, JDOMException
	{				
		KAFDocument kafinst = new KAFDocument("","");
		
		String savename = sId.replace(':', '_');
		String savePath = nafdir+File.separator+savename+".kaf";			
		if (FileUtilsElh.checkFile(savePath))
		{
			System.err.println("CorpusReader::tagSentence : file already there:"+savePath);
		}
		/* if language is basque 'posModel' argument can be used to pass the path to the 
		 * basque morphological analyzer eustagger. If the path does not contain "eustagger" or "euslem" 
		 * (usual executable names for the tagger) it defaults to ixa-pipes.
		 * */ 
		else if (eustagger.matcher(posModel).find())
		{
			int ok =NLPpipelineWrapper.eustaggerCall(posModel, getSentences().get(sId), nafdir+File.separator+savename);
		}
		else
		{
			String conll = NLPpipelineWrapper.ixaPipesTokPosConll(getSentences().get(sId), lang, posModel, lemmatizerModel);
			FileUtils.writeStringToFile(new File(savePath), conll);										
		}
		return savePath;
	}
	
}
