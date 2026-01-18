package de.crafttogether.ctcommands.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.crafttogether.CTCommands;
import de.crafttogether.ctcommands.text.Texts;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Commands implements SimpleCommand {

    private final CTCommands plugin;
    private final Logger logger;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final String version;
    private final String authors; // Kommagetrennt

    public Commands(CTCommands plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        // Version/Autoren aus der @Plugin-Annotation der Main-Klasse ziehen (falls vorhanden)
        String v = "unknown";
        String a = "Unknown";
        try {
            com.velocitypowered.api.plugin.Plugin anno =
                    CTCommands.class.getAnnotation(com.velocitypowered.api.plugin.Plugin.class);
            if (anno != null) {
                if (!anno.version().isEmpty()) v = anno.version();
                String[] auth = anno.authors();
                if (auth != null && auth.length > 0) a = String.join(", ", auth);
            }
        } catch (Throwable ignored) { }
        this.version = v;
        this.authors = a;
    }

    @Override
    public void execute(Invocation invocation) {
        final var source = invocation.source();

        // Im Original nur für Spieler erlaubt – wir übernehmen das Verhalten.
        if (!(source instanceof Player player)) {
            source.sendMessage(Texts.error("Dieser Befehl ist nur für Spieler."));
            logger.warn("Dieser Befehl ist nur für Spieler. {}",source);
            return;
        }

        String[] args = invocation.arguments();

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("ctcommands.reload")) {
                player.sendMessage(Texts.error("Dazu hast du keine Berechtigung."));
                return;
            }

            // Unterschiedliche Namen je nach deiner Main (loadConfigs vs. loadConfig).
            boolean ok = plugin.ReloaddConfig();
            if (ok) {
                source.sendMessage(Texts.parse("&aConfigurations reloaded!"));
            } else {
                source.sendMessage(Texts.error("Konnte Konfiguration nicht neu laden."));
            }
            return;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("version")) {
            if (!player.hasPermission("ctcommands.version")) {
                player.sendMessage(Texts.error("Dazu hast du keine Berechtigung."));
                return;
            }

            source.sendMessage(Texts.parse("&8&m----------------------"));
            source.sendMessage(Texts.parse("  &3CTCommands &b" + version));
            source.sendMessage(Texts.parse("  &bby " + authors));
            source.sendMessage(Texts.parse("&8&m----------------------"));
            return;
        }

        // Usage-Hinweis
        source.sendMessage(Texts.parse("&7Verwendung: &e/ctcommands &f<reload|version>"));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        List<String> proposals = new ArrayList<>();

        // entspricht: if (args.length == 1) { ... }
        if (args.length == 0) {
            // Player tippt direkt nach "/ctcommands " -> erstes Argument
            if (sender.hasPermission("ctcommands.reload")) {
                proposals.add("reload");
            }
            proposals.add("version");
        } else if (args.length == 1) {
            // Filtern nach Prefix (wie bei dir)
            String prefix = args[0].toLowerCase();

            if (sender.hasPermission("ctcommands.reload") && "reload".startsWith(prefix)) {
                proposals.add("reload");
            }
            if ("version".startsWith(prefix)) {
                proposals.add("version");
            }
        } else {
            // keine weiteren Vorschläge
            return Collections.emptyList();
        }

        return proposals;
    }


}
