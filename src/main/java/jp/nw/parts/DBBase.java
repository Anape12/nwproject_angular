package jp.nw.parts;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jp.nw.model.CodedException;

public class DBBase {

	private static final String HOST = requiredEnvironmentVariable("DB_HOST");
	private static final String PORT = requiredEnvironmentVariable("DB_PORT");
	private static final String DATABASE = requiredEnvironmentVariable("DB_NAME");
	private static final String USER = requiredEnvironmentVariable("DB_USER");
	private static final String PASSWORD = requiredEnvironmentVariable("DB_PASSWORD");

	private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
			+ "?connectionTimeZone=LOCAL" +
			"&forceConnectionTimeZoneToSession=true" +
			"&preserveInstants=false" +
			"&useUnicode=true" +
			"&characterEncoding=UTF-8" +
			"&allowPublicKeyRetrieval=true" +
			"&useSSL=false";

	private Connection con;

	public DBBase() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			this.con = DriverManager.getConnection(
					URL,
					USER,
					PASSWORD);

			try (Statement st = con.createStatement()) {
				st.execute("SET time_zone = '+09:00'");
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Connection getConnection() {
		return this.con;
	}

	public <T> List<T> selectList(Query query, RowMapper<T> mapper) {
		requireSelect(query);
		try (PreparedStatement ps = con.prepareStatement(new SqlBuilder().build(query))) {
			bindParameter(ps, query);
			try (ResultSet rs = ps.executeQuery()) {
				List<T> rows = new ArrayList<>();
				while (rs.next()) {
					rows.add(mapper.map(rs));
				}
				return rows;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public <T> Optional<T> selectOne(Query query, RowMapper<T> mapper) {
		requireSelect(query);
		// SqlBuilderでSQLを構築
		try (PreparedStatement ps = con.prepareStatement(new SqlBuilder().build(query))) {
			bindParameter(ps, query);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? Optional.ofNullable(mapper.map(rs)) : Optional.empty();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public int executeUpdate(Query query) {
		if (query.getSqlType() == SqlType.SELECT) {
			throw new IllegalArgumentException("Use selectList or selectOne for SELECT queries");
		}
		try (PreparedStatement ps = con.prepareStatement(new SqlBuilder().build(query))) {
			bindParameter(ps, query);
			return ps.executeUpdate();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String requiredEnvironmentVariable(String name) {
		String value = System.getenv(name);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Required environment variable is not set: " + name);
		}
		return value;
	}

	public <T> List<T> selectEntities(Query query, Class<T> clazz) {
		requireSelect(query);
		try (PreparedStatement ps = con.prepareStatement(new SqlBuilder().build(query))) {
			bindParameter(ps, query);
			try (ResultSet rs = ps.executeQuery()) {
				List<Map<String, Object>> rows = getResultList(rs);
				return new EntityMapper().toEntityList(rows, clazz);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void requireSelect(Query query) {
		if (query.getSqlType() != SqlType.SELECT) {
			throw new IllegalArgumentException("Only SELECT queries can be mapped to rows");
		}
	}

	public long executeInsert(Query query) {
		if (query.getSqlType() != SqlType.INSERT) {
			throw new IllegalArgumentException("Only INSERT queries can return generated keys");
		}
		try (PreparedStatement ps = con.prepareStatement(
				new SqlBuilder().build(query), Statement.RETURN_GENERATED_KEYS)) {
			bindParameter(ps, query);
			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next()) {
					return rs.getLong(1);
				}
			}
			throw new CodedException.Failure(jp.nw.model.ErrorCode.APP_120);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * PreparedStatementへ値を設定
	 */
	private void bindParameter(
			PreparedStatement ps,
			Query query) throws Exception {

		int index = 1;

		switch (query.getSqlType()) {

			case INSERT:
				for (Object value : query.getValues().values()) {
					ps.setObject(index++, value);
				}
				break;
			case UPDATE:
				for (Object value : query.getValues().values()) {
					ps.setObject(index++, value);
				}
				for (Object value : query.getConditions().values()) {
					ps.setObject(index++, value);
				}
				break;
			case DELETE:
			case SELECT:
				for (Object value : query.getConditions().values()) {
					ps.setObject(index++, value);
				}
				break;
		}
	}

	/**
	 * ResultSet → List<Map>
	 */
	private List<Map<String, Object>> getResultList(ResultSet rs) throws Exception {

		ResultMapper mapper = new ResultMapper();

		return mapper.toList(rs);
	}

}
