package jp.nw.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.nw.domain.user.PermissionAction;
import jp.nw.entity.UserEntity;
import jp.nw.util.PermissionCheckUtil;
import jp.nw.util.AngularPage;

@WebServlet(urlPatterns = { "/MenuSelect", "/BusinessMenu", "/CommunicationMenu" })
public class MenuController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final DateTimeFormatter HOME_DATE =
            DateTimeFormatter.ofPattern("yyyy年M月d日（E）", Locale.JAPANESE);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        String pageName = switch (request.getServletPath()) {
            case "/MenuSelect" -> "menu-select";
            case "/CommunicationMenu" -> "communication-menu";
            default -> "business-menu";
        };
        if ("json".equals(request.getParameter("format"))) {
            HttpSession session = request.getSession(false);
            UserEntity user = (UserEntity) session.getAttribute("loginUser");
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("contextPath", request.getContextPath());
            data.put("firstName", user.getFirstName());
            data.put("lastName", user.getLastName());
            data.put("loginContext", session.getAttribute("loginContext"));
            if ("business-menu".equals(pageName)) {
                addBusinessData(data, user);
            }
            response.setContentType("application/json;charset=UTF-8");
            JSON.writeValue(response.getWriter(), data);
            return;
        }

        AngularPage.serve(getServletContext(), response, "navigation/" + pageName);
    }

    private void addBusinessData(Map<String, Object> data, UserEntity user) {
        data.put("searchUserInfo", PermissionCheckUtil.can(user, PermissionAction.SEARCH_USER_INFO));
        data.put("canManageUsers", PermissionCheckUtil.can(user, PermissionAction.USER_MANAGE));
        data.put("canAudit", PermissionCheckUtil.can(user, PermissionAction.AUDIT_VIEW));
        data.put("canManageAi", PermissionCheckUtil.can(user, PermissionAction.AI_CHARACTER_MANAGE));
        data.put("canViewDesign", PermissionCheckUtil.can(user, PermissionAction.DESIGN_REVERSE_VIEW));
        data.put("canUseExternalTools", PermissionCheckUtil.can(user, PermissionAction.EXTERNAL_TOOLS_VIEW));
        data.put("canReviewApprovals", new jp.nw.model.ApprovalLogic().canReview(user));
        data.put("todayLabel", LocalDate.now(ZoneId.of("Asia/Tokyo")).format(HOME_DATE));
    }
}
