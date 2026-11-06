package jp.nw.model;

import jp.nw.model.CodedException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.nw.parts.DBBase;

/** ダッシュボード、通知、横断検索を提供する共通サービス。 */
public class PortalLogic {
    public Map<String, Object> dashboard(String userId, boolean admin) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("notifications", rows(
                "SELECT notification_id,title,message,link_url,created_at FROM notification WHERE user_id=? AND read_at IS NULL ORDER BY created_at DESC LIMIT 8",
                userId));
        result.put("todayEvents", rows(
                "SELECT event_id,title,start_at,end_at,all_day,color FROM schedule_event e WHERE (e.user_id=? OR e.visibility='SHARED' OR EXISTS(SELECT 1 FROM schedule_participant p WHERE p.event_id=e.event_id AND p.user_id=?)) AND start_at<CURDATE()+INTERVAL 1 DAY AND end_at>CURDATE() ORDER BY start_at",
                userId, userId));
        result.put("recentReports", rows(
                "SELECT report_id,title,report_date,status FROM work_report WHERE author_id=? ORDER BY report_date DESC,report_id DESC LIMIT 5",
                userId));
        result.put("myApprovals", rows(
                "SELECT approval_id,application_type,status,submitted_at FROM approval_request WHERE applicant_id=? ORDER BY submitted_at DESC LIMIT 8",
                userId));
        result.put("attendance", one(
                "SELECT COUNT(*) recorded,SUM(CASE WHEN approval_status IN ('DRAFT','REJECTED') THEN 1 ELSE 0 END) actionable,COALESCE(SUM(overtime_minutes),0) overtime FROM attendance_record WHERE user_id=? AND work_date>=DATE_FORMAT(CURDATE(),'%Y-%m-01') AND work_date<DATE_FORMAT(CURDATE()+INTERVAL 1 MONTH,'%Y-%m-01')",
                userId));
        result.put("unreadCount",
                scalar("SELECT COUNT(*) FROM notification WHERE user_id=? AND read_at IS NULL", userId));
        result.put("pendingApprovals",
                admin ? scalar("SELECT COUNT(*) FROM approval_request WHERE status='SUBMITTED'") : 0L);
        return result;
    }

    public List<Map<String, Object>> search(String userId, String term) {
        if (term == null || term.trim().length() < 2)
            return List.of();
        String q = "%" + term.trim() + "%";
        List<Map<String, Object>> all = new ArrayList<>();
        add(all, "ユーザー", "/UserSearch?keyword=" + url(term), rows(
                "SELECT user_id id,CONCAT(last_name,' ',first_name) title,'ユーザー情報' detail FROM users_info WHERE delete_flg='0' AND (user_id LIKE ? OR last_name LIKE ? OR first_name LIKE ?) LIMIT 20",
                q, q, q));
        add(all, "報告書", "/DairyWrite?edit=", rows(
                "SELECT report_id id,title,CONCAT(report_date,' / ',status) detail FROM work_report WHERE author_id=? AND (title LIKE ? OR body LIKE ?) LIMIT 20",
                userId, q, q));
        add(all, "予定", "/OpenCalender?edit=", rows(
                "SELECT event_id id,title,CONCAT(start_at,' / ',COALESCE(description,'')) detail FROM schedule_event WHERE (user_id=? OR visibility='SHARED') AND (title LIKE ? OR description LIKE ?) LIMIT 20",
                userId, q, q));
        add(all, "チャット", "/ChatChanelRoom?roomId=", rows(
                "SELECT DISTINCT r.room_id id,COALESCE(r.room_name,'ダイレクトチャット') title,m.message detail FROM chat_room r JOIN chat_room_member me ON me.room_id=r.room_id JOIN chat_message m ON m.room_id=r.room_id WHERE me.user_id=? AND m.message LIKE ? ORDER BY m.created_at DESC LIMIT 20",
                userId, q));
        add(all, "スレッド", "/ThreadDetailController?threadId=", rows(
                "SELECT thread_id id,title,thread_content detail FROM thread_info WHERE title LIKE ? OR thread_content LIKE ? LIMIT 20",
                q, q));
        return all;
    }

    public void markRead(String userId, long id) {
        execute("UPDATE notification SET read_at=NOW() WHERE notification_id=? AND user_id=?", id, userId);
    }

    public void notifyUser(Connection con, String user, String category, String title, String message, String link)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO notification(user_id,category,title,message,link_url) VALUES(?,?,?,?,?)")) {
            ps.setString(1, user);
            ps.setString(2, category);
            ps.setString(3, title);
            ps.setString(4, message);
            ps.setString(5, link);
            ps.executeUpdate();
        }
    }

    public void notifyAdmins(Connection con, String category, String title, String message, String link)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO notification(user_id,category,title,message,link_url) SELECT user_id,?,?,?,? FROM users_info WHERE permission IN ('0','1') AND delete_flg='0'")) {
            ps.setString(1, category);
            ps.setString(2, title);
            ps.setString(3, message);
            ps.setString(4, link);
            ps.executeUpdate();
        }
    }

    private void add(List<Map<String, Object>> out, String type, String prefix, List<Map<String, Object>> values) {
        for (Map<String, Object> row : values) {
            row.put("type", type);
            row.put("url", prefix + row.get("id"));
            out.add(row);
        }
    }

    private String url(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }

    private List<Map<String, Object>> rows(String sql, Object... args) {
        DBBase db = new DBBase();
        try (Connection c = db.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            bind(p, args);
            try (ResultSet r = p.executeQuery()) {
                List<Map<String, Object>> list = new ArrayList<>();
                ResultSetMetaData m = r.getMetaData();
                while (r.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= m.getColumnCount(); i++) {
                        Object value = r.getObject(i);
                        if (value instanceof TemporalAccessor || value instanceof java.util.Date) {
                            value = value.toString();
                        }
                        row.put(m.getColumnLabel(i), value);
                    }
                    list.add(row);
                }
                return list;
            }
        } catch (SQLException e) {
            throw new CodedException.Failure(ErrorCode.APP_086, e);
        }
    }

    private Map<String, Object> one(String sql, Object... a) {
        List<Map<String, Object>> rows = rows(sql, a);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private long scalar(String sql, Object... a) {
        Map<String, Object> r = one(sql, a);
        return r.isEmpty() ? 0 : ((Number) r.values().iterator().next()).longValue();
    }

    private void execute(String sql, Object... a) {
        DBBase db = new DBBase();
        try (Connection c = db.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            bind(p, a);
            p.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void bind(PreparedStatement p, Object... a) throws SQLException {
        for (int i = 0; i < a.length; i++)
            p.setObject(i + 1, a[i]);
    }
}
