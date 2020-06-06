package com.library.api.search.engine.google;

public class Result {

    private String title;
    private String unescapedUrl;
    private String clusterUrl;
    private String url;
    private String publisher;
    private String publishedDate;
    private String location;
    private String content;
    private Image image;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUnescapedUrl() {
        return unescapedUrl;
    }

    public void setUnescapedUrl(String unescapedUrl) {
        this.unescapedUrl = unescapedUrl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String p) {
        this.publisher = p;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }


    public String getLocation() {
        return location;
    }

    public void setLocation(String loc) {
        this.location = loc;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image im) {
        this.image = im;
    }


    public String getClusterUrl() {
        return clusterUrl;
    }

    public void setClusterUrl(String u) {
        this.clusterUrl = u;
    }


    public String getContent() {
        return content;
    }

    public void setContent(String c) {
        this.content = c;
    }

    public String toString() {
        return this.getTitle() + "\n"
                + this.getUnescapedUrl() + "\n"
                + this.getPublisher() + "\n"
                + this.getPublishedDate() + "\n"
                + this.getLocation() + "\n"
                + ((this.getImage() == null) ? "no image" : "Image url: " + this.getImage().getUrl());
    }
}
