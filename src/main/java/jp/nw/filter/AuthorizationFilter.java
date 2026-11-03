package jp.nw.filter;

import java.io.IOException;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.nw.domain.user.PermissionAction;
import jp.nw.entity.UserEntity;
import jp.nw.util.PermissionCheckUtil;

/** Coarse route authorization. Resource and delegated permissions remain at the service boundary. */
public class AuthorizationFilter implements Filter {
    private static final Map<String, PermissionAction> ROUTES = Map.of(
            "/AuditLog", PermissionAction.AUDIT_VIEW,
            "/AiCharacterAdmin", PermissionAction.AI_CHARACTER_MANAGE,
            "/ReverseEngineering", PermissionAction.DESIGN_REVERSE_VIEW,
            "/SelectApp", PermissionAction.EXTERNAL_TOOLS_VIEW,
            "/UserSearch", PermissionAction.SEARCH_USER_INFO,
            "/UserInsert", PermissionAction.USER_MANAGE,
            "/UserSecurityAdmin", PermissionAction.USER_MANAGE);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        PermissionAction action = ROUTES.get(httpRequest.getServletPath());
        if (action != null) {
            HttpSession session = httpRequest.getSession(false);
            UserEntity user = session == null ? null : (UserEntity) session.getAttribute("loginUser");
            if (!PermissionCheckUtil.can(user, action)) {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
