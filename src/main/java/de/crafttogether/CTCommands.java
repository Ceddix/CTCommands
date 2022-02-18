package de.crafttogether;

import com.google.common.io.ByteStreams;
import de.crafttogether.ctcommands.commands.CTextCommand;
import de.crafttogether.ctcommands.events.ChatPacketListener;
import de.crafttogether.ctcommands.events.CommandsPacketListener;
import de.crafttogether.ctcommands.events.LogFile;
import de.crafttogether.ctcommands.events.PlayerListener;
import dev.simplix.protocolize.api.Protocolize;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;

import java.io.*;

public class CTCommands
extends Plugin {
    private static CTCommands plugin;
    private Configuration whitelist;
    private Configuration blacklist;

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
            throw new RuntimeException("Unable to create " + fileName, e);
        }

        try {
            config = ConfigurationProvider.getProvider(net.md_5.bungee.config.YamlConfiguration.class).load(new InputStreamReader(new FileInputStream(file), "UTF8"));
        } catch(IOException e) {
            e.printStackTrace();
        }

        return config;
    }

    public Configuration getWhitelist() {
        return this.whitelist;
    }
    public Configuration getBlacklist() {
        return this.blacklist;
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