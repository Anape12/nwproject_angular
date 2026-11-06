package jp.nw.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.nw.application.UserListViewCommand;
import jp.nw.application.UserSearchCommand;
import jp.nw.base.BaseModel;
import jp.nw.base.CommandData;
import jp.nw.entity.PermissionMasterEntity;
import jp.nw.entity.UserEntity;
import jp.nw.util.PermissionGetUtil;

@WebServlet("/UserSearch")
public class UserSearchController extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final ObjectMapper JSON = new ObjectMapper();
	private BaseModel logger = null;

	private CommandData commandOutput;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public UserSearchController() {
		super();
		this.logger = new BaseModel();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String action = request.getParameter("action");

		// 編集画面起動
		if ("edit".equals(action)) {
			String userId = request.getParameter("userId");
			request.setAttribute("userId", userId);
			// ユーザ情報取得処理
			UserListViewCommand command = new UserListViewCommand();
			command.setCommandData(request, response);

			// 処理を実行
			CommandData output = command.postExec();

			// 権限レベルマスタを参照
			List<PermissionMasterEntity> permissionLevels = PermissionGetUtil.getAllPermissionLevels();
			request.setAttribute("permissionLevels", permissionLevels);

			RequestDispatcher dispatcher = ((HttpServletRequest) output.getValue("request"))
					.getRequestDispatcher("/WEB-INF/jsp/otherUser/editUserInfo.jsp");
			dispatcher.forward(((HttpServletRequest) output.getValue("request")),
					((HttpServletResponse) output.getValue("response")));
		} else {
			// ユーザー情報検索画面起動
			RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/jsp/otherUser/userSearch.jsp");
			dispatcher.forward(request, response);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// 入力テキストを取得
		request.setCharacterEncoding("UTF-8");
		String userId = request.getParameter("userId");

		CommandData output;
		if (userId == null || userId.trim().isEmpty()) {
			// 検索テキストが空の場合は、全件検索を行う
			UserListViewCommand command = new UserListViewCommand();
			command.setCommandData(request, response);
			// 処理を実行
			output = command.execute();
		} else {
			// ユーザーIDで検索を行う
			UserSearchCommand command = new UserSearchCommand();
			command.setCommandData(request, response);

			output = command.execute();
		}

		if (output.getValue("loginUser") == null) {
			response.sendRedirect(request.getContextPath() + "/Login");
			return;
		}

		if (request.getAttribute("userList") instanceof List<?> users) {
			request.setAttribute("searchResultsJson", JSON.writeValueAsString(users.stream()
					.map(UserEntity.class::cast)
					.map(user -> new SearchRow(user.getId(), user.getUserId(), user.getPermission()))
					.toList()));
		} else {
			request.setAttribute("searchFailed", true);
		}
		request.setAttribute("searchUserId", userId);
		request.getRequestDispatcher("/WEB-INF/jsp/otherUser/userSearch.jsp").forward(request, response);
	}

	private record SearchRow(int id, String userId, String permission) {}
}
