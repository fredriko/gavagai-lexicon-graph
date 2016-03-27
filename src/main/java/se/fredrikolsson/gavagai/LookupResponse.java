package se.fredrikolsson.gavagai;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
class LookupResponse {
    private static Logger logger = LoggerFactory.getLogger(LookupResponse.class);

    private final JSONObject payload;
    private final int currentDepth;
    private final String languageCode;
    private String targetTerm;

    LookupResponse(JSONObject payload, int currentDepth, String languageCode, String targetTerm) {
        this.payload = payload;
        this.currentDepth = currentDepth;
        this.languageCode = languageCode;
        setTargetTerm(targetTerm);
    }

    JSONObject getPayload() {
        return payload;
    }

    String getLanguageCode() {
        return languageCode;
    }

    int getCurrentDepth() {
        return currentDepth;
    }

    private void setTargetTerm(String targetTerm) {
        this.targetTerm = targetTerm;
    }

    String getTargetTerm() {
        return this.targetTerm;
    }

    private JSONObject getWordInformation() throws JSONException {
        JSONObject result = null;
        if (getPayload() != null && getPayload().getJSONObject("wordInformation") != null) {
            result = getPayload().getJSONObject("wordInformation");
        }
        return result;
    }

    int getFrequency() throws JSONException {
        int result = 0;
        if (getWordInformation() != null) {
            result = getWordInformation().getInt("frequency");
        }
        return result;
    }

    int getDocumentFrequency() throws JSONException {
        int result = 0;
        if (getWordInformation() != null) {
            result = getWordInformation().getInt("documentFrequency");
        }
        return result;
    }

    int getAbsoluteRank() throws JSONException {
        int result = 0;
        if (getWordInformation() != null) {
            result = getWordInformation().getInt("absoluteRank");
        }
        return result;
    }

    double getRelativeRank() throws JSONException {
        double result = 0.0;
        if (getWordInformation() != null) {
            result = getWordInformation().getDouble("relativeRank");
        }
        return result;
    }

    List<String> getSemanticallySimilarTerms() throws JSONException {
        List<String> terms = new ArrayList<>();
        if (getPayload() == null) {
            return terms;
        }
        JSONArray semanticallySimilarWordFilaments = (JSONArray) getPayload().get("semanticallySimilarWordFilaments");
        if (semanticallySimilarWordFilaments == null) {
            return terms;
        }
        for (int i = 0; i < semanticallySimilarWordFilaments.length(); i++) {
            JSONArray words = (JSONArray) ((JSONObject) semanticallySimilarWordFilaments.get(i)).get("words");
            for (int j = 0; j < words.length(); j++) {
                terms.add((String) ((JSONObject) words.get(j)).get("word"));
            }
        }
        return terms;
    }

    public String toString() {
        return getPayload().toString();
    }
}
