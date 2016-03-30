package se.fredrikolsson.gavagai;

/**
 *
 */
class LookupRequest {

    private final String term;
    private final String languageCode;
    private int distance;

    LookupRequest(String term, String languageCode) {
        this.term = term;
        this.languageCode = languageCode;
        setDistance(0);
    }

    LookupRequest(String term, String languageCode, int currentDistance) {
        this(term, languageCode);
        setDistance(currentDistance);
    }

    String getTerm() {
        return term;
    }

    String getLanguageCode() {
        return languageCode;
    }

    int getDistance() {
        return distance;
    }

    private void setDistance(int distance) {
        this.distance = distance;
    }

    public String toString() {
        return "LookupRequest[" +
                "term=" +
                getTerm() +
                ", distance=" +
                getDistance() +
                "]";
    }
}
