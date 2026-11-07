package jp.nw.controller;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.nw.util.AngularPage;

/** Opens the separately deployed Qiita Guide application from the business menu. */
@WebServlet("/QiitaGuide")
public class QiitaGuideController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String LOCAL_URL = "http://localhost:3000/";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String url = System.getenv("QIITA_GUIDE_URL");
        if (url == null || url.isBlank()) {
            String host = request.getServerName();
            if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)) {
                url = LOCAL_URL;
            }
        }

        if (!isValidUrl(url)) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            AngularPage.serve(getServletContext(), response, "integrations/qiita-unavailable");
            return;
        }

        response.sendRedirect(url.trim());
    }

    private boolean isValidUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null
                    && uri.getUserInfo() == null
                    && uri.getFragment() == null;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
