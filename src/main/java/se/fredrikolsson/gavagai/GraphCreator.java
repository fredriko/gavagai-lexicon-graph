package se.fredrikolsson.gavagai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * TODO cannot handle / in target term: need url encoding of that particular case?
 * TODO add index to neo4j db to make searching for terms faster
 * TODO change to custom http client instead of unirest since it has problems with connection leaks (not consuming http entity)
 * TODO implement stopping criterion: triggered by call-back from response worker. shut down request workers, response workers, neo4j database, and print statistics
 * TODO implement logging of queue sizes: facilitate optimizing of workers vs and queue size
 *
 */
public class GraphCreator {
    private static Logger logger = LoggerFactory.getLogger(GraphCreator.class);

    private final static int REQUEST_QUEUE_SIZE = 100000;
    private final static int RESPONSE_QUEUE_SIZE = 1000;
    private final static int NUM_PRODUCER_THREADS = 200;

    private final int numProducerThreads;
    private final int maxDepth;
    private final String apiKey;
    private final String neo4jDbName;
    private final BlockingQueue<LookupRequest> lookupRequestQueue;
    private final BlockingQueue<LookupResponse> lookupResponseQueue;
    private final ExecutorService lexiconLookupRequestWorkerExecutor;
    private final ExecutorService lexiconLookupResponseWorkerExecutor;

    public static void main(String[] args) throws Exception {

        String apiKey = "4c775d38fe2d12c43d99858dd0130fa0";
        String neo4jDbName = "/Users/fredriko/mcdonalds-1";
        int maxDepth = 4;

        GraphCreator populator = new GraphCreator(apiKey, NUM_PRODUCER_THREADS, neo4jDbName, maxDepth);
        populator.start();

        populator.addLookupRequest(new LookupRequest("mc donalds", "sv"));
        // TODO remove when stopping criterion is in place.
        Thread.sleep(1000000000);
    }

    private GraphCreator(String apiKey, int numProducerThreads, String neo4jDbName, int maxDepth) {
        this.apiKey = apiKey;
        this.numProducerThreads = numProducerThreads;
        this.maxDepth = maxDepth;
        this.neo4jDbName = neo4jDbName;
        this.lookupRequestQueue = new LinkedBlockingQueue<>(getRequestQueueSize());
        this.lookupResponseQueue = new LinkedBlockingQueue<>(getResponseQueueSize());
        ThreadFactory lexiconLookupThreadFactory = new NamingThreadFactory("requestWorker");
        this.lexiconLookupRequestWorkerExecutor =
                new ThreadPoolExecutor(
                        numProducerThreads,
                        numProducerThreads,
                        0L,
                        TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>(),
                        lexiconLookupThreadFactory);
        this.lexiconLookupResponseWorkerExecutor = Executors.newSingleThreadExecutor();
    }

    private void start() {
        logger.info("Starting GraphCreator");
        startLexiconLookupRequestWorkers(getNumProducerThreads(), getLexiconLookupRequestWorkerExecutor());
        logger.info("Starting a single LexiconLookupResponseWorker");
        getLexiconLookupResponseWorkerExecutor().execute(
                new LexiconLookupResponseWorker(
                        getLookupRequestQueue(),
                        getLookupResponseQueue(),
                        getMaxDepth(),
                        getNeo4jDbName()));
    }

    private void addLookupRequest(LookupRequest request) {
        getLookupRequestQueue().add(request);
    }

    private int getNumProducerThreads() {
        return numProducerThreads;
    }

    private String getApiKey() {
        return apiKey;
    }

    private BlockingQueue<LookupRequest> getLookupRequestQueue() {
        return lookupRequestQueue;
    }

    private BlockingQueue<LookupResponse> getLookupResponseQueue() {
        return lookupResponseQueue;
    }

    private ExecutorService getLexiconLookupRequestWorkerExecutor() {
        return lexiconLookupRequestWorkerExecutor;
    }

    private ExecutorService getLexiconLookupResponseWorkerExecutor() {
        return lexiconLookupResponseWorkerExecutor;
    }

    private int getMaxDepth() {
        return maxDepth;
    }

    private int getRequestQueueSize() {
        return REQUEST_QUEUE_SIZE;
    }

    private int getResponseQueueSize() {
        return RESPONSE_QUEUE_SIZE;
    }

    private String getNeo4jDbName() {
        return neo4jDbName;
    }

    private void startLexiconLookupRequestWorkers(int numThreads, ExecutorService service) {
        logger.info("Starting {} LexiconLookupRequestWorkers", numThreads);
        for (int i = 0; i < numThreads; i++) {
            service.execute(
                    new LexiconLookupRequestWorker(getLookupRequestQueue(), getLookupResponseQueue(), getApiKey()));
        }
    }

}
