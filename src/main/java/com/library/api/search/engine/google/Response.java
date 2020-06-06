package com.library.api.search.engine.google;

import java.util.ArrayList;
import java.util.List;

public class Response {
    private String json;
    private ResponseData responseData;

    public void setJson(String j) {
        this.json = j;
    }

    public ResponseData getResponseData() {
        return responseData;
    }

    public void setResponseData(ResponseData responseData) {
        this.responseData = responseData;
    }

    public String getJson() {
        return json;
    }

    public static class ResponseData {
        // @SerializedName("results")
        private List<Result> results;

        public List<Result> getResults() {
            if (results == null) {
                results = new ArrayList<Result>();
            }
            return results;
        }

        public void setResults(List<Result> resultList) {
            this.results = resultList;
        }
    }
}