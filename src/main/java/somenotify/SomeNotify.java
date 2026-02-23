package somenotify;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Plugin(
        id = "somenotify",
        name = "SomeNotify",
        version = "1.0.0",
        description = "Broadcast notify messages to all servers behind Velocity",
        authors = {"bonda"}
)
public final class SomeNotify {
    private static final String NOTIFY_PERMISSION = "somenotify.notify";
    private static final String RELOAD_PERMISSION = "somenotify.reload";
    private static final String DEFAULT_FORMAT = "<gray>[<gold>Notify</gold>]</gray> <yellow>{sender}</yellow><gray>:</gray> <white>{message}</white>";

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;
    private final MiniMessage miniMessage;

    private Config config;

    @Inject
    public SomeNotify(ProxyServer proxyServer, Logger logger, @com.velocitypowered.api.plugin.annotation.DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.config = loadConfigOrDefault();

        CommandManager commandManager = proxyServer.getCommandManager();
        commandManager.register(commandManager.metaBuilder("notify").build(), new NotifyCommand());

        logger.info("SomeNotify initialized.");
    }

    private Config loadConfigOrDefault() {
        Optional<Config> loaded = tryLoadConfig();
        if (loaded.isPresent()) {
            return loaded.get();
        }
        logger.warn("Using built-in fallback config.");
        return Config.defaultConfig();
    }

    private Optional<Config> tryLoadConfig() {
        try {
            return Optional.of(loadConfigInternal());
        } catch (Exception exception) {
            logger.error("Failed to load config.yml", exception);
            return Optional.empty();
        }
    }

    private Config loadConfigInternal() throws IOException, ConfigurateException {
        Path configPath = dataDirectory.resolve("config.yml");
        Files.createDirectories(dataDirectory);

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configPath)
                .nodeStyle(NodeStyle.BLOCK)
                .build();

        CommentedConfigurationNode root;
        if (Files.notExists(configPath)) {
            root = loader.createNode();
            writeDefaults(root);
            loader.save(root);
            logger.info("Default config created at {}", configPath);
            return readConfig(root);
        }

        root = loader.load();
        boolean changed = writeDefaults(root);
        if (changed) {
            loader.save(root);
        }
        return readConfig(root);
    }

    private boolean writeDefaults(CommentedConfigurationNode root) throws ConfigurateException {
        boolean changed = false;

        changed |= setIfMissing(root.node("language"), "ru");
        changed |= setIfMissing(root.node("permission"), NOTIFY_PERMISSION);
        changed |= setIfMissing(root.node("reload-permission"), RELOAD_PERMISSION);
        changed |= setIfMissing(root.node("message-format"), DEFAULT_FORMAT);
        changed |= setIfMissing(root.node("sender-mode"), "player");
        changed |= setIfMissing(root.node("custom-sender-name"), "Администрация");
        changed |= setIfMissing(root.node("console-name"), "Console");

        changed |= setIfMissing(root.node("messages", "ru", "usage"), "<red>Использование: /notify <сообщение> или /notify reload</red>");
        changed |= setIfMissing(root.node("messages", "ru", "no-permission"), "<red>У вас нет прав на использование этой команды.</red>");
        changed |= setIfMissing(root.node("messages", "ru", "empty-message"), "<red>Сообщение не может быть пустым.</red>");
        changed |= setIfMissing(root.node("messages", "ru", "sent"), "<green>Сообщение отправлено ({online} игроков).</green>");
        changed |= setIfMissing(root.node("messages", "ru", "reload-success"), "<green>Конфиг успешно перезагружен.</green>");
        changed |= setIfMissing(root.node("messages", "ru", "reload-failed"), "<red>Не удалось перезагрузить конфиг. Проверьте консоль.</red>");

        changed |= setIfMissing(root.node("messages", "en", "usage"), "<red>Usage: /notify <message> or /notify reload</red>");
        changed |= setIfMissing(root.node("messages", "en", "no-permission"), "<red>You do not have permission to use this command.</red>");
        changed |= setIfMissing(root.node("messages", "en", "empty-message"), "<red>Message cannot be empty.</red>");
        changed |= setIfMissing(root.node("messages", "en", "sent"), "<green>Message sent ({online} players).</green>");
        changed |= setIfMissing(root.node("messages", "en", "reload-success"), "<green>Config reloaded successfully.</green>");
        changed |= setIfMissing(root.node("messages", "en", "reload-failed"), "<red>Config reload failed. Check console.</red>");

        return changed;
    }

    private boolean setIfMissing(CommentedConfigurationNode node, Object value) throws ConfigurateException {
        if (node.virtual()) {
            node.set(value);
            return true;
        }
        return false;
    }

    private Config readConfig(CommentedConfigurationNode root) throws ConfigurateException {
        Language language = Language.from(root.node("language").getString("ru"));
        String permission = root.node("permission").getString(NOTIFY_PERMISSION);
        String reloadPermission = root.node("reload-permission").getString(RELOAD_PERMISSION);
        String messageFormat = readMultiLineValue(root.node("message-format"), DEFAULT_FORMAT);
        String senderMode = root.node("sender-mode").getString("player");
        String customSenderName = root.node("custom-sender-name").getString("Администрация");
        String consoleName = root.node("console-name").getString("Console");

        Messages ruMessages = new Messages(
                root.node("messages", "ru", "usage").getString("<red>Использование: /notify <сообщение> или /notify reload</red>"),
                root.node("messages", "ru", "no-permission").getString("<red>У вас нет прав на использование этой команды.</red>"),
                root.node("messages", "ru", "empty-message").getString("<red>Сообщение не может быть пустым.</red>"),
                root.node("messages", "ru", "sent").getString("<green>Сообщение отправлено ({online} игроков).</green>"),
                root.node("messages", "ru", "reload-success").getString("<green>Конфиг успешно перезагружен.</green>"),
                root.node("messages", "ru", "reload-failed").getString("<red>Не удалось перезагрузить конфиг. Проверьте консоль.</red>")
        );

        Messages enMessages = new Messages(
                root.node("messages", "en", "usage").getString("<red>Usage: /notify <message> or /notify reload</red>"),
                root.node("messages", "en", "no-permission").getString("<red>You do not have permission to use this command.</red>"),
                root.node("messages", "en", "empty-message").getString("<red>Message cannot be empty.</red>"),
                root.node("messages", "en", "sent").getString("<green>Message sent ({online} players).</green>"),
                root.node("messages", "en", "reload-success").getString("<green>Config reloaded successfully.</green>"),
                root.node("messages", "en", "reload-failed").getString("<red>Config reload failed. Check console.</red>")
        );

        return new Config(language, permission, reloadPermission, messageFormat, senderMode, customSenderName, consoleName, ruMessages, enMessages);
    }

    private String readMultiLineValue(CommentedConfigurationNode node, String fallback) throws ConfigurateException {
        String asString = node.getString();
        if (asString != null) {
            return asString;
        }

        List<String> lines = node.getList(String.class);
        if (lines != null && !lines.isEmpty()) {
            return String.join("\n", lines);
        }

        return fallback;
    }

    private Component parseMini(String text) {
        return miniMessage.deserialize(text);
    }

    private final class NotifyCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            Config currentConfig = Optional.ofNullable(config).orElseGet(Config::defaultConfig);
            Messages messages = currentConfig.messagesForSelectedLanguage();
            String[] arguments = invocation.arguments();

            if (arguments.length == 1 && arguments[0].equalsIgnoreCase("reload")) {
                if (!invocation.source().hasPermission(currentConfig.reloadPermission())) {
                    invocation.source().sendMessage(parseMini(messages.noPermissionMessage()));
                    return;
                }

                Optional<Config> reloaded = tryLoadConfig();
                if (reloaded.isPresent()) {
                    config = reloaded.get();
                    invocation.source().sendMessage(parseMini(config.messagesForSelectedLanguage().reloadSuccessMessage()));
                } else {
                    invocation.source().sendMessage(parseMini(messages.reloadFailedMessage()));
                }
                return;
            }

            if (!invocation.source().hasPermission(currentConfig.permission())) {
                invocation.source().sendMessage(parseMini(messages.noPermissionMessage()));
                return;
            }

            if (arguments.length == 0) {
                invocation.source().sendMessage(parseMini(messages.usageMessage()));
                return;
            }

            String rawMessage = String.join(" ", arguments).trim();
            if (rawMessage.isEmpty()) {
                invocation.source().sendMessage(parseMini(messages.emptyMessage()));
                return;
            }

            String senderName = resolveSender(currentConfig, invocation.source() instanceof Player ? (Player) invocation.source() : null);
            String payload = currentConfig.messageFormat()
                    .replace("{sender}", escapeTags(senderName))
                    .replace("{message}", escapeTags(rawMessage));

            Component broadcast = parseMini(payload);

            int online = 0;
            for (Player player : proxyServer.getAllPlayers()) {
                player.sendMessage(broadcast);
                online++;
            }

            String sentMessage = messages.sentMessage().replace("{online}", String.valueOf(online));
            invocation.source().sendMessage(parseMini(sentMessage));
        }

        private String resolveSender(Config currentConfig, Player player) {
            String mode = currentConfig.senderMode().toLowerCase(Locale.ROOT);
            return switch (mode) {
                case "custom" -> currentConfig.customSenderName();
                case "console" -> currentConfig.consoleName();
                default -> player != null ? player.getUsername() : currentConfig.consoleName();
            };
        }
    }

    private static String escapeTags(String input) {
        return input.replace("<", "\\<");
    }

    private enum Language {
        RU,
        EN;

        private static Language from(String value) {
            if (value == null) {
                return RU;
            }
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "en" -> EN;
                default -> RU;
            };
        }
    }

    private record Messages(
            String usageMessage,
            String noPermissionMessage,
            String emptyMessage,
            String sentMessage,
            String reloadSuccessMessage,
            String reloadFailedMessage
    ) {
    }

    private record Config(
            Language language,
            String permission,
            String reloadPermission,
            String messageFormat,
            String senderMode,
            String customSenderName,
            String consoleName,
            Messages ruMessages,
            Messages enMessages
    ) {
        private Messages messagesForSelectedLanguage() {
            return language == Language.EN ? enMessages : ruMessages;
        }

        private static Config defaultConfig() {
            Messages ru = new Messages(
                    "<red>Использование: /notify <сообщение> или /notify reload</red>",
                    "<red>У вас нет прав на использование этой команды.</red>",
                    "<red>Сообщение не может быть пустым.</red>",
                    "<green>Сообщение отправлено ({online} игроков).</green>",
                    "<green>Конфиг успешно перезагружен.</green>",
                    "<red>Не удалось перезагрузить конфиг. Проверьте консоль.</red>"
            );
            Messages en = new Messages(
                    "<red>Usage: /notify <message> or /notify reload</red>",
                    "<red>You do not have permission to use this command.</red>",
                    "<red>Message cannot be empty.</red>",
                    "<green>Message sent ({online} players).</green>",
                    "<green>Config reloaded successfully.</green>",
                    "<red>Config reload failed. Check console.</red>"
            );

            return new Config(
                    Language.RU,
                    NOTIFY_PERMISSION,
                    RELOAD_PERMISSION,
                    DEFAULT_FORMAT,
                    "player",
                    "Администрация",
                    "Console",
                    ru,
                    en
            );
        }
    }
}
