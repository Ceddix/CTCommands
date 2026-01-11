package de.crafttogether.ctcommands.events;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.crafttogether.CTCommands;
import litebans.api.Database;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.*;

public class PlayerListener {
    private final CTCommands plugin;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDir;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public PlayerListener(CTCommands plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.logger = plugin.getLogger();
        this.dataDir = plugin.getDataDir();
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) throws SerializationException {
        final Player player = event.getPlayer();
        logger.info("{} connected with protocol version: {}", player.getUsername(), player.getProtocolVersion().getProtocol());

        boolean firstJoin = false;

        // JOIN MESSAGES
        ConfigurationNode jm = plugin.getJoinMessages();
        if (jm != null && jm.node("showjoin").getBoolean(true)) {
            if (isLiteBansBanned(player) || isLiteBansMuted(player))
                return;

            Component joinformat        = parsed(jm.node("serverjoin").getString(""), player);
            Component silentjoinformat  = parsed(jm.node("silentjoin").getString(""), player);
            Component welcomeMessage    = parsed(jm.node("welcome_message").getString(""), player);
            Component privateWelcomeMsg = parsed(jm.node("private_welcome_message").getString(""), player);

            boolean broadcastWelcome = jm.node("welcome").getBoolean(true);
            boolean privateWelcome   = jm.node("private_welcome").getBoolean(false);

            firstJoin = !uuidList().contains(player.getUniqueId().toString());
            boolean broadcastJoin = !has(player, "ctcommands.staff.silentjoin");

            for (Player online : server.getAllPlayers()) {
                if (broadcastJoin) {
                    if (firstJoin && broadcastWelcome) {
                        online.sendMessage(welcomeMessage);
                    }
                    if (firstJoin && privateWelcome && online.getUniqueId().equals(player.getUniqueId())) {
                        online.sendMessage(privateWelcomeMsg);
                    }
                    online.sendMessage(joinformat);
                } else {
                    if (has(online, "ctcommands.staff.silentjoin")) {
                        online.sendMessage(silentjoinformat);
                    }
                }
            }
        }

        final boolean finalFirstJoin = firstJoin;

        // Async Task: Logs, UUID speichern, Welcome-Text aus Datei
        server.getScheduler().buildTask(plugin, () -> {
            if (plugin.getCmdLog() != null)
                plugin.getCmdLog().write(player.getUsername() + " hat das Spiel betreten.");
            if (plugin.getChatLog() != null)
                plugin.getChatLog().write(player.getUsername() + " hat das Spiel betreten.");

            if (finalFirstJoin) {
                ConfigurationNode uuids = plugin.getUUIDs();
                List<String> list = null;
                try {
                    list = uuidList();
                } catch (SerializationException e) {
                    throw new RuntimeException(e);
                }
                list.add(player.getUniqueId().toString());
                try {
                    uuids.node("uuids").set(List.class, list);
                } catch (SerializationException e) {
                    throw new RuntimeException(e);
                }
                plugin.setUUIDs(uuids);
                plugin.saveYaml(uuids, "uuids.yml");
            }

            // welcomeText.txt
            File file = dataDir.resolve("ctext").resolve("welcomeText.txt").toFile();
            if (!file.exists()) return;

            try (Scanner sc = new Scanner(file)) {
                while (sc.hasNextLine()) {
                    player.sendMessage(miniMessage.deserialize(sc.nextLine().replace("%NAME%", player.getUsername())));
                }
            } catch (FileNotFoundException ex) {
                logger.warn("welcomeText.txt not found");
            } catch (Exception ex) {
                logger.error("Fehler beim Senden des Welcome-Texts", ex);
            }
        }).schedule();
    }

    @Subscribe
    public void onPlayerLeave(DisconnectEvent event) {
        final Player player = event.getPlayer();

        // LEAVE MESSAGES
        ConfigurationNode jm = plugin.getJoinMessages();
        if (jm != null && jm.node("showleave").getBoolean(true)) {
            if (isLiteBansBanned(player) || isLiteBansMuted(player))
                return;

            Component leaveformat       = parsed(jm.node("serverleave").getString(""), player);
            Component silentleaveformat = parsed(jm.node("silentleave").getString(""), player);

            boolean broadcastLeave = !has(player, "ctcommands.staff.silentjoin");
            for (Player online : server.getAllPlayers()) {
                if (broadcastLeave) {
                    online.sendMessage(leaveformat);
                } else {
                    if (has(online, "ctcommands.staff.silentjoin")) {
                        online.sendMessage(silentleaveformat);
                    }
                }
            }
        }

        server.getScheduler().buildTask(plugin, () -> {
            if (plugin.getCmdLog() != null)
                plugin.getCmdLog().write(player.getUsername() + " hat das Spiel verlassen.");
            if (plugin.getChatLog() != null)
                plugin.getChatLog().write(player.getUsername() + " hat das Spiel verlassen.");
        }).schedule();
    }

    // ---------- Helpers ----------

    private boolean has(PermissionSubject subject, String perm) {
        return subject.hasPermission(perm);
    }

    private Component parsed(String raw, Player player) {
        if (raw == null) raw = "";
        String replaced = raw.replace("%NAME%", player.getUsername());
        return miniMessage.deserialize(replaced);
    }

    private List<String> uuidList() throws SerializationException {
        List<String> list = plugin.getUUIDs() != null
                ? plugin.getUUIDs().node("uuids").getList(String.class)
                : null;
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    private boolean isLiteBansBanned(Player p) {
        try {
            return Database.get().isPlayerBanned(p.getUniqueId(), null);
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean isLiteBansMuted(Player p) {
        try {
            return Database.get().isPlayerMuted(p.getUniqueId(), null);
        } catch (Throwable t) {
            return false;
        }
    }

    @SuppressWarnings("unused")
    private boolean isVanished(Player p) {
        return false;
    }
}
