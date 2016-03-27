BestBuy Reviews Retriever
=========================

This is a utility for retrieving product reviews from [BestBuy](http://www.bestbuy.com/) and output the reviews as 
a CSV spreadsheet.


Prerequisites
-------------

You will need an API key from BestBuy. Sign up for one [here](https://developer.bestbuy.com/).

You will need Maven installed. Refer to the [Maven homepage](https://maven.apache.org/) for instructions on how to install it.


Build
-----

On your local machine: Clone this project, change to the directory where the source code landed, and build it with

    mvn package

This will generate a package including all dependencies in `bestbuy-reviews-retriever/target/bestbuy-reviews-retriever.jar`.

Run
---

Before you can retrieve reviews, you need to decide on what products you are interested in. Go to 
[BestBuys homepage](http://www.bestbuy.com/), search for the product of choice, and take note of its SKU code.

Once the retriever has been built, and assuming you're still at the top level in the project directory, run 
with the following command:

    java -cp target/bestbuy-reviews-retriever.jar se.fredrikolsson.bestbuy.BestBuyReviewsRetriever <yourBestBuyApiKey> <outputDirectory> <skuCode_1> ... <skuCode_N>

