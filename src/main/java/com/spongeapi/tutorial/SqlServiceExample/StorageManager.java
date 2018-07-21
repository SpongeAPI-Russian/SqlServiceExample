package com.spongeapi.tutorial.SqlServiceExample;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.text.Text;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;

public final class StorageManager {
    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS `sqlservice_chat_logs` (" +
            "  `id` BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "  `uuid` CHAR(36) NOT NULL," +
            "  `name` VARCHAR(16) NOT NULL," +
            "  `time` DATETIME NOT NULL," +
            "  `message` VARCHAR(256)" +
            ");";

    private static final String INSERT_MESSAGE = "INSERT INTO `sqlservice_chat_logs` (`uuid`, `name`, `time`, `message`) VALUES (?, ?, ?, ?);";

    private final DataSource dataSource;
    private final String jdbcAlias;

    private StorageManager(PluginContainer container, SqlService sqlService, String jdbcAlias) throws SQLException {
        Objects.requireNonNull(container, "container");
        Objects.requireNonNull(sqlService, "sqlService");
        this.jdbcAlias = Objects.requireNonNull(jdbcAlias, "jdbcAlias");
        String jdbcUrl = getJdbcUrl(sqlService, Objects.requireNonNull(jdbcAlias, "jdbcAlias"));
        this.dataSource = sqlService.getDataSource(container, jdbcUrl);
    }

    String getJdbcAlias() {
        return jdbcAlias;
    }

    private void createTables() throws SQLException {
        try(Connection connection = dataSource.getConnection();
            PreparedStatement stmt = connection.prepareStatement(CREATE_TABLE)) {
            stmt.execute();
        }
    }

    void insertChatMessage(Text message, Player source) throws SQLException {
        try(Connection connection = dataSource.getConnection();
            PreparedStatement stmt = connection.prepareStatement(INSERT_MESSAGE)) {
            stmt.setString(1, source.getUniqueId().toString());
            stmt.setString(2, source.getName());
            stmt.setObject(3, Instant.now());
            stmt.setString(4, message.toPlain());
            stmt.executeUpdate();
        }
    }

    private static String getJdbcUrl(SqlService sqlService, String jdbcAlias) throws IllegalArgumentException {
        return sqlService.getConnectionUrlFromAlias(jdbcAlias)
                .orElseThrow(() -> new IllegalArgumentException(String.format("JDBC alias with name '%s' not found!", jdbcAlias)));
    }

    static StorageManager createStorageManagerAndTable(PluginContainer container, SqlService sqlService, String jdbcAlias) throws SQLException {
        StorageManager storageManager = new StorageManager(container, sqlService, jdbcAlias);
        storageManager.createTables();
        return storageManager;
    }
}
