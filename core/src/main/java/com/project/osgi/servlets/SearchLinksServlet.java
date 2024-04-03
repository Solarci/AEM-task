package main.java.com.project.osgi.servlets;

import com.day.cq.wcm.api.Page;
import main.java.com.project.osgi.services.SearchLinksService;
import main.java.com.project.osgi.util.Link;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Component(service = Servlet.class, property = {
        ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/project/searchlinks",
        ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
})
public class SearchLinksServlet extends SlingSafeMethodsServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchLinksServlet.class);
    private static final String PATH_PARAMETER = "path";
    private static final String URL_PARAMETER = "url";
    private static final String PAGE_PARAMETER = "page";
    private static final String LINK_PARAMETER = "link";
    private static final int PAGE_SIZE = 2;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private SearchLinksService searchLinksService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String searchPath = request.getParameter(PATH_PARAMETER);
        int page = Integer.parseInt(request.getParameter(PAGE_PARAMETER));
        if (StringUtils.isEmpty(searchPath) || page <= 0) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(null)) {
            Resource searchResource = resourceResolver.resolve(searchPath);
            if (ResourceUtil.isNonExistingResource(searchResource)) {
                response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
                LOGGER.info("Resource not found: {}", searchPath);
                return;
            }
            Page pageObj = searchResource.adaptTo(Page.class);
            if (Objects.isNull(pageObj)) {
                response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
                LOGGER.info("Page not found: {}", searchPath);
                return;
            }
            String link = request.getParameter(LINK_PARAMETER);

            if (StringUtils.isEmpty(link)) {
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            List<Link> links = searchLinksService.searchLinksRecursively(searchResource, link, new ArrayList<>());

            int count = 0;
            int startIndex = (page - 1) * PAGE_SIZE;
            int endIndex = page * PAGE_SIZE;
            int totalLinks = links.size();
            int totalPages = totalLinks > 0 ? (int) Math.ceil((double) totalLinks / PAGE_SIZE) : 1;

            JSONArray jsonArray = new JSONArray();
            while (count < links.size() && count < endIndex) {
                if (count >= startIndex) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(URL_PARAMETER, links.get(count).getUrl());
                    jsonObject.put(PATH_PARAMETER, links.get(count).getPath());
                    jsonArray.put(jsonObject);
                }
                count++;
            }

            response.setHeader("X-Total-Pages", String.valueOf(totalPages));
            response.setHeader("X-Total-Links", String.valueOf(totalLinks));
            response.setContentType("application/json");
            response.getWriter().print(jsonArray);

        } catch (LoginException e) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            LOGGER.warn("Unable to obtain resource resolver: {} ", searchPath);

        } catch (JSONException e) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            LOGGER.warn("Error occurred during parsing from JSON: {} ", searchPath);
        }
    }
}