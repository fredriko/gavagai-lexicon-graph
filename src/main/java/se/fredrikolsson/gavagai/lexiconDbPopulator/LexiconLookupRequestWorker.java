package se.fredrikolsson.gavagai.lexiconDbPopulator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

/**
 * TODO targetTerm must be url encoded to accomodate for e.g. the slash in "sir/madam"
 */
public class LexiconLookupRequestWorker implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(LexiconLookupRequestWorker.class);

    private final BlockingQueue<LookupRequest> lookupRequestQueue;
    private final BlockingQueue<LookupResponse> lookupResponseQueue;
    private final LexiconApiClient lexiconApiClient;
    private boolean isRunning;

    public LexiconLookupRequestWorker(BlockingQueue<LookupRequest> lookupRequestQueue, BlockingQueue<LookupResponse> lookupResponseQueue, String apiKey) {
        this.lookupRequestQueue = lookupRequestQueue;
        this.lookupResponseQueue = lookupResponseQueue;
        this.lexiconApiClient = new LexiconApiClient(apiKey);
        setRunning(true);
    }

    @Override
    public void run() {
        logger.info("Starting to run");
        while(isRunning()) {
            LookupRequest request = null;
            try {
                request = getLookupRequestQueue().take();
                if (request != null) {
                    long start = System.currentTimeMillis();
                    JSONObject rawResponse = getLexiconApiClient().process(request.getTerm(), request.getLanguageCode());
                    // Continue processing a response only if there is useful information in it
                    if (rawResponse.get("semanticallySimilarWordFilaments") != null
                            && ((JSONArray) rawResponse.get("semanticallySimilarWordFilaments")).length() > 0) {
                        LookupResponse response =
                                new LookupResponse(rawResponse, request.getDepth() + 1, request.getLanguageCode(), request.getTerm());
                        getLookupResponseQueue().put(response);
                        logger.info("Term: \"{}\" - got {} similar terms in {} milliseconds",
                                request.getTerm(), response.getSemanticallySimilarTerms().size(), System.currentTimeMillis() - start);
                    } else {
                        logger.warn("Found nothing in the lexicon for term: {}", request.getTerm());
                    }
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted! Aborting processing: {}", e.getMessage(), e);
                setRunning(false);
            } catch (Exception e) {
                if (request != null) {
                    logger.error("Caught exception: {}. Re-adding request for term \"{}\" to queue for later processing",
                            e.getMessage(), request.getTerm() != null ? request.getTerm() : "<undefined>");
                    getLookupRequestQueue().add(request);
                }
            }
        }
        logger.info("Exiting run method");
    }

    public BlockingQueue<LookupRequest> getLookupRequestQueue() {
        return lookupRequestQueue;
    }

    public BlockingQueue<LookupResponse> getLookupResponseQueue() {
        return lookupResponseQueue;
    }

    public LexiconApiClient getLexiconApiClient() {
        return lexiconApiClient;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }
}
