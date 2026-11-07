package jp.nw.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.nw.application.ChatChanelListCommand;
import jp.nw.base.CommandData;
import jp.nw.entity.ChatRoomEntity;
import jp.nw.util.AngularPage;
import jp.nw.util.PageJson;

/**
 * Servlet implementation class UserView
 */
@WebServlet("/ChatChanelList")
public class ChatChanelListController extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (!"json".equals(request.getParameter("format"))) {
			AngularPage.serve(getServletContext(), response, "communication/chat-list");
			return;
		}

		// 自分自身が参照できるChatチャンネルを表示
		ChatChanelListCommand command = new ChatChanelListCommand();
		command.setCommandData(request, response);
		CommandData output = command.execute();

		List<ChatRoomEntity> targetChatRoom = (List<ChatRoomEntity>) output.getValue("chatchanels");
		PageJson.write(response, targetChatRoom == null ? List.of() : targetChatRoom.stream()
				.map(room -> new RoomRow(room.getRoomId(), room.getDisplayName(), "1".equals(room.getRoomType()),
						room.getAiMemberCount() != null && room.getAiMemberCount() > 0))
				.toList());
	}

	private record RoomRow(String roomId, String displayName, boolean direct, boolean hasAi) {
	}
}
