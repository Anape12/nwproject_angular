package jp.nw.controller;

import jp.nw.model.CodedException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.nw.entity.UserEntity;
import jp.nw.model.AuditLogLogic;
import jp.nw.parts.DBBase;
import jp.nw.parts.PasswordUtil;
import jp.nw.util.AngularPage;
import jp.nw.util.PageJson;

@WebServlet("/ChangePassword")
public class ChangePasswordController extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!"json".equals(req.getParameter("format"))) {
            AngularPage.serve(getServletContext(), res, "auth/change-password");
            return;
        }
        HttpSession s = req.getSession();
        String token = (String) s.getAttribute("passwordCsrfToken");
        if (token == null) {
            token = UUID.randomUUID().toString();
            s.setAttribute("passwordCsrfToken", token);
        }
        UserEntity user = (UserEntity) s.getAttribute("loginUser");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contextPath", req.getContextPath());
        data.put("firstName", user.getFirstName());
        data.put("lastName", user.getLastName());
        data.put("loginContext", s.getAttribute("loginContext"));
        data.put("csrfToken", token);
        PageJson.write(res, data);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession s = req.getSession(false);
        UserEntity u = (UserEntity) s.getAttribute("loginUser");
        if (!Objects.equals(s.getAttribute("passwordCsrfToken"), req.getParameter("csrfToken"))) {
            res.sendError(403);
            return;
        }
        String current = req.getParameter("currentPassword"), next = req.getParameter("newPassword"),
                confirm = req.getParameter("confirmation");
        boolean json = "json".equals(req.getParameter("format"));
        try {
            validate(next, confirm);
            DBBase db = new DBBase();
            try (Connection c = db.getConnection()) {
                c.setAutoCommit(false);
                try {
                    String encoded;
                    try (PreparedStatement p = c
                            .prepareStatement("SELECT password FROM users_info WHERE user_id=? FOR UPDATE")) {
                        p.setString(1, u.getUserId());
                        try (var r = p.executeQuery()) {
                            if (!r.next() || !PasswordUtil.matches(current, r.getString(1)))
                                throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_003);
                            encoded = r.getString(1);
                        }
                    }
                    if (PasswordUtil.matches(next, encoded))
                        throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_004);
                    try (PreparedStatement p = c.prepareStatement(
                            "UPDATE users_info SET password=?,password_changed_at=NOW(),password_expiration=?,force_password_change=FALSE WHERE user_id=?")) {
                        p.setString(1, PasswordUtil.encode(next));
                        p.setString(2, LocalDate.now().plusDays(90).format(DateTimeFormatter.BASIC_ISO_DATE));
                        p.setString(3, u.getUserId());
                        p.executeUpdate();
                    }
                    AuditLogLogic.record(c, u.getUserId(), "SECURITY", "PASSWORD_CHANGED", "USER", u.getUserId(), true,
                            AuditLogLogic.clientIp(req), req.getHeader("User-Agent"), "本人による変更");
                    c.commit();
                } catch (Exception e) {
                    c.rollback();
                    throw e;
                }
            }
            s.setAttribute("forcePasswordChange", false);
            s.setAttribute("passwordChangedMessage", "パスワードを変更しました。");
        } catch (Exception e) {
            String message = jp.nw.model.ErrorMessageLogic.forDisplay(e);
            if (json) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                PageJson.write(res, Map.of("message", message));
            } else {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
            }
            return;
        }
        String destination = req.getContextPath() + "/MenuSelect";
        if (json) PageJson.write(res, Map.of("destination", destination));
        else res.sendRedirect(destination);
    }

    private void validate(String p, String c) {
        if (p == null || p.length() < 8 || p.length() > 72 || !p.matches(".*[A-Za-z].*") || !p.matches(".*[0-9].*"))
            throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_005);
        if (!p.equals(c))
            throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_006);
    }
}
