package de.crafttogether.ctcommands.events;

import de.crafttogether.CTCommands;
import de.myzelyam.api.vanish.BungeeVanishAPI;
import de.themoep.minedown.MineDown;
import litebans.api.Database;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class PlayerListener
implements Listener {
    private CTCommands plugin;

    public PlayerListener(CTCommands plugin) {
        this.plugin = plugin;
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @EventHandler
    public void onPlayerJoin(final PostLoginEvent e) {
        final ProxiedPlayer player = e.getPlayer();
        plugin.getLogger().info(player.getName() + " is using protocol-version: " + player.getPendingConnection().getVersion());

        this.plugin.getProxy().getScheduler().runAsync(this.plugin, new Runnable() {
            @Override
            public void run() {
                CTCommands plugin = CTCommands.getInstance();
                LogFile cmdLog = plugin.getCmdLog();
                LogFile chatLog = plugin.getChatLog();

                boolean banned = Database.get().isPlayerBanned(player.getUniqueId(), null);
                boolean muted = Database.get().isPlayerMuted(player.getUniqueId(), null);

                if (muted || banned)
                    return;

                if (cmdLog != null)
                    cmdLog.write(player.getName() + " hat das Spiel betreten.");

                if (chatLog != null)
                    chatLog.write(player.getName() + " hat das Spiel betreten.");

                /* welcome text */
                File file = new File(PlayerListener.this.plugin.getDataFolder(), File.separator + "ctext" + File.separator + "welcomeText.txt");
                Scanner sc = null;

                if (!file.exists()) {
                    return;
                }
                try {
                    sc = new Scanner(file);
                } catch (FileNotFoundException exx) {
                    ProxyServer.getInstance().getLogger().warning("welcomeText.txt not found");
                }

                if (sc != null) {
                    while (sc.hasNextLine()) {
                        e.getPlayer().sendMessage(new TextComponent(new MineDown(sc.nextLine()).toComponent()));
                    }
                }
            }
        });

        /*if (!isVanished(e.getPlayer())) {
            for (ProxiedPlayer p: this.plugin.getProxy().getPlayers()) {
                if (p.equals(e.getPlayer()))
                    continue;
                p.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&e" + e.getPlayer().getDisplayName() + " &ehat das Spiel betreten")));
            }
        }*/
    }


    @EventHandler
    public void onPlayerLeave(PlayerDisconnectEvent e) {
        final ProxiedPlayer player = e.getPlayer();

        this.plugin.getProxy().getScheduler().runAsync(this.plugin, new Runnable() {
            @Override
            public void run() {
                CTCommands plugin = CTCommands.getInstance();
                LogFile cmdLog = plugin.getCmdLog();
                LogFile chatLog = plugin.getChatLog();

                if (cmdLog != null)
                    cmdLog.write(player.getName() + " hat das Spiel verlassen.");

                if (chatLog != null)
                    chatLog.write(player.getName() + " hat das Spiel verlassen.");
            }
        });

        /*if (!isVanished(e.getPlayer())) {
	        for (ProxiedPlayer p: this.plugin.getProxy().getPlayers()) {
	            if (p.equals(e.getPlayer()))
	                continue;
	            p.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&e" + e.getPlayer().getDisplayName() + " &ehat das Spiel verlassen")));
	        }
        }*/
    }

    private boolean isVanished(ProxiedPlayer player) {
        Boolean isVanished = false;
        try {
            isVanished = BungeeVanishAPI.isInvisible(player);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isVanished;
    }
}