package jp.nw.util;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

public final class AngularPage {
    private AngularPage() {
    }

    public static void serve(ServletContext context, HttpServletResponse response, String name) throws IOException {
        try (var page = context.getResourceAsStream("/WEB-INF/angular/" + name + ".html")) {
            if (page == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.setContentType("text/html;charset=UTF-8");
            page.transferTo(response.getOutputStream());
        }
    }
}
