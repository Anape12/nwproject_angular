package jp.nw.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.nw.application.UserListViewCommand;
import jp.nw.application.UserSearchCommand;
import jp.nw.base.CommandData;
import jp.nw.entity.UserEntity;
import jp.nw.util.AngularPage;
import jp.nw.util.PageJson;

@WebServlet("/UserSearch")
public class UserSearchController extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		AngularPage.serve(getServletContext(), response, "users/user-search");
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
			PageJson.write(response, users.stream()
					.map(UserEntity.class::cast)
					.map(user -> new SearchRow(user.getId(), user.getUserId(), user.getPermission()))
					.toList());
		} else {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private record SearchRow(int id, String userId, String permission) {}
}
