package jp.nw.model;

import jp.nw.model.CodedException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import jp.nw.entity.AiCharacterEntity;
import jp.nw.parts.DBBase;

public class AiCharacterLogic {
    public List<AiCharacterEntity> findAll() {
        String sql = "SELECT a.character_id,a.user_id,u.first_name character_name,a.prompt_key,a.model_name,a.reply_mode,a.active_flg FROM ai_character a JOIN users_info u ON u.user_id=a.user_id ORDER BY a.character_id";
        List<AiCharacterEntity> result = new ArrayList<>();
        DBBase db = new DBBase();
        try (Connection c = db.getConnection();
                PreparedStatement p = c.prepareStatement(sql);
                ResultSet r = p.executeQuery()) {
            while (r.next())
                result.add(map(r));
            return result;
        } catch (SQLException e) {
            throw new CodedException.Failure(ErrorCode.APP_042, e);
        }
    }

    public AiCharacterEntity find(long id) {
        DBBase db = new DBBase();
        try (Connection c = db.getConnection();
                PreparedStatement p = c.prepareStatement(
                        "SELECT a.character_id,a.user_id,u.first_name character_name,a.prompt_key,a.model_name,a.reply_mode,a.active_flg FROM ai_character a JOIN users_info u ON u.user_id=a.user_id WHERE a.character_id=?")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? map(r) : null;
            }
        } catch (SQLException e) {
            throw new CodedException.Failure(ErrorCode.APP_042, e);
        }
    }

    public List<Map<String, Object>> findRecentJobs() {
        String sql = "SELECT j.job_id,u.first_name character_name,j.source_type,j.status,j.retry_count,j.error_message,j.created_at,j.completed_at FROM ai_response_job j JOIN ai_character a ON a.character_id=j.character_id JOIN users_info u ON u.user_id=a.user_id ORDER BY j.job_id DESC LIMIT 20";
        List<Map<String, Object>> list = new ArrayList<>();
        DBBase db = new DBBase();
        try (Connection c = db.getConnection(); PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            while (r.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("jobId", r.getLong("job_id")); row.put("characterName", r.getString("character_name"));
                row.put("sourceType", r.getString("source_type")); row.put("status", r.getString("status"));
                row.put("retryCount", r.getInt("retry_count")); row.put("errorMessage", r.getString("error_message"));
                row.put("createdAt", r.getTimestamp("created_at")); row.put("completedAt", r.getTimestamp("completed_at"));
                list.add(row);
            }
        } catch (SQLException ignored) { }
        return list;
    }

    public void create(AiCharacterEntity v) {
        validate(v);
        DBBase db = new DBBase();
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO users_info(user_id,password,birthday,permission,account_type,password_expiration,delete_flg,first_name,last_name) VALUES(?,'$2a$10$45HSUdWr4xrIYVymHlDmL.v0sc6xpENpHAszdaiSUG8bVWKuUs5LK',NULL,'2','AI','00000000','0',?,'AI住人')")) {
                    p.setString(1, v.getUserId());
                    p.setString(2, v.getCharacterName());
                    p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO ai_character(user_id,prompt_key,model_name,reply_mode,active_flg) VALUES(?,?,?,?,'1')")) {
                    p.setString(1, v.getUserId());
                    p.setString(2, v.getPromptKey());
                    p.setString(3, blankToNull(v.getModelName()));
                    p.setString(4, v.getReplyMode());
                    p.executeUpdate();
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new CodedException.Failure(ErrorCode.APP_043, e);
        }
    }

    public void update(AiCharacterEntity v) {
        validate(v);
        DBBase db = new DBBase();
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE ai_character SET prompt_key=?,model_name=?,reply_mode=?,active_flg=? WHERE character_id=?")) {
                    p.setString(1, v.getPromptKey());
                    p.setString(2, blankToNull(v.getModelName()));
                    p.setString(3, v.getReplyMode());
                    p.setString(4, v.getActiveFlg());
                    p.setLong(5, v.getCharacterId());
                    if (p.executeUpdate() != 1)
                        throw new CodedException.Validation(ErrorCode.APP_044);
                }
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE users_info u JOIN ai_character a ON a.user_id=u.user_id SET u.first_name=?,u.delete_flg=IF(?='1','0','1') WHERE a.character_id=?")) {
                    p.setString(1, v.getCharacterName());
                    p.setString(2, v.getActiveFlg());
                    p.setLong(3, v.getCharacterId());
                    p.executeUpdate();
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new CodedException.Failure(ErrorCode.APP_045, e);
        }
    }

    private void validate(AiCharacterEntity v) {
        if (v.getCharacterName() == null || v.getCharacterName().isBlank())
            throw new CodedException.Validation(ErrorCode.APP_046);
        if (v.getPromptKey() == null || !v.getPromptKey().matches("[a-z0-9][a-z0-9_-]{0,63}"))
            throw new CodedException.Validation(ErrorCode.APP_047);
        if (!"MENTION".equals(v.getReplyMode()) && !"ALWAYS".equals(v.getReplyMode()))
            throw new CodedException.Validation(ErrorCode.APP_048);
        if (v.getUserId() != null && !v.getUserId().matches("[a-zA-Z0-9_-]{3,20}"))
            throw new CodedException.Validation(ErrorCode.APP_049);
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private AiCharacterEntity map(ResultSet r) throws SQLException {
        return AiCharacterEntity.builder().characterId(r.getLong("character_id")).userId(r.getString("user_id"))
                .characterName(r.getString("character_name")).promptKey(r.getString("prompt_key"))
                .modelName(r.getString("model_name")).replyMode(r.getString("reply_mode"))
                .activeFlg(r.getString("active_flg")).build();
    }
}
