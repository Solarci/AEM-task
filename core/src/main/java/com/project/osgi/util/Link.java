package main.java.com.project.osgi.util;

public class Link {
    private final String url;
    private final String path;

    public Link(String url, String path) {
        this.url = url;
        this.path = path;
    }

    public String getUrl() {
        return url;
    }

    public String getPath() {
        return path;
    }
}