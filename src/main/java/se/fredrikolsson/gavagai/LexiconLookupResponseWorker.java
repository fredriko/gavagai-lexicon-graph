package se.fredrikolsson.gavagai;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.BlockingQueue;


/**
 * Class responsible for processing a queue of responses obtained from Gavagai's semantic memories,
 * and persist them in Neo4j.
 * <p>
 * There will only be one instance of this class present in the application.
 */
class LexiconLookupResponseWorker implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(LexiconLookupResponseWorker.class);

    private final BlockingQueue<LookupRequest> lookupRequestQueue;
    private final BlockingQueue<LookupResponse> lookupResponseQueue;
    private final GraphDatabaseService neo4jDb;
    private final Map<String, Integer> lookupRequestsMadeForTerms;
    private final Map<String, Integer> termsPersisted;
    private boolean isRunning;
    private int maxDistance;


    LexiconLookupResponseWorker(
            BlockingQueue<LookupRequest> lookupRequestQueue,
            BlockingQueue<LookupResponse> lookupResponseQueue,
            int maxDistance,
            String dbPath) {

        this.lookupRequestQueue = lookupRequestQueue;
        this.lookupResponseQueue = lookupResponseQueue;
        this.neo4jDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(dbPath));
        this.lookupRequestsMadeForTerms = new TreeMap<>();
        this.termsPersisted = new TreeMap<>();

        setMaxDistance(maxDistance);
        setRunning(true);
    }


    void init() {
        setUpDbConstraint();
        setUpDbIndex();
    }


    @Override
    public void run() {
        while (isRunning()) {
            LookupResponse response;
            try {
                response = getLookupResponseQueue().take();
                if (response != null) {

                    createAddRequests(
                            response,
                            getMaxDistance(),
                            getLookupRequestQueue(),
                            getLookupRequestsMadeForTerms());

                    persistInDb(response);
                }
                Thread.sleep(10);
            } catch (InterruptedException e) {
                logger.debug("Interrupted! Aborting processing");
                shutDown();
            } catch (Exception e) {
                logger.error("Caught exception: {}", e.getMessage(), e);
            }
        }
        logger.debug("Exiting run method");
    }


    String getStatisticsMessage(boolean verbose) {
        StringBuilder s = new StringBuilder("Processed a total of ")
                .append(getTermsPersisted().size())
                .append(" unique terms");

        if (verbose) {
            for (Map.Entry<String, Integer> entry : getTermsPersisted().entrySet()) {
                s.append("\n").append(entry.getKey()).append(": ").append(entry.getValue());
            }
        }
        return s.toString();
    }


    private Map<String, Integer> getTermsPersisted() {
        return termsPersisted;
    }


    private void updateTermsPersisted(Map<String, Integer> termsPersisted, String term) {
        if (termsPersisted.containsKey(term)) {
            int c = termsPersisted.get(term);
            termsPersisted.put(term, ++c);
        } else {
            termsPersisted.put(term, 1);
        }
    }


    private void persistInDb(LookupResponse response) throws JSONException {
        try (Transaction tx = getNeo4jDb().beginTx()) {

            Node targetTerm = createTargetTermNode(response);

            JSONArray n = response.getPayload().getJSONArray("semanticallySimilarWordFilaments");
            for (int i = 0; i < n.length(); i++) {
                JSONArray labels = n.getJSONObject(i).getJSONArray("labels");
                String semanticLabel = createSemanticLabel(labels);
                JSONArray words = n.getJSONObject(i).getJSONArray("words");
                for (int j = 0; j < words.length(); j++) {
                    updateTermsPersisted(getTermsPersisted(), words.getJSONObject(j).getString("word"));
                    Node node = getOrCreateNode(getNeo4jDb(), words.getJSONObject(j).getString("word"));
                    node.addLabel(TermLabel.TERM);

                    Iterable<Relationship> relationships = targetTerm.getRelationships();
                    boolean shouldConnect = true;
                    for (Relationship relationship : relationships) {
                        if (relationship.getOtherNode(targetTerm).equals(node)
                                && relationship.hasProperty("semanticLabel")
                                && relationship.getProperty("semanticLabel").equals(semanticLabel)) {
                            shouldConnect = false;
                            break;
                        }
                    }
                    if (shouldConnect) {
                        Relationship relationship = targetTerm.createRelationshipTo(node, TermRelation.NEIGHBOR);
                        relationship.setProperty("semanticLabel", semanticLabel);
                        relationship.setProperty("strength", words.getJSONObject(j).getDouble("strength"));
                    }
                }
            }
            tx.success();
        }
    }


    private Node getOrCreateNode(GraphDatabaseService graphDb, String term) {
        Node result;
        ResourceIterator<Node> resultIterator;
        try (Transaction tx = graphDb.beginTx()) {
            String queryString = "MERGE (n:TERM {name: {name}}) RETURN n";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", term);
            resultIterator = graphDb.execute(queryString, parameters).columnAs("n");
            result = resultIterator.next();
            tx.success();
        }
        return result;
    }


    private String createSemanticLabel(JSONArray labels) throws JSONException {
        if (labels.length() == 0) {
            return "";
        }
        StringBuilder left = new StringBuilder();
        StringBuilder right = new StringBuilder();
        for (int i = 0; i < labels.length(); i++) {
            JSONObject label = labels.getJSONObject(i);
            if (label.getString("type").equalsIgnoreCase("LEFT")) {
                left.append(label.getString("label")).append(" | ");
            } else {
                right.append(label.getString("label")).append(" | ");
            }
        }
        if (left.length() > 0) {
            left.delete(left.length() - 3, left.length() - 1);
        }
        if (right.length() > 0) {
            right.delete(right.length() - 3, right.length() - 1);
        }
        left.append(" * ").append(right);
        return left.length() > 3 ? left.toString().trim() : "";
    }


    private Node createTargetTermNode(LookupResponse response) throws JSONException {
        Node targetTerm = getOrCreateNode(getNeo4jDb(), response.getTargetTerm());
        targetTerm.addLabel(TermLabel.TERM);
        if (!targetTerm.hasProperty("numTokens")) {
            targetTerm.setProperty("numTokens", computeNumWhitespaces(response.getTargetTerm()) + 1);
        }
        if (!targetTerm.hasProperty("frequency")) {
            targetTerm.setProperty("frequency", response.getFrequency());
        }
        if (!targetTerm.hasProperty("documentFrequency")) {
            targetTerm.setProperty("documentFrequency", response.getDocumentFrequency());
        }
        if (!targetTerm.hasProperty("absoluteRank")) {
            targetTerm.setProperty("absoluteRank", response.getAbsoluteRank());
        }
        if (!targetTerm.hasProperty("relativeRank")) {
            targetTerm.setProperty("relativeRank", response.getRelativeRank());
        }
        return targetTerm;
    }


    private void createAddRequests(
            LookupResponse response,
            int maxDistance,
            BlockingQueue<LookupRequest> lookupRequestQueue,
            Map<String, Integer> lookupRequestsMadeForTerms) throws JSONException {

        if (response.getCurrentDistance() <= maxDistance) {
            List<String> terms = response.getSemanticallySimilarTerms();
            for (String term : terms) {
                // Avoid issuing requests containing slash since a bug in the API prevents them from being fulfilled.
                if (term.contains("/")) {
                    continue;
                }
                if (!lookupRequestsMadeForTerms.containsKey(term)) {
                    lookupRequestQueue.add(new LookupRequest(term, response.getLanguageCode(), response.getCurrentDistance()));
                    lookupRequestsMadeForTerms.put(term, 1);
                } else {
                    Integer c = lookupRequestsMadeForTerms.get(term);
                    lookupRequestsMadeForTerms.put(term, ++c);
                }
            }
        } else {
            logger.debug("Not spawning new requests. Current distance: {}, max distance: {}",
                    response.getCurrentDistance(), maxDistance);
        }
    }


    private int computeNumWhitespaces(String input) {
        int numSpaces = 0;
        int i = 0;
        while (i < input.length()) {
            if (input.charAt(i) == ' ') {
                numSpaces++;
            }
            i++;
        }
        return numSpaces;
    }


    private void setUpDbConstraint() {
        try (Transaction tx = getNeo4jDb().beginTx()) {
            try {
                getNeo4jDb().schema()
                        .constraintFor(TermLabel.TERM)
                        .assertPropertyIsUnique("name")
                        .create();
            } catch (ConstraintViolationException e) {
                logger.warn("Database constraint already exists!");
            }
            tx.success();
        }
    }


    private void setUpDbIndex() {
        try (Transaction tx = getNeo4jDb().beginTx()) {
            Iterator<IndexDefinition> indices = getNeo4jDb().schema().getIndexes(TermLabel.TERM).iterator();
            if (indices == null || !indices.hasNext()) {
                try {
                    getNeo4jDb().schema().indexFor(TermLabel.TERM).on("name").create();
                } catch (ConstraintViolationException e) {
                    logger.warn("Index already defined for label: {}", TermLabel.TERM);
                }
            }
            tx.success();
        }
    }


    private GraphDatabaseService getNeo4jDb() {
        return neo4jDb;
    }


    private BlockingQueue<LookupRequest> getLookupRequestQueue() {
        return lookupRequestQueue;
    }


    private BlockingQueue<LookupResponse> getLookupResponseQueue() {
        return lookupResponseQueue;
    }


    private boolean isRunning() {
        return isRunning;
    }


    private void shutDown() {
        setRunning(false);
        getNeo4jDb().shutdown();
    }


    private void setRunning(boolean running) {
        isRunning = running;
    }


    private int getMaxDistance() {
        return maxDistance;
    }


    private void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
    }


    private Map<String, Integer> getLookupRequestsMadeForTerms() {
        return lookupRequestsMadeForTerms;
    }


    private enum TermLabel implements Label {
        TERM
    }


    private enum TermRelation implements RelationshipType {
        NEIGHBOR
    }

}
