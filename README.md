# Gavagai Lexicon Graph

This is a utility for creating a Neo4j graph database from the word knowledge available in [Gavagai Living Lexicon](http://lexicon.gavagai.se/). 

Given one or more target terms, the graph creator retrieves all semantically similar neighbors up to a pre-specified distance. Two terms are semantically similar if they appear in similar contexts in the texts indexed by Gavagai's semantic memories. Examples of similar terms are **data science**, **machine learning**, **natural language processing**, and **computer vision**. The semantic memories learn, completely unsupervised, by continuously reading documents from online media, blogs, and forum posts.

Use the [Gavagai Living Lexicon](http://lexicon.gavagai.se/) to peak into the semantic memories, and to check the presence of terms. The Lexicon provides information for a single target term at a time, while the Gavagai Lexicon Graph application combines the information for multiple terms into a graph database.

## Prerequisites

You will need:
 
 - an API key from [Gavagai](http://gavagai.se). Sign up for one [here](https://developer.gavagai.se/).
 - Java 1.7. Refer to [Java Help Center](https://java.com/en/download/help/index_installing.xml) for installation instructions.
 - Maven. Refer to the [Maven homepage](https://maven.apache.org/) for instructions on how to install it.
 - Neo4j Community Edition installed. Instructions are available at [the Neo4j homepage](http://neo4j.com/download/).


## Build

On your local machine: Clone this project, change to the directory where the source code landed, and build it with

    mvn package

This will generate an executable jar file including all dependencies in `gavagai-lexicon-graph/target/gavagai-lexicon-graph.jar`.

## Run

To see information on how to use the application, invoke it with the following command in a terminal

    java -jar target/gavagai-lexicon-graph.jar -h
    
## Example

Get information for the term "no-fly zone" and its neighbors:

    java -jar target/gavagai-lexicon-graph.jar -a <api-key> -d /tmp/lexicon-1 -l en -m 5 -t "no-fly zone"
    
Replace `<api-key>` with your own Gavagai Api key. The above command creates a Neo4j database in `/tmp/lexicon-1` by retrieving all semantically similar neighbors for the term "no-fly zone" in Gavagai's English semantic memory, up to and including those that are 5 hops away.

Once the data has been retrieved, start Neo4j and point it to `/tmp/lexicon-1`.