package elh.eus.absa;

import java.util.HashMap;

public class TfIdf {
	
	//structures to compute tf and idf values. Same structures are used for word forms and lemmas.
	private HashMap<String, HashMap<String,Integer>> tfMatrix;
	private HashMap<String, Integer> dfMatrix;
		
	TfIdf(){
		tfMatrix = new HashMap<String, HashMap<String,Integer>>();
		dfMatrix = new HashMap<String,Integer>();		
	}
	
	
	/**
	 *  Simple function to update df matrix
	 *  
	 * @param matrix
	 * @param term
	 */
	public void updateDf(String term)
	{
		if (!dfMatrix.containsKey(term))
		{
			dfMatrix.put(term, 1);
		}
		else
		{
			Integer updateStat = dfMatrix.get(term)+ 1;
			dfMatrix.put(term, updateStat);
		}
	}
	
	/**
	 *  Simple function to update Tf matrix
	 *  
	 * @param matrix
	 * @param term
	 */
	public void updateTf(String doc, String term)
	{
		if (!tfMatrix.containsKey(doc)){
			HashMap<String,Integer> tf = new HashMap<String,Integer>();
			tf.put(term, 0);
			tfMatrix.put(doc, tf);
		}
		if (!tfMatrix.get(doc).containsKey(term))
		{
			tfMatrix.get(doc).put(term, 0);			
		}
		
		Integer updateStat = tfMatrix.get(doc).get(term)+ 1;
		tfMatrix.get(doc).put(term, updateStat);
		
	}
	
	/**
	 * @param term
	 * @return
	 */
	public Integer getDf(String term){
		return dfMatrix.getOrDefault(term, 0);
	}
	
	/**
	 * @param doc
	 * @param term
	 * @return
	 */
	public Integer getTf(String doc, String term){
		if (!tfMatrix.containsKey(doc))
		{
			return 0;
		}
		else
		{
			return tfMatrix.get(doc).getOrDefault(term, 0);			
		}

	}
	
	/**
	 *  Function to compute tf-idf statitstic. The number of documents in the collection is retrieved
	 *  from the number of keys in tfMatrix object.
	 * 
	 * @param term
	 * @return
	 */
	public float computeTfIdf(String term){
		float result= 0;
		Integer docCount=tfMatrix.keySet().size();
		
		return result;
	}
	
}
