# Gavagai Lexicon Graph

This is a utility for creating a Neo4j graph database from the word knowledge available in [Gavagai Living Lexicon](http://lexicon.gavagai.se/).

Given one or more target terms, the graph creator retrieves all semantically similar neighbors up to a pre-specified distance. Two terms are semantically similar if they appear in similar contexts in the texts indexed by Gavagai's semantic memories. Examples of similar terms are **data science**, **machine learning**, **natural language processing**, and **computer vision**. The semantic memories learn, completely unsupervised, by continuously reading documents from online media, blogs, and forum posts.

Use the [Gavagai Living Lexicon](http://lexicon.gavagai.se/) to peak into the semantic memories, and to check the presence of terms. The Lexicon provides information for a single target term at a time, while the Gavagai Lexicon Graph application combines the information for multiple terms into a graph database.

See the following posts for examples of output produced the Gavagai Lexicon Graph

 - [Business bingo - Is your text analytics system up-to-date with current affairs?](https://www.linkedin.com/pulse/business-bingo-your-text-analytics-system-current-affairs-olsson/)
 - [The extraordinary productivity of foul language](https://www.linkedin.com/pulse/extraordinary-productivity-foul-language-do-you-your-fredrik-olsson/)


## How to use the pre-compiled binary (jar)

To run the pre-compiled Gavagai Lexicon Graph utility, you need:

 - Java 1.7 or greater. Refer to [Java Help Center](https://java.com/en/download/help/index_installing.xml) for installation instructions.
 - an API key from [Gavagai](http://gavagai.se). Sign up for one [here](https://developer.gavagai.se/).
 - The JAR file containing the graph builder [Download it here](distribution/)
 - Neo4j Community Edition installed. Instructions are available at [the Neo4j homepage](http://neo4j.com/download/). The current version of Gavagai Lexicon Graph has been tested with Neo4j version 3.3.5. 

Follow the instructions in the **Run** section below.

## How to build the Gavagai Lexicon Graph utility from the source

To build the utility from source, you need:

 - Java 1.7 or greater. Refer to [Java Help Center](https://java.com/en/download/help/index_installing.xml) for installation instructions.
 - Maven. Refer to the [Maven homepage](https://maven.apache.org/) for instructions on how to install it.
 - Neo4j Community Edition installed. Instructions are available at [the Neo4j homepage](http://neo4j.com/download/). The current version of Gavagai Lexicon Graph has been tested with Neo4j version 3.3.5. 

On your local machine: Clone this project, change to the directory where the source code landed, and build it with

    mvn package

This will generate an executable jar file including all dependencies in `gavagai-lexicon-graph/target/gavagai-lexicon-graph.jar`.

Follow the instructions in the **Run** section below.

## Run

To see information on how to use the application, invoke it with the following command in a terminal

    java -jar target/gavagai-lexicon-graph.jar -h
    
## Example

Get information for the term "no-fly zone" and its neighbors:

    java -jar target/gavagai-lexicon-graph.jar -a <api-key> -d /tmp/lexicon-1 -l en -m 5 -t "no-fly zone"
    
Replace `<api-key>` with your own Gavagai Api key. The above command creates a Neo4j database in `/tmp/lexicon-1` by retrieving all semantically similar neighbors for the term "no-fly zone" in Gavagai's English semantic memory, up to and including those that are 5 hops away.

Once the data has been retrieved, start Neo4j and point it to `/tmp/lexicon-1`. Issue a Cypher query like the following:

    MATCH (a)-[r1:NEIGHBOR]-(b)-[r2:NEIGHBOR]-(c)-[r3:NEIGHBOR]-(d)
    WHERE r1.strength > 0.5 AND r2.strength > 0.5 AND r3.strength > 0.5
    RETURN a, b, c
    
and you will end up with a number of tuples, each of which contains terms that are fairly tight connected when it comes to their use in general language. Here's the output I got for the above question (click the image for a larger version):


![alt tag](images/example-tuples.png)


Another example. If you wish to find out the immediate neighborhood of our initial target term, "no-fly zone", issue the following query (again, click the image for a larger version):

    MATCH (a {name:"no-fly zone"})-[r1:NEIGHBOR]-(b)-[r2:NEIGHBOR]-(c)-[r3:NEIGHBOR]-(d)
    WHERE r1.strength > 0.3 AND r2.strength > 0.3 AND r3.strength > 0.3
    RETURN a, b, c
    

![alt tag](images/no-fly-zone-neighborhood.png)

## Known issues/TODO:s

 - Add logging of what requests were dropped and why: make it possible to treat lost requests separately, in a new session (Save information to, e.g., MongoDb)
 - Enable subsequent sessions to continue from an aborted or terminated session: use information in Neo4j for this - do not lookup terms that are already in the db.
