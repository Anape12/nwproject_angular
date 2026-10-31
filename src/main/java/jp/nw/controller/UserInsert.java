package jp.nw.controller;

import jp.nw.model.CodedException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.nw.entity.UserEntity;
import jp.nw.domain.user.PermissionAction;
import jp.nw.model.AuditLogLogic;
import jp.nw.model.UserInsertLogic;
import jp.nw.util.PermissionCheckUtil;
import jp.nw.util.PermissionGetUtil;

@WebServlet("/UserInsert")
public class UserInsert extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Pattern USER_ID_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{3,19}");
    private static final Pattern HAS_LETTER = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern HAS_NUMBER = Pattern.compile(".*[0-9].*");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        if (!isAdministrator(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, jp.nw.model.ErrorMessageLogic.get(jp.nw.model.ErrorCode.APP_127));
            return;
        }

        String csrfToken = (String) session.getAttribute("userInsertCsrfToken");
        if (csrfToken == null) {
            csrfToken = UUID.randomUUID().toString();
            session.setAttribute("userInsertCsrfToken", csrfToken);
        }
        request.setAttribute("csrfToken", csrfToken);
        request.setAttribute("permissionLevels", PermissionGetUtil.getAllPermissionLevels().stream()
                .filter(level -> PermissionCheckUtil.canManageRole((UserEntity) session.getAttribute("loginUser"),
                        level.getPermissionId())).toList());
        request.setAttribute("defaultExpiration", LocalDate.now().plusDays(90));
        request.setAttribute("currentDate", LocalDate.now());
        request.setAttribute("successMessage", session.getAttribute("userInsertSuccess"));
        session.removeAttribute("userInsertSuccess");
        request.getRequestDispatcher("/WEB-INF/jsp/RegUser/userInsert.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if (!isAdministrator(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, jp.nw.model.ErrorMessageLogic.get(jp.nw.model.ErrorCode.APP_127));
            return;
        }
        if (!Objects.equals(session.getAttribute("userInsertCsrfToken"), request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, jp.nw.model.ErrorMessageLogic.get(jp.nw.model.ErrorCode.APP_121));
            return;
        }

        try {
            UserEntity user = validateAndBuild(request);
            UserEntity actor = (UserEntity) session.getAttribute("loginUser");
            if (!PermissionCheckUtil.canManageRole(actor, user.getPermission()))
                throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_022);
            LocalDate expiration = LocalDate.parse(request.getParameter("passwordExpiration"));
            if (expiration.isBefore(LocalDate.now()))
                throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_023);

            UserInsertLogic logic = new UserInsertLogic();
            if (logic.userIdExists(user.getUserId()))
                throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_024);
            logic.insert(user, expiration);
            AuditLogLogic.record(request, "USER", "USER_CREATED", "USER", user.getUserId(), true,
                    "権限=" + user.getPermission());
            session.setAttribute("userInsertSuccess", user.getUserId() + " を登録しました。");
            response.sendRedirect(request.getContextPath() + "/UserInsert");
        } catch (CodedException.Failure e) {
            response.sendError(503, jp.nw.model.ErrorMessageLogic.forDisplay(e));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            AuditLogLogic.record(request, "USER", "USER_CREATE_FAILED", "USER",
                    trim(request.getParameter("userId")), false, e.getMessage());
            request.setAttribute("errorMessage", jp.nw.model.ErrorMessageLogic.forDisplay(e));
            setEnteredValues(request);
            request.setAttribute("csrfToken", session.getAttribute("userInsertCsrfToken"));
            request.setAttribute("permissionLevels", PermissionGetUtil.getAllPermissionLevels().stream()
                    .filter(level -> PermissionCheckUtil.canManageRole((UserEntity) session.getAttribute("loginUser"),
                            level.getPermissionId())).toList());
            request.setAttribute("defaultExpiration", LocalDate.now().plusDays(90));
            request.setAttribute("currentDate", LocalDate.now());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.getRequestDispatcher("/WEB-INF/jsp/RegUser/userInsert.jsp").forward(request, response);
        }
    }

    private UserEntity validateAndBuild(HttpServletRequest request) {
        String userId = trim(request.getParameter("userId"));
        String lastName = trim(request.getParameter("lastName"));
        String firstName = trim(request.getParameter("firstName"));
        String birthdayValue = request.getParameter("birthday");
        String permission = request.getParameter("permission");
        String password = request.getParameter("password");
        String passwordConfirmation = request.getParameter("passwordConfirmation");

        if (!USER_ID_PATTERN.matcher(userId).matches())
            throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_025);
        if (lastName.isBlank() || lastName.length() > 36 || firstName.isBlank() || firstName.length() > 36)
            throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_026);
        LocalDate birthday = LocalDate.parse(birthdayValue);
        if (birthday.isAfter(LocalDate.now()) || birthday.isBefore(LocalDate.of(1900, 1, 1)))
            throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_027);
        if (password == null || password.length() < 8 || password.length() > 72
                || !HAS_LETTER.matcher(password).matches() || !HAS_NUMBER.matcher(password).matches()) {
            throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_028);
        }
        if (!password.equals(passwordConfirmation))
            throw new CodedException.Validation(jp.nw.model.ErrorCode.APP_006);

        return UserEntity.builder().userId(userId).lastName(lastName).firstName(firstName)
                .birthDate(birthday.toString()).permission(permission).password(password).build();
    }

    private void setEnteredValues(HttpServletRequest request) {
        request.setAttribute("enteredUserId", trim(request.getParameter("userId")));
        request.setAttribute("enteredLastName", trim(request.getParameter("lastName")));
        request.setAttribute("enteredFirstName", trim(request.getParameter("firstName")));
        request.setAttribute("enteredBirthday", request.getParameter("birthday"));
        request.setAttribute("enteredPermission", request.getParameter("permission"));
        request.setAttribute("enteredExpiration", request.getParameter("passwordExpiration"));
    }

    private boolean isAdministrator(HttpSession session) {
        if (session == null)
            return false;
        UserEntity loginUser = (UserEntity) session.getAttribute("loginUser");
        return PermissionCheckUtil.can(loginUser, PermissionAction.USER_MANAGE);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
