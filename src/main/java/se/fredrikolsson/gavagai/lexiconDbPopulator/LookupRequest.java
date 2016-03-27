package se.fredrikolsson.gavagai.lexiconDbPopulator;

/**
 *
 */
public class LookupRequest {

    private final String term;
    private final String languageCode;
    private int depth;

    public LookupRequest(String term, String languageCode) {
        this.term = term;
        this.languageCode = languageCode;
        setDepth(0);
    }

    public LookupRequest(String term, String languageCode, int currentDepth) {
        this(term, languageCode);
        setDepth(currentDepth);
    }

    public String getTerm() {
        return term;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String toString() {
        return new StringBuilder("LookupRequest[")
                .append("term=")
                .append(getTerm())
                .append(", depth=")
                .append(getDepth())
                .append("]")
                .toString();
    }
}
