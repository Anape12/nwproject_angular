package jp.nw.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.nw.domain.user.PermissionAction;
import jp.nw.entity.UserEntity;
import jp.nw.model.ReverseEngineeringLogic;
import jp.nw.util.PermissionCheckUtil;
import jp.nw.util.AngularPage;

@WebServlet("/ReverseEngineering")
public class ReverseEngineeringController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ReverseEngineeringLogic logic = new ReverseEngineeringLogic();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        UserEntity user = session == null ? null : (UserEntity) session.getAttribute("loginUser");
        if (!PermissionCheckUtil.can(user, PermissionAction.DESIGN_REVERSE_VIEW)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        String view = request.getParameter("view");
        if (view == null) {
            AngularPage.serve(request.getServletContext(), response, "admin/reverse-engineering");
            return;
        }
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        try {
            if ("database".equals(view)) {
                mapper.writeValue(response.getWriter(), logic.database());
            } else if ("java".equals(view)) {
                mapper.writeValue(response.getWriter(), logic.javaClasses(getServletContext()));
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (SQLException | RuntimeException error) {
            throw new ServletException("リバースエンジニアリング情報の取得に失敗しました。", error);
        }
    }
}
