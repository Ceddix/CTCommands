package de.crafttogether.ctcommands.events;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import de.crafttogether.CTCommands;
import com.velocitypowered.api.scheduler.Scheduler;


public final class ChatCommandLoggerListener {

    private final CTCommands plugin;
    private final Scheduler scheduler;

    public ChatCommandLoggerListener(CTCommands plugin, Scheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    /** Chatnachrichten protokollieren (nicht mit Slash beginnend) */
    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        scheduler.buildTask(plugin, () -> {
            LogFile chatLog = plugin.getChatLog();
            if (chatLog != null) {
                chatLog.write(player.getUsername() + ": " + message);
            }
        }).schedule();
    }

    /** Befehle protokollieren */
    @Subscribe
    public void onCommandExecute(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player player)) {
            return;
        }

        String fullCommand = "/" + event.getCommand().stripLeading();

        scheduler.buildTask(plugin, () -> {
            LogFile cmdLog = plugin.getCmdLog();
            if (cmdLog != null) {
                cmdLog.write(player.getUsername() + " sendet Befehl: " + fullCommand);
            }
        }).schedule();
    }
}
