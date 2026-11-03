package jp.nw.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;

import jp.nw.entity.UserEntity;

class AuthorizationFilterTest {
    @Test
    void restrictedRoutesUseCommonPolicy() throws Exception {
        assertRequest("/AuditLog", "0", 1, 0);
        assertRequest("/AuditLog", "1", 1, 0);
        assertRequest("/AuditLog", "2", 0, 403);
        assertRequest("/ReverseEngineering", "0", 1, 0);
        assertRequest("/ReverseEngineering", "1", 0, 403);
        assertRequest("/UserSearch", "0", 1, 0);
        assertRequest("/UserSearch", "1", 0, 403);
        assertRequest("/UserSearch", "2", 0, 403);
        assertRequest("/SelectApp", "2", 0, 403);
        assertRequest("/UserSecurityAdmin", null, 0, 403);
        assertRequest("/WorkManagement", "2", 1, 0);
    }

    private void assertRequest(String path, String permission, int expectedCalls, int expectedError) throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger error = new AtomicInteger();
        HttpSession session = (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { HttpSession.class }, (proxy, method, args) ->
                        "getAttribute".equals(method.getName()) ? UserEntity.builder().permission(permission).build() : null);
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "getServletPath" -> path;
                    case "getSession" -> session;
                    default -> null;
                });
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { HttpServletResponse.class }, (proxy, method, args) -> {
                    if ("sendError".equals(method.getName())) error.set((Integer) args[0]);
                    return null;
                });
        FilterChain chain = (req, res) -> calls.incrementAndGet();
        new AuthorizationFilter().doFilter(request, response, chain);
        assertEquals(expectedCalls, calls.get(), path + " role=" + permission);
        assertEquals(expectedError, error.get(), path + " role=" + permission);
    }
}
