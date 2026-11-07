package jp.nw.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;

import jp.nw.entity.UserEntity;

class ReverseEngineeringControllerTest {
    @Test
    void systemAdministratorCanOpenPage() throws Exception {
        AtomicInteger reads = new AtomicInteger();
        HttpServletRequest request = request("0", null, reads);
        AtomicInteger errors = new AtomicInteger();

        new ReverseEngineeringController().doGet(request, response(errors));

        assertEquals(1, reads.get());
        assertEquals(0, errors.get());
    }

    @Test
    void otherPermissionsCannotOpenPageOrDataApi() throws Exception {
        ReverseEngineeringController controller = new ReverseEngineeringController();
        for (String permission : new String[] { "1", "2" }) {
            for (String view : new String[] { null, "database", "java" }) {
                AtomicInteger errors = new AtomicInteger();
                AtomicInteger reads = new AtomicInteger();
                controller.doGet(request(permission, view, reads), response(errors));
                assertEquals(HttpServletResponse.SC_FORBIDDEN, errors.get());
                assertEquals(0, reads.get());
            }
        }
    }

    private HttpServletRequest request(String permission, String view, AtomicInteger reads) {
        UserEntity user = UserEntity.builder().permission(permission).build();
        HttpSession session = (HttpSession) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { HttpSession.class }, (proxy, method, arguments) ->
                        "getAttribute".equals(method.getName()) ? user : null);
        ServletContext context = (ServletContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { ServletContext.class }, (proxy, method, arguments) -> {
                    if ("getResourceAsStream".equals(method.getName())) {
                        assertEquals("/WEB-INF/angular/admin/reverse-engineering.html", arguments[0]);
                        reads.incrementAndGet();
                        return java.io.InputStream.nullInputStream();
                    }
                    return null;
                });
        return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { HttpServletRequest.class }, (proxy, method, arguments) -> {
                    return switch (method.getName()) {
                        case "getSession" -> session;
                        case "getParameter" -> view;
                        case "getServletContext" -> context;
                        default -> null;
                    };
                });
    }

    private HttpServletResponse response(AtomicInteger errors) {
        return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { HttpServletResponse.class }, (proxy, method, arguments) -> {
                    if ("sendError".equals(method.getName())) errors.set((Integer) arguments[0]);
                    if ("getOutputStream".equals(method.getName())) {
                        return new ServletOutputStream() {
                            @Override public boolean isReady() { return true; }
                            @Override public void setWriteListener(WriteListener listener) { }
                            @Override public void write(int value) { }
                        };
                    }
                    return null;
                });
    }
}
