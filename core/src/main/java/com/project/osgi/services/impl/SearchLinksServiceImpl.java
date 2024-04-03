package main.java.com.project.osgi.services.impl;

import main.java.com.project.osgi.services.SearchLinksService;
import main.java.com.project.osgi.util.Link;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(service = SearchLinksService.class)
public class SearchLinksServiceImpl implements SearchLinksService {
    private final static Map<String, String> COMPONENTS_MAP = new HashMap<>();

    static {
        COMPONENTS_MAP.put("project/components/content/text", "text");
        COMPONENTS_MAP.put("project/components/content/image-and-text", "text");
        COMPONENTS_MAP.put("project/components/content/richtext", "richtext");
    }

    @Override
    public List<Link> searchLinksRecursively(Resource parentResource, String link, List<Link> links) {
        if (parentResource != null) {
            for (Resource child : parentResource.getChildren()) {
                links.addAll(searchLinksUnderComponent(child, link));
                searchLinksRecursively(child, link, links);
            }
        }
        return links;
    }

    private List<Link> searchLinksUnderComponent(Resource resource, String link) {
        List<Link> linksUnderComponent = new ArrayList<>();
        String resourceType = resource.getResourceType();
        if (StringUtils.isNotEmpty(resourceType) && COMPONENTS_MAP.containsKey(resourceType)) {
            ValueMap properties = resource.getValueMap();
            if (properties.containsKey(COMPONENTS_MAP.get(resourceType))) {
                String property = properties.get(COMPONENTS_MAP.get(resourceType), String.class);
                if (StringUtils.isNotEmpty(property) && property.contains(link)) {
                    String regex = "<a\\s+(?:[^>]*?\\s+)?href=([\"'])(.*?)\\1";
                    Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(property);
                    while (matcher.find()) {
                        String url = matcher.group(2);
                        linksUnderComponent.add(new Link(url, resource.getPath()));
                    }
                }
            }
        }
        return linksUnderComponent;
    }
}