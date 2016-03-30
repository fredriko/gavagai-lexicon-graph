package se.fredrikolsson.gavagai;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

/**
 * Class responsible for building a Neo4j graph database from entries in the Gavagai Living Lexicon.
 * <p>
 *     TODO set reasonable times for stopping criterion
 * TODO and print statistics: how many terms persisted, how many api calls made
 * TODO clean up logging: with the perspective of the console
 * <p>
 * TODO cannot handle / in target term: need url encoding of that particular case? Write JIRA!
 * TODO add index to neo4j db to make searching for terms faster
 * TODO change to custom http client instead of unirest since it has problems with connection leaks (not consuming http entity)
 */
class GraphCreator implements Stoppable {

    private static Logger logger = LoggerFactory.getLogger(GraphCreator.class);

    private final static int REQUEST_QUEUE_SIZE = 100000;
    private final static int RESPONSE_QUEUE_SIZE = 1000;
    private final static int NUM_PRODUCER_THREADS = 100;
    private final static int DEFAULT_MAX_DISTANCE = 2;

    private final int maxDistance;
    private final String apiKey;
    private final String neo4jDbName;
    private final BlockingQueue<LookupRequest> lookupRequestQueue;
    private final BlockingQueue<LookupResponse> lookupResponseQueue;
    private final ExecutorService lexiconLookupRequestWorkerExecutor;
    private final ExecutorService lexiconLookupResponseWorkerExecutor;
    private final ScheduledExecutorService stopperExecutor;

    private boolean isRunning;

    public static void main(String[] args) throws Exception {

        OptionSet options = null;
        try {
            options = new OptionParser("a:d:m:l:t:h").parse(args);
        } catch (Throwable t) {
            System.err.println("\nError: " + t.getMessage() + ". Exiting.\n");
            GraphCreator.printUsage();
            System.exit(1);
        }

        if (options.has("h") || !(options.has("a") && options.has("d") && options.has("l") && options.has("t"))) {
            GraphCreator.printUsage();
            System.exit(1);
        }

        GraphCreator populator = new GraphCreator(
                (String) options.valueOf("a"),
                (String) options.valueOf("d"),
                options.has("m") ? Integer.valueOf((String) options.valueOf("m")) : DEFAULT_MAX_DISTANCE);

        populator.start();
        Runtime.getRuntime().addShutdownHook(new ShutDownHook(populator));
        for (String term : (List<String>) options.valuesOf("t")) {
            populator.addLookupRequest(new LookupRequest(term, (String) options.valueOf("l")));
        }
        populator.awaitCompletion();
        logger.info("Exiting main program");
    }

    private static void printUsage() {
        String s = "Usage:\n" +
                "  -a <apiKey> -d <dBDir> -l <lang> -t <term> (-m <maxDistance>)\n" +
                "  -h\n\n" +
                "where  -a <apiKey> is your Gavagai API key, obtained from gavagai.se\n" +
                "       -d <dBDir>  is the empty directory in which to store the resulting Neo4j graph database\n" +
                "       -l <lang>   is the iso 639-1 two character code for the langugage to look up. Check\n" +
                "                   http://lexicon.gavagai.se for available languages\n" +
                "       -t <term>   is the term from which you wish to start your graph. This option can be\n" +
                "                   specified multiple times to generate a graph with many starting terms\n" +
                "       -m <dist>   is the maximum distance, in the graph, allowed from a starting term before\n" +
                "                   the program terminates. Optional. Default value is " + DEFAULT_MAX_DISTANCE + "\n" +
                "       -h          prints this usage information\n";

        System.out.println(s);
    }

    private GraphCreator(String apiKey, String neo4jDbName, int maxDistance) {
        this.apiKey = apiKey;
        this.maxDistance = maxDistance;
        this.neo4jDbName = neo4jDbName;
        this.lookupRequestQueue = new LinkedBlockingQueue<>(getRequestQueueSize());
        this.lookupResponseQueue = new LinkedBlockingQueue<>(getResponseQueueSize());
        this.lexiconLookupRequestWorkerExecutor =
                new ThreadPoolExecutor(
                        NUM_PRODUCER_THREADS,
                        NUM_PRODUCER_THREADS,
                        0L,
                        TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>(),
                        new NamingThreadFactory("requestWorker"));
        this.lexiconLookupResponseWorkerExecutor = Executors.newSingleThreadExecutor();
        this.stopperExecutor = Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory("stopper"));
    }

    private void start() {
        logger.info("Starting Graph Creator");
        startLexiconLookupRequestWorkers(getNumProducerThreads(), getLexiconLookupRequestWorkerExecutor());
        logger.info("Starting a single Lexicon Lookup Response Worker");

        getLexiconLookupResponseWorkerExecutor().execute(
                new LexiconLookupResponseWorker(
                        getLookupRequestQueue(),
                        getLookupResponseQueue(),
                        getMaxDistance(),
                        getNeo4jDbName()));

        logger.info("Starting the Stopper watchdog");
        getStopperExecutor().scheduleAtFixedRate(new Stopper(this, getLookupRequestQueue()), 20, 10, TimeUnit.SECONDS);

        setRunning(true);
    }

    private void awaitCompletion() {
        while (isRunning()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.debug("Done waiting for completion");
                break;
            }
        }
    }

    private void addLookupRequest(LookupRequest request) {
        logger.info("Adding term to lookup in Gavagai Living Lexicon: \"{}\"", request.getTerm());
        getLookupRequestQueue().add(request);
    }

    private int getNumProducerThreads() {
        return NUM_PRODUCER_THREADS;
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

    private ScheduledExecutorService getStopperExecutor() {
        return stopperExecutor;
    }

    private int getMaxDistance() {
        return maxDistance;
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

    private boolean isRunning() {
        return isRunning;
    }

    private void setRunning(boolean running) {
        isRunning = running;
    }

    private void startLexiconLookupRequestWorkers(int numThreads, ExecutorService service) {
        logger.info("Starting {} Lexicon Lookup Request Workers", numThreads);
        for (int i = 0; i < numThreads; i++) {
            service.execute(
                    new LexiconLookupRequestWorker(getLookupRequestQueue(), getLookupResponseQueue(), getApiKey()));
        }
    }

    @Override
    public void stop() {
        getLexiconLookupRequestWorkerExecutor().shutdownNow();
        getLexiconLookupResponseWorkerExecutor().shutdownNow();
        getStopperExecutor().shutdownNow();
        setRunning(false);
    }

    private static class ShutDownHook extends Thread {
        private final Stoppable stoppable;
        ShutDownHook(Stoppable stoppable) {
            this.stoppable = stoppable;
        }

        @Override
        public void run() {
            getStoppable().stop();
        }

        Stoppable getStoppable() {
            return stoppable;
        }
    }

}
