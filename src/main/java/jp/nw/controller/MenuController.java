package jp.nw.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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
        String path = request.getServletPath();
        if ("/MenuSelect".equals(path) || "/CommunicationMenu".equals(path)) {
            renderMenu(request, response, "/MenuSelect".equals(path) ? "menu-select" : "communication-menu");
            return;
        }
        UserEntity user = (UserEntity) request.getSession().getAttribute("loginUser");
        request.setAttribute("searchUserInfo", PermissionCheckUtil.can(user, PermissionAction.SEARCH_USER_INFO));
        request.setAttribute("canManageUsers", PermissionCheckUtil.can(user, PermissionAction.USER_MANAGE));
        request.setAttribute("canAudit", PermissionCheckUtil.can(user, PermissionAction.AUDIT_VIEW));
        request.setAttribute("canManageAi", PermissionCheckUtil.can(user, PermissionAction.AI_CHARACTER_MANAGE));
        request.setAttribute("canViewDesign", PermissionCheckUtil.can(user, PermissionAction.DESIGN_REVERSE_VIEW));
        request.setAttribute("canUseExternalTools",
                PermissionCheckUtil.can(user, PermissionAction.EXTERNAL_TOOLS_VIEW));
        request.setAttribute("canReviewApprovals", new jp.nw.model.ApprovalLogic().canReview(user));
        request.setAttribute("todayLabel", LocalDate.now(ZoneId.of("Asia/Tokyo")).format(HOME_DATE));
        request.getRequestDispatcher("/WEB-INF/jsp/Menu/menuHome.jsp").forward(request, response);
    }

    private void renderMenu(HttpServletRequest request, HttpServletResponse response, String pageName)
            throws IOException {
        if ("json".equals(request.getParameter("format"))) {
            HttpSession session = request.getSession(false);
            UserEntity user = (UserEntity) session.getAttribute("loginUser");
            response.setContentType("application/json;charset=UTF-8");
            JSON.writeValue(response.getWriter(), new MenuData(request.getContextPath(), user.getFirstName(),
                    user.getLastName(), (String) session.getAttribute("loginContext")));
            return;
        }

        try (var page = getServletContext().getResourceAsStream("/WEB-INF/angular/" + pageName + ".html")) {
            if (page == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.setContentType("text/html;charset=UTF-8");
            page.transferTo(response.getOutputStream());
        }
    }

    private record MenuData(String contextPath, String firstName, String lastName, String loginContext) {
    }
}
