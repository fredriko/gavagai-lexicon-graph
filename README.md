Gavagai Lexicon Graph
=========================

This is a utility for creating a Neo4j graph database from the word knowledge in [Gavagai Living Lexicon](http://lexicon.gavagai.se/).


# Prerequisites

You will need an API key from [Gavagai](http://gavagai.se). Sign up for one [here](https://developer.gavagai.se/).

You will need Maven installed. Refer to the [Maven homepage](https://maven.apache.org/) for instructions on how to install it.

You will need the Neo4j Community Edition installed. Instructions are available at [the Neo4j homepage](http://neo4j.com/download/).


# Build

On your local machine: Clone this project, change to the directory where the source code landed, and build it with

    mvn package

This will generate a package including all dependencies in `gavagai-lexicon-graph/target/gavagai-lexicon-graph.jar`.

# Run


    java -cp target/gavagai-lexicon-graph.jar se.fredrikolsson.gavagai.

