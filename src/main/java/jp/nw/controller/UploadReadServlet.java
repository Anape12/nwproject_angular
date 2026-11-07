package jp.nw.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

/**
 * Servlet implementation class Login
 */
@WebServlet("/UploadReadServlet")
@MultipartConfig(location = "/Users/tominagakouhei/Desktop/tmp/uploadFile", maxFileSize = 1000000, maxRequestSize = 1000000, fileSizeThreshold = 1000000)
public class UploadReadServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#
	 *
	 *
	 *      HttpServlet()
	 */
	public UploadReadServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession session = request.getSession();
		session.invalidate();

		response.sendRedirect(request.getContextPath() + "/Login");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		request.setCharacterEncoding("UTF-8");

		uloadFileRead(request);
	}

	private void uloadFileRead(HttpServletRequest request) throws IOException, ServletException {

		Part part = request.getPart("READ_FILE");
		String name = this.getFileName(part);
		String filePath = "/Users/tominagakouhei/Downloads/" + name;
		Path file = Paths.get(filePath);
		Files.lines(file).forEach(System.out::println); // UTF-8
	}

	/**
	 *
	 * @param part
	 * @return
	 */
	private String getFileName(Part part) {
		String name = null;
		for (String dispotion : part.getHeader("Content-Disposition").split(";")) {
			if (dispotion.trim().startsWith("filename")) {
				name = dispotion.substring(dispotion.indexOf("=") + 1).replace("\"", "").trim();
				name = name.substring(name.lastIndexOf("\\") + 1);
				break;
			}
		}
		return name;
	}
}
