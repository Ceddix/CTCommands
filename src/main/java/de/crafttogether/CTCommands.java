package de.crafttogether;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;

import de.crafttogether.ctcommands.commands.CTextCommand;
import de.crafttogether.ctcommands.events.*;

import de.crafttogether.ctcommands.text.JoinMessagesConfig;
import de.crafttogether.ctcommands.text.Texts;
import org.slf4j.Logger;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

@Plugin(
        id = "ctcommands",
        name = "CTCommands",
        version = "1.1.1-SNAPSHOT",
        authors = {"J0schlZ", "Ceddix"}
)
public final class CTCommands {

    private static CTCommands instance;

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDir;

    private ConfigurationNode whitelist;
    private ConfigurationNode blacklist;
    private ConfigurationNode uuids;
    private JoinMessagesConfig config;

    private LogFile chatLog;
    private LogFile cmdLog;

    @Inject
    public CTCommands(ProxyServer server, Logger logger, @DataDirectory Path dataDir) {
        this.server = server;
        this.logger = logger;
        this.dataDir = dataDir;
        instance = this;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e) {
        // Ordnerstruktur
        createDirIfMissing(dataDir);
        Path logs = dataDir.resolve("logs");
        createDirIfMissing(logs);
        createDirIfMissing(dataDir.resolve("ctext"));

        chatLog = new LogFile(logger, dataDir.resolve("logs").resolve("chat").toString());
        cmdLog  = new LogFile(logger, dataDir.resolve("logs").resolve("commands").toString());
            
        // Konfigurationen laden/erzeugen
        loadConfigs();
        try {
            Path configPath = dataDir.resolve("joinmessages.yml");
            config = new JoinMessagesConfig(configPath);

            if (config.isShowJoin()) {
                logger.info("Join Messages aktiv");
            }

        } catch (IOException exception) {
            logger.error("Config konnte nicht geladen werden", exception);
        }

        // Listener + Commands registrieren
        CommandManager cm = server.getCommandManager();
        cm.register(cm.metaBuilder("ctext").build(), new CTextCommand(this));
        cm.register(cm.metaBuilder("ctcommands").build(), new de.crafttogether.ctcommands.commands.Commands(this));

        server.getEventManager().register(this, new ChatCommandLoggerListener(this, server.getScheduler(), server));
        server.getEventManager().register(this, new CommandsAvailabilityListener(this ));
        server.getEventManager().register(this, new PlayerListener(this));

        logger.info("CTCommands initialized.");
    }

    private void loadConfigs() {
        this.whitelist    = loadYaml("whitelist.yml");
        this.blacklist    = loadYaml("blacklist.yml");
        this.uuids        = loadYaml("uuids.yml");

    }

    // -------- config handling --------
    public boolean ReloaddConfig() {
        try {
            loadConfigs();
            config.reload();
            return true;
        } catch (Throwable t) {
            logger.error("Fehler beim Reload von");
            return false;
        }
    }

    private CommentedConfigurationNode loadYaml(String fileName) {
        Path target = dataDir.resolve(fileName);

        // falls nicht vorhanden -> aus JAR kopieren
        if (Files.notExists(target)) {
            try (InputStream in = resource(fileName)) {
                if (in == null) {
                    logger.warn("Resource {} nicht gefunden – leere Datei wird erstellt.", fileName);
                    Files.createFile(target);
                } else {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException ex) {
                logger.error("Konnte {} nicht erstellen/kopieren", fileName, ex);
            }
        }

        try {
            return YamlConfigurationLoader.builder()
                    .path(target)
                    .build()
                    .load();
        } catch (IOException ex) {
            logger.error("Konnte {} nicht laden", fileName, ex);
            return null;
        }
    }

    public void saveYaml(ConfigurationNode node, String fileName) {
        if (node == null) return;
        Path target = dataDir.resolve(fileName);
        try {
            YamlConfigurationLoader.builder()
                    .path(target)
                    .build()
                    .save(node);
        } catch (IOException ex) {
            logger.error("Konnte {} nicht speichern", fileName, ex);
        }
    }

    public void getVersion(CommandSource source){
        // Version/Autoren aus der @Plugin-Annotation der Main-Klasse ziehen (falls vorhanden)
        String version = "unknown";
        String authors = "Unknown";
        try {
            com.velocitypowered.api.plugin.Plugin anno =
                    CTCommands.class.getAnnotation(com.velocitypowered.api.plugin.Plugin.class);
            if (anno != null) {
                if (!anno.version().isEmpty()) version = anno.version();
                String[] auth = anno.authors();
                if (auth != null && auth.length > 0) authors = String.join(", ", auth);
            }
        } catch (Throwable ignored) { }

        source.sendMessage(Texts.parse("&8&m----------------------"));
        source.sendMessage(Texts.parse("  &3CTCommands &b" + version));
        source.sendMessage(Texts.parse("  &bby " + authors));
        source.sendMessage(Texts.parse("&8&m----------------------"));
    }

    private InputStream resource(String name) {
        return CTCommands.class.getClassLoader().getResourceAsStream(name);
    }

    private void createDirIfMissing(Path p) {
        try {
            Files.createDirectories(p);
        } catch (IOException ex) {
            logger.error("Konnte Ordner {} nicht erstellen", p, ex);
        }
    }

    // -------- lifecycle --------

    public void shutdown() {
        if (chatLog != null) chatLog.close();
        if (cmdLog  != null) cmdLog.close();
    }

    // -------- getters (Migration-kompatibel) --------

    public static CTCommands getInstance() { return instance; }
    public ProxyServer getServer() { return server; }
    public Logger getLogger() { return logger; }
    public Path getDataDir() { return dataDir; }
    public JoinMessagesConfig getConfig() { return config; }


    public ConfigurationNode getWhitelist() { return whitelist; }
    public ConfigurationNode getBlacklist() { return blacklist; }
    public ConfigurationNode getUUIDs() { return uuids; }
    public void setUUIDs(ConfigurationNode uuids) { this.uuids = uuids; }

    public LogFile getChatLog() { return chatLog; }
    public LogFile getCmdLog()  { return cmdLog; }

    @Subscribe(order = PostOrder.LAST)
    public void onAfterInitialization(ProxyInitializeEvent event) {
        CommandManager cm = server.getCommandManager();

        CommandMeta serverMeta = cm.getCommandMeta("server");
        CommandMeta sendMeta = cm.getCommandMeta("send");

        if (serverMeta != null) {
            logger.info("Found command 'server', unregistering it...");
            cm.unregister("server");
        } else {
            logger.warn("Could not find command 'server' to unregister!");
        }

        if (sendMeta != null) {
            logger.info("Found command 'send', unregistering it...");
            cm.unregister("send");
        } else {
            logger.warn("Could not find command 'send' to unregister!");
        }

    }
}
