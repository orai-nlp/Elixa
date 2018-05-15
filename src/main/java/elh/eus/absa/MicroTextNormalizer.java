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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.validator.routines.UrlValidator;


/**
 * MicroTextNormalization class - This class offers microtext normalization normalizations, 
 * mostly Twitter specific normalizations such as:
 *    - url/@user standarization (e.g. http://.* = URL , @user = USER) 
 *    - hashtag divisions (e.g. #NewYorkMarathon = New York Marathon)
 *    - emoticon normalization (not implemented) 
 *    - Non standard words normalization (e.g. q -> que)
 *    
 *    
 * @author inaki
 */

public class MicroTextNormalizer {

	//various patterns used for normalization. 
	private static Pattern affixes = Pattern.compile("^([^\\p{L}\\p{M}\\p{Nd}]*)([\\p{L}\\p{M}\\p{Nd}]+)([^\\p{L}\\p{M}\\p{Nd}]*)$"); 
	private static Pattern allowedAcronyms = Pattern.compile("^([\\p{L}\\p{M}])\\1{1,2}(([\\p{L}\\p{M}])\\3{1,2})?$");
	//words attached to hashtags (e.g., hello#world)
	private static Pattern attachedWords = Pattern.compile("([^\\s])([@#][\\p{L}\\p{M}\\p{Nd}])");
	
	
	// interjections
	private static Pattern jajeji = Pattern.compile("(?i)\\b([j][jiea]+)\\b");
	private static Pattern hahehi = Pattern.compile("(?i)\\b([h]*[iea]([hiea]+))\\b"); //hahaha but no ha (ha salido)
	private static Pattern hjuas = Pattern.compile("(?i)\\b([hj]uas)+\\b");
	private static Pattern lol = Pattern.compile("(?i)\\b(lol(ol)*)\\b");
	private static Pattern hojo = Pattern.compile("(?i)\\b([hj]o[hj]o([hj]o)*)\\b");
	private static Pattern buajaha = Pattern.compile("(?i)\\b([bm]ua[hj]a([hj]a)+)\\b");
	private static Pattern muacks = Pattern.compile("(?i)\\b(m+u+a+c*k*s*)\\b");    
	private static Pattern puff = Pattern.compile("(?i)\\b(p+u*f+)\\b");//INTERNEG
	private static Pattern uf = Pattern.compile("(?i)\\b(u+f+)\\b"); //INTERNEG
	
	//twitter users
	private static Pattern user = Pattern.compile("@([\\p{L}\\p{M}\\p{Nd}_]{1,15})");
	
	//hashtags
	private static Pattern hashtag = Pattern.compile("#([^\\.,:;!?¿¡\\[\\]\\{\\}\'\"\\(\\)%&$@]+)");
	private static Pattern multiWordhashtag = Pattern.compile("^([\\p{Nd}]+)?([\\p{L}\\p{M}]+)([\\p{Nd}]+)?");
	private static Pattern multiUpper = Pattern.compile("([\\p{Lu}][\\p{Ll}]+)");
	
	private String language;
	
	private HashMap<String,String> emodict;
	
	private HashMap<String,String> nonStandard;
	
	private HashMap<String,String> correctedNonStandard = new HashMap<String,String>();
	
	private List<String> formDict;
		
	/**
	 * 
	 */
	public MicroTextNormalizer (String lang)
	{
		this.language = lang;
		setEmodict(new HashMap<String,String>());
		setNonStandard(this.getClass().getClassLoader().getResourceAsStream(language+"/OOV.txt"));
		setFormDict(this.getClass().getClassLoader().getResourceAsStream(language+"/formDict.txt"));
	}
		
	/**
	 * @param emodictFile
	 */
	public MicroTextNormalizer(String lang, InputStream emodictFile) {
		this(lang);
		setEmodict(emodictFile);		
	}
	
	
	/**
	 * @param nonStandard
	 */
	public void setLanguage(String lang) {
		this.language = lang;
	}
	
	
	/**
	 * @return
	 */
	public HashMap<String, String> getEmodict() {
		return emodict;
	}

	/**
	 * @param emodict
	 */
	public void setEmodict(HashMap<String, String> emodict) {
		this.emodict = emodict;
	}
	
	/**
	 * @param emodictFile
	 */
	public void setEmodict(InputStream emodictFile) {
		try{
			this.emodict = FileUtilsElh.loadTwoColumnResource(emodictFile);
		}catch(IOException ioe){
			System.err.println("MicroTextNormalizer::Constructor - Emoticon dictionary file could not be read. Emoticon normalization won't be performed.");
			setEmodict(new HashMap<String, String>());
		}
		System.err.println("MicroTextNormalizer::setEmodict - Emoticon dictionary file loaded: "+emodict.size());
	}

	/**
	 * @return
	 */
	public HashMap<String, String> getNonStandard() {
		return nonStandard;
	}

	/**
	 * @param nonStandard
	 */
	public void setNonStandard(HashMap<String, String> nonStandard) {
		this.nonStandard = nonStandard;
	}
	
	/**
	 * @param emodictFile
	 */
	public void setNonStandard(InputStream OOVDictFile) {
		try{
			this.nonStandard = FileUtilsElh.loadTwoColumnResource(OOVDictFile);
		}catch (IOException ioe){
			System.err.println("MicroTextNormalizer::Constructor - OOV dictionary file could not be read. Emoticon normalization won't be performed.");
			setNonStandard(new HashMap<String, String>());
		}
		System.err.println("MicroTextNormalizer::setNonStandard - OOV dictionary file loaded: "+nonStandard.size());
	}
	
	
	/**
	 * @param forms
	 */
	private void setFormDict(InputStream forms) {
		formDict = new ArrayList<String>();
		try{
			BufferedReader breader = new BufferedReader(new InputStreamReader(forms));
			String line;
			while ((line = breader.readLine()) != null) 
			{
				if (line.startsWith("#") || line.matches("^\\s*$"))
				{
					continue;
				}
				line=line.trim();
				formDict.add(line);			
			}
		}catch (IOException ioe){
			System.err.println("MicroTextNormalizer::setFormDict - Form dictionary file could not be read. Repeated character normalization won't be performed.");
			formDict = new ArrayList<String>();
		}
		System.err.println("MicroTextNormalizer::setFormDict - Form dictionary file loaded: "+formDict.size());
	}
	
	
	
	/**
	 * @param input
	 * @param url
	 * @param user
	 * @param hashtag
	 * @return
	 */
	public String normalizeSentence (String input, boolean url, boolean user, boolean hashtag, boolean emot, boolean nonStandard)
	{
		//System.out.println("MicroTextNormalization::normalizeSentence - input: "+input);
		String out = "";
		//separate words attached to hashtags and usernames (e.g., hello#world -> hello #world)
		//String in = input.replaceAll("([^\\s])([@#][\\p{L}\\p{M}\\p{Nd}])", "$1 $2");
		String in = attachedWords.matcher(input).replaceAll("$1 $2");
		if (emot && !getEmodict().isEmpty())
		{
			in = emoticonMapping(in);
		}
		
		for (String s: in.split("\\s+"))
		{
			String token = s;
			if (nonStandard)
			{
				token = correctNonStandardWord(s);
			}
			
			if (url)
			{
				token = normalizeURL(token);				
			}
			if (user)
			{
				token = normalizeUSR(token,true);
			}
			if (hashtag)
			{
				if (token.startsWith("#"))
				{
					token = hashtagNormalization(token,true);					
				}
			} 			
			out+=token+" ";
		}
		out = out.trim().replaceAll("\\s+", " ");
		// Debugging messages.
		//if (input.compareTo(out)!=0)
		//{
		//	System.out.println("MicroTextNormalization::normalizeSentence - input: "+input+" - out: "+out+"-");
		//}
		return out;
		
		
	}
	
	/**
	 *   
	 * @param input : input string to normalize
	 * @returns decodes the emoticons in the given string and maps them to the following schema:
	 * 
	 * SMILEYEMOT - emoticons matching smiley faces
	 * CRYEMOT - emoticons matching smiley faces
	 * SHOCKEMOT - emoticons matching shocking faces
	 * MUTEEMOT - emoticons matching mute faces
	 * ANGRYEMOT - emoticons matching angry faces
	 * KISSEMOT - emoticons matching kisses
	 * SADEMOT - emoticons matching sad faces
	 * 
	 * IMPORTANT NOTE: it is up to the user to include the elements of this schema in the polarity lexicon, 
	 *                 and assign them polarities.
	 *   
	 * 
	 */
	private String emoticonMapping(String input) {
		String result = input;
		
		for (String k: emodict.keySet())
		{				
			String kk=k;//k.replaceAll("\\\\", "\\\\\\\\");
			result=result.replaceAll(kk," . "+emodict.get(k)+" . ");			
		}		
		return result;
	}

	/**
	 *  Normalize input String (urls -> URL) 
	 * 
	 * @param input : input string to normalize
	 * @returns String
	 */
	private static String normalizeURL (String input){			
		
		//URL normalization
		UrlValidator defaultValidator = new UrlValidator(UrlValidator.ALLOW_ALL_SCHEMES);
		
		if (defaultValidator.isValid(input)) 
		{
	          return "URLURLURL"; // valid
		}			
		else
		{
			return input;
		}
	}
	
	/**
	 *  Normalize input String (@user -> USRID) 
	 * 
	 * @param input : input string to normalize
	 * @returns String
	 */
	private String normalizeUSR (String input, boolean anonimize){			
		
		String result = input;
		Matcher m = user.matcher(result);
		if (anonimize)
		{
			result = m.replaceAll("USRID");
		}
		else
		{
			result = m.replaceAll("$1");
		}
		return result;
		
	}
	
	/**
	 *  Try to find the standard form of a given input word 
	 * 
	 * @param input : input string to normalize (a single word)
	 * @returns String
	 */
	private String correctNonStandardWord(String input)
	{
		String variations = input;
		String prefix = "";
		String suffix = "";
		
		// Since we use a whitespace based tokenizer, input could have attached symbols, such as quotes 
		// or punctuation marks (e.g., great!, "nice"), Here separate and store those symbols to later restore 
		// them (we want the text as close to the original as posible, only with non-standard words normalized).		
		//Pattern affixes = Pattern.compile("^([^\\p{L}\\p{M}\\p{Nd}]*)([\\p{L}\\p{M}\\p{Nd}]+)([^\\p{L}\\p{M}\\p{Nd}]*)$"); 
		Matcher m = affixes.matcher(input);
		if (m.matches())
		{
			prefix = m.group(1);
			variations = m.group(2);
			suffix = m.group(3);
		}		
		//System.err.println("MicroTextNormalization::correctNonStandardWords - "+input+" - "+variations+" - "+prefix+"("+prefix.length()+") - "+suffix+"("+suffix.length()+")");

		//if word is in the dictionary return it as it is but only for words with a minimum length (2).
		if ((variations.length()>1) && (formDict.contains(variations)|| formDict.contains(variations.toLowerCase())))
		{
			return input;
		}
			
		// OOV dictionary matching. (e.g. xo -> pero; q -> que)		
		
		//OOV dictionary matching if we already have the correct form it should be stored in the correctedNonStandard hashmap.
		if (nonStandard.containsKey(variations))
		{
			variations = nonStandard.get(variations);
		}
		// Do not correct acronyms such as PP or WWW or CCAA CCOO (only one and two letter acronyms are treated)
		//variations.matches("^([\\p{L}\\p{M}])\\1{1,2}(([\\p{L}\\p{M}])\\3{1,2})?$")
		else
		{
			Matcher acro =  allowedAcronyms.matcher(variations);
			if (acro.matches())
			{
				nonStandard.put(variations, variations);
			}
		//try to find the standard form of the word. Character repetition (e.g. feoooo -> feo; cooooool -> cool)
			else
			{
				String repetitions = removeRepetitions(variations,2);
				// we only try to apply the corrected form if it has a minimum length (it is very difficult to know
				// when a single letter is the correct form of a repetition)   
				if (repetitions.length() > 1)
				{
					// WARNING lowercase conversion is locale dependent
					if (formDict.contains(repetitions) || formDict.contains(repetitions.toLowerCase()))
					{
						nonStandard.put(variations, repetitions);
						variations = repetitions;
					}
					else
					{				
						repetitions = removeRepetitions(variations,1);
						if (formDict.contains(repetitions)|| formDict.contains(repetitions.toLowerCase()))
						{	
							nonStandard.put(variations, repetitions);
							variations = repetitions;
						}
						else
						{
							nonStandard.put(variations, variations);
						}
					}
				}
				else
				{
					nonStandard.put(variations, variations);
				}
				///System.err.println(input+" - "+variations+" - "+prefix+variations+suffix);		
			}
		}
		String wNorm = prefix+variations+suffix;
		
		//onomatomeiak pos
		wNorm = jajeji.matcher(wNorm).replaceAll(" . INTERPOS .");
		wNorm = hahehi.matcher(wNorm).replaceAll(" . INTERPOS .");//hahaha but no ha (ha salido)
		wNorm = hjuas.matcher(wNorm).replaceAll(" . INTERPOS .");
		wNorm = lol.matcher(wNorm).replaceAll(" . INTERPOS .");
		wNorm = hojo.matcher(wNorm).replaceAll(" . INTERPOS .");
		wNorm = buajaha.matcher(wNorm).replaceAll(" . INTERPOS .");
		wNorm = muacks.matcher(wNorm).replaceAll(" . INTERPOS .");
	
		//onomatomeiak neg    
		wNorm = puff.matcher(wNorm).replaceAll(" . INTERNEG .");//INTERNEG
		wNorm = uf.matcher(wNorm).replaceAll(" . INTERNEG .");//INTERNEG
	        	    			
		return wNorm;
	}
	
	/**
	 *  Try to find the standard form of a given input String 
	 * 
	 * @param input : input string to normalize (a single word)
	 * @returns String
	 */
	@Deprecated
	private String correctNonStandardWords(String input)
	{
		String variations = input;
		String prefix = "";
		String suffix = "";
		
		// Since we use a whitespace based tokenizer, input could have attached symbols, such as quotes 
		// or punctuation marks (e.g., great!, "nice"), Here separate and store those symbols to later restore 
		// them (we want the text as close to the original as posible, only with non-standard words normalized).		
		Pattern affixes = Pattern.compile("^([^\\p{L}\\p{M}\\p{Nd}]*)([\\p{L}\\p{M}\\p{Nd}]+)([^\\p{L}\\p{M}\\p{Nd}]*)$"); 
		Matcher m = affixes.matcher(input);
		if (m.matches())
		{
			prefix = m.group(1);
			variations = m.group(2);
			suffix = m.group(3);
		}		
		//System.err.println("MicroTextNormalization::correctNonStandardWords - "+input+" - "+variations+" - "+prefix+"("+prefix.length()+") - "+suffix+"("+suffix.length()+")");

		//if word is in the dictionary return it as it is but only for words with a minimum length (2).
		if ((variations.length()>1) && (formDict.contains(variations)|| formDict.contains(variations.toLowerCase())))
		{
			return input;
		}
			
		// OOV dictionary matching. (e.g. xo -> pero; q -> que)
		for (String k: nonStandard.keySet())
		{				
			//there are 1-n standarizations, where word boundaries are marked by '_' chars. 
			// for the moment convert them to separate words - 2015/07/24
			String replacement = nonStandard.get(k).replaceAll("_", " ");			
			String kk = k.replaceAll("\\?", "\\\\\\\\?");
			variations=variations.replaceAll("\\b"+kk+"\\b",replacement);			
		}		
		
		//Character repetition (e.g. feoooo -> feo; cooooool -> cool)
		//if we already corrected it should be stored in the correctedNonStandard hashmap.
		if (correctedNonStandard.containsKey(variations))
		{
			variations = correctedNonStandard.get(variations);
		}
		// Do not correct acronyms such as PP or WWW or CCAA CCOOO (only one and two letter acronyms are treated)
		else if (variations.matches("^([\\p{L}\\p{M}])\\1{1,2}(([\\p{L}\\p{M}])\\1{1,2})?$"))
		{
			correctedNonStandard.put(variations, variations);
		}
		//try to find the standard form of the word.
		else
		{
			String repetitions = removeRepetitions(variations,2);
			// we only try to apply the corrected form if it has a minimum length (it is very difficult to know
			// when a single letter is the correct form of a repetition)   
			if (repetitions.length() > 1)
			{
				// WARNING lowercase conversion is locale dependent
				if (formDict.contains(repetitions) || formDict.contains(repetitions.toLowerCase()))
				{
					correctedNonStandard.put(variations, repetitions);
					variations = repetitions;
				}
				else
				{				
					repetitions = removeRepetitions(variations,1);
					if (formDict.contains(repetitions)|| formDict.contains(repetitions.toLowerCase()))
					{	
						correctedNonStandard.put(variations, repetitions);
						variations = repetitions;
					}
					else
					{
						correctedNonStandard.put(variations, variations);
					}
				}
			}
			else
			{
				correctedNonStandard.put(variations, variations);
			}
			///System.err.println(input+" - "+variations+" - "+prefix+variations+suffix);		
		}
		String wNorm = prefix+variations+suffix;
		
		
		//onomatomeiak pos
		wNorm = jajeji.matcher(wNorm).replaceAll(" . INTERPOS .");
		wNorm = hahehi.matcher(wNorm).replaceAll(" . INTERPOS .");//hahaha but no ha (ha salido)
		wNorm = hjuas.matcher(wNorm).replaceAll(" . INTERPOS .");
		wNorm = lol.matcher(wNorm).replaceAll(" . INTERPOS .");
		wNorm = hojo.matcher(wNorm).replaceAll(" . INTERPOS .");
		wNorm = buajaha.matcher(wNorm).replaceAll(" . INTERPOS .");
		wNorm = muacks.matcher(wNorm).replaceAll(" . INTERPOS .");
	
		//onomatomeiak neg    
		wNorm = puff.matcher(wNorm).replaceAll(" . INTERNEG .");//INTERNEG
		wNorm = uf.matcher(wNorm).replaceAll(" . INTERNEG .");//INTERNEG
	        	    			
		return wNorm;
	}
	

	/**
	 *  hashtag division (#NewYorkMarathon -> New York Marathon, #Donostia2016 -> Donostia 2016) 
	 * 
	 * @param input : input string to normalize
	 * @returns String
	 */
	private String hashtagNormalization (String input, boolean divide)
	{					
			String result = input;
			//erase the leading #char
			//result.replaceAll("#([a-zA-Z0-9-]+)", "$1");		
			result = hashtag.matcher(result).replaceAll("$1");
			//perform division 
			if (divide)
			{
				//word-number heuristics (donostia2016 | windows8 | 38Congreso | 2016Conference21)
				result = multiWordhashtag.matcher(result).replaceAll("$1 $2 $3");
				
				//UpperCase					
				result = multiUpper.matcher(result).replaceAll("$1 ");
			}			
			return result;
	}

	
	/**
	 * @param variations
	 * @param i
	 * @return
	 */
	private String removeRepetitions(String variations, int i) {
		String result=variations;	
		switch (i)
		{
		case 0: result = variations.replaceAll("([\\p{L}\\p{M}])\\1{"+i+",}", ""); break;
		case 1: result = variations.replaceAll("([\\p{L}\\p{M}])\\1{"+i+",}", "$1"); break;
		case 2: result = variations.replaceAll("([\\p{L}\\p{M}])\\1{"+i+",}", "$1$1"); break;
		default: 
			System.err.println("MicroTextNormalization::removeRepetitions - i="+i+" only a max of two repetitions per char are allowed, function will default to i=2");
			result = removeRepetitions(variations, 2);
		}
		return result;
	}
	
}
