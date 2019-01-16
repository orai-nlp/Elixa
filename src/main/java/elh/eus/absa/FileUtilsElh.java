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
import ixa.kaflib.WF;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.jdom2.JDOMException;
import org.jdom2.input.JDOMParseException;

public final class FileUtilsElh {

	
	
	/**
	* Check input file integrity.
	* @param name
	* the name of the file
	* @param inFile
	* the file
	*/
	public static boolean checkFile(final String name) 
	{
		return checkFile(new File(name));
	}

	/**
	* Check input file integrity.
	* @param name
	* the name of the file
	* @param inFile
	* the file
	*/
	public static boolean checkFile(final File f) 
	{
		boolean isFailure = true;
		
		if (! f.isFile()) {
			isFailure = false;
		} 
		else if (!f.canRead()) {
			isFailure = false;
		}
		
		return isFailure;
	}

	
	/**
	* Check input directory integrity.
	* @param name
	* the name of the directory
	* @param inFile
	* the directory
	*/
	public static boolean checkDir(final String name) 
	{
		return checkDir(new File(name));
	}
	
	/**
	* Check input directory integrity.
	* @param name
	* the name of the directory
	* @param inFile
	* the directory
	*/
	public static boolean checkDir(final File f) 
	{
		boolean isFailure = true;
		
		if (! f.isDirectory()) {
			isFailure = false;
		} 
		else if (!f.canRead()) {
			isFailure = false;
		}
		return isFailure;

	}
	
	/**
	* return the name (without path) of a string. The function assumes that the input is a file path.
	* 
	* @param fname : string to extract the name from
	* the directory
	*/
	public static String fileName(String fname) 
	{
		File f = new File(fname);
		return f.getName();
	}
	
	/**
	 * Function creates a temporal directory with a random name.
	 */
	public static File createTempDirectory()
			throws IOException
	{
		final File temp;

		temp = File.createTempFile("elixa-temp", Long.toString(System.nanoTime()));

		if(!(temp.delete()))
		{
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
		}

		if(!(temp.mkdir()))
		{
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}

		return (temp);
	}

	/**
	 * Function renames a file to a new name. if the new name already exists throws an exception.
	 *  
	 */
	public static void renameFile (String oldFile, String newName) throws IOException 
	{
		File file1 = new File(oldFile);
		File file2 = new File(newName);
		if(file2.exists())
		{
			throw new java.io.IOException("FileUtilsElh::renameFile : file exists");
		}
		else if (! file1.renameTo(file2)) 
		{
			System.err.println("FileUtilsElh::renameFile : moving file failed\n\t"+file1.getAbsolutePath()+"\t"+file2.getAbsolutePath()); 
		}
		// Rename file (or directory)
		//file1.renameTo(file2);		 
	}
	
	

	/**
	 * Function prints a list of polar words from an input naf to standard output
	 * 
	 * @param doc
	 * @throws Exception
	 * @throws JDOMException
	 */
	public static void printPolarWordsFromNaf(KAFDocument doc) throws Exception, JDOMException
	{
		String toprint = "";

		for (Term term : doc.getTerms())
		{ 
			if (term != null && term.hasSentiment() && (term.getSentiment().hasPolarity() || term.getSentiment().hasSentimentModifier()))
			{
				String tPol="";
				if (term.getSentiment().hasPolarity())
				{
					tPol = term.getSentiment().getPolarity();
				}
				else if (term.getSentiment().hasSentimentModifier())
				{
					tPol = term.getSentiment().getSentimentModifier();
				}
				else //this should never be fulfilled. If so forget this term.
				{
					continue;
				}
				toprint=term.getLemma()+"	"+tPol;
				// Commented code prints found sentiment term word forms
				//toprint="";				
				//for (WF form : term.getWFs())
				//{
				//	toprint+=form.getForm()+" ";
				//}
				//toprint = toprint.trim()+"	"+tPol;
				System.out.println(toprint);
			}
		}	
	}
	
	
	
	/**
	 * Function prints a kaf document containing sentiment annotations to a colored html file.
	 *  
	 */
	public static void prettyPrintSentKaf(KAFDocument doc, String fname) throws Exception, JDOMException
	{
		String toprint = "";
		Map <String, String> wfspans = new HashMap <String,String>();

		int sentNum = doc.getNumSentences();
		for (int i=0; i<sentNum; i++)    //(List<WF> sentence : doc.getSentences())
		{			
			// store spans of polarity terms in wfspans variable.
			for (Term term : doc.getTermsBySent(i))
			{ 
				if (term != null && term.hasSentiment() && (term.getSentiment().hasPolarity() || term.getSentiment().hasSentimentModifier()))
				{
					String tPol="";
					if (term.getSentiment().hasPolarity())
					{
						tPol = term.getSentiment().getPolarity();
					}
					else if (term.getSentiment().hasSentimentModifier())
					{
						tPol = term.getSentiment().getSentimentModifier();
					}
					else //this should never be fulfilled. If so forget this term.
					{
						continue;
					}
					// single form term
					if (term.getWFs().size() < 2)
					{
						wfspans.put(term.getWFs().get(0).getId(),tPol+"single");						 						 							 
					}
					// multiword term
					else
					{
						wfspans.put(term.getWFs().get(0).getId(),tPol);
						wfspans.put(term.getWFs().get(term.getWFs().size()-1).getId() ,"end");
					}
				}
			}

			// from now on write the html code to print the document.
			for (WF form : doc.getWFsBySent(i))
			{

				String wId = form.getId();
				if (wfspans.containsKey(wId))
				{
					if (wfspans.get(wId).compareTo("end") == 0)
					{
						toprint+=form.getForm()+"</span> ";							 
					}
					else
					{
						String color = wfspans.get(wId).substring(0, 3);
						boolean single = wfspans.get(wId).endsWith("single");

						color = color.replaceAll("pos", "Green");
						color = color.replaceAll("neg", "Red");
						color = color.replaceAll("neu", "Orange");
						color = color.replaceAll("int", "Indigo");
						color = color.replaceAll("weak", "MediumPurple");
						color = color.replaceAll("shi", "Blue");
						if (single)
						{
							toprint+="<span style=\"font-weight:bold; color:"+color+"\">"+form.getForm()+"</span> ";								 
						}
						else
						{
							toprint+="<span style=\"font-weight:bold; color:"+color+"\">"+form.getForm()+" ";
						}

					}
				}
				else 
				{
					toprint+=form.getForm()+" ";					 
				}					 
				//System.out.print(form.getForm()+" ");
			}
			toprint+="\n"; 
		};
		BufferedWriter bwriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fname+"_tagged.html")));			  
		toprint = toprint.replaceAll("([a-z>]) ([.,;:\\)\\?\\!])","$1$2");
		toprint = toprint.replaceAll("\\.\n",".\n<br/>");
		bwriter.write("<html>\n<body>\n<div id=text\">"+toprint+"\n</div>\n</body>\n</html>");
		bwriter.close();
		//System.out.println("\n\n"+toprint+"\n\nlisto!");
		//System.out.println("\n\nlisto!");
	}
	
	/**
	 * Function prints a kaf document containing sentiment annotations and polarity tagger results to a colored 
	 * html file.
	 *  
	 */	
	public static void prettyPrintSentKaf(Map<String,String> kaf) throws Exception, JDOMException
	{
		String fname = kaf.get("taggedFile");
		BufferedReader breader = new BufferedReader(new FileReader(fname));
		KAFDocument doc = KAFDocument.createFromStream(breader);
		String toprint = "";
		Map <String, String> wfspans = new HashMap <String,String>();

		int sentNum = doc.getNumSentences();
		for (int i=0; i<sentNum; i++)    //(List<WF> sentence : doc.getSentences())
		{			
			// store spans of polarity terms in wfspans variable.
			for (Term term : doc.getTermsBySent(i))
			{ 
				if (term != null && term.hasSentiment() && (term.getSentiment().hasPolarity() || term.getSentiment().hasSentimentModifier()))
				{
					String tPol="";
					if (term.getSentiment().hasPolarity())
					{
						tPol = term.getSentiment().getPolarity();
					}
					else if (term.getSentiment().hasSentimentModifier())
					{
						tPol = term.getSentiment().getSentimentModifier();
					}
					else //this should never be fulfilled. If so forget this term.
					{
						continue;
					}
					// single form term
					if (term.getWFs().size() < 2)
					{
						wfspans.put(term.getWFs().get(0).getId(),tPol+"single");						 						 							 
					}
					// multiword term
					else
					{
						wfspans.put(term.getWFs().get(0).getId(),tPol);
						wfspans.put(term.getWFs().get(term.getWFs().size()-1).getId() ,"end");
					}
				}
			}

			// from now on write the html code to print the document.
			for (WF form : doc.getWFsBySent(i))
			{

				String wId = form.getId();
				if (wfspans.containsKey(wId))
				{
					if (wfspans.get(wId).compareTo("end") == 0)
					{
						toprint+=form.getForm()+"</span> ";							 
					}
					else
					{
						String color = wfspans.get(wId).substring(0, 3);
						boolean single = wfspans.get(wId).endsWith("single");

						color = color.replaceAll("pos", "Green");
						color = color.replaceAll("neg", "Red");
						color = color.replaceAll("neu", "Orange");
						color = color.replaceAll("int", "Indigo");
						color = color.replaceAll("weak", "MediumPurple");
						color = color.replaceAll("shi", "Blue");
						if (single)
						{
							toprint+="<span style=\"font-weight:bold; color:"+color+"\">"+form.getForm()+"</span> ";								 
						}
						else
						{
							toprint+="<span style=\"font-weight:bold; color:"+color+"\">"+form.getForm()+" ";
						}

					}
				}
				else 
				{
					toprint+=form.getForm()+" ";					 
				}					 
				//System.out.print(form.getForm()+" ");
			}
			toprint+="\n"; 
		};
		BufferedWriter bwriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fname+"_tagged.html")));			  
		toprint = toprint.replaceAll("([a-z>]) ([.,;:\\)\\?\\!])","$1$2");
		toprint = toprint.replaceAll("\\.\n",".\n<br/>");
		bwriter.write("<html>\n<body>\n<div id=\"details\">"		    			
				+ "\n<br/><strong>Number of words containing sentiment found:</strong> "+kaf.get("sentTermNum")
				+ "\n<br/><strong>Polarity score:</strong> "+kaf.get("avg")
				+ "\n<br/><strong>Polarity (threshold -> "+kaf.get("thresh")+"):</strong> "+kaf.get("polarity")
				+ "</div>\n<div id=text\">"+toprint+"\n</div>\n</body>\n</html>");
		bwriter.close();
		//System.out.println("\n\n"+toprint+"\n\nlisto!");
		//System.out.println("\n\nlisto!");
	}
	
	/**
	 * Function reads two column file and stores the values into a HashMap<String,String> object 
	 * 
	 * @param resource : InputStream containing the two column resource
	 * @return HashMap<String,String> contains the elements and their respective attribute values 
	 * 
	 * @throws IOException if the given resource has reading problems.
	 */
	public static HashMap<String, String> loadTwoColumnResource(InputStream resource) 
			throws IOException
	{
		HashMap<String, String> result = new HashMap<String, String>();		
		
		if (resource == null)
		{
			System.err.println("FileUtilsElh::loadTwoColumnResource - resource is null");
			return result;
		}	
		
		BufferedReader breader = new BufferedReader(new InputStreamReader(resource));
		String line;
		while ((line = breader.readLine()) != null) 
		{
			if (line.startsWith("#") || line.matches("^\\s*$"))
			{
				continue;
			}
			String[] fields = line.split("\t");
			try{
				result.put(fields[0], fields[1]);
			}catch (IndexOutOfBoundsException ioobe){
				System.err.println("FileUtilsElh::loadTwoColumnResource - "+line);
			}
		}											
		breader.close();
		return result;
	}
	
	/**
	 * Function reads a single column file and stores the values into a HashSet<String> object 
	 * 
	 * @param resource : InputStream containing the two column resource
	 * @return HashSet<String> contains the elements and their respective attribute values 
	 * 
	 * @throws IOException if the given resource has reading problems.
	 */
	public static HashSet<String> loadOneColumnResource(InputStream resource) 
			throws IOException
	{
		HashSet<String> result = new HashSet<String>();		
		if (resource == null)
		{
			System.err.println("FileUtilsElh::loadOneColumnResource - resource is null");
			return result;
		}		
		
		BufferedReader breader = new BufferedReader(new InputStreamReader(resource));
		String line;
		while ((line = breader.readLine()) != null) 
		{
			if (line.startsWith("#") || line.matches("^\\s*$"))
			{
				continue;
			}
			try{
				result.add(line.trim());
				//System.err.println("FileUtilsElh::loadOneColumnResource - "+line.trim());
			}catch (IndexOutOfBoundsException ioobe){
				System.err.println("FileUtilsElh::loadOneColumnResource - "+line);
			}
		}											
		breader.close();
		return result;
	}
	
    
	public static KAFDocument loadNAFfromFile (String nafPath)
	{
		try{
			KAFDocument naf = KAFDocument.createFromFile(new File(nafPath));
			return naf;
		}catch (IOException je)
		{
			return null;
		}
	}

	public static InputStream parseModelArgument (String modelPath, String lang)
	{
		if (modelPath.equalsIgnoreCase("default"))
		{
			return FileUtilsElh.class.getClassLoader().getResourceAsStream("elixa-models/"+lang+"-twt.model");
		}
		else
		{
			try {
				return new FileInputStream(modelPath);
			}catch (IOException ioe) {
				// TODO Auto-generated catch block
				System.err.println("No polarity model loaded. Check that the file exist, "
						+ "or that the resource was correctly packaged. EliXa will try to extract the features from the corpus.");
				return null;
			}
		} 
	}
	
	
	public static String getElixaResource (InputStream rsrc, String prefix){
		String result="none";
		try {
			File tempRsrcFile = File.createTempFile(prefix, Long.toString(System.nanoTime()));
			tempRsrcFile.deleteOnExit();
			//System.err.println(lang+"-"+type+" --> "+rsrcPath+" -- "+rsrc+" --- "+tempModelFile.getAbsolutePath());
			FileUtils.copyInputStreamToFile(rsrc, tempRsrcFile);
			return tempRsrcFile.getAbsolutePath();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.err.println("ERROR: EliXa::FileUtilsElh - Resource was could not be loaded. "
					+ "Execution may end with errors.");			
			return result;
		}		
	}
	
}
