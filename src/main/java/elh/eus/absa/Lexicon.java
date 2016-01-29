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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author isanvi
 * 
 * Lexicon class: lexicons have a two score (pos|neg) representation. 
 * 
 * For each entry, a positive and a negative polarity score is stored. 
 * 
 * Lexicons with a single score representation are supported, and they will be mapped as follows:
 *      lets as say we have the entry "good \t positive". In our lexicon this will be represented as:  
 *      "lemma \t pos_score \t neg_score"   
 *      "good \t 1.0 \t 0.0"
 * 
 * Lexicons may also contain intensifiers, weakeners or modifiers (e.g., very, not,...). Apart from the two 
 * score numeric representation, lexicon class also stores a scalar representation of the lexicon, with the
 * following values: 
 * 
 *  -1 -> neg ; 0 -> neu ; 1 -> pos ; 2 -> intensifier ; 3 -> weakener ; 4 -> modifier
 *
 */
public class Lexicon {

	private Map<String, Polarity> lexicon = new HashMap<String, Polarity>();
	private int formaterror;
	private float minAbsPolarity;
	
	public class Polarity {
		//private String scalar;
		private int scalar;
		private float numeric;
		//numeric polarities: positive and negative numeric polarity scores. 
		private float pos_numeric;
		private float neg_numeric;
		
		
		/**
		 * Constructor, both numeric and scalar polarities are provided. 
		 * Single score representation (only one numeric polarity is provided)
		 * 
		 * @param float pol : polarity score
		 * @param int pols : scalar polarity. Scalar polarity value is (-1|0|1|2|3|4).
		 * 						2=intensifier
		 * 						3=weakener
		 * 						4=shifter
		 * 
		 */
		public Polarity(float pol, int pols){			
			numeric = pol;
			pos_numeric = 0;
			neg_numeric = 0;
			if (numeric > 0)
			{
				pos_numeric = pol;
			}
			else if (numeric < 0)
			{
				neg_numeric = Math.abs(pol);
			}
			
			if ((pols < -1) && (pols > 4))
			{
				System.err.println("scalar value is not a valid polarity, scalar polarity will be induced from numeric polarity\n");
				scalar = 0;
				if (numeric > 0)
				{
					scalar = 1;
				}
				else if (numeric < 0)
				{
					scalar = -1;
				}
			}
			else
			{
				scalar=pols;							
			}
			
		}	

		/**
		 * Constructor, both numeric and scalar polarities are provided. 
		 * Two score representation (both positive and negative numeric polarities are provided) 
		 * 
		 * @param float pos : positive polarity weight
		 * @param float neg : negative polarity weight
		 * @param int pols : scalar polarity. Scalar polarity value is (-1|0|1|2|3|4).
		 * 						2=intensifier
		 * 						3=weakener
		 * 						4=shifter
		 * 
		 */
		public Polarity(float pos, float neg, int pols){
			pos_numeric = pos;
			neg_numeric = neg;
			//single score representation is also filled
			numeric = pos-neg;

			if ((pols < -1) && (pols > 4))
			{
				System.err.println("scalar value is not a valid polarity, scalar polarity will be induced from numeric polarity\n");
				scalar = 0;
				if (numeric > 0)
				{
					scalar = 1;
				}
				else if (numeric < 0)
				{
					scalar = -1;
				}
			}
			else
			{
				scalar=pols;							
			}
		}	

		
		/**
		 * Constructor, only scalar polarity provided. 
		 * Numeric polarities are derived from the scalar value.
		 * 
		 * @param String pol : scalar polarity value (pos|neg|neu|int|wea|shi)
		 */
		public Polarity(String pol){
			pol.replaceFirst("(?i)polarityshifter", "shifter");
			String value = pol.substring(0,3).toLowerCase();
			switch (value)
			{
			case "neg": numeric = scalar = -1; pos_numeric = 0; neg_numeric = 1; break;
			case "pos": numeric = pos_numeric = scalar = 1; neg_numeric =0; break;
			case "neu": numeric = pos_numeric = neg_numeric = scalar = 0; break;
			case "int": scalar = 2; numeric = pos_numeric= neg_numeric=0; break; //intensifiers|weakeners|shifters must have numeric polarity 0
			case "wea": scalar = 3; numeric = pos_numeric= neg_numeric=0; break; 
			case "shi": scalar = 4; numeric = pos_numeric= neg_numeric=0; break;
			default: System.err.println("scalar value is not a valid polarity\n"); scalar = 123456789; break; 
			}
		}	


		/**
		 * Constructor, only scalar polarity provided (-1|0|1|2|3|4). Numeric polarity derived from scalar value.
		 * 
		 * @param int pols : scalar polarity value (-1|0|1|2|3|4) (equals to: (pos|neg|neu|int|wea|shi))
		 */
		public Polarity(int pols){
						
			switch (scalar)
			{
			case -1: scalar=pols; numeric = neg_numeric = -1; pos_numeric = 0; break;
			case 1: scalar=pols; numeric = pos_numeric = 1; neg_numeric = 0; break;
			//neutral (0) and int(2)|wea(3)|shi(4) have 0.0 numeric scores.
			case 0: case 2: case 3: case 4: scalar=pols; numeric = pos_numeric = neg_numeric = 0; break;
			default: System.err.println("Scalar value is not a valid polarity\n"); scalar = 123456789; break;
			}
		}	
		
		/**
		 * Constructor, only numeric polarity provided. 
		 * Single score representation (only one numeric polarity is provided)
		 * 
		 * Scalar polarity is derived from the numeric value.
		 * 
		 * @param float pol : numeric polarity value
		 * 
		 */
		public Polarity(float pol){
			// call the first constructor with and invalid scalar polarity, it will take care of the rest. 
			this(pol,123456789);
		}	

		/**
		 * Constructor, only numeric polarity provided. 
		 * Two score representation (both positive and negative numeric polarities are provided)
		 *  
		 * Scalar polarity is derived from the numeric value.
		 * 
		 * @param float pos : positive polarity weight
		 * @param float neg : negative polarity weight
		 * 
		 */
		public Polarity(float pos, float neg){
			// call the first constructor with and invalid scalar polarity, it will take care of the rest. 
			this(pos,neg,123456789);
		}	

		
		
		/**
		 * Constructor, both numeric and scalar polarities are provided.
		 * Single score representation (only one numeric polarity is provided)
		 * 
		 * @param float pol : polarity score 
		 * @param String pols : scalar polarity (pos|neu|neg|int|wea|shi) 
		 */
		public Polarity(float pol, String pols){
			this(pols);
			// update numeric polarities
			if (pol > 0)
			{
				numeric = pos_numeric = pol;
			}
			else if (pol < 0)
			{
				numeric = neg_numeric = Math.abs(pol);				
			}
			
			// if scalar has an invalid value derive it from numeric values
			if (scalar == 123456789) 
			{
				System.err.println("scalar value is not a valid polarity, scalar polarity will be induced from numeric polarity\n");
				scalar = 0;
				if (numeric > 0)
				{
					scalar = 1;
				}
				else if (numeric < 0)
				{
					scalar = -1;
				}
			}

			
		}	
	
		
		/**
		 * Constructor, both numeric and scalar polarities are provided. 
		 * Two score representation (both positive and negative numeric polarities are provided)
		 * 
		 * @param float pol : polarity score 
		 * @param String pols : scalar polarity (pos|neu|neg|int|wea|shi) 
		 */
		public Polarity(float pos, float neg, String pols){
			this(pols);
			// update numeric polarities
			pos_numeric = pos;
			neg_numeric = neg;
			//single score representation is also filled
			numeric = pos-neg;
			
			// if scalar has an invalid value derive it from numeric values
			if (scalar == 123456789) 
			{
				System.err.println("scalar value is not a valid polarity, scalar polarity will be induced from numeric polarity\n");
				scalar = 0;
				if (numeric > 0)
				{
					scalar = 1;
				}
				else if (numeric < 0)
				{
					scalar = -1;
				}
			}			
		}	
	
		
		
		public float getNumeric(){
			return numeric;			
		}	
				
		
		public int getScalar(){
			return scalar;			
		}	
		
		public float getPositiveScore(){
			return pos_numeric;			
		}	

		public float getNegativeScore(){
			return neg_numeric;			
		}	
		
	} // end for polarity class
	
	/*
	 * constructor requires a path to the file containing the lexicon and 
	 */
	public Lexicon(File fname, String syn)
	{
		this(fname, syn, 0);
	}
	
	/*
	 * constructor requires a path to the file containing the lexicon and 
	 */
	public Lexicon(File fname, String syn, float minEntryPolarity)
	{
		try {
			this.formaterror=0;
			this.setMinAbsPolarity(minEntryPolarity);
			loadLexicon(fname, syn);
		} catch (IOException e) {
			System.err.println("Lexicon class: error when loading lexicon from file: "+fname);
			e.printStackTrace();
		}
	}
	
	/*
	 * Set minAbsPolarity variable. It determines the minimum absolute polarity score an entry shall have
	 * to accept it in the lexicon. (this is only for evaluation purposes.)
	 */
	private void setMinAbsPolarity (float m)
	{
		this.minAbsPolarity = m;
	}
	
	/**
	 * load a lexicon from a file given the file path. The format of the lexicon must be as follows:
	 * 
	 *  "offset<tab>(pos|neg|neu|int|wea|shi)<tab>lemma1, lemma2, lemma3, ...<tab>score<tab>..."	
	 * 
	 * 	First two columns are mandatory. Alternatively, first column can contain lemmas instead of offsets.
	 * 
	 * @param LexiconPath
	 * @param syn
	 * @throws IOException
	 */
	private void loadLexicon(File LexiconPath, String syn) throws IOException
	{
		BufferedReader lexreader = new BufferedReader(new FileReader(LexiconPath));   		
		String line;
		while ((line = lexreader.readLine()) != null) 
		{
			if (formaterror > 10)
			{
				System.err.println("Lexicon class ERROR: too many format errors. Check that the specified lemma/sense is compatile with the format of the lexicon"
						+ "pass a valid lexicon or select the correct lemma/sense option\n");
				System.exit(formaterror);
			}
			
			if (line.startsWith("#") || line.matches("^\\s*$"))
			{
				continue;
			}
			String[] fields = line.split("\t");
			int ok = 0;
			//LexiconEntry entry = null;
			switch (fields.length)
			{
			// not enough info, too few columns
			case 0: case 1:
				break;
			// Only offset/lemma and polarity info
			case 2: 				
				ok = addEntry(fields[0], fields[1], syn);
				//entry = new LexiconEntry(fields[0], fields[1], score);
				break;
			case 3:
				//third column contains polarity score
				try {
					float score = Float.parseFloat(fields[2]);
					ok = addEntry(fields[0],fields[1],score,syn);
				}
				//third column contains lemmas, no polarity scores
				catch (NumberFormatException ne)
				{
					String[] lemmas = fields[2].split(", ");
					for (String l : lemmas)
					{
						l = l.replaceFirst("#[0-9]+$","");
						ok = addEntry(l,fields[1], syn);			
					}
				}
				break;
			// if the lexicon contains more than three columns is should have a standard format:
			// "offset<tab>(pos|neg|neu)<tab>lemma1, lemma2, lemma3, ...<tab>score<tab>..."	
			default:
				//third column contains lemmas, fourth column has polarity score
				if (syn.matches("(first|rank|mfs)"))
				{
					ok = addEntry(fields[0],fields[1],Float.parseFloat(fields[3]), syn);					
				}
				else
				{
					String[] lemmas = fields[2].split(", ");
					for (String l : lemmas)
					{
						l = l.replaceFirst("#[0-9]+$","");
						ok = addEntry(l,fields[1],Float.parseFloat(fields[3]), syn);						
					}
				}
				//entry = new LexiconEntry(fields[0], fields[1], score, fields[2]);
				break;
			}
			//this.lexicon.add(entry);
			ok++;
		}
		lexreader.close();			
	}
	
	/**
	 * Add entry to lexicon. If the key already exists it replaces the polarity with the new value.
	 *
	 * @param key : lemma or offset
	 * @param pol : scalar polarity (pos|neg|neu|mod|shi)
	 * @param syn : whether lemmas or senses (offset) should be stored in the lexicon.
	 * @return : int. 0 success, 1 error, 2 ok, but entry not included (no offset or lemma, or neutral polarity).
	 */
	private int addEntry (String key, String pol, String syn)
	{
		float score = 0; 
		//scalar polarity (pos| neg| neu)
		if (pol.length() < 3)
		{ 
			return 1;
		}
		pol = pol.substring(0,3);			

		switch (pol)
		{
		case "neg": score = -1; break;
		case "pos": score = 1; break;
		case "neu": score = 0; break;
		case "int": score = 2; break;
		case "wea": score = 3; break;
		case "shi": score = 4; break;
		default: this.formaterror++; return 1;
		}

		return addEntry(key, pol, score, syn); 
	}
	
	
	/**
	 * Add entry to lexicon. If the key already exists it replaces the polarity with the new value.
	 * 
	 * @param key (String): lemma or synset to add to the lexicon.
	 * @param pol String (pos|neg|neu|mod|shi): polarity of the new entry 
	 * @param scoreValue String: polarity score
	 * @param syn String (lemma|first|mfs|rank): type of the entry (first, mfs and rank) are represented by WN synsets 

	 * @return int:
	 * 				- 0 = success
	 * 				- 1 = format error expected lemma and synset given, or viceversa, or polarity value is not valid.
	 * 				- 2 = entry not added (because synset is unknown or lemma is unavailable)
	 */
	private int addEntry (String key, String pol, float scoreValue, String syn)
	{		
		float currentNumeric = 0;
		int currentScalar = 0;
		if (this.lexicon.containsKey(key))
		{
			currentScalar = lexicon.get(key).getScalar();
			currentNumeric = lexicon.get(key).getNumeric();
			//System.err.println(key+" - "+currentScalar+" - "+currentNumeric);
		}
		
		// control that lemma/sense in the lexicon is coherent with the lemma/sense mode selected by the user
		if ((key.matches("^[0-9]{4,}-[arsvn]$") && syn.compareTo("lemma") == 0) || (!key.matches("^[0-9]{4,}-[arsvn]$") && syn.matches("(first|rank|mfs)")))
		{
			// u-00000 is the code used in a lexicon if no synset is found for a lemma. 
			// If found such an element the entry is ok, just ignore it (we are in synset mode)  
			if (key.matches("^u-[0]{4,}$"))
			{
				return 2;
			}
			// If format unknown there is a format error 
			else
			{
				this.formaterror++;
				return 1;
			}
		}
		
		//If lemma value is "Not available" it means there is no lemma for the current entry.
		if ((syn.compareTo("lemma") == 0)  && (key.matches("Not (Available|in Dictionary)")) )
		{
			//System.err.println(key+"- lemma not available.\n");
			return 2;
		}

		float numericScore=currentNumeric;
		int scalarScore = currentScalar;
		//numeric polarity 
		/*try {
			float score = Float.parseFloat(scoreValue);
			// if the entry does not reach the minimum required value do not include it in the lexicon.
			if (Math.abs(score) < this.minAbsPolarity)
			{
				//System.err.println("added to lexicon: - "+Math.abs(score)+" - "+this.minAbsPolarity);				
				return 2;			
			}
			numericScore =  numericScore+score;
			
			if (score < 0)
			{
				scalarScore = -1+currentScalar;	//"neg"
			}
			else if (score > 0)
			{
				scalarScore = 1+currentScalar; //"pos"
			}
						
			//Polarity polar = new Polarity(currentPol+score);
			//this.lexicon.put(key, polar);		
		}
		catch (NumberFormatException ne)
		{*/
			//scalar polarity (pos|neg|neu|int|wea|shi)
			if (pol.length() < 3)
			{ 
				return 1;
			}
			pol = pol.substring(0,3);			
			switch (pol)
			{
			case "neg":	scalarScore= -1+currentScalar; numericScore=scalarScore; break;
			case "pos": scalarScore= 1+currentScalar; numericScore=scalarScore; break;
			case "neu": numericScore=scalarScore; break;
			case "int": scalarScore = 2; numericScore=0; break; //intensifier, weak and shifters must have numeric polarity 0
			case "wea": scalarScore = 3; numericScore=0; break; 
			case "shi": scalarScore = 4; numericScore=0; break;
			default: return 1;
			}
		//}	
				
		// add/update entry in the lexicon.
		Polarity polar = new Polarity(numericScore, scalarScore);
		this.lexicon.put(key, polar);
		
		//System.err.println("added to lexicon: - "+key+" - "+numericScore+" - "+scalarScore);

		return 0;
	}
	
	/**
	 *  Function returns an int containing the scalar polarity the given entry has in the lexicon.
	 *  Scalar polarity values:
	 * 
	 * 	-1 -> neg ; 0 -> neu ; 1 -> pos ; 2 -> intensifier ; 3 -> weakener ; 4 -> modifier 
	 *  
	 *  Note that 
	 *  If the given entry is not in the lexicon the '123456789' value is returned
	 * 
	 * @param String entrykey 
	 * @return the numeric polarity for the given entry ([-1,0,1] score). 
	 */
	public int getScalarPolarity (String entrykey)
	{
		if (this.lexicon.containsKey(entrykey))
		{
			int result = lexicon.get(entrykey).getScalar();
			//scalar polarity is normalized to -1|0|1 values
			if (result > 0)
			{
				return result;
			}
			else if (result < 0)
			{
				return -1;
			}
			else
			{
				return 0;
			}
		}
		else
		{
			return 123456789;
		}	
	}

	/**
	 *  Function returns a float containing the numeric polarity ([-1,0,1] score)
	 *  the given entry has in the lexicon. 
	 *  
	 *  If the given entry is not in the lexicon the '123456789' value is returned
	 * 
	 * @param String entrykey 
	 * @return the numeric polarity for the given entry ([-1,0,1] score). 
	 */
	public float getNumericPolarity (String entrykey)
	{
		if (this.lexicon.containsKey(entrykey))
		{
			return lexicon.get(entrykey).getNumeric();
		}
		else
		{
			return (float) 123456789;
		}	
	}

	/**
	 *  Function returns if the given entry exists in the lexicon.
	 * 
	 * @param entrykey
	 * @return
	 */
	public boolean isInLexicon(String entrykey)
	{
		return this.lexicon.containsKey(entrykey);
	}
	
	/**
	 * Returns the Polarity element for a given entry in the lexicon.
	 * If the given entry is not found null is returned.
	 * 
	 * @param entryKey
	 * @return
	 */
	public Polarity getPolarity(String entryKey)
	{
		if (this.lexicon.containsKey(entryKey))
		{
			return lexicon.get(entryKey);
		}
		else
		{
			return null;
		}
	}
	
	/**
	 * Return a all entries in the lexicon
	 * @return
	 */
	public Set<String> getEntrySet()
	{
		return lexicon.keySet();
	}
	
	/**
	 *  Return the number of entries in the lexicon
	 * 
	 * @return
	 */
	public int size()
	{
		return lexicon.size();
	}
	
	/**
	 * Void to print the lexicon to the stdout. Scalar polarities are printed (p|n|neu)
	 * 
	 * 
	 */
	public void printLexicon()
	{		
		System.out.println("# Lexicon printing - class legend: \n"
				+ "# -1 -> neg ; 0 -> neu ; 1 -> pos ; 2 -> intensifier ; 3 -> weakener ; 4 -> modifier .\n");
		for (String s : lexicon.keySet())
		{
			System.out.println(s+" - "+lexicon.get(s).getScalar());
		}
	}
	
}
