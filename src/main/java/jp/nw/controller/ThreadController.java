package jp.nw.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.nw.dao.ThreadDao;
import jp.nw.dto.ThreadDto;
import jp.nw.util.AngularPage;
import jp.nw.util.PageJson;

/**
 * Servlet implementation class UserView
 */
@WebServlet("/ThreadController")
public class ThreadController extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		if (!"json".equals(request.getParameter("format"))) {
			AngularPage.serve(getServletContext(), response, "communication/thread-list");
			return;
		}

		List<ThreadDto> threadList = new ThreadDao().findAll();

		PageJson.write(response, threadList.stream()
				.map(thread -> new ThreadRow(thread.getThreadId(), thread.getTitle(), thread.getContent(),
						thread.getAuthorId(), thread.getStatus(), thread.isClosed()))
				.toList());
	}

	private record ThreadRow(int threadId, String title, String content, String authorId, String status, boolean closed) {
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

	}

}
