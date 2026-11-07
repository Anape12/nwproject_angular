package jp.nw.controller;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.nw.application.ThreadCreateCommand;
import jp.nw.base.CommandData;
import jp.nw.util.AngularPage;

/**
 * Servlet implementation class ThreadCreateController
 */
@WebServlet("/ThreadCreate")
public class ThreadCreateController extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		AngularPage.serve(getServletContext(), response, "communication/new-thread");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// スレッド内容を確定
		ThreadCreateCommand threadCommand = new ThreadCreateCommand();
		threadCommand.setCommandData(request, response);

		// スレッド情報登録コマンドを実行
		CommandData commnadOutput = threadCommand.execute();

		// 追加したスレッドのIDを取得
		commnadOutput.getValue("createId");

		// 登録後、詳細取得処理へリダイレクトして表示情報を統一
		int targetId = ((Long)commnadOutput.getValue("createId")).intValue();
		response.sendRedirect(request.getContextPath() + "/ThreadDetailController?id=" + targetId);
	}

}
