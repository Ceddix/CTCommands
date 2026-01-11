package de.crafttogether.ctcommands.events;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import de.crafttogether.CTCommands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.lang.reflect.Method;

public final class ChatAndCommandListener {

    private final CTCommands plugin;

    public ChatAndCommandListener(CTCommands plugin) {
        this.plugin = plugin;
    }

    /** Chatnachrichten protokollieren (nicht mit Slash beginnend) */
    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        final Player player = event.getPlayer();

        String message = "";
        try {
            // Neue Velocity-API (>=3.1.0): chatMessage()
            Method m = event.getClass().getMethod("chatMessage");
            Object comp = m.invoke(event);
            if (comp instanceof Component) {
                message = PlainTextComponentSerializer.plainText().serialize((Component) comp);
            }
        } catch (NoSuchMethodException nsme) {
            try {
                // Ältere API: getMessage()
                Method m = event.getClass().getMethod("getMessage");
                Object comp = m.invoke(event);
                if (comp instanceof Component) {
                    message = PlainTextComponentSerializer.plainText().serialize((Component) comp);
                }
            } catch (Exception ignore) {
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (message.isEmpty() || message.startsWith("/"))
            return;

        LogFile chatLog = plugin.getChatLog();
        if (chatLog != null) {
            chatLog.write(player.getUsername() + ": " + message);
        }
    }

    /** Befehle protokollieren */
    @Subscribe
    public void onCommandExecute(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player player))
            return;

        // getCommand() liefert "command arg1 arg2"
        String raw = event.getCommand();
        String full = "/" + raw;

        LogFile cmdLog = plugin.getCmdLog();
        if (cmdLog != null) {
            cmdLog.write(player.getUsername() + " sendet Befehl: " + full);
        }
    }
}
