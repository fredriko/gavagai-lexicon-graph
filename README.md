# Gavagai Lexicon Graph

This is a utility for creating a Neo4j graph database from the word knowledge available in [Gavagai Living Lexicon](http://lexicon.gavagai.se/). 

Given one or more target terms, the graph creator retrieves all semantically similar neighbors up to a pre-specified distance. Two terms are semantically similar if they appear in similar contexts in the texts read by Gavagai's semantic memories. Examples of similar terms include **data science** and **machine learning**, or **touch base** and **re-connect**. Read more about the methods used for computing semantic similarity and related information in the following publications:

- [The Word-Space Model: Using distributional analysis to represent syntagmatic and paradigmatic relations between words in high-dimensional vector spaces](https://scholar.google.se/citations?view_op=view_citation&hl=en&user=Nf2NNVwAAAAJ&citation_for_view=Nf2NNVwAAAAJ:u-x6o8ySG0sC)
- [The Distributional Hypothesis](https://scholar.google.se/citations?view_op=view_citation&hl=en&user=Nf2NNVwAAAAJ&citation_for_view=Nf2NNVwAAAAJ:UeHWp8X0CEIC)

Use the [Gavagai Living Lexicon](http://lexicon.gavagai.se/) to peak into the semantic memories, and to check the presence of terms. The Lexicon provides information for a single target term at a time, while the Gavagai Lexicon Graph application combines the information for multiple terms into a graph database.

## Prerequisites

You will need an API key from [Gavagai](http://gavagai.se). Sign up for one [here](https://developer.gavagai.se/).

You will need Java 1.7. Refer to [Java Help Center](https://java.com/en/download/help/index_installing.xml) for installation instructions.

You will need Maven installed. Refer to the [Maven homepage](https://maven.apache.org/) for instructions on how to install it.

You will need the Neo4j Community Edition installed. Instructions are available at [the Neo4j homepage](http://neo4j.com/download/).


## Build

On your local machine: Clone this project, change to the directory where the source code landed, and build it with

    mvn package

This will generate an executable jar file including all dependencies in `gavagai-lexicon-graph/target/gavagai-lexicon-graph.jar`.

## Run

To see information on how to use the application, invoke it with the following command in a terminal

    java -jar target/gavagai-lexicon-graph.jar -h
    
## Examples

Get information for the term "no-fly zone" and its neighbors up to, and including, those five hops away.

