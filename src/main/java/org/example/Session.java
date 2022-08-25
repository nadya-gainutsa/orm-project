package org.example;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.example.annotation.Table;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class Session {
    private final DataSource dataSource;
    private Map<SessionCacheKey<?>, Object> sessionCache;

    private static final String SELECT_QUERY = "select * from %s where id = ?";

    public Session(DataSource dataSource) {
        this.dataSource = dataSource;
        sessionCache = new HashMap<>();
    }

    @SneakyThrows
    public <T> T find(Class<T> entityType, Object id) {
        var key = new SessionCacheKey<T>(entityType, id);
        var obj = sessionCache.computeIfAbsent(key, k -> getFromDB(entityType, id));

        return entityType.cast(obj);
    }

    @SneakyThrows
    private <T> T getFromDB(Class<T> entityType, Object id) {
        log.info("Getting entity {} with id {} from DB...", entityType.getName(), id);
        String tableName = entityType.getAnnotation(Table.class).value();
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_QUERY.formatted(tableName))) {
                preparedStatement.setObject(1, id);
                log.trace("SQL: " + preparedStatement);
                ResultSet resultSet = preparedStatement.executeQuery();

                return processResultSet(resultSet, entityType);

            } catch (SQLException e) {
                log.error("Error occurred while finding entity: ", e);
            }
        }
        return null;
    }



    @SneakyThrows
    private <T> T processResultSet(ResultSet resultSet, Class<T> entityType) {
        if (resultSet.next()) {
            T result = entityType.getConstructor().newInstance();
            Field[] fields = entityType.getDeclaredFields();
            for (Field f : fields) {
                f.setAccessible(true);
                f.set(result, resultSet.getObject(f.getName()));
            }
            return result;
        }
        return null;
    }

    public void close() {
        sessionCache.clear();
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    public static class SessionCacheKey<T> {
        private Class<T> entityType;

        private Object id;
    }
}
