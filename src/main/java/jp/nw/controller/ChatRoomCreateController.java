package jp.nw.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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
import jp.nw.util.AngularPage;

@WebServlet("/ChatRoomCreate")
public class ChatRoomCreateController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!"json".equals(request.getParameter("format"))) {
            AngularPage.serve(getServletContext(), response, "communication/chat-create");
            return;
        }
        UserEntity loginUser = getLoginUser(request);
        PageJson.write(response, Map.of("rows", new ChatRoomCreateLogic().getAvailableUsers(loginUser.getUserId())
                .stream().map(user -> new ChatUserRow(user.getUserId(), user.getFirstName(), user.getLastName(),
                        "AI".equals(user.getAccountType()))).toList()));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        UserEntity loginUser = getLoginUser(request);
        ChatRoomCreateLogic logic = new ChatRoomCreateLogic();
        boolean json = "json".equals(request.getParameter("format"));

        try {
            CreatedRoom room = logic.createOrGetDirectRoom(
                    loginUser.getUserId(), request.getParameter("targetUserId"));
            String displayName = URLEncoder.encode(room.displayName(), StandardCharsets.UTF_8);
            String destination = request.getContextPath() + "/ChatChanelRoom?roomId="
                    + URLEncoder.encode(room.roomId(), StandardCharsets.UTF_8) + "&displayName=" + displayName;
            if (json) PageJson.write(response, Map.of("destination", destination));
            else response.sendRedirect(destination);
        } catch (IllegalArgumentException e) {
            String message = jp.nw.model.ErrorMessageLogic.forDisplay(e);
            if (json) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                PageJson.write(response, Map.of("message", message));
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
            }
        }
    }

    private record ChatUserRow(String userId, String firstName, String lastName, boolean ai) {
    }

    private UserEntity getLoginUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (UserEntity) session.getAttribute("loginUser");
    }
}
