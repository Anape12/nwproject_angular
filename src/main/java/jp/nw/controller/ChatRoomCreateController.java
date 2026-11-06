package jp.nw.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.nw.entity.UserEntity;
import jp.nw.model.ChatRoomCreateLogic;
import jp.nw.model.ChatRoomCreateLogic.CreatedRoom;
import jp.nw.util.PageJson;

@WebServlet("/ChatRoomCreate")
public class ChatRoomCreateController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        UserEntity loginUser = getLoginUser(request);
        ChatRoomCreateLogic logic = new ChatRoomCreateLogic();
        render(request, response, logic, loginUser.getUserId());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        UserEntity loginUser = getLoginUser(request);
        ChatRoomCreateLogic logic = new ChatRoomCreateLogic();

        try {
            CreatedRoom room = logic.createOrGetDirectRoom(
                    loginUser.getUserId(), request.getParameter("targetUserId"));
            String displayName = URLEncoder.encode(room.displayName(), StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath() + "/ChatChanelRoom?roomId="
                    + room.roomId() + "&displayName=" + displayName);
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMessage", jp.nw.model.ErrorMessageLogic.forDisplay(e));
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            render(request, response, logic, loginUser.getUserId());
        }
    }

    private void render(HttpServletRequest request, HttpServletResponse response, ChatRoomCreateLogic logic,
            String userId) throws ServletException, IOException {
        PageJson.set(request, "chatUsersJson", logic.getAvailableUsers(userId).stream()
                .map(user -> new ChatUserRow(user.getUserId(), user.getFirstName(), user.getLastName(),
                        "AI".equals(user.getAccountType())))
                .toList());
        request.getRequestDispatcher("/WEB-INF/jsp/chat/ChatRoomCreate.jsp").forward(request, response);
    }

    private record ChatUserRow(String userId, String firstName, String lastName, boolean ai) {
    }

    private UserEntity getLoginUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (UserEntity) session.getAttribute("loginUser");
    }
}
