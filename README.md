
EliXa
=======

Aspect based sentiment analysis platform developed by the Elhuyar Foundation. Licensed under GNU GPL v3 terms.


Contents
========

The contents of the module are the following:

    + pom.xml                 maven pom file which deals with everything related to compilation and execution of the module
    + src/                    java source code of the module
    + Furthermore, the installation process, as described in the README.md, will generate another directory:
    target/                 it contains binary executable and other directories


INSTALLATION
============

Installing the elh-absa requires the following steps:

If you already have installed in your machine JDK7 and MAVEN 3, please go to step 3
directly. Otherwise, follow these steps:

1. Install JDK 1.7
-------------------

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

2. Install MAVEN 3
------------------

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

You should see reference to the MAVEN version you have just installed plus the JDK 6 that is using.

2. Get module source code
--------------------------

````shell
git clone https://github.com/Elhuyar/Elixa
````

3. Dependencies - IXA-PIPES
---------------------------

EliXa uses internally ixa-pipes as default NLP chain (Other NLP processors are supported so long as the input is in one of the accepted formats [tab|naf-xml]). Ixa-pipes require certain resources to be included in the src/main/resources directory before compilation.

Get the resources from https://github.com/ixa-ehu/ixa-pipe-pos#resources and unpack them in the src/main/resources directory

````shell
cd elixa
wget http://ixa2.si.ehu.es/ixa-pipes/models/pos-resources.tgz
src/main/resources
tar -xzvf pos-resources.tgz -C src/main/resources
````

If you wish a better multiword term (MWT) recognition (for Spanish only) overwrite the file src/main/resources/lemmatizer-dicts/freeling/es-locutions.txt with the one in src/main/resources/es/es-locutions.txt

WARNING: This multiword list is compiled for sentiment analysis purposes. It tags expressions such as "Buenos días (good morning)". In order for it to be effective, those MWT should also be included in the polarity lexicon, in order to match then later. 

````shell
cp src/main/resources/lemmatizer-dicts/freeling/es-locutions.txt src/main/resources/lemmatizer-dicts/freeling/es-locutions.txt.backup
cp src/main/resources/es/es-locutions.txt src/main/resources/lemmatizer-dicts/freeling/es-locutions.txt
````

Also, you will need the postagging models for ixa-pipes. This are not needed at compilation time as you specify the pos-model file in the configuration file. You can get the model from https://github.com/ixa-ehu/ixa-pipe-pos#models . Elixa uses the 1.4.6 models.

4. Resources
---------------------------

EliXa may use several language specific resources, such as polarity lexicons and other resources for text normalization. We currently provide such resources for 4 languages; Basque (eu), Spanish (es), English (en) and French (fr). 
You can get the resources from http://komunitatea.elhuyar.org/ig/files/2016/02/elixa-resources_0.8.tgz and unpack them in the src/main/resources directory

````shell
cd elixa
wget http://komunitatea.elhuyar.org/ig/files/2016/02/elixa-resources_0.8.tgz
cd src/main/resources
tar -xzvf elixa-resources_0.8.tgz
````

We also basic polarity classification model for the previous languages. Models have been trained over Twitter data in the context of the Behagunea (behagunea.dss2016.eu) project. Data is specific on the topic "San Sebastian 2016 Cultural Capital of Europe". 

You can get the models from http://komunitatea.elhuyar.org/ig/files/2016/02/elixa-behagunea-models_0.8.tar.gz.


````shell
cd elixa
wget http://komunitatea.elhuyar.org/ig/files/2016/02/elixa-behagunea-models_0.8.tar.gz
cd src/main/resources
tar -xzvf elixa-behagunea-models_0.8.tar.gz
````



6. Installing using maven
---------------------------

````shell
cd elixa
mvn clean package
````

This step will create a directory called target/ which contains various directories and files.
Most importantly, there you will find the module executable:

elixa-0.5.jar

This executable contains every dependency the module needs, so it is completely portable as long
as you have a JVM 1.7 installed.

To install the module in the local maven repository, usually located in ~/.m2/, execute:

````shell
mvn clean install
````

7. USING EliXa
=========================


EliXa Funcionalities
==========================
EliXa aims to provide 4 main funcionalities:

   - **ate**: Aspect term extraction (term category classification included.
   - **atp**: Aspect term polarity
   - **tgp**: Text global polarity.

Currently the following command are available:

    train-atp|train-gp	 ATP training CLI
    eval-atp|eval-gp	 ATP evaluation CLI
    tag-atp|tag-gp   	 ATP Tagging CLI
    slot2                Semeval 2015 slot2 (ATE) formatting CLI
    tagSentences         Lemmatization and PoS tagging CLI
    tag-naf              Predict polarity of a text in naf format 

Example uses
---------------------------

- train-atp|train-gp
upcomming...
- eval-atp|eval-gp
upcomming...
- tag-atp|tag-gp 
upcomming...

- tagSentences

````shell
java -jar target/elixa-0.5.jar tagSentences -d testTag -m absa-models/pos-models/en/en-maxent-100-c5-baseline-dict-penn.bin -l en < input_file.txt
 ````
    where:
        "-d  TestTag" is the directory where tagged files will be stored with the following name: <id>_g.kaf
        "-m path/to/pos-model.bin" is the path to the ixa-pipes-pos pos-model (version 1.4.6).
        "-l en" language of the texts

    For more info: 
````shell
 java -jar target/elixa-0.5.jar tagSentences -h
````

- tag-naf 

````shell
 java -jar target/elixa-0.5.jar tag-naf -l path/to/lexicon.lex < posTagged_input.naf > SentTagged_output.naf
````

    where: 
	"-l path/to/lexicon.lex" is the path to the lexicon. Various lexicon formats are accepted:
            
     /*
     *  "offset_synset<tab>(pos|neg|neu|int|wea|shi)<tab>lemma1, lemma2, lemma3, ...<tab>score<tab>..."   
     *
     *     First two columns are mandatory. Alternatively, first column can contain lemmas instead of offsets.
     */




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
