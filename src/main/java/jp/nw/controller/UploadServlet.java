package jp.nw.controller;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import jp.nw.fileUpload.FileUploadLogic;
import jp.nw.util.AngularPage;

/**
 * Servlet implementation class Login
 */
@WebServlet("/UploadServlet")
@MultipartConfig(location = "/Users/tominagakouhei/Desktop/tmp/uploadFile", maxFileSize = 1000000, maxRequestSize = 1000000, fileSizeThreshold = 1000000)
public class UploadServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private String fileName = "";

	/**
	 * @see HttpServlet#
	 *
	 *
	 *      HttpServlet()
	 */
	public UploadServlet() {
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

		// FileUpload_Logic
		uloadFileRead(request);

		// FileUpload Action
		FileUploadLogic fileUpload = new FileUploadLogic();
		fileUpload.fileUpload(request, response, this.fileName);

		HttpSession session = request.getSession();
		session.setAttribute("UPLOADFILE", session);
		AngularPage.serve(getServletContext(), response, "integrations/external-app-read");
	}

	private void uloadFileRead(HttpServletRequest request) throws IOException, ServletException {
		Part part = request.getPart("FILE_INFO");
		String name = this.getFileName(part);
		this.fileName = "/Users/tominagakouhei/Desktop/tmp/uploadFile/" + name;
		part.write("/Users/tominagakouhei/Desktop/tmp/uploadFile/" + name);
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
