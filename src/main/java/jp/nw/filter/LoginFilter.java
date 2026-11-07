package jp.nw.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.nw.entity.UserEntity;
import jp.nw.model.AuditLogLogic;
import jp.nw.util.SecurityToken;

public class LoginFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // getParameter()が一度でも呼ばれると、その時点の文字コードでPOST全体が解析される。
        // ログイン画面識別子を読む前に必ずUTF-8を設定する。
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String uri = req.getRequestURI();
        String applicationRoot = req.getContextPath() + "/";

        // 認証不要画面
        if (uri.endsWith("/Login")
                || uri.equals(applicationRoot)
                || uri.contains("/css/")
                || uri.contains("/js/")
                || uri.contains("/images/")) {

            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);

        if (session == null) {
            res.sendRedirect(req.getContextPath() + "/Login");
            return;
        }

        if (session.getAttribute("loginUser") == null) {
            res.sendRedirect(req.getContextPath() + "/Login");
            return;
        }

        res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        res.setHeader("Pragma", "no-cache");
        res.setDateHeader("Expires", 0);
        res.setHeader("X-NW-Login-Checked", "1");

        // DB上の最新トークンとリクエスト元セッションを毎回照合する
        if (!integrityToken(req)) {
            UserEntity loginUser = (UserEntity) session.getAttribute("loginUser");
            if (loginUser != null) {
                try {
                    AuditLogLogic.record(req, "AUTH", "SESSION_REVOKED", "USER",
                            loginUser.getUserId(), false, "別セッションでのログインを検出");
                } catch (RuntimeException ignored) {
                    // 監査ログの失敗でセッション失効処理を止めない
                }
            }
            session.setAttribute("intentionalLogout", Boolean.TRUE);
            session.invalidate();
            res.sendRedirect(req.getContextPath() + "/Login?sessionInvalid=1");
            return;
        }

        // 同じブラウザで再ログインした後の、古いタブ・ウィンドウからの操作を拒否する
        if (!windowContextIsValid(req, session)) {
            rejectStaleWindow(req, res, session);
            return;
        }

        if (Boolean.TRUE.equals(session.getAttribute("forcePasswordChange"))
                && !uri.endsWith("/ChangePassword") && !uri.endsWith("/Logout")) {
            res.sendRedirect(req.getContextPath() + "/ChangePassword");
            return;
        }

        if (session.getAttribute("attachmentCsrfToken") == null) {
            session.setAttribute("attachmentCsrfToken", UUID.randomUUID().toString());
        }

        chain.doFilter(request, response);
    }

    private boolean integrityToken(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            return false;
        }

        UserEntity loginUser = (UserEntity) session.getAttribute("loginUser");

        if (loginUser == null) {
            return false;
        }

        String sessionToken = (String) session.getAttribute("loginToken");

        try {
            return SecurityToken.matchesCurrentToken(loginUser.getUserId(), sessionToken);
        } catch (RuntimeException e) {
            // DB照合に失敗した場合は安全側に倒す
            return false;
        }
    }

    private boolean windowContextIsValid(HttpServletRequest request, HttpSession session) {
        String expected = (String) session.getAttribute("loginContext");
        if (expected == null || expected.isBlank()) {
            expected = UUID.randomUUID().toString();
            session.setAttribute("loginContext", expected);
        }
        String presented = request.getHeader("X-NW-Login-Context");
        if (presented == null || presented.isBlank())
            presented = request.getParameter("_loginContext");

        if (presented != null && !presented.isBlank())
            return Objects.equals(expected, presented);

        // 新しいタブでURLを直接開いた最初のGETだけは、画面側トークンの初期化を許可する
        return "GET".equalsIgnoreCase(request.getMethod()) && !uriRequiresContext(request);
    }

    private boolean uriRequiresContext(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.endsWith("/Logout");
    }

    private void rejectStaleWindow(HttpServletRequest request, HttpServletResponse response,
            HttpSession session) throws IOException {
        UserEntity loginUser = (UserEntity) session.getAttribute("loginUser");
        if (loginUser != null) {
            try {
                AuditLogLogic.record(request, "AUTH", "STALE_WINDOW_REJECTED", "USER",
                        loginUser.getUserId(), false, "再ログイン前に開かれた画面からの操作を拒否");
            } catch (RuntimeException ignored) {
                // 監査ログの失敗で拒否処理を止めない
            }
        }

        if (request.getHeader("X-NW-Login-Context") != null) {
            response.setHeader("X-NW-Window-Invalid", "1");
            response.sendError(HttpServletResponse.SC_CONFLICT,
                    jp.nw.model.ErrorMessageLogic.get(jp.nw.model.ErrorCode.APP_132));
            return;
        }
        response.sendRedirect(request.getContextPath() + "/Login?windowInvalid=1");
    }
}
