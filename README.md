
# EliXa

Aspect based sentiment analysis platform developed by the Elhuyar Foundation. Licensed under GNU GPL v3 terms.


# Contents

The contents of the module are the following:

    + pom.xml                 maven pom file which deals with everything related to compilation and execution of the module
    + src/                    java source code of the module
    + Furthermore, the installation process, as described in the README.md, will generate another directory:
    target/                 it contains binary executable and other directories


# INSTALLATION

Installing EliXa requires the following steps:

If you already have installed in your machine JDK7 and MAVEN 3, please go to step 3
directly. Otherwise, follow these steps:

## 1. Install JDK 1.7


If you do not install JDK 1.7 in a default location, you will probably need to configure the PATH in .bashrc or .bash_profile:

````shell
export JAVA_HOME=/yourpath/local/java17
export PATH=${JAVA_HOME}/bin:${PATH}
````

If you use tcsh you will need to specify it in your .login as follows:

````shell
setenv JAVA_HOME /usr/java/java17
setenv PATH ${JAVA_HOME}/bin:${PATH}
````

If you re-login into your shell and run the command

````shell
java -version
````

You should now see that your jdk is 1.7

## 2. Install MAVEN 3

Download MAVEN 3 from

````shell
wget http://apache.rediris.es/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.tar.gz
````

Now you need to configure the PATH. For Bash Shell:

````shell
export MAVEN_HOME=/home/myuser/local/apache-maven-3.0.5
export PATH=${MAVEN_HOME}/bin:${PATH}
````

For tcsh shell:

````shell
setenv MAVEN3_HOME ~/local/apache-maven-3.0.5
setenv PATH ${MAVEN3}/bin:{PATH}
````

If you re-login into your shell and run the command

````shell
mvn -version
````

You should see reference to the MAVEN version you have just installed plus the JDK version that is using.

## 3. Get module source code


````shell
git clone https://github.com/Elhuyar/Elixa
````

## 4. Dependencies - IXA-PIPES


EliXa uses internally ixa-pipes as default NLP chain (Other NLP processors are supported so long as the input is in one of the accepted formats [tab|naf-xml]). Ixa-pipes require certain resources to be included in the `src/main/resources` directory before compilation.

Get the resources from https://github.com/ixa-ehu/ixa-pipe-pos#resources and unpack them in the `src/main/resources` directory

````shell
cd elixa
wget http://ixa2.si.ehu.es/ixa-pipes/models/lemmatizer-dicts.tar.gz
tar -xzvf lemmatizer-dicts.tar.gz -C src/main/resources
````

If you wish a better multiword term (MWT) recognition (for Spanish only) overwrite the file `src/main/resources/lemmatizer-dicts/freeling/es-locutions.txt` with the one in `src/main/resources/es/es-locutions.txt`

WARNING: This multiword list is compiled for sentiment analysis purposes. It tags expressions such as "Buenos días (good morning)". In order for it to be effective, those MWT should also be included in the polarity lexicon, in order to match then later. 

````shell
cp src/main/resources/lemmatizer-dicts/freeling/es-locutions.txt src/main/resources/lemmatizer-dicts/freeling/es-locutions.txt.backup
cp src/main/resources/es/es-locutions.txt src/main/resources/lemmatizer-dicts/freeling/es-locutions.txt
````

Also, you will need the postagging models for ixa-pipes. This are not needed at compilation time as you specify the pos-model file in the configuration file. You can get the models from https://github.com/ixa-ehu/ixa-pipe-pos#models . 

Elixa uses the 1.5.0 models, and configuration files provided in the Resources section assume that the models are unpacked to the `src/main/resources` directory. The following commands will download and unpack the models to your src/main resources directory. If you change the destination or the models remember to set the paths in the configuration files accordingly.  

````shell
cd elixa
wget http://ixa2.si.ehu.es/ixa-pipes/models/morph-models-1.5.0.tar.gz
tar -xzvf morph-models-1.5.0.tar.gz -C src/main/resources
````



## 5. Resources


EliXa may use several language specific resources, such as polarity lexicons and other resources for text normalization. We currently provide such resources for 4 languages; Basque (eu), Spanish (es), English (en) and French (fr). 
You can get the resources from http://komunitatea.elhuyar.eus/ig/files/2016/02/elixa-resources_0.8.tgz and unpack them in the src/main/resources directory

````shell
cd elixa
wget http://komunitatea.elhuyar.eus/ig/files/2016/02/elixa-resources_0.8.tgz
cd src/main/resources
tar -xzvf elixa-resources_0.8.tgz
````

We also provide basic polarity classification models for the previous languages. Models have been trained over Twitter data in the context of the [Behagunea](behagunea.dss2016.eu) project. Data is specific on the topic "San Sebastian 2016 Cultural Capital of Europe". 

You can get the models from http://komunitatea.elhuyar.org/ig/files/2017/04/elixa-models-0.9.tgz

````shell
cd elixa
wget http://komunitatea.elhuyar.org/ig/files/2017/04/elixa-models-0.9.tgz
cd src/main/resources
tar -xzvf elixa-models-0.9.tgz
````


Old versions' models:
   - 0.8: http://komunitatea.elhuyar.eus/ig/files/2016/02/elixa-behagunea-models_0.8.tar.gz


## 6. Installing using maven


````shell
cd elixa
mvn clean package
````

This step will create a directory called `target/` which contains various directories and files.
Most importantly, there you will find the module executable:

elixa-0.9.jar

This executable contains every dependency the module needs, so it is completely portable as long
as you have a JVM 1.7 installed.

To install the module in the local maven repository, usually located in ~/.m2/, execute:

````shell
mvn clean install
````

## 7. Test installation

In order to test the Installation, a simple script is provided (test_Elixa.sh). The script will run EliXa and tag the files in the `src/main/resources/examples` folder. The script assumes the executable is in the target directory (`target/elixa-0.9.jar`) and that models and their configurations have been unpacked in the installation directory (`./elixa-models-0.9`)

````shell
sh test_Elixa.sh
````
 



# USING EliXa


## EliXa Funcionalities


EliXa aims to provide 4 main functionalities:

   - **ate**: Aspect term extraction (term category classification included.
   - **atp**: Aspect term polarity
   - **tgp**: Text global polarity.

Currently the following command are available:

    train-gp	 TGP training CLI
    eval-gp	 TGP evaluation CLI
    tag-gp   	 TGP Tagging CLI
    slot2                Semeval 2015 slot2 (ATE) formatting CLI
    tagSentences         Lemmatization and PoS tagging CLI
    tag-naf              Predict polarity of a text in naf format 


## Example uses

### tag-gp 

Tag-gp command is intended to tag new examples with a pre-existing model. The examples can be either sentences or full texts. For each examples the system will return a polarity class depending on the model we use (typically p|neu|n). 


````shell
java -jar target/elixa-0.9.jar tag-gp -f ireom -m path/to/model/en-twt.model -cn 3 -p path/to/model/en-twt.cfg -l en < input.tab  > output_tagged.txt
 ````
    where:
	"-f ireom" is the format of the corpus:
              - "ireom" format means: "id<tab>text" per line, with not polarity annotations.
              - "tabNotagged" format means: "id<tab>polarity<tab>text" if a previous polarity annotation is available (if not this format can also be used and using '?' or 'null' char as polarity.
      "-m model"  is the path to the Elixa global polarity classifier model. If you didn't train your own model use one of the aforementioned models.
      "-cn 3" the number of classes of the classifier. This parameter depends on the model used. The models offered here provide 3-category classification (pos|neg|neu). If we have 5-category model (p+|p|neu|n|n+) this parameter can be used to tell Elixa to map the results into a 3-category model.
      "-p configurationFile"  path to the configuration file. Provided model contain its corresponding configuration files (.cfg extension). IMPORTANT: properties containing paths in the config file must be correctly set according to your system locations
      "-l en" language of the corpus (iso-639 code), English in this example. Elixa allows the following languages to be used: es|eu|en|fr

For more information on the parameters of the tag-gp command you can type:
````shell
java -jar target/elixa-0.9.jar tag-gp -h
````

### train-gp

Train-gp is used to train polarity classification models using a previously tagged data-set. This process can be time consuming depending on the size of the corpus and the features we choose to use.

````shell
java -jar target/elixa-0.9.jar train-gp -f tabNotagged -cn 3 -l es -p models/es-twt.cfg < ~/corpora/opinion-Datasets/Behagune/es-behagtwtpressUniq.tsv > rslt/es-modTreatment/es-twtBehag201602twtpressUniq-Bsline-Old-NonegFix.rslt
````		       

      explanation:
       Parameters are very similar to the tag-gp commands. 
       "-f tabNotagged" is the format of the corpus:
       	   - "tabNotagged" format means: "id<tab>polarity<tab>text" where text is raw text. if used this format, Elixa takes care of linguistically tagging the texts through ixa-pipes.
	   - "tabglobal" format means: an already linguistically tagged corpus in conll format (if you have a corpus tagged with a tagger other than ixa-pipes for example). A pseudo xml format is used to pass document boundaries and polarity annotations. The format of the corpus must be as follows:
	   <doc id=\"([^\"]+)\" (pol|polarity)=\"([^\"]+)\"( score=\"([^\"]+)\")?>
	   forma<tab>lema<tab>PoS
	   forma<tab>lema<tab>PoS
	   ...	   
	   </doc>
	   ...
	   
	   where
		- id is any character string ([^\"]+)
		- "pol|polarity" = pos,neg,neu 
		- score is the same as polarity but in a numeric scale (e.g. [1..5])

       The rest of the parameters have the same meaning as in the tag-gp command. By the default, train-gp command performs 10-fold cross validation and 90 train /10 test division evaluation of the trained model. this can be change by passing "--foldNum" and "--validation" parameters. For further information on those parameters you can type:

````shell
java -jar target/elixa-0.9.jar train-gp -h
````


### eval-gp

eval-gp command is intended to evaluate a previously trained model on a new tagged dataset. The input is a corpus with polarity annotations at document level. eval-gp evaluates the given model against the dataset and outputs evaluation result statistics. Predictions for each document can also be included in the output.

````shell
java -jar target/elixa-0.9.jar eval-gp -f tabNotagged -cn 3 -l es -p models/es-twt.cfg -m path/es-twt.model < /path/to/input/dataset.tsv > /path/to/evaluation.rslt
````

      explanation:
       Again, parameters are very similar to those of the train-gp command. Specific parameters of this command include:

       -r, --ruleBasedClassifier
            Whether rule based classifier should be used instead of the default ML classifier for computing polarity. A polarity lexicon is mandatory if the rule based classifier is used (polarity lexicon path is specified in the configuration file).
                         
       -o, --outputPredictions
            Output predictions or not; output is the corpus annotated with semeval2015 format.

       The rest of the parameters have the same meaning as in the tag-gp command. For further information on those parameters you can type:

````shell
java -jar target/elixa-0.9.jar eval-gp -h
````



### tagSentences

````shell
java -jar target/elixa-0.5.jar tagSentences -d testTag -m absa-models/pos-models/en/en-maxent-100-c5-baseline-dict-penn.bin -l en < input_file.txt
 ````
 	
    where:
        "-d  TestTag" is the directory where tagged files will be stored with the following name: <id>_g.kaf
        "-m path/to/pos-model.bin" is the path to the ixa-pipes-pos pos-model (version 1.4.6).
        "-l en" language of the texts

    For more information you can type: 
````shell
 java -jar target/elixa-0.9.jar tagSentences -h
````

### tag-naf 

````shell
 java -jar target/elixa-0.9.jar tag-naf -l path/to/lexicon.lex < posTagged_input.naf > SentTagged_output.naf
````

    where: 
	"-l path/to/lexicon.lex" is the path to the lexicon. Various lexicon formats are accepted:
            
     /*
     *  "offset_synset<tab>(pos|neg|neu|int|wea|shi)<tab>lemma1, lemma2, lemma3, ...<tab>score<tab>..."   
     *
     *     First two columns are mandatory. Alternatively, first column can contain lemmas instead of offsets.
     */

 The output of this command is a NAF file, but with the prior polarity of the lemmas in the text annotated. e.g.:
     
````shell
     - input NAF term element example: 

     <term id="t28" type="open" lemma="irrelevance" pos="N" morphofeat="NN">
      <span>
        <target id="w28" />
      </span>
    </term>
````
     - output would be:

````shell

     <term id="t28" type="open" lemma="irrelevance" pos="N" morphofeat="NN">
      **<sentiment polarity="negative" />**
      <span>
        <target id="w28" />
      </span>
    </term>
````


GENERATING JAVADOC
==================

You can also generate the javadoc of the module by executing:

````shell
mvn javadoc:jar
````

Which will create a jar file core/target/elixa-1.0-javadoc.jar


References
===================

If you use EliXa please cite the following paper:

 - I. San Vicente, X. Saralegi, y R. Agerri, «EliXa: A modular and flexible ABSA platform», in Proceedings of the 9th International Workshop on Semantic Evaluation (SemEval 2015), Denver, Colorado, 2015, pp. 748-752.


Contact information
===================

````shell
Iñaki San Vicente and Xabier Saralegi
Elhuyar Foundation
{i.sanvicente,x.saralegi}@elhuyar.com
Rodrigo Agerri
IXA NLP Group
rodrigo.agerri@ehu.es
````
