package se.fredrikolsson.gavagai.lexiconDbPopulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * TODO cannot handle / in target term: need url encoding of that particular case?
 * TODO change to custom http client instead of unirest since it has problems with connection leaks (not consuming http entity)
 * TODO implement stopping criterion: triggered by call-back from response worker. shut down request workers, response workers, neo4j database, and print statistics
 * TODO implement logging of queue sizes: facilitate optimizing of workers vs and queue size
 *
 */
public class LexiconDbPopulator {
    private static Logger logger = LoggerFactory.getLogger(LexiconDbPopulator.class);

    private final int numProducerThreads;
    private final int maxDepth;
    private final String apiKey;
    private final String neo4jDbName;
    private final BlockingQueue<LookupRequest> lookupRequestQueue;
    private final BlockingQueue<LookupResponse> lookupResponseQueue;
    private final ExecutorService lexiconLookupRequestWorkerExecutor;
    private final ExecutorService lexiconLookupResponseWorkerExecutor;
    private final int requestQueueSize = 100000;
    private final int responseQueueSize = 1000;

    public static void main(String[] args) throws Exception {

        String apiKey = "4c775d38fe2d12c43d99858dd0130fa0";
        int numProducerThreads = 200;
        String neo4jDbName = "/Users/fredriko/mcdonalds-1";
        int maxDepth = 4;

        LexiconDbPopulator populator = new LexiconDbPopulator(apiKey, numProducerThreads, neo4jDbName, maxDepth);
        populator.start();

        populator.addLookupRequest(new LookupRequest("mc donalds", "sv"));
        //populator.addLookupRequest(new LookupRequest("ignorance is bliss", "en"));
        //populator.addLookupRequest(new LookupRequest("data science", "en"));

        // mac and cheese
        //populator.addLookupRequest(new LookupRequest("stockholm", "sv"));
        //populator.addLookupRequest(new LookupRequest("oslo", "no"));
        //populator.addLookupRequest(new LookupRequest("københavn", "da"));
        //populator.addLookupRequest(new LookupRequest("helsinki", "fi"));
        //populator.addLookupRequest(new LookupRequest("berlin", "de"));
        //populator.addLookupRequest(new LookupRequest("paris", "fr"));
        //populator.addLookupRequest(new LookupRequest("london", "en"));
        //populator.addLookupRequest(new LookupRequest("roma", "it"));

        //populator.addLookupRequest(new LookupRequest("fredagsmys", "sv"));
        //populator.addLookupRequest(new LookupRequest("up the ante", "en"));
        //populator.addLookupRequest(new LookupRequest("donald trump", "en"));
        //populator.addLookupRequest(new LookupRequest("hillary clinton", "en"));

        //populator.addLookupRequest(new LookupRequest("apple", "en"));
        //populator.addLookupRequest(new LookupRequest("pivotal moment", "en"));
        //populator.addLookupRequest(new LookupRequest("reach out", "en"));
        //populator.addLookupRequest(new LookupRequest("help me understand", "en"));
        //populator.addLookupRequest(new LookupRequest("leverage", "en"));
        //populator.addLookupRequest(new LookupRequest("best practice", "en"));
        //populator.addLookupRequest(new LookupRequest("paradigm shift", "en"));
        //populator.addLookupRequest(new LookupRequest("back to back", "en"));
        //populator.addLookupRequest(new LookupRequest("non-disclosure agreement", "en"));
        //populator.addLookupRequest(new LookupRequest("service level agreement", "en"));
        //populator.addLookupRequest(new LookupRequest("touch base", "en"));
        //populator.addLookupRequest(new LookupRequest("go to market", "en"));
        //populator.addLookupRequest(new LookupRequest("sales revenue", "en"));

        Thread.sleep(1000000000);
    }

    public LexiconDbPopulator(String apiKey, int numProducerThreads, String neo4jDbName, int maxDepth) {
        this.apiKey = apiKey;
        this.numProducerThreads = numProducerThreads;
        this.maxDepth = maxDepth;
        this.neo4jDbName = neo4jDbName;
        this.lookupRequestQueue = new LinkedBlockingQueue<LookupRequest>(getRequestQueueSize());
        this.lookupResponseQueue = new LinkedBlockingQueue<LookupResponse>(getResponseQueueSize());
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

    public void start() {
        logger.info("Starting LexiconDbPopulator");
        startLexiconLookupRequestWorkers(getNumProducerThreads(), getLexiconLookupRequestWorkerExecutor());
        logger.info("Starting a single LexiconLookupResponseWorker");
        getLexiconLookupResponseWorkerExecutor().execute(
                new LexiconLookupResponseWorker(
                        getLookupRequestQueue(),
                        getLookupResponseQueue(),
                        getMaxDepth(),
                        getNeo4jDbName()));
    }

    public void addLookupRequest(LookupRequest request) {
        getLookupRequestQueue().add(request);
    }

    public int getNumProducerThreads() {
        return numProducerThreads;
    }

    public String getApiKey() {
        return apiKey;
    }

    public BlockingQueue<LookupRequest> getLookupRequestQueue() {
        return lookupRequestQueue;
    }

    public BlockingQueue<LookupResponse> getLookupResponseQueue() {
        return lookupResponseQueue;
    }

    public ExecutorService getLexiconLookupRequestWorkerExecutor() {
        return lexiconLookupRequestWorkerExecutor;
    }

    public ExecutorService getLexiconLookupResponseWorkerExecutor() {
        return lexiconLookupResponseWorkerExecutor;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getRequestQueueSize() {
        return requestQueueSize;
    }

    public int getResponseQueueSize() {
        return responseQueueSize;
    }

    public String getNeo4jDbName() {
        return neo4jDbName;
    }

    protected void startLexiconLookupRequestWorkers(int numThreads, ExecutorService service) {
        logger.info("Starting {} LexiconLookupRequestWorkers", numThreads);
        for (int i = 0; i < numThreads; i++) {
            service.execute(
                    new LexiconLookupRequestWorker(getLookupRequestQueue(), getLookupResponseQueue(), getApiKey()));
        }
    }

}
