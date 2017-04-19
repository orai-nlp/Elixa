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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AddClassification;
import weka.filters.unsupervised.attribute.Remove;

public class WekaWrapper {
	private Instances traindata;
	private Instances testdata;
	private Classifier MLclass;

	private static final String modelDir = "elixa-models";
	private static final Properties defaultModels = new Properties();
	static {
		try {
		    //System.err.println(modelDir+File.separator+"morph-models-1.5.0.txt"); 
			defaultModels.load(WekaWrapper.class.getClassLoader().getResourceAsStream(modelDir+File.separator+"elixa-models.txt"));
		} catch (Exception e) {
			System.err.println("WARNING: No default polarity models found. EliXa will only be able to tag polarity with user especified models");
			//e.printStackTrace();
		}
		
	}
	
	/**
	 * @param model path (InputStream containing the serialized model)
	 * WARNING: the input stream is closed by WEKA after reading it. 
	 * 
	 * @throws Exception
	 */
	public WekaWrapper (String modelPath, String lang) throws Exception
	{		
		setMLclass(loadModel(getModelResource(modelPath, lang, "twt")));
	}
	
	/**
	 * @param traindata
	 * @param id : whether the first attribute represents the instance id and should be filtered out for classifying
	 * @throws Exception
	 */
	public WekaWrapper (Instances traindata, boolean id) throws Exception
	{
		this(traindata, null, id);
	}
	
	/**
	 * @param traindata
	 * @param testdata
	 * @param id : whether the first attribute represents de instance id and should be filtered out for classifying
	 * @throws Exception
	 */
	public WekaWrapper (Instances traindata, Instances testdata, boolean id) throws Exception{

		// classifier
		weka.classifiers.functions.SMO SVM = new weka.classifiers.functions.SMO();
		SVM.setOptions(weka.core.Utils.splitOptions("-C 1.0 -L 0.0010 -P 1.0E-12 -N 0 -V -1 -W 1 "
					+ "-K \"weka.classifiers.functions.supportVector.PolyKernel -C 250007 -E 1.0\""));		
		setTraindata(traindata);	
		setTestdata(testdata);	
			
		// first attribute reflects instance id, delete it when building the classifier
		if (id)
		{	
			//filter
			Remove rm = new Remove();
			rm.setAttributeIndices("1");  // remove 1st attribute
			// meta-classifier
			FilteredClassifier fc = new FilteredClassifier();
			fc.setFilter(rm);
			fc.setClassifier(SVM);
			setMLclass(fc);
		}
		else
		{
			setMLclass(SVM);
		}
	}

	/**
	 * @param traindata
	 * @param testdata
	 * @param classif
	 */
	public WekaWrapper (Instances traindata, Instances testdata, Classifier classif){
		setTraindata(traindata);
		setTestdata(testdata);
		setMLclass(classif);
	}
	
	
	/**
	 * @param traindata
	 */
	public void setTraindata(Instances traindata) {
		this.traindata = traindata;
	}

	public void setTestdata(Instances testdata) {
		this.testdata = testdata;
	}

	public void setMLclass(Classifier mLclass) {
		MLclass = mLclass;
	}
	
	public Classifier getMLclass() {
		return this.MLclass;
	}
	
	
	/**
	 * Perform cross validation evaluation of the classifier with the given number of folds.
	 * @param foldNum
	 * @throws Exception
	 */
	public void crossValidate(int foldNum) throws Exception
	{
		System.out.println("WekaWrapper: "+foldNum+"-fold cross validation over train data.");
		System.err.println("WekaWrapper: "+foldNum+"-fold cross validation over train data.");
		Evaluation eTest = new Evaluation(traindata);				
		eTest.crossValidateModel(this.MLclass, traindata, foldNum, new Random(1));	//seed = 1;		
		/* it remains for the future to inspect the random generation. 
		 * It seems using the same seed over an specific sequence generates the same randomization. 
		 * Thus, for the same sequence of instances, fold generation is always the same.  
		 */
		//eTest.crossValidateModel(this.MLclass, traindata, foldNum, new Random((int)(Math.random()*traindata.numInstances())));
		printClassifierResults (eTest);
	}
	
	
	/**
	 *  Trains the classifier with the current training data and stores it in the "SVM.model" file.
	 */
	public void saveModel(String savePath)
	{
		//train the classisfier
	    try {	    	
	    	this.MLclass.buildClassifier(this.traindata);	    	
			System.err.println("WekaWrapper: saveModel() - Training ready.");
			Instances header = new Instances(this.traindata, 0);		
			// serialize model
			weka.core.SerializationHelper.writeAll(savePath, new Object[]{MLclass, header});
		} catch (Exception e) {
			e.printStackTrace();
		}      
	}
	
	/**
	 * Loads the model stored in the given file and evaluates it against the current test data. 
	 * The void returns and error if no test data is presents.  
	 * 
	 * @param model
	 * @throws Exception 
	 */
	public void testModel(String model) throws Exception
	{
		if ((testdata == null) || testdata.isEmpty())
		{
			System.err.println("WekaWrapper: testModel() - no test data available, model won't be evaluated");
			System.exit(9);
		}
		
		// check model file
		//if (! FileUtilsElh.checkFile(modelPath))
		//{
		//	System.err.println("WekaWrapper: testModel() - model couldn't be loaded");
		//	System.exit(8);
		//}

		// deserialize model
		this.MLclass = (Classifier) weka.core.SerializationHelper.readAll(model)[0];		
		System.err.println("WekaWrapper: testModel() - Classifier ready.");
				
		Evaluation eTest = new Evaluation(this.testdata);
		eTest.evaluateModel(this.MLclass, this.testdata);
		System.err.println("WekaWrapper: testModel() - Test ready.");
			
		printClassifierResults (eTest);      
	}

	/**
	 * Loads the model stored in the given file and returns it.  
	 * 
	 * @param modelPath
	 * @return
	 * @throws Exception
	 */
	public Classifier loadModel(String modelPath) throws Exception
	{
		System.err.println("WekaWrapper: loadModel() - model: "+modelPath);
		// deserialize model
		Object object_ser[] = weka.core.SerializationHelper.readAll(modelPath);
		return (Classifier) object_ser[0];		
		//System.err.println("WekaWrapper: loadModel() - Classifier ready.");				      
	}

	/**
	 * Loads the header stored in the given model file and returns it.  
	 * 
	 * @param modelPath
	 * @return
	 * @throws Exception
	 */
	public Instances loadHeader(String model) throws Exception
	{
		// deserialize model
		Object object_ser[] = weka.core.SerializationHelper.readAll(model);
		return (Instances) object_ser[1];		
		//System.err.println("WekaWrapper: loadModel() - Classifier ready.");				      
	}
	
	/**
	 *  Trains the current classifier with the current training data and tests it with the current test data.
	 *  
	 *  If no test data is currently available train data is split in two parts (train 90% / test 10%). 
	 * 
	 * @throws Exception
	 */
	public void trainTest() throws Exception
	{
		if ((testdata == null) || testdata.isEmpty())
		{
			System.err.println("WekaWrapper: trainTest() - test data is empty. Train data will be divided in two (90% train / 10% test)");
			//traindata.randomize(new Random((int)(Math.random()*traindata.numInstances())));			
			/* it remains for the future to inspect the random generation. 
			 * It seems using the same seed over an specific sequence generates the same randomization. 
			 * Thus, for the same sequence of instances, fold generation is always the same.  
			 */
			traindata.randomize(new Random(1));
			Instances trainset90 = traindata.trainCV(10, 9);
			Instances testset10 = traindata.testCV(10, 9);
			setTestdata(testset10);
			setTraindata(trainset90);
		}
		
		//train the classisfier
		this.MLclass.buildClassifier(this.traindata);
		System.err.println(" Classifier ready.");
			
		Evaluation eTest = new Evaluation(this.testdata);
		eTest.evaluateModel(this.MLclass, this.testdata);
		System.err.println("WekaWrapper: trainTest() - Test ready.");
			
		printClassifierResults (eTest);
	}
	
	/**
	 * @return HashMap<Instance, double[]> predictions : HashMap containing Instances and their corresponding 
	 *          prediction results (distribution across classes is returned.
	 * @throws Exception
	 */
	public HashMap<Instance, double[]> multiLabelPrediction() throws Exception
	{
		HashMap<Instance, double[]> rslt = new HashMap<Instance, double[]>();	
		
		if ((testdata == null) || testdata.isEmpty())
		{
			System.err.println("WekaWrapper: multiLabelPrediction() - test data is empty. Train data will be divided in two (90% train / 10% test)");
			traindata.randomize(new Random(1));			
			Instances trainset90 = traindata.trainCV(10, 9);
			Instances testset10 = traindata.testCV(10, 9);
			setTestdata(testset10);
			setTraindata(trainset90);
		}
		
		//train the classisfier
		this.MLclass.buildClassifier(this.traindata);
		System.err.println("WekaWrapper: multiLabelPrediction() - Classifier ready.");
			
		for (Instance i : this.testdata )
		{
			double[] dist = this.MLclass.distributionForInstance(i);
			rslt.put(i, dist);
		}
		
		System.err.println("WekaWrapper: multiLabelPrediction() - Test ready.");
		return rslt;		
	}
	
	/**
	 * @return HashMap<Instance, double[]> predictions : HashMap containing Instances and their corresponding 
	 *          prediction results (distribution across classes is returned.
	 *          
	 * @param modelPath path to the serialized model stored in a file.
	 * 
	 * @throws Exception
	 */
	public HashMap<Instance, double[]> multiLabelPrediction(String modelPath) throws Exception
	{
		HashMap<Instance, double[]> rslt = new HashMap<Instance, double[]>();
		if ((testdata == null) || testdata.isEmpty())
		{
			System.err.println("WekaWrapper: multiLabelPrediction() - test data is empty. No test will be performed");
			System.exit(9);
		}
		
		
		if (! FileUtilsElh.checkFile(modelPath))
		{
			System.err.println("WekaWrapper: multiLabelPrediction() - model couldn't be loaded");
			System.exit(8);
		}
		
		// load classifier model		
		this.MLclass = (Classifier) weka.core.SerializationHelper.readAll(modelPath)[0];		
		System.err.println("WekaWrapper: multiLabelPrediction() - Classifier ready.");
			
		for (Instance i : this.testdata )
		{
			double[] dist = this.MLclass.distributionForInstance(i);
			rslt.put(i, dist);
		}
		
		System.err.println("WekaWrapper: multiLabelPrediction() - Test ready.");
		return rslt;		
	}
	
	/**
	 *  Prints the results stored in an Evaluation object to standard output
	 *  (summary, class results and confusion matrix)
	 * 
	 * @param Evaluation eval
	 * @throws Exception
	 */
	public void printClassifierResults (Evaluation eval) throws Exception
	{
		// Print the result à la Weka explorer:
        String strSummary = eval.toSummaryString();
        System.out.println(strSummary);
          
        // Print per class results
        String resPerClass = eval.toClassDetailsString();
        System.out.println(resPerClass);
        
        // Get the confusion matrix
        String cMatrix = eval.toMatrixString();
        System.out.println(cMatrix);	
        
        System.out.println();
	}	
	
	/**
	 *   Simple function to print the results of a multilabel prediction.
	 * 
	 * @param HashMap<Instance, double[]> pred hashmap containing a set of instances and their corresponding
	 *         multilabel prediction, as computed by the multiLabelPrediction() function in this class.
	 */
	public void printMultilabelPredictions (HashMap<Instance, double[]> pred)
	{
		for (Instance i : pred.keySet())
		{
			double[] kk = pred.get(i);
			int c = 0;
			System.out.print("instance "+Integer.parseInt(Double.toString(i.value(0)))+" ("+i.classValue()+"|"+i.stringValue(i.classIndex())+") --> ");
			for (double d: kk)
			{					
				System.out.print("cl_"+c+"="+d+"; ");
				c++;
			}
			System.out.print("\n");
		}
	
	}
	
	
	public Instances addClassification (Classifier cl, Instances data) throws Exception
	{
		//filter
		AddClassification add = new AddClassification();
		add.setRemoveOldClass(true); //remove the old class attribute
		add.setOutputDistribution(true);
		add.setClassifier(cl);
		add.setInputFormat(data);

		// meta-classifier
		Instances rslt = Filter.useFilter(data, add);
		return rslt;
	}

	
	public void filterAttribute (String index) throws Exception
	{
		//filter
		Remove rm = new Remove();
		rm.setAttributeIndices(index);  // remove 1st attribute indexes start from 1
		// meta-classifier
		FilteredClassifier fc = new FilteredClassifier();
		fc.setFilter(rm);
		fc.setClassifier(this.MLclass);
		setMLclass(fc);
	}
	

	/**
	 *      Train one vs all models over the given training data.
	 *  
	 * @param modelpath directory to store each model for the one vs. all method
	 * @param prefix prefix the models should have (each model will have the name of its class appended
	 * @throws Exception
	 */
	public void trainOneVsAll (String modelpath, String prefix) throws Exception
	{
		Instances orig = new Instances(traindata);
		Enumeration<Object> classValues = traindata.classAttribute().enumerateValues();
		String classAtt = traindata.classAttribute().name();
		while (classValues.hasMoreElements())
		{			
			String v = (String)classValues.nextElement();
			System.err.println("trainer onevsall for class "+v+" classifier");
			//needed because of weka's sparse data format problems THIS IS TROUBLE! ...
			if (v.equalsIgnoreCase("dummy"))
			{
				continue;
			}
			// copy instances and set the same class value
			Instances ovsa = new Instances(orig); 
			//create a new class attribute			
			//   // Declare the class attribute along with its values
			ArrayList<String> classVal = new ArrayList<String>();
			classVal.add("dummy"); //needed because of weka's sparse data format problems...
			classVal.add(v);
			classVal.add("UNKNOWN");
			ovsa.insertAttributeAt(new Attribute(classAtt+"2",classVal), ovsa.numAttributes());			
			//change all instance labels that have not the current class value to "other"
			for (int i=0; i < ovsa.numInstances();i++)
			{
				Instance inst = ovsa.instance(i);
				String instClass = inst.stringValue(ovsa.attribute(classAtt).index());
				if (instClass.equalsIgnoreCase(v))
				{
					inst.setValue(ovsa.attribute(classAtt+"2").index(),v);					
				}
				else
				{
					inst.setValue(ovsa.attribute(classAtt+"2").index(),"UNKNOWN");
				}
			}
			//delete the old class attribute and set the new.			
			ovsa.setClassIndex(ovsa.attribute(classAtt+"2").index());
			ovsa.deleteAttributeAt(ovsa.attribute(classAtt).index());
			ovsa.renameAttribute(ovsa.attribute(classAtt+"2").index(), classAtt);
			ovsa.setClassIndex(ovsa.attribute(classAtt).index());
			
			
			//build the classifier, crossvalidate and store the model
			setTraindata(ovsa);
			saveModel(modelpath+File.separator+prefix+"_"+v+".model");
			setTestdata(ovsa);
			testModel(modelpath+File.separator+prefix+"_"+v+".model");

			System.err.println("trained onevsall "+v+" classifier");
		}
		
		setTraindata(orig);
	}

	
	/**
	 *      Train one vs all models over the given training data.
	 *  
	 * @param modelpath directory to store each model for the one vs. all method
	 * @param prefix prefix the models should have (each model will have the name of its class appended
	 * @throws Exception
	 */
	public HashMap<Integer, HashMap<String, Double>> predictOneVsAll (String modelpath, String prefix) throws Exception
	{
		HashMap<Integer, HashMap<String, Double>> rslt = new HashMap<Integer, HashMap<String, Double>>();
		if ((testdata == null) || testdata.isEmpty())
		{
			System.err.println("WekaWrapper: testModel() - no test data available, model won't be evaluated");
			System.exit(9);
		}
		
		Enumeration<Object> classValues = traindata.classAttribute().enumerateValues();
		HashMap<String, Classifier> cls = new HashMap<String, Classifier>();		
		while (classValues.hasMoreElements())
		{				
			String v = (String)classValues.nextElement();
			//needed because of weka's sparse data format problems THIS IS TROUBLE! ...

			if (v.equalsIgnoreCase("dummy"))
			{
				continue;
			}

			
			try {
				Classifier cl = loadModel(modelpath+File.separator+prefix+"_"+v+".model");
				cls.put(v, cl);
			} catch (Exception e){
				System.err.println("classifier for class "+v+" could not be loaded, prediction aborted");
				System.exit(9);
			}
		}		
		
		for (int i=0; i < testdata.numInstances();i++)
		{
			HashMap<String, Double> clResults = new HashMap<String, Double>();
			Instance inst = testdata.instance(i);
			int instId = (int)inst.value(testdata.attribute("instanceId").index());
			inst.setClassMissing();
			for (String currentClass : cls.keySet())
			{
				double[] dist = cls.get(currentClass).distributionForInstance(inst);			
				String[] classes = {"dummy",currentClass,"UNKNOWN"};
				System.out.print("instance "+instId+" ("+currentClass+") --> ");
				for (int c=0; c<dist.length; c++)
				{					
					System.out.print("\t cl_"+c+" ("+classes[c]+") = "+dist[c]+"; ");					
				}
				System.out.print("\n");
			
				//first class is always the class to identify, if unknown class has better score store -1 for the class
				clResults.put(currentClass, dist[1]);								
			}
			rslt.put(instId, clResults);
		}	
				
		return rslt;	
	}
	
	/**
	 *      Train one vs all models over the given training data.
	 *  
	 * @param modelpath directory to store each model for the one vs. all method
	 * @param prefix prefix the models should have (each model will have the name of its class appended
	 * @throws Exception
	 */
	public HashMap<Integer, HashMap<String, Double>> addOneVsAllPredictions (String modelpath, String prefix, double thres) throws Exception
	{
		HashMap<Integer, HashMap<String, Double>> rslt = new HashMap<Integer, HashMap<String, Double>>();
		if ((testdata == null) || testdata.isEmpty())
		{
			System.err.println("WekaWrapper: testModel() - no test data available, model won't be evaluated");
			System.exit(9);
		}
		
		Enumeration<Object> classValues = traindata.classAttribute().enumerateValues();
		HashMap<String, Classifier> cls = new HashMap<String, Classifier>();		
		while (classValues.hasMoreElements())
		{				
			String v = (String)classValues.nextElement();
			//needed because of weka's sparse data format problems THIS IS TROUBLE! ...

			if (v.equalsIgnoreCase("dummy"))
			{
				continue;
			}

			
			try {
				Classifier cl = loadModel(modelpath+File.separator+prefix+"_"+v+".model");
				cls.put(v, cl);
			} catch (Exception e){
				System.err.println("classifier for class "+v+" could not be loaded, prediction aborted");
				System.exit(9);
			}
		}		
		
		for (int i=0; i < testdata.numInstances();i++)
		{
			HashMap<String, Double> clResults = new HashMap<String, Double>();
			Instance inst = testdata.instance(i);
			int instId = (int)inst.value(testdata.attribute("instanceId").index());
			inst.setClassMissing();
			for (String currentClass : cls.keySet())
			{
				double[] dist = cls.get(currentClass).distributionForInstance(inst);			
				
				System.out.print("instance "+instId+" ("+currentClass+") --> \n");
			/*	for (int c=0; c<dist.length; c++)
				{					
					System.out.print("\t cl_"+c+" ("+") = "+dist[c]+"; ");					
				}
				System.out.print("\n");
			*/
				//first class is always the class to identify, if unknown class has better score store -1 for the class
				clResults.put(currentClass, dist[1]);								
			}
			rslt.put(instId, clResults);
		}	
				
		return rslt;	
	}
	
	/**
	 * 
	 *  Function to get the resource path to pass it to Ixa-pipes. Needed to pass the default lemma and 
	 *  pos models. In cases where specific models are used instead of the defaults, 
	 *  this function returns the same input. No IO problems are handled here. 
	 * @param model
	 * @param lang [en|es|eu|fr]
	 * @param type [twt|...]
	 * @return
	 */
	public static String getModelResource(String model, String lang, String type){
		String rsrcStr = "";
		if (model.equalsIgnoreCase("default"))
		{
			String rsrcPath = defaultModels.getProperty(lang+"-"+type);
			InputStream rsrc = WekaWrapper.class.getClassLoader().getResourceAsStream((modelDir+File.separator+rsrcPath+".model"));
			try {
				File tempModelFile = File.createTempFile("Elixa-Polarity-Model", Long.toString(System.nanoTime()));
				tempModelFile.deleteOnExit();
				System.err.println(lang+"-"+type+" --> "+rsrcPath+" -- "+rsrc+" --- "+tempModelFile.getAbsolutePath());
				FileUtils.copyInputStreamToFile(rsrc, tempModelFile);
				return tempModelFile.getAbsolutePath();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.err.println("EliXa::WekaWrapper - Error when loading default model for language "+lang+". Execution will probably end badly.");
				//e.printStackTrace();
				return model;
			}
		}
		else
		{
			return model;
		}
	}
}
