package se.fredrikolsson.gavagai;

/**
 *
 */
class LookupRequest {

    private final String term;
    private final String languageCode;
    private int depth;

    LookupRequest(String term, String languageCode) {
        this.term = term;
        this.languageCode = languageCode;
        setDepth(0);
    }

    LookupRequest(String term, String languageCode, int currentDepth) {
        this(term, languageCode);
        setDepth(currentDepth);
    }

    String getTerm() {
        return term;
    }

    String getLanguageCode() {
        return languageCode;
    }

    int getDepth() {
        return depth;
    }

    private void setDepth(int depth) {
        this.depth = depth;
    }

    public String toString() {
        return "LookupRequest[" +
                "term=" +
                getTerm() +
                ", depth=" +
                getDepth() +
                "]";
    }
}
