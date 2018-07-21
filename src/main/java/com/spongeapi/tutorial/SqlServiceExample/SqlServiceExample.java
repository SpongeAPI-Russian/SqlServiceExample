package com.spongeapi.tutorial.SqlServiceExample;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.sql.SqlService;

import javax.inject.Inject;
import java.io.IOException;
import java.sql.SQLException;

@Plugin(
        id = "sqlserviceexample",
        name = "SqlServiceExample",
        version = "0.1-SNAPSHOT",
        description = "Краткое описание SqlService",
        url = "https://spongeapi.com",
        authors = "Xakep_SDK"
)
public class SqlServiceExample {
    private final Logger logger;
    private final PluginContainer container;
    private final ConfigurationLoader<CommentedConfigurationNode> configLoader;
    private StorageManager storageManager;

    @Inject
    public SqlServiceExample(Logger logger,
                             PluginContainer container,
                             @DefaultConfig(sharedRoot = true) ConfigurationLoader<CommentedConfigurationNode> configLoader) {
        this.logger = logger;
        this.container = container;
        this.configLoader = configLoader;
        this.storageManager = null;
    }

    @Listener
    public void onGamePreInit(GamePreInitializationEvent event) throws IOException, SQLException {
        logger.info("Preparing database");
        SqlService sqlService = Sponge.getServiceManager().provideUnchecked(SqlService.class);
        String jdbcAlias = getJdbcAliasAndSaveDefaults(container, configLoader);
        this.storageManager = StorageManager.createStorageManagerAndTable(container, sqlService, jdbcAlias);
        logger.info("Database prepared!");
    }

    @Listener
    public void onServiceChange(ChangeServiceProviderEvent event) throws IOException, SQLException {
        if (event.getService() == SqlService.class) {
            logger.info("Changing SqlService");
            SqlService sqlService = (SqlService) event.getNewProvider();
            String jdbcAlias;
            if (storageManager != null) {
                jdbcAlias = storageManager.getJdbcAlias();
            } else {
                jdbcAlias = getJdbcAliasAndSaveDefaults(container, configLoader);
            }
            this.storageManager = null;
            this.storageManager = StorageManager.createStorageManagerAndTable(container, sqlService, jdbcAlias);
            logger.info("SqlService changed!");

        }
    }

    @Listener
    public void onPlayerChat(MessageChannelEvent.Chat event, @Root Player player) {
        if (storageManager == null) {
            return;
        }

        Task.builder().async().execute(() -> {
            try {
                storageManager.insertChatMessage(event.getRawMessage(), player);
            } catch (SQLException e) {
                logger.error("Error while saving chat message!", e);
            }
        }).submit(this);
    }

    @Listener
    public void onPluginReload(GameReloadEvent event) throws IOException, SQLException {
        logger.info("Reloading configuration and database connections");
        String jdbcAlias = getJdbcAliasAndSaveDefaults(container, configLoader);
        SqlService sqlService = Sponge.getServiceManager().provideUnchecked(SqlService.class);
        this.storageManager = null;
        this.storageManager = StorageManager.createStorageManagerAndTable(container, sqlService, jdbcAlias);
        logger.info("Reloaded!");
    }

    private static String getJdbcAliasAndSaveDefaults(PluginContainer container, ConfigurationLoader<CommentedConfigurationNode> configLoader)
            throws IOException {
        ConfigurationNode rootNode = configLoader.load(ConfigurationOptions.defaults().setShouldCopyDefaults(true));
        String urlAlias = rootNode.getNode("alias").getString(container.getId());
        configLoader.save(rootNode);
        return urlAlias;
    }
}
