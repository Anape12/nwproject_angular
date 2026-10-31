package jp.nw.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.http.*;
import jp.nw.entity.UserEntity;
import org.junit.jupiter.api.Test;

class UserBirthdayControllerTest {

    @Test
    void anonymousRequestsAreDeniedBeforeAccessingBirthdayStorage() throws Exception {
        var status = new AtomicInteger();
        new UserBirthdayController().doGet(request(null, Map.of()), response(status));
        assertEquals(403, status.get());
    }

    @Test
    void missingAndMismatchedCsrfAreDeniedWithAValidLogin() throws Exception {
        for (var attributes : java.util.List.of(
            Map.<String, Object>of("loginUser", UserEntity.builder().userId("alice").build()),
            Map.<String, Object>of(
                "loginUser",
                UserEntity.builder().userId("alice").build(),
                "birthdayCsrf",
                "valid-token"
            )
        )) {
            var status = new AtomicInteger();
            var session = proxy(HttpSession.class, (method, args) ->
                method.equals("getAttribute") ? attributes.get(args[0]) : null
            );
            new UserBirthdayController().doPost(
                request(session, Map.of("csrfToken", "wrong-token")),
                response(status)
            );
            assertEquals(403, status.get());
        }
    }

    private HttpServletRequest request(HttpSession session, Map<String, String> parameters) {
        return proxy(
            HttpServletRequest.class,
            (method, args) ->
                switch (method) {
                    case "getSession" -> session;
                    case "getParameter" -> parameters.get(args[0]);
                    default -> null;
                }
        );
    }

    private HttpServletResponse response(AtomicInteger status) {
        return proxy(HttpServletResponse.class, (method, args) -> {
            if (method.equals("sendError")) status.set((Integer) args[0]);
            return null;
        });
    }

    private interface Call {
        Object invoke(String method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, Call call) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] { type },
            (p, m, a) -> call.invoke(m.getName(), a)
        );
    }
}
