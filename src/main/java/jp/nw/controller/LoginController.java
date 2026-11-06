package jp.nw.controller;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
		if ("1".equals(request.getParameter("sessionInvalid"))) {
			request.setAttribute("errorMessage", jp.nw.model.ErrorMessageLogic.get(jp.nw.model.ErrorCode.APP_128));
		} else if ("1".equals(request.getParameter("windowInvalid"))) {
			request.setAttribute("errorMessage", jp.nw.model.ErrorMessageLogic.get(jp.nw.model.ErrorCode.APP_129));
		}
		renderLogin(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		request.setCharacterEncoding("UTF-8");

		String userId = request.getParameter("userId");
		AuthenticationLogic.Result result = new AuthenticationLogic().authenticate(
				userId == null ? "" : userId.trim(), request.getParameter("password"), request);

		if (!result.authenticated()) {
			this.baseModel.writeInfo("ログイン失敗");
			renderLoginFailure(request, response, result.message());
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
			renderLoginFailure(request, response, jp.nw.model.ErrorMessageLogic.get(jp.nw.model.ErrorCode.APP_134));
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
		response.sendRedirect(
				request.getContextPath()
						+ (result.forcePasswordChange() ? "/ChangePassword?loginFresh=1" : "/MenuSelect?loginFresh=1"));
	}

	private void renderLoginFailure(HttpServletRequest request, HttpServletResponse response, String message)
			throws ServletException, IOException {
		request.setAttribute("loginFailed", true);
		request.setAttribute("errorMessage", message);
		renderLogin(request, response);
	}

	private void renderLogin(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/jsp/login/login.jsp");
		dispatcher.forward(request, response);
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
