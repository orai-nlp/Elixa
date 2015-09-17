
EliXa
=======

Aspect based sentiment analysis platform


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
git clone https://bitbucket.org/elh-eus/elh-absa
````

3. Dependencies - IXA-PIPES
---------------------------

EliXa uses internally ixa-pipes as default NLP chain (Other NLP processors are supported so long as the input is in one of the accepted formats [tab|naf-xml]). These tools require certain resources to be included in the src/main/resources directory before compilation.

Get the resources from https://github.com/ixa-ehu/ixa-pipe-pos#resources and unpack them in the src/main/resources

````shell
cd EliXa
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


4. Installing using maven
---------------------------

````shell
cd elh-absa
mvn clean package
````

This step will create a directory called target/ which contains various directories and files.
Most importantly, there you will find the module executable:

elixa-1.0.jar

This executable contains every dependency the module needs, so it is completely portable as long
as you have a JVM 1.7 installed.

To install the module in the local maven repository, usually located in ~/.m2/, execute:

````shell
mvn clean install
````

7. USING elh-absa
=========================


Elh-absa Funcionalities
==========================
Elh-absa provides 4 main funcionalities:
   - **ate**: Aspect term extraction (term category classification included.
   - **atp**: Aspect term polarity
   - **acp**: Aspect category polarity.
   - **tgp**: Text global polarity.


GENERATING JAVADOC
==================

You can also generate the javadoc of the module by executing:

````shell
mvn javadoc:jar
````

Which will create a jar file core/target/elixa-1.0-javadoc.jar


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
