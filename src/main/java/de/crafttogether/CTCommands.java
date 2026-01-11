package de.crafttogether;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;

import de.crafttogether.ctcommands.commands.CTextCommand;
import de.crafttogether.ctcommands.events.CommandVisibilityGuard;
import de.crafttogether.ctcommands.events.LogFile;

import org.slf4j.Logger;

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
    private ConfigurationNode joinMessages;
    private ConfigurationNode uuids;

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

        // Listener + Commands registrieren
        CommandManager cm = server.getCommandManager();
        cm.register(cm.metaBuilder("ctext").build(), new CTextCommand(this));
        cm.register(cm.metaBuilder("ctcommands").build(), new de.crafttogether.ctcommands.commands.Commands(this));

        // Protocolize (Velocity) – deine Listener sollten kompatibel sein
        server.getEventManager().register(this, new CommandVisibilityGuard(this));

        logger.info("CTCommands initialized.");
    }

    // -------- config handling --------

    private void loadConfigs() {
        this.whitelist    = loadYaml("whitelist.yml");
        this.blacklist    = loadYaml("blacklist.yml");
        this.joinMessages = loadYaml("joinmessages.yml");
        this.uuids        = loadYaml("uuids.yml");
    }

    private ConfigurationNode loadYaml(String fileName) {
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

    public ConfigurationNode getWhitelist() { return whitelist; }
    public ConfigurationNode getBlacklist() { return blacklist; }
    public ConfigurationNode getJoinMessages() { return joinMessages; }
    public ConfigurationNode getUUIDs() { return uuids; }
    public void setUUIDs(ConfigurationNode uuids) { this.uuids = uuids; }

    public LogFile getChatLog() { return chatLog; }
    public LogFile getCmdLog()  { return cmdLog; }
}
