package main.java.com.project.osgi.services;

import main.java.com.project.osgi.util.Link;
import org.apache.sling.api.resource.Resource;

import java.util.List;

public interface SearchLinksService {
    List<Link> searchLinksRecursively(Resource parentResource, String link, List<Link> links);

}
