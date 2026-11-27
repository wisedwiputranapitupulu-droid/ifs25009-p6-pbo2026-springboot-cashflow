package org.delcom.ifs25009springbootcashflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.delcom.ifs25009springbootcashflow.config.VersionedAssetCacheFilter;
import org.junit.jupiter.api.Test;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class VersionedAssetCacheFilterTest {
    private final VersionedAssetCacheFilter filter = new VersionedAssetCacheFilter();

    @Test
    void cachesHashedBuildFilesAndVersionedStylesheets() throws Exception {
        assertCacheHeader("GET", "/VAADIN/build/generated-flow-imports-B-aQruVm.js", null);
        assertCacheHeader("GET", "/VAADIN/build/generated-A1b2c3d4.css", null);
        assertCacheHeader("GET", "/VAADIN/build/A1b2c3d4.js", null);
        assertCacheHeader("GET", "/styles.css", "10e1177a");
        assertCacheHeader("GET", "/lumo/lumo.css", "80042ca7");
    }

    @Test
    void doesNotCacheUnversionedOrNonStaticRequests() throws Exception {
        assertNoCacheHeader("GET", "/VAADIN/build/generated-flow-imports.js", null);
        assertNoCacheHeader("GET", "/assets/generated-A1b2c3d4.js", null);
        assertNoCacheHeader("GET", "/VAADIN/build/generated-A1b2c3d4.txt", null);
        assertNoCacheHeader("GET", "/styles.css", null);
        assertNoCacheHeader("GET", "/styles.css", "short");
        assertNoCacheHeader("GET", "/styles.css", "invalid value");
        assertNoCacheHeader("GET", "/login", null);
        assertNoCacheHeader("HEAD", "/VAADIN/build/generated-flow-imports-B-aQruVm.js", null);
    }

    @Test
    void overridesShortCacheHeadersForSuccessfulHashedAssets() throws Exception {
        assertCacheHeaderWithResponse("GET", "/VAADIN/build/generated-flow-imports-B-aQruVm.js",
                (request, response) -> {
                    HttpServletResponse httpResponse = (HttpServletResponse) response;
                    httpResponse.setHeader("Cache-Control", "public, max-age=3600");
                    httpResponse.addHeader("Cache-Control", "public, max-age=3600");
                    httpResponse.setHeader("Content-Type", "text/javascript");
                    httpResponse.addHeader("Content-Type", "text/javascript");
                });
    }

    @Test
    void avoidsCachingFailedAssetsAndHandlesCommittedResponses() throws Exception {
        assertNoStoreHeader("GET", "/VAADIN/build/generated-flow-imports-B-aQruVm.js",
                (request, response) -> {
                    HttpServletResponse httpResponse = (HttpServletResponse) response;
                    httpResponse.setStatus(404);
                    httpResponse.setHeader("Cache-Control", "public, max-age=31536000");
                });
        assertNoStoreHeader("GET", "/VAADIN/build/generated-flow-imports-B-aQruVm.js",
                (request, response) -> ((HttpServletResponse) response).sendError(404));
        assertNoStoreHeader("GET", "/VAADIN/build/generated-flow-imports-B-aQruVm.js",
                (request, response) -> ((HttpServletResponse) response).sendError(404, "Not found"));
        assertCacheHeaderWithResponse("GET", "/VAADIN/build/generated-flow-imports-B-aQruVm.js",
                (request, response) -> ((HttpServletResponse) response).setStatus(200));
        assertCacheHeaderWithResponse("GET", "/VAADIN/build/generated-flow-imports-B-aQruVm.js",
                (request, response) -> ((HttpServletResponse) response).flushBuffer());
    }

    private void assertCacheHeader(String method, String path, String version) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        if (version != null) {
            request.setParameter("v-c", version);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals("public, max-age=31536000, immutable", response.getHeader("Cache-Control"));
    }

    private void assertNoCacheHeader(String method, String path, String version) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        if (version != null) {
            request.setParameter("v-c", version);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertNull(response.getHeader("Cache-Control"));
    }

    private void assertCacheHeaderWithResponse(String method, String path, FilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        assertEquals("public, max-age=31536000, immutable", response.getHeader("Cache-Control"));
    }

    private void assertNoStoreHeader(String method, String path, FilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        assertEquals("no-store", response.getHeader("Cache-Control"));
    }
}
