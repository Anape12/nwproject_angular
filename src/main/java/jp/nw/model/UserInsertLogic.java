package jp.nw.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import jp.nw.entity.UserEntity;
import jp.nw.model.CodedException;
import jp.nw.parts.DBBase;
import jp.nw.parts.PasswordUtil;
import jp.nw.security.BirthdayRuntime;

public class UserInsertLogic {

    public void insert(UserEntity user, LocalDate passwordExpiration) {
        if (!permissionExists(user.getPermission())) {
            throw new CodedException.Validation(ErrorCode.APP_097);
        }

        String sql =
            "INSERT INTO users_info " +
            "(user_id, password, birthday, permission, password_expiration, delete_flg, first_name, last_name) " +
            "VALUES (?, ?, ?, ?, ?, '0', ?, ?)";
        var birthdays = BirthdayRuntime.repository();
        DBBase db = new DBBase();
        try (
            Connection connection = db.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
            )
        ) {
            connection.setAutoCommit(false);
            try {
                statement.setString(1, user.getUserId());
                statement.setString(2, PasswordUtil.encode(user.getPassword()));
                statement.setNull(3, Types.DATE);
                statement.setString(4, user.getPermission());
                statement.setString(5, passwordExpiration.format(DateTimeFormatter.BASIC_ISO_DATE));
                statement.setString(6, user.getFirstName());
                statement.setString(7, user.getLastName());
                statement.executeUpdate();
                try (var generated = statement.getGeneratedKeys()) {
                    if (!generated.next()) throw new SQLException("Missing generated user ID");
                    birthdays.attach(
                        connection,
                        generated.getInt(1),
                        LocalDate.parse(user.getBirthDate())
                    );
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollback) {
                    e.addSuppressed(rollback);
                }
                throw e;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new CodedException.Validation(ErrorCode.APP_024);
        } catch (SQLException e) {
            throw new CodedException.Failure(ErrorCode.APP_098, e);
        }
    }

    public boolean userIdExists(String userId) {
        String sql = "SELECT 1 FROM users_info WHERE user_id = ?";
        DBBase db = new DBBase();
        try (
            Connection connection = db.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            throw new CodedException.Failure(ErrorCode.APP_099, e);
        }
    }

    private boolean permissionExists(String permission) {
        String sql = "SELECT 1 FROM permission_mst WHERE permission_id = ? AND delete_flg = '0'";
        DBBase db = new DBBase();
        try (
            Connection connection = db.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, permission);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            throw new CodedException.Failure(ErrorCode.APP_100, e);
        }
    }
}
