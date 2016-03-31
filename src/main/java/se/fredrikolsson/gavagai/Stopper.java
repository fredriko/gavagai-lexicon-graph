package se.fredrikolsson.gavagai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

/**
 *
 */
class Stopper implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(Stopper.class);

    private final Stoppable stoppable;
    private final BlockingQueue<LookupRequest> lookupRequestsQueue;

    private boolean initializeShutdown = false;

    Stopper(Stoppable stoppable, BlockingQueue<LookupRequest> lookupRequestQueue) {
        this.stoppable = stoppable;
        this.lookupRequestsQueue = lookupRequestQueue;
    }

    @Override
    public void run() {
        int numRequests = getLookupRequestsQueue().size();
        if (isInitializeShutdown()) {
            if (numRequests == 0) {
                logger.info("No more Lexicon Lookup Requests available. Shutting down.");
                // Make sure to only call stop once
                getStoppable().stop();
            } else {
                setInitializeShutdown(false);
                logger.info("{} Lexicon Lookup Requests await processing. Revoking initiated stopping criterion.", numRequests);
            }
        } else {
            if (numRequests == 0) {
                logger.info("First stage of stopping criterion met. No Lexicon Lookup Requests await processing");
                setInitializeShutdown(true);
            } else {
                logger.info("{} Lexicon Lookup Requests await processing", numRequests);
            }
        }
    }

    private boolean isInitializeShutdown() {
        return initializeShutdown;
    }

    private void setInitializeShutdown(boolean initializeShutdown) {
        this.initializeShutdown = initializeShutdown;
    }

    private Stoppable getStoppable() {
        return stoppable;
    }

    private BlockingQueue<LookupRequest> getLookupRequestsQueue() {
        return lookupRequestsQueue;
    }

}
