package jp.nw.controller;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.nw.entity.UserEntity;
import jp.nw.util.AngularPage;
import jp.nw.util.PageJson;

/**
 * Servlet implementation class UserUpdate
 */
@WebServlet("/SelectApp")
public class SelectApp extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public SelectApp() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
		if ("json".equals(request.getParameter("format"))) {
			HttpSession session = request.getSession(false);
			UserEntity user = (UserEntity) session.getAttribute("loginUser");
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("contextPath", request.getContextPath());
			data.put("loginContext", session.getAttribute("loginContext"));
			data.put("firstName", user.getFirstName());
			data.put("lastName", user.getLastName());
			data.put("appName", session.getAttribute("resultApp"));
			PageJson.write(response, data);
			return;
		}

		RequestDispatcher dispatcher =
				request.getRequestDispatcher("/html/userupd.html");
		dispatcher.forward(request,response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		String result = switch (Objects.toString(request.getParameter("AppName"), "")) {
			case "NC30001" -> "NC3 Apploci";
			case "NC40001" -> "NC4 Applogic";
			default -> "";
		};
		HttpSession session = request.getSession();
		session.setAttribute("resultApp", result);

		AngularPage.serve(getServletContext(), response, "integrations/external-app");

	}

}
