package se.fredrikolsson.gavagai;

/**
 * Class holding the information required when looking up a term in Gavagai's semantic memories.
 */
class LookupRequest {

    private final String term;
    private final String languageCode;
    private int distance;
    private int numberOfLookupAttempts = 0;


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


    int getNumberOfLookupAttempts() {
        return numberOfLookupAttempts;
    }


    void increaseNumberOfLookupAttempts() {
        this.numberOfLookupAttempts++;
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
