package models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


public class SearchResponseModel {


    @JsonProperty("keyword")
    private String keyword;

    @JsonProperty("results")
    private List<SearchResultModel> results;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private long timestamp;

    public SearchResponseModel() {
        this.timestamp = System.currentTimeMillis();
    }

    public SearchResponseModel(String keyword, List<SearchResultModel> results, boolean success, String message) {
        this.keyword = keyword;
        this.results = results;
        this.success = success;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<SearchResultModel> getResults() {
        return results;
    }

    public void setResults(List<SearchResultModel> results) {
        this.results = results;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
