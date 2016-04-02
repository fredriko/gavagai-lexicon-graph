package se.fredrikolsson.gavagai;

import org.apache.http.impl.execchain.RequestAbortedException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

/**
 */
class LexiconLookupRequestWorker implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(LexiconLookupRequestWorker.class);

    private final BlockingQueue<LookupRequest> lookupRequestQueue;
    private final BlockingQueue<LookupResponse> lookupResponseQueue;
    private final LexiconApiClient lexiconApiClient;
    private boolean isRunning;

    LexiconLookupRequestWorker(BlockingQueue<LookupRequest> lookupRequestQueue, BlockingQueue<LookupResponse> lookupResponseQueue, String apiKey) {
        this.lookupRequestQueue = lookupRequestQueue;
        this.lookupResponseQueue = lookupResponseQueue;
        this.lexiconApiClient = new LexiconApiClient(apiKey);
        setRunning(true);
    }

    @Override
    public void run() {
        while (isRunning()) {
            LookupRequest request = null;
            try {
                request = getLookupRequestQueue().take();
                if (request != null) {
                    JSONObject rawResponse = getLexiconApiClient().process(request.getTerm(), request.getLanguageCode());
                    // Continue processing a response only if there is useful information in it
                    if (rawResponse.get("semanticallySimilarWordFilaments") != null
                            && ((JSONArray) rawResponse.get("semanticallySimilarWordFilaments")).length() > 0) {
                        LookupResponse response =
                                new LookupResponse(rawResponse, request.getDistance() + 1, request.getLanguageCode(), request.getTerm());
                        getLookupResponseQueue().put(response);
                        logger.info("At distance {}, got {} similar terms for \"{}\"", request.getDistance(), response.getSemanticallySimilarTerms().size(), request.getTerm());
                    } else {
                        logger.info("Got no similar terms for \"{}\"", request.getTerm());
                    }
                    Thread.sleep(100);
                }
            } catch (RequestAbortedException e) {
                // We never end up here, despite this exception type being thrown when hitting ctrl-c and the lexicon
                // client is aborted. Why? In those cases, we should not treat this as an 'ordinary' exception and re-add
                // the request to the queue. We should abort.
                logger.debug("Honoring abort request. Aborting processing.");
                setRunning(false);
            } catch (InterruptedException e) {
                logger.debug("Interrupted! Aborting processing.");
                setRunning(false);
            } catch (Exception e) {
                if (request != null && request.getNumberOfLookupAttempts() < 3) {
                    logger.warn("Caught exception: {}. Re-adding request for term \"{}\" to queue for later processing",
                            e.getMessage(), request.getTerm() != null ? request.getTerm() : "<undefined>");
                    request.increaseNumberOfLookupAttempts();
                    getLookupRequestQueue().add(request);
                } else if (request != null) {
                    logger.error("Dropping lookup request for term \"{}\" due to too many re-tries.", request.getTerm());
                }
            }
        }
        logger.debug("Exiting run method");
    }

    private BlockingQueue<LookupRequest> getLookupRequestQueue() {
        return lookupRequestQueue;
    }

    private BlockingQueue<LookupResponse> getLookupResponseQueue() {
        return lookupResponseQueue;
    }

    private LexiconApiClient getLexiconApiClient() {
        return lexiconApiClient;
    }

    private boolean isRunning() {
        return isRunning;
    }

    private void setRunning(boolean running) {
        isRunning = running;
    }
}
