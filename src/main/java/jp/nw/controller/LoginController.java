package jp.nw.controller;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.nw.base.BaseModel;
import jp.nw.domain.user.PermissionAction;
import jp.nw.entity.UserEntity;
import jp.nw.model.AuthenticationLogic;
import jp.nw.util.PermissionCheckUtil;
import jp.nw.util.SecurityToken;

/**
 * Servlet implementation class Login
 */
@WebServlet("/Login")
public class LoginController extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final ObjectMapper JSON = new ObjectMapper();

	private BaseModel baseModel = null;

	/**
	 * @see HttpServlet#
	 *
	 *
	 *      HttpServlet()
	 */
	public LoginController() {
		super();
		this.baseModel = new BaseModel();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setHeader("Cache-Control", "no-store");
		if ("json".equals(request.getParameter("format"))) {
			String message = "";
			if ("1".equals(request.getParameter("sessionInvalid"))) {
				message = jp.nw.model.ErrorMessageLogic.get(jp.nw.model.ErrorCode.APP_128);
			} else if ("1".equals(request.getParameter("windowInvalid"))) {
				message = jp.nw.model.ErrorMessageLogic.get(jp.nw.model.ErrorCode.APP_129);
			}
			writeJson(response, Map.of("message", message));
			return;
		}
		response.setContentType("text/html;charset=UTF-8");
		try (var page = getServletContext().getResourceAsStream("/WEB-INF/angular/auth/login.html")) {
			if (page == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			page.transferTo(response.getOutputStream());
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		request.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-store");

		String userId = request.getParameter("userId");
		AuthenticationLogic.Result result = new AuthenticationLogic().authenticate(
				userId == null ? "" : userId.trim(), request.getParameter("password"), request);

		if (!result.authenticated()) {
			this.baseModel.writeInfo("ログイン失敗");
			writeLoginFailure(response, result.message());
			return;
		}

		UserEntity userEntity = result.user();

		// 現在のブラウザに既存セッションがあれば、先に破棄する
		HttpSession oldSession = request.getSession(false);
		if (oldSession != null) {
			oldSession.setAttribute("intentionalLogout", Boolean.TRUE);
			oldSession.invalidate();
		}

		String token = UUID.randomUUID().toString();
		boolean isTokenUpdated;
		try {
			isTokenUpdated = SecurityToken.updateToken(userEntity.getUserId(), token);
		} catch (RuntimeException e) {
			isTokenUpdated = false;
		}

		// トークンの更新に失敗した場合もログイン不可
		if (!isTokenUpdated) {
			this.baseModel.writeInfo("トークン更新失敗");
			// ログイン失敗
			writeLoginFailure(response, jp.nw.model.ErrorMessageLogic.get(jp.nw.model.ErrorCode.APP_134));
			return;
		}

		// ログイン処理成功の場合、ユーザーID/トークンをセッションに保存
		HttpSession session = request.getSession(true);
		session.setMaxInactiveInterval(sessionTimeoutSeconds());
		session.setAttribute("loginToken", token);
		session.setAttribute("loginContext", UUID.randomUUID().toString());
		session.setAttribute("loginUser", userEntity);
		session.setAttribute("forcePasswordChange", result.forcePasswordChange());

		this.baseModel
				.writeInfo(PermissionCheckUtil.can(userEntity, PermissionAction.USER_MANAGE) ? "ログイン成功（管理者）"
						: "ログイン成功（一般）");
		String destination = request.getContextPath()
				+ (result.forcePasswordChange() ? "/ChangePassword?loginFresh=1" : "/MenuSelect?loginFresh=1");
		if ("json".equals(request.getParameter("format"))) {
			writeJson(response, Map.of("destination", destination));
		} else {
			response.sendRedirect(destination);
		}
	}

	private void writeLoginFailure(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		writeJson(response, Map.of("message", message == null ? "" : message));
	}

	private void writeJson(HttpServletResponse response, Map<String, String> payload) throws IOException {
		response.setContentType("application/json;charset=UTF-8");
		JSON.writeValue(response.getWriter(), payload);
	}

	private int sessionTimeoutSeconds() {
		try {
			int minutes = Integer.parseInt(System.getenv("SESSION_TIMEOUT_MINUTES"));
			return Math.max(minutes, 1) * 60;
		} catch (Exception e) {
			return 30 * 60;
		}
	}
}
