package jp.nw.util;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Serializes view data for a JSP host element. The JSP must escape the attribute. */
public final class PageJson {
    private static final ObjectMapper JSON = new ObjectMapper();

    private PageJson() {
    }

    public static void set(HttpServletRequest request, String attribute, Object view) throws JsonProcessingException {
        request.setAttribute(attribute, JSON.writeValueAsString(view));
    }
}
