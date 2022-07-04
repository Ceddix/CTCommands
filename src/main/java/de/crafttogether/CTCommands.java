package de.crafttogether;

import com.google.common.io.ByteStreams;
import de.crafttogether.ctcommands.commands.Commands;
import de.crafttogether.ctcommands.commands.CTextCommand;
import de.crafttogether.ctcommands.events.ChatPacketListener;
import de.crafttogether.ctcommands.events.CommandsPacketListener;
import de.crafttogether.ctcommands.events.LogFile;
import de.crafttogether.ctcommands.events.PlayerListener;
import dev.simplix.protocolize.api.Protocolize;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;

public class CTCommands
extends Plugin {
    private static CTCommands plugin;
    private Configuration whitelist;
    private Configuration blacklist;
    private Configuration joinMessages;
    private Configuration uuids;

    private LogFile chatLog = null;
    private LogFile cmdLog = null;

    public void onEnable() {
        plugin = this;

        if (!getDataFolder().exists()) getDataFolder().mkdir();

        // Create Logfiles
        File logDir = new File(getDataFolder() + File.separator + "logs");
        if (!logDir.exists()) logDir.mkdir();

        chatLog = new LogFile(this, getDataFolder() + File.separator + "logs" + File.separator + "chat");
        cmdLog = new LogFile(this, getDataFolder() + File.separator + "logs" + File.separator + "commands");

        // Create CText-Directory
        File cTextDir = new File(getDataFolder() + File.separator + "ctext");
        if (!cTextDir.exists()) cTextDir.mkdir();

        loadConfig();

        new PlayerListener(this);
        getProxy().getPluginManager().registerCommand(this, new CTextCommand());
        getProxy().getPluginManager().registerCommand(this, new Commands());
        Protocolize.listenerProvider().registerListener(new ChatPacketListener());
        Protocolize.listenerProvider().registerListener(new CommandsPacketListener());
    }

    public void onDisable() {
        chatLog.close();
        cmdLog.close();
    }

    public void loadConfig() {
        this.whitelist = loadConfig("whitelist.yml");
        this.blacklist = loadConfig("blacklist.yml");
        this.joinMessages = loadConfig("joinmessages.yml");
        this.uuids = loadConfig("uuids.yml");
    }

    public void saveConfig(Configuration config, String fileName) {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, new File(getDataFolder(), fileName));
        } catch (IOException e) {
            System.out.println("[CTCommands] Unable to save to file " + fileName);
            e.printStackTrace();
        }
    }

    private Configuration loadConfig(String fileName) {
        File file = new File(getDataFolder() + File.separator + fileName);
        Configuration config = null;

        try {
            if (!file.exists()) {
                file.createNewFile();
                InputStream is = getResourceAsStream(fileName);
                OutputStream os = new FileOutputStream(file);
                ByteStreams.copy(is, os);
            }
        } catch(IOException e) {
            System.out.println("[CTCommands] Unable to create file " + fileName);
            e.printStackTrace();
        }

        try {
            config = ConfigurationProvider.getProvider(net.md_5.bungee.config.YamlConfiguration.class).load(new InputStreamReader(new FileInputStream(file), "UTF8"));
        } catch(IOException e) {
            e.printStackTrace();
        }

        return config;
    }

    public void setUUIDs(Configuration uuids) {
        this.uuids = uuids;
    }

    public Configuration getWhitelist() {
        return this.whitelist;
    }
    public Configuration getBlacklist() {
        return this.blacklist;
    }

    public Configuration getJoinMessages() {
        return this.joinMessages;
    }

    public Configuration getUUIDs() {
        return this.uuids;
    }

    public LogFile getChatLog() {
      return this.chatLog;
    }

    public LogFile getCmdLog() {
      return this.cmdLog;
    }

    public static CTCommands getInstance() {
        return plugin;
    }
}