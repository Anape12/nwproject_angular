package jp.nw.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jp.nw.entity.UserEntity;
import jp.nw.model.AuditLogLogic;
import jp.nw.model.CodedException;
import jp.nw.model.ErrorCode;
import jp.nw.model.ErrorMessageLogic;
import jp.nw.security.BirthdayRuntime;

@WebServlet("/UserBirthday")
public final class UserBirthdayController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest q, HttpServletResponse s)
        throws ServletException, IOException {
        s.setHeader("Cache-Control", "no-store");
        UserEntity actor = actor(q);
        if (actor == null) {
            s.sendError(403);
            return;
        }
        String target = target(q, actor);
        try {
            var date = BirthdayRuntime.repository().read(
                actor.getUserId(),
                target,
                AuditLogLogic.clientIp(q),
                q.getHeader("User-Agent")
            );
            String token = (String) q.getSession(false).getAttribute("birthdayCsrf");
            if (token == null) {
                token = UUID.randomUUID().toString();
                q.getSession(false).setAttribute("birthdayCsrf", token);
            }
            q.setAttribute("csrfToken", token);
            q.setAttribute("targetUserId", target);
            q.setAttribute("birthday", date.map(LocalDate::toString).orElse(""));
            q.getRequestDispatcher("/WEB-INF/jsp/security/userBirthday.jsp").forward(q, s);
        } catch (CodedException.Denied e) {
            s.sendError(403);
        } catch (CodedException.Validation e) {
            s.sendError(409, ErrorMessageLogic.forDisplay(e));
        } catch (Exception e) {
            s.sendError(503, ErrorMessageLogic.get(ErrorCode.APP_140));
        }
    }

    @Override
    protected void doPost(HttpServletRequest q, HttpServletResponse s) throws IOException {
        q.setCharacterEncoding("UTF-8");
        s.setHeader("Cache-Control", "no-store");
        UserEntity actor = actor(q);
        if (actor == null) {
            s.sendError(403);
            return;
        }
        Object expected = q.getSession(false).getAttribute("birthdayCsrf");
        if (
            !(expected instanceof String) || !Objects.equals(expected, q.getParameter("csrfToken"))
        ) {
            s.sendError(403);
            return;
        }
        String target = target(q, actor);
        try {
            BirthdayRuntime.repository().update(
                actor.getUserId(),
                target,
                LocalDate.parse(q.getParameter("birthday")),
                AuditLogLogic.clientIp(q),
                q.getHeader("User-Agent")
            );
            s.sendRedirect(
                q.getContextPath() +
                    "/UserBirthday?userId=" +
                    URLEncoder.encode(target, StandardCharsets.UTF_8)
            );
        } catch (CodedException.Denied e) {
            s.sendError(403);
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
            s.sendError(400, ErrorMessageLogic.get(ErrorCode.APP_027));
        } catch (Exception e) {
            s.sendError(503, ErrorMessageLogic.get(ErrorCode.APP_140));
        }
    }

    private UserEntity actor(HttpServletRequest q) {
        return q.getSession(false) == null
            ? null
            : (UserEntity) q.getSession(false).getAttribute("loginUser");
    }

    private String target(HttpServletRequest q, UserEntity actor) {
        String value = q.getParameter("userId");
        return value == null || value.isBlank() ? actor.getUserId() : value;
    }
}
