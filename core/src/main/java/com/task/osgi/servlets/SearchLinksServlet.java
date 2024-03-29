package main.java.com.task.osgi.servlets;

import acscommons.com.google.common.collect.Iterators;
import com.day.cq.wcm.api.Page;
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
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Objects;

@Component(service = Servlet.class, property = {
        ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/myproject/searchlinks",
        ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
})
public class SearchLinksServlet extends SlingSafeMethodsServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchLinksServlet.class);
    private static final String PATH_PARAMETER = "path";
    private static final String URL_PARAMETER = "url";
    private static final String PAGE_PARAMETER = "page";
    private static final String DATA_PARAMETER = "data";
    private static final int PAGE_SIZE = 2;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String path = request.getParameter(PATH_PARAMETER);
        int page = Integer.parseInt(request.getParameter(PAGE_PARAMETER));
        if (StringUtils.isEmpty(path) || page <= 0) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(null)) {
            Resource searchResource = resourceResolver.resolve(path);
            if (ResourceUtil.isNonExistingResource(searchResource)) {
                response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
                LOGGER.info(MessageFormat.format("Resource not found: {0}", path));
                return;
            }
            JSONArray jsonArray = new JSONArray();
            Page pageObj = searchResource.adaptTo(Page.class);
            if (Objects.isNull(pageObj)) {
                response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
                LOGGER.info(MessageFormat.format("Page not found: {0}", path));
                return;
            }

            Iterator<Page> children = pageObj.listChildren();
            int count = 0;
            int startIndex = (page - 1) * PAGE_SIZE;
            int endIndex = page * PAGE_SIZE;
            int totalChildren = Iterators.size(pageObj.listChildren());
            int totalPages = (int) Math.ceil((double) totalChildren / PAGE_SIZE);

            while (children.hasNext() && count < endIndex) {
                Page child = children.next();
                if (count >= startIndex) {
                    String childPath = child.getPath();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(URL_PARAMETER, resourceResolver.map(childPath));
                    jsonObject.put(PATH_PARAMETER, childPath);
                    jsonArray.put(jsonObject);
                }
                count++;
            }

            JSONObject jsonResponse = new JSONObject();
            response.setHeader("X-Total-Pages", String.valueOf(totalPages));
            jsonResponse.put(DATA_PARAMETER, jsonArray);
            response.setContentType("application/json");
            response.getWriter().print(jsonArray);

        } catch (LoginException e) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            LOGGER.warn(MessageFormat.format("Unable to obtain resource resolver: {0} ", path));

        } catch (JSONException e) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            LOGGER.warn(MessageFormat.format("Error occurred during parsing from JSON: {0} ", path));
        }
    }
}