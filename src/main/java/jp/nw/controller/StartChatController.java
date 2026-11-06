package jp.nw.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jp.nw.application.RegistrationChatCommand;
import jp.nw.base.CommandData;
import jp.nw.entity.ChatMessageEntity;

/**
 * Servlet implementation class UserView
 */
@WebServlet("/StartChat")
public class StartChatController extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String roomId = request.getSession().getAttribute(ChatChanelRoomController.LAST_ROOM)
				instanceof ChatChanelRoomController.LastRoom last ? last.id() : null;
		response.sendRedirect(request.getContextPath() + (roomId == null ? "/ChatChanelList"
				: "/ChatChanelRoom?roomId=" + URLEncoder.encode(roomId, StandardCharsets.UTF_8)));

	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		request.setCharacterEncoding("UTF-8");
		RegistrationChatCommand command = new RegistrationChatCommand();
		command.setCommandData(request, response);
		CommandData output = command.execute();

		List<ChatMessageEntity> chatMessageList = (List<ChatMessageEntity>) output.getValue("ChatMessageList");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		JSON_MAPPER.writeValue(response.getWriter(), chatMessageList);
	}

}
