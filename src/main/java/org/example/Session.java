package org.example;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.example.annotation.Column;
import org.example.annotation.Id;
import org.example.annotation.Table;
import org.example.exception.ORMException;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class Session {
    private final DataSource dataSource;
    private final Map<SessionCacheKey<?>, Object> sessionCache;
    private final Map<SessionCacheKey<?>, Object[]> snapshots;

    private static final String SELECT_QUERY = "select * from %s where id = ?";

    private static final String UPDATE_QUERY = "update %s set %s where id = %s";

    public Session(DataSource dataSource) {
        this.dataSource = dataSource;
        sessionCache = new HashMap<>();
        snapshots = new HashMap<>();
    }

    @SneakyThrows
    public <T> T find(Class<T> entityType, Object id) {
        var key = new SessionCacheKey<T>(entityType, id);
        var entity = sessionCache.computeIfAbsent(key, k -> getFromDB(key));

        return entityType.cast(entity);
    }

    @SneakyThrows
    private <T> T getFromDB(SessionCacheKey<T> sessionCacheKey) {
        Class<T> entityType = sessionCacheKey.getEntityType();
        Object id = sessionCacheKey.getId();
        log.trace("Getting entity {} with id {} from DB...", entityType.getName(), id);
        String tableName = entityType.getAnnotation(Table.class).value();
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_QUERY.formatted(tableName))) {
                preparedStatement.setObject(1, id);
                log.trace("SQL: " + preparedStatement);
                ResultSet resultSet = preparedStatement.executeQuery();

                return processResultSet(sessionCacheKey, resultSet);
            }
        }
    }

    @SneakyThrows
    private <T> T processResultSet(SessionCacheKey<T> sessionCacheKey, ResultSet resultSet) {
        if (resultSet.next()) {
            Class<T> entityType = sessionCacheKey.getEntityType();
            T result = entityType.getConstructor().newInstance();
            Field[] fields = Arrays.stream(entityType.getDeclaredFields())
                    .sorted(Comparator.comparing(Field::getName))
                    .toArray(Field[]::new);
            Object[] fieldValues = new Object[fields.length];
            for(int i = 0; i < fieldValues.length; i++) {
                fields[i].setAccessible(true);
                Column column = fields[i].getDeclaredAnnotation(Column.class);
                Object value = resultSet.getObject(column.value());
                fields[i].set(result, value);
                fieldValues[i] = value;
            }

            log.trace("Creating snapshot of entity {} with id {}", entityType.getName(), sessionCacheKey.getId());
            snapshots.put(sessionCacheKey, fieldValues);

            return result;
        }
        return null;
    }

    public void close() {
        snapshots.forEach((key, snapshotFieldValues) -> {
            var cachedEntity = sessionCache.get(key);
            Field[] fields = cachedEntity.getClass().getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                try {
                    fields[i].setAccessible(true);
                    if (!snapshotFieldValues[i].equals(fields[i].get(cachedEntity))) {
                        update(cachedEntity);
                        break;
                    }
                } catch (Exception e) {
                    throw new ORMException("Error occurred while closing a session: ", e);
                }
            }
        });
        sessionCache.clear();
        snapshots.clear();
    }

    @SneakyThrows
    private void update(Object entity) {
        Class<?> entityType = entity.getClass();
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = prepareUpdateQuery(entityType, entity);
                log.trace("SQL: " + query);
                statement.executeUpdate(query);
            }
        }
    }

    @SneakyThrows
    private <T> String prepareUpdateQuery(Class<T> entityType, Object entity) {
        Field[] declaredFields = entityType.getDeclaredFields();
        String[] fieldsNameAndValue = new String[declaredFields.length-1];
        Object id = null;
        int fieldsNameValueIndex = 0;
        for (Field field: declaredFields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Id.class)) {
                id = field.get(entity);
            } else {
                String fieldName = field.getAnnotation(Column.class).value();
                fieldsNameAndValue[fieldsNameValueIndex] = "%s = '%s'".formatted(fieldName, field.get(entity));
                fieldsNameValueIndex++;
            }
        }

        String tableName = entityType.getAnnotation(Table.class).value();
        String fields = String.join(",", fieldsNameAndValue);
        return UPDATE_QUERY.formatted(tableName, fields, id);
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    @Getter
    public static class SessionCacheKey<T> {
        private Class<T> entityType;

        private Object id;
    }
}
