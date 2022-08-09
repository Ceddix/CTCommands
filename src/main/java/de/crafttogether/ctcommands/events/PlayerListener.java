package de.crafttogether.ctcommands.events;

import de.crafttogether.CTCommands;
import de.myzelyam.api.vanish.BungeeVanishAPI;
import de.themoep.minedown.MineDown;
import litebans.api.Database;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
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

        boolean firstJoin = false;

        /* JOIN MESSAGES */
        if (plugin.getJoinMessages().getBoolean("showjoin")) {
            boolean isBanned = Database.get().isPlayerBanned(player.getUniqueId(), null);
            boolean isMuted = Database.get().isPlayerMuted(player.getUniqueId(), null);
            /*boolean vanished = ... TODO: add vanish check */

            if (isBanned || isMuted)
                return;

            BaseComponent[] joinformat = TextComponent.fromLegacyText(new MineDown(plugin.getJoinMessages().getString("serverjoin")).toString().replace("%NAME%", player.toString()));
            BaseComponent[] silentjoinformat = TextComponent.fromLegacyText(new MineDown(plugin.getJoinMessages().getString("silentjoin")).toString().replace("%NAME%", player.toString()));
            BaseComponent[] welcomeMessage = TextComponent.fromLegacyText(new MineDown(plugin.getJoinMessages().getString("welcome_message")).toString().replace("%NAME%", player.toString()));
            BaseComponent[] privateWelcomeMessage = TextComponent.fromLegacyText(new MineDown(plugin.getJoinMessages().getString("private_welcome_message")).toString().replace("%NAME%", player.toString()));

            boolean broadcastWelcome = true;
            if (plugin.getJoinMessages().contains("welcome")) {
                broadcastWelcome = plugin.getJoinMessages().getBoolean("welcome");
            }

            boolean privateWelcome = false;
            if (plugin.getJoinMessages().contains("private_welcome")) {
                privateWelcome = plugin.getJoinMessages().getBoolean("private_welcome");
            }

            firstJoin = !plugin.getUUIDs().getStringList("uuids").contains(player.getUniqueId().toString());

            boolean broadcastJoin = !player.hasPermission("ctcommands.staff.silentjoin");
            for (ProxiedPlayer onlineplayer : ProxyServer.getInstance().getPlayers()) {

                if (broadcastJoin) {

                    if (firstJoin && broadcastWelcome) {
                        onlineplayer.sendMessage(welcomeMessage);
                    }

                    if (firstJoin && privateWelcome && onlineplayer.getName().equals(player.getName())) {
                        onlineplayer.sendMessage(privateWelcomeMessage);
                    }

                    onlineplayer.sendMessage(joinformat);

                } else {

                    if (onlineplayer.hasPermission("ctcommands.staff.silentjoin") ) {
                        onlineplayer.sendMessage(silentjoinformat);
                    }

                }

            }

        }

        boolean finalFirstJoin = firstJoin;
        this.plugin.getProxy().getScheduler().runAsync(this.plugin, () -> {
            LogFile cmdLog = plugin.getCmdLog();
            LogFile chatLog = plugin.getChatLog();

            if (cmdLog != null)
                cmdLog.write(player.getName() + " hat das Spiel betreten.");

            if (chatLog != null)
                chatLog.write(player.getName() + " hat das Spiel betreten.");

            if (finalFirstJoin) {
                Configuration uuids = plugin.getUUIDs();

                List<String> uuidList = uuids.getStringList("uuids");
                uuidList.add(player.getUniqueId().toString());
                uuids.set("uuids", uuidList);

                plugin.setUUIDs(uuids);
                plugin.saveConfig(uuids, "uuids.yml");
            }

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
                    e.getPlayer().sendMessage(new MineDown(sc.nextLine()).toComponent());
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

        /* LEAVE MESSAGES */
        if (plugin.getJoinMessages().getBoolean("showleave")) {
            boolean isBanned = Database.get().isPlayerBanned(player.getUniqueId(), null);
            boolean isMuted = Database.get().isPlayerMuted(player.getUniqueId(), null);
            /*boolean vanished = ... TODO: add vanish check */

            if (isBanned || isMuted)
                return;

            BaseComponent[] leaveformat = TextComponent.fromLegacyText(new MineDown(plugin.getJoinMessages().getString("serverleave")).toString().replace("%NAME%", player.toString()));
            BaseComponent[] silentleaveformat = TextComponent.fromLegacyText(new MineDown(plugin.getJoinMessages().getString("silentleave")).toString().replace("%NAME%", player.toString()));

            boolean broadcastJoin = !player.hasPermission("ctcommands.staff.silentjoin");
            for (ProxiedPlayer onlineplayer : ProxyServer.getInstance().getPlayers()) {

                if (broadcastJoin) {

                    onlineplayer.sendMessage(leaveformat);

                } else {

                    if (onlineplayer.hasPermission("ctcommands.staff.silentjoin") ) {
                        onlineplayer.sendMessage(silentleaveformat);
                    }

                }

            }

        }

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