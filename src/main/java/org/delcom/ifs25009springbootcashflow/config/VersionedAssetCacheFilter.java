package org.delcom.ifs25009springbootcashflow.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class VersionedAssetCacheFilter extends OncePerRequestFilter {
    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";
    private static final String BUILD_ASSET_PATH = "/VAADIN/build/";
    private static final Pattern HASH_SUFFIX = Pattern.compile("(?=.*[A-Z0-9])[A-Za-z0-9_-]{8,}");
    private static final Pattern VERSION =
            Pattern.compile("[A-Za-z0-9_-]{8,}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean cacheable = isCacheable(request);
        if (!cacheable) {
            filterChain.doFilter(request, response);
            return;
        }

        CacheControlResponseWrapper cacheResponse = new CacheControlResponseWrapper(response);
        cacheResponse.setHeader("Cache-Control", CACHE_CONTROL);
        filterChain.doFilter(request, cacheResponse);

        if (response.getStatus() < HttpServletResponse.SC_BAD_REQUEST && !response.isCommitted()) {
            cacheResponse.setHeader("Cache-Control", CACHE_CONTROL);
        }
    }

    private static boolean isCacheable(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod())) {
            return false;
        }

        String contextPath = request.getContextPath();
        String path = request.getRequestURI().substring(contextPath.length());
        if (isHashedBuildAsset(path)) {
            return true;
        }

        String version = request.getParameter("v-c");
        return ("/styles.css".equals(path) || "/lumo/lumo.css".equals(path))
                && version != null
                && VERSION.matcher(version).matches();
    }

    private static boolean isHashedBuildAsset(String path) {
        if (!path.startsWith(BUILD_ASSET_PATH) || !(path.endsWith(".js") || path.endsWith(".css"))) {
            return false;
        }

        String fileName = path.substring(path.lastIndexOf('/') + 1);
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        int lastDash = baseName.lastIndexOf('-');
        int previousDash = lastDash < 0 ? -1 : baseName.lastIndexOf('-', lastDash - 1);
        String hash = baseName.substring(previousDash + 1);
        return HASH_SUFFIX.matcher(hash).matches();
    }

    private static class CacheControlResponseWrapper extends HttpServletResponseWrapper {
        private CacheControlResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setHeader(String name, String value) {
            if ("Cache-Control".equalsIgnoreCase(name)) {
                super.setHeader(name, getStatus() < HttpServletResponse.SC_BAD_REQUEST
                        ? CACHE_CONTROL
                        : "no-store");
            } else {
                super.setHeader(name, value);
            }
        }

        @Override
        public void addHeader(String name, String value) {
            if ("Cache-Control".equalsIgnoreCase(name)) {
                setHeader(name, value);
            } else {
                super.addHeader(name, value);
            }
        }

        @Override
        public void setStatus(int status) {
            super.setStatus(status);
            if (status >= HttpServletResponse.SC_BAD_REQUEST) {
                super.setHeader("Cache-Control", "no-store");
            }
        }

        @Override
        public void sendError(int status) throws IOException {
            super.setHeader("Cache-Control", "no-store");
            super.sendError(status);
        }

        @Override
        public void sendError(int status, String message) throws IOException {
            super.setHeader("Cache-Control", "no-store");
            super.sendError(status, message);
        }
    }
}
