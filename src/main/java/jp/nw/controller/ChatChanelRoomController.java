package jp.nw.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.nw.application.ChatRoomOpenCommand;
import jp.nw.base.CommandData;
import jp.nw.entity.ChatMessageEntity;
import jp.nw.entity.UserEntity;
import jp.nw.model.AttachmentAccessPolicy;
import jp.nw.model.AttachmentOwnerType;
import jp.nw.model.ChatRoomPermissionPolicy;
import jp.nw.util.DateFormatUtil;
import jp.nw.util.PageJson;
import jp.nw.util.AngularPage;

/**
 * Servlet implementation class UserView
 */
@WebServlet("/ChatChanelRoom")
public class ChatChanelRoomController extends HttpServlet {

	private static final long serialVersionUID = 1L;
	static final String LAST_ROOM = "lastChatRoom";

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// チャットを表示
		String roomId = request.getParameter("roomId");
		UserEntity loginUser = (UserEntity) request.getSession().getAttribute("loginUser");
		try {
			roomId = AttachmentOwnerType.CHAT.validateOwnerId(roomId);
		} catch (IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		if (!new AttachmentAccessPolicy().canRead(loginUser, AttachmentOwnerType.CHAT, roomId)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		if (!"json".equals(request.getParameter("format"))) {
			AngularPage.serve(getServletContext(), response, "communication/chat-room");
			return;
		}

		ChatRoomOpenCommand command = new ChatRoomOpenCommand();
		request.setAttribute("targetRoomId", roomId);
		command.setCommandData(request, response);

		CommandData output = command.execute();

		HttpSession session = request.getSession();
		List<MessageRow> messageRows = output.getValue("chatDetail") instanceof List<?> messages
				? messages.stream().map(ChatMessageEntity.class::cast)
					.map(message -> new MessageRow(message.getMessageId(), message.getPostedById(),
							message.getPostedByName(), message.getPostedByAccountType(), message.getMessage(),
							DateFormatUtil.format(message.getCreatedAt())))
					.toList() : List.of();
		String displayName = request.getParameter("displayName");
		if (displayName == null || displayName.isBlank()) {
			displayName = session.getAttribute(LAST_ROOM) instanceof LastRoom last && roomId.equals(last.id())
					? last.name() : "チャット";
		}
		List<AttachmentRow> attachments = new jp.nw.model.AttachmentLogic().find("CHAT", roomId).stream()
				.map(file -> new AttachmentRow(file.get("attachment_id").toString(), (String) file.get("original_name")))
				.toList();
		boolean canManage = new ChatRoomPermissionPolicy().canManageMembers(loginUser, roomId);
		session.setAttribute(LAST_ROOM, new LastRoom(roomId, displayName));
		PageJson.write(response, new RoomPage(roomId, displayName, loginUser.getUserId(), canManage,
				(String) session.getAttribute("attachmentCsrfToken"), messageRows, attachments));
	}

	private record RoomPage(String roomId, String roomName, String userId, boolean canManage, String csrfToken,
			List<MessageRow> messages, List<AttachmentRow> attachments) {
	}

	private record MessageRow(int messageId, String postedById, String postedByName, String postedByAccountType,
			String message, String createdAt) {
	}

	private record AttachmentRow(String id, String name) {
	}

	record LastRoom(String id, String name) implements java.io.Serializable {
	}

}
