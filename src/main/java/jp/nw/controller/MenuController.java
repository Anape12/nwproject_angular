package jp.nw.controller;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.nw.domain.user.PermissionAction;
import jp.nw.entity.UserEntity;
import jp.nw.util.PermissionCheckUtil;

@WebServlet(urlPatterns = { "/MenuSelect", "/BusinessMenu", "/CommunicationMenu" })
public class MenuController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        String path = request.getServletPath();
        if ("/MenuSelect".equals(path)) {
            request.getRequestDispatcher("/WEB-INF/jsp/Menu/menuSelect.jsp").forward(request, response);
            return;
        }
        if ("/CommunicationMenu".equals(path)) {
            request.getRequestDispatcher("/WEB-INF/jsp/Menu/communicationMenu.jsp").forward(request, response);
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
        String page = PermissionCheckUtil.can(user, PermissionAction.USER_MANAGE)
                ? "/WEB-INF/jsp/Menu/perMenu.jsp"
                : "/WEB-INF/jsp/Menu/genMenu.jsp";
        request.getRequestDispatcher(page).forward(request, response);
    }
}
