package com.library.api.search.engine.google;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * http://code.google.com/apis/ajaxsearch/documentation/#fonje
 * <p/>
 * News search examples:
 * http://ajax.googleapis.com/ajax/services/search/news?v=1.0&geo=Portland+Oregon
 * http://ajax.googleapis.com/ajax/services/search/news?v=1.0&geo=97202
 * http://ajax.googleapis.com/ajax/services/search/news?v=1.0&geo=Singapore
 * <p/>
 * Local search example:
 * http://ajax.googleapis.com/ajax/services/search/local?v=1.0&q=coffee&sll=45.521694,-122.691806&mrt=localonly
 * <p/>
 * Web search example:
 * http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=google+gson
 * <p/>
 * Book search example:
 * http://ajax.googleapis.com/ajax/services/search/books?v=1.0&q=economy
 * <p/>
 * Image search example:
 * http://ajax.googleapis.com/ajax/services/search/images?v=1.0&q=lolcats
 * <p/>
 * Video search example:
 * http://ajax.googleapis.com/ajax/services/search/video?v=1.0&q=lolcats&orderBy=order-by-date
 * <p/>
 * Blog search example:
 * http://ajax.googleapis.com/ajax/services/search/blogs?v=1.0&q=lolcats
 */
public class Client {

    private HttpClient httpClient;
    private static final String NEWS_SEARCH_ENDPOINT = "http://ajax.googleapis.com/ajax/services/search/news";
    private static final String LOCAL_SEARCH_ENDPOINT = "http://ajax.googleapis.com/ajax/services/search/local";
    private static final String WEB_SEARCH_ENDPOINT = "http://ajax.googleapis.com/ajax/services/search/web";
    private static final String BOOK_SEARCH_ENDPOINT = "http://ajax.googleapis.com/ajax/services/search/books";
    private static final String IMAGE_SEARCH_ENDPOINT = "http://ajax.googleapis.com/ajax/services/search/images";
    private static final String VIDEO_SEARCH_ENDPOINT = "http://ajax.googleapis.com/ajax/services/search/videos";
    private static final String BLOG_SEARCH_ENDPOINT = "http://ajax.googleapis.com/ajax/services/search/blogs";

    private boolean compressionEnabled = false;

    static public Gson createGson() {
        GsonBuilder builder = new GsonBuilder();
        // builder.setFieldNamingPolicy(namingConvention)
        // builder.registerTypeAdapter(type, typeAdapter);
        Gson gson = builder.create();
        return gson;
    }

    public Client() {
        this(new DefaultHttpClient());
    }

    public Client(HttpClient hClient) {
        this.httpClient = hClient;

        //
        //  this user agent string has been crafted this way
        //  so that Google's service will return gzip compressed responses
        //  when Accept-Encoding: gzip is present in the request.
        //
        setUserAgent("Mozilla/5.0 (Java) Gecko/20081007 gsearch-java-client");
        setConnectionTimeout(10 * 1000);
        setSocketTimeout(25 * 1000);
    }

    public void setUserAgent(String ua) {
        this.httpClient.getParams().setParameter(AllClientPNames.USER_AGENT, ua);
    }

    public void setConnectionTimeout(int milliseconds) {
        httpClient.getParams().setIntParameter(AllClientPNames.CONNECTION_TIMEOUT, milliseconds);
    }

    public void setSocketTimeout(int milliseconds) {
        httpClient.getParams().setIntParameter(AllClientPNames.SO_TIMEOUT, milliseconds);
    }

    protected Response sendSearchRequest(String url, Map<String, String> params) {

        if (params.get("v") == null) {
            params.put("v", "1.0");
        }

        String json = sendHttpRequest("GET", url, params);
        Response r = fromJson(json);
        r.setJson(json);
        return r;
    }

    protected Response fromJson(String json) {
        Gson gson = createGson();
        Response r = gson.fromJson(json, Response.class);
        return r;
    }

    protected String sendHttpRequest(String httpMethod, String url, Map<String, String> params) {
        HttpClient c = getHttpClient();
        HttpUriRequest request = null;

        if ("GET".equalsIgnoreCase(httpMethod)) {

            String queryString = buildQueryString(params);
            url = url + queryString;
            System.out.println(url);
            request = new HttpGet(url);
        } else {
            throw new RuntimeException("unsupported method: " + httpMethod);
        }

        HttpResponse response = null;
        HttpEntity entity = null;

        try {
            response = c.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {
                throw new RuntimeException("unexpected HTTP response status code = " + statusCode);
            }

            entity = response.getEntity();
            return EntityUtils.toString(entity);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            // todo :
        }
    }

    private String buildQueryString(Map<String, String> params) {
        StringBuffer query = new StringBuffer();

        if (params.size() > 0) {
            query.append("?");

            for (String key : params.keySet()) {
                query.append(key);
                query.append("=");
                query.append(encodeParameter(params.get(key)));
                query.append("&");
            }

            if (query.charAt(query.length() - 1) == '&') {
                query.deleteCharAt(query.length() - 1);
            }
        }
        return query.toString();
    }

    protected String decode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected String encodeParameter(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected HttpClient getHttpClient() {

        if (this.httpClient instanceof DefaultHttpClient) {
            DefaultHttpClient defaultClient = (DefaultHttpClient) httpClient;

            HttpHost proxy = new HttpHost("proxyv.dpn.deere.com", 3129, "http");

            defaultClient.getCredentialsProvider().setCredentials(
                    new AuthScope("proxyv.dpn.deere.com", 3129),
                    new UsernamePasswordCredentials("pp63680", "pwd")
            );
            defaultClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
/*
            defaultClient.removeRequestInterceptorByClass(GzipRequestInterceptor.class);
            defaultClient.removeResponseInterceptorByClass(GzipResponseInterceptor.class);

            if (this.isCompressionEnabled()) {
                defaultClient.addRequestInterceptor(GzipRequestInterceptor.getInstance());
                defaultClient.addResponseInterceptor(GzipResponseInterceptor.getInstance());
            }
*/
        }

        return this.httpClient;
    }

    /**
     * @param location use "city, state" (example: "Miami, FL") or zip code  ("97202") or country ("Singapore")
     * @return
     */
    public List<Result> searchNewsByLocation(String location) {
        return searchNews(null, location, null);
    }

    /**
     * @param query    may be null
     * @param location use "city, state" (example: "Miami, FL") or zip code  ("97202") or country ("Singapore")
     * @param topic    may be null
     * @return
     */

    public List<Result> searchNews(String query, String location, NewsTopic topic) {
        Map<String, String> params = new LinkedHashMap<String, String>();

        if ((query != null) && (query.trim().length() > 0)) {
            params.put("q", query);
        }

        if (location != null) {
            params.put("geo", location);
        }

        if (topic != null) {
            params.put("topic", topic.getCode());
        }

        Response r = sendNewsSearchRequest(params);
        return r.getResponseData().getResults();
    }


    protected Response sendNewsSearchRequest(Map<String, String> params) {
        return sendSearchRequest(NEWS_SEARCH_ENDPOINT, params);
    }

    protected Response sendLocalSearchRequest(Map<String, String> params) {
        return sendSearchRequest(LOCAL_SEARCH_ENDPOINT, params);
    }

    protected Response sendWebSearchRequest(Map<String, String> params) {
        return sendSearchRequest(WEB_SEARCH_ENDPOINT, params);
    }

    protected Response sendBookSearchRequest(Map<String, String> params) {
        return sendSearchRequest(BOOK_SEARCH_ENDPOINT, params);
    }

    protected Response sendImageSearchRequest(Map<String, String> params) {
        return sendSearchRequest(IMAGE_SEARCH_ENDPOINT, params);
    }

    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    public void setCompressionEnabled(boolean b) {
        this.compressionEnabled = b;
    }

    /**
     * send HTTP GET
     * <p/>
     * This method can be used to retrieve images  (JPEG, PNG, GIF)
     * or any other file type
     *
     * @return byte array
     */

    public byte[] getBytesFromUrl(String url) {
        try {
            HttpGet get = new HttpGet(url);
            HttpResponse response = this.getHttpClient().execute(get);
            HttpEntity entity = response.getEntity();

            if (entity == null) {
                throw new RuntimeException("response body was empty");
            }

            return EntityUtils.toByteArray(entity);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public String get(String url) {
        try {
            HttpGet get = new HttpGet(url);
            HttpResponse response = this.getHttpClient().execute(get);
            HttpEntity entity = response.getEntity();

            if (entity == null) {
                throw new RuntimeException("response body was empty");
            }

            return EntityUtils.toString(entity);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<Result> searchNews(NewsTopic topic) {
        return searchNews(null, null, topic);
    }

    public List<Result> searchWeb(String query) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("q", query);
        Response r = sendWebSearchRequest(params);
        return r.getResponseData().getResults();
    }

    public List<Result> searchBooks(String query) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("q", query);
        Response r = sendBookSearchRequest(params);
        return r.getResponseData().getResults();
    }

    public List<Result> searchImages(String query) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("q", query);
        Response r = sendImageSearchRequest(params);
        return r.getResponseData().getResults();
    }

    public List<Result> searchLocal(double lat, double lon, String query) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("sll", lat + "," + lon);
        params.put("mrt", "localonly");

        if (query != null) {
            params.put("q", query);
        }

        Response r = sendLocalSearchRequest(params);
        return r.getResponseData().getResults();
    }

    public List<Result> searchVideos(String query, OrderBy order) {
        Map<String, String> params = new LinkedHashMap<String, String>();

        params.put("q", query);

        if (order == null) {
            order = OrderBy.RELEVANCE;
        }

        params.put("orderBy", order.getValue());
        Response r = sendVideoSearchRequest(params);
        return r.getResponseData().getResults();
    }

    protected Response sendVideoSearchRequest(Map<String, String> params) {
        return sendSearchRequest(VIDEO_SEARCH_ENDPOINT, params);
    }

    public List<Result> searchBlogs(String query) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("q", query);
        Response r = sendBlogSearchRequest(params);
        return r.getResponseData().getResults();
    }

    protected Response sendBlogSearchRequest(Map<String, String> params) {
        return sendSearchRequest(BLOG_SEARCH_ENDPOINT, params);
    }

    public void googleSearch(String text) throws IOException {
        String google = WEB_SEARCH_ENDPOINT + "?v=1.0&q=";
        String charset = "UTF-8";

        System.out.println("HTTP_PROXY : " + System.getenv("HTTP_PROXY"));
        System.setProperty("http.proxyHost", "proxy.url.com");
        System.setProperty("http.proxyPort", "80");

        URL url = new URL(google + URLEncoder.encode(text, charset));
        Reader reader = new InputStreamReader(url.openStream(), charset);
        GoogleResults results = new Gson().fromJson(reader, GoogleResults.class);

        // Show title and URL of 1st result.
        List<GoogleResults.Result> resultList = results.getResponseData().getResults();
        for (GoogleResults.Result result : resultList) {
            System.out.println(result.getTitle());
            System.out.println(result.getUrl());
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        List<Result> results = client.searchWeb("java 8 features");
        for (Result result : results) {
            System.out.println(result);
        }
    }
}
