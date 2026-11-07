package jp.nw.util;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Writes view data as a JSON response. */
public final class PageJson {
    private static final ObjectMapper JSON = new ObjectMapper();

    private PageJson() {
    }

    public static void write(HttpServletResponse response, Object view) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        JSON.writeValue(response.getWriter(), view);
    }
}
