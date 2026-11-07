package jp.nw.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jp.nw.entity.UserEntity;
import jp.nw.model.ChatRoomPermissionPolicy;
import jp.nw.model.ErrorMessageLogic;
import jp.nw.parts.DBBase;
import jp.nw.util.PageJson;
import jp.nw.util.AngularPage;

@WebServlet("/ChatMemberManage")
public class ChatMemberManageController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest q, HttpServletResponse s) throws ServletException, IOException {
        UserEntity u = user(q);
        String room = q.getParameter("roomId");
        if (!new ChatRoomPermissionPolicy().canManageMembers(u, room)) {
            s.sendError(403);
            return;
        }
        if (!"json".equals(q.getParameter("format"))) {
            AngularPage.serve(getServletContext(), s, "communication/chat-members");
            return;
        }
        HttpSession session = q.getSession();
        if (session.getAttribute("chatMemberCsrf") == null)
            session.setAttribute("chatMemberCsrf", UUID.randomUUID().toString());
        List<MemberRow> availableUsers = available(room).stream().map(MemberRow::from).toList();
        List<MemberRow> currentMembers = members(room).stream().map(MemberRow::from).toList();
        String flash = Objects.toString(session.getAttribute("chatMemberFlash"), "");
        session.removeAttribute("chatMemberFlash");
        PageJson.write(s, new MemberPage(room, (String) session.getAttribute("chatMemberCsrf"),
                flash, currentMembers, availableUsers));
    }

    private record MemberPage(String roomId, String csrfToken, String flash, List<MemberRow> members,
            List<MemberRow> available) {
    }

    protected void doPost(HttpServletRequest q, HttpServletResponse s) throws IOException {
        q.setCharacterEncoding("UTF-8");
        UserEntity u = user(q);
        HttpSession session = q.getSession(false);
        String room = q.getParameter("roomId");
        if (!new ChatRoomPermissionPolicy().canManageMembers(u, room)) {
            s.sendError(403);
            return;
        }
        if (!Objects.equals(session.getAttribute("chatMemberCsrf"), q.getParameter("csrfToken"))) {
            s.sendError(403);
            return;
        }
        DBBase db = new DBBase();
        try (Connection c = db.getConnection();
                PreparedStatement p = c.prepareStatement(
                        "INSERT IGNORE INTO chat_room_member(room_id,user_id) SELECT ?,user_id FROM users_info WHERE user_id=? AND delete_flg='0'")) {
            p.setString(1, room);
            p.setString(2, q.getParameter("userId"));
            session.setAttribute("chatMemberFlash", p.executeUpdate() == 1 ? "メンバーを招待しました。" : ErrorMessageLogic.get(jp.nw.model.ErrorCode.APP_136));
        } catch (Exception e) {
            session.setAttribute("chatMemberFlash", ErrorMessageLogic.get(jp.nw.model.ErrorCode.APP_137));
        }
        s.sendRedirect(q.getContextPath() + "/ChatMemberManage?roomId="
                + java.net.URLEncoder.encode(room, java.nio.charset.StandardCharsets.UTF_8));
    }

    private List<UserEntity> available(String room) {
        return users(
                "SELECT user_id,first_name,last_name,account_type FROM users_info WHERE delete_flg='0' AND user_id NOT IN(SELECT user_id FROM chat_room_member WHERE room_id=?) ORDER BY account_type DESC,last_name,first_name",
                room);
    }

    private List<UserEntity> members(String room) {
        return users(
                "SELECT u.user_id,u.first_name,u.last_name,u.account_type FROM users_info u JOIN chat_room_member m ON m.user_id=u.user_id WHERE m.room_id=? ORDER BY u.account_type DESC,u.last_name,u.first_name",
                room);
    }

    private List<UserEntity> users(String sql, String room) {
        List<UserEntity> list = new ArrayList<>();
        DBBase db = new DBBase();
        try (Connection c = db.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, room);
            try (ResultSet r = p.executeQuery()) {
                while (r.next())
                    list.add(UserEntity.builder().userId(r.getString(1)).firstName(r.getString(2))
                            .lastName(r.getString(3)).accountType(r.getString(4)).build());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    private UserEntity user(HttpServletRequest q) {
        HttpSession s = q.getSession(false);
        return s == null ? null : (UserEntity) s.getAttribute("loginUser");
    }

    private record MemberRow(String userId, String firstName, String lastName, boolean ai) {
        static MemberRow from(UserEntity user) {
            return new MemberRow(user.getUserId(), user.getFirstName(), user.getLastName(),
                    "AI".equals(user.getAccountType()));
        }
    }
}
