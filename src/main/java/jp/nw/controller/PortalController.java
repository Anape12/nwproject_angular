package jp.nw.controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import jp.nw.entity.UserEntity;
import jp.nw.domain.user.PermissionAction;
import jp.nw.model.PortalLogic;
import jp.nw.util.PermissionCheckUtil;

@WebServlet("/Portal")
public class PortalController extends HttpServlet {
    // Keep database IDs exact when the browser sends them back in forms.
    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS)
            .build();

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        HttpSession session = req.getSession();
        if (!"json".equals(req.getParameter("format"))) {
            req.setAttribute("portalError", session.getAttribute("portalError"));
            session.removeAttribute("portalError");
            req.getRequestDispatcher("/WEB-INF/jsp/portal/dashboard.jsp").forward(req, res);
            return;
        }

        UserEntity u = (UserEntity) session.getAttribute("loginUser");
        PortalLogic l = new PortalLogic();
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        JSON.writeValue(res.getWriter(), Map.of(
                "dashboard", l.dashboard(u.getUserId(), PermissionCheckUtil.can(u, PermissionAction.ADMIN_DASHBOARD_VIEW)),
                "searchResults", l.search(u.getUserId(), req.getParameter("q"))));
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        UserEntity u = (UserEntity) req.getSession().getAttribute("loginUser");
        try {
            if (req.getParameter("approvalId") != null)
                new jp.nw.model.ApprovalLogic().withdraw(Long.parseLong(req.getParameter("approvalId")), u.getUserId());
            else
                new PortalLogic().markRead(u.getUserId(), Long.parseLong(req.getParameter("notificationId")));
        } catch (Exception e) {
            req.getSession().setAttribute("portalError", jp.nw.model.ErrorMessageLogic.forDisplay(e));
        }
        res.sendRedirect(req.getContextPath() + "/Portal");
    }
}
