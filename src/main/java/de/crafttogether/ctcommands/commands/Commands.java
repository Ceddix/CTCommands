package de.crafttogether.ctcommands.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.crafttogether.CTCommands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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
            source.sendMessage(text("<red>Dieser Befehl ist nur für Spieler."));
            return;
        }

        String[] args = invocation.arguments();

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("ctcommands.reload")) {
                player.sendMessage(text("<red>Dazu hast du keine Berechtigung."));
                return;
            }

            // Unterschiedliche Namen je nach deiner Main (loadConfigs vs. loadConfig).
            boolean ok = callReloadMethod();
            if (ok) {
                source.sendMessage(text("<green>Configurations reloaded!"));
            } else {
                source.sendMessage(text("<red>Konnte Konfiguration nicht neu laden (keine passende Methode gefunden)."));
            }
            return;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("version")) {
            if (!player.hasPermission("ctcommands.version")) {
                player.sendMessage(text("<red>Dazu hast du keine Berechtigung."));
                return;
            }

            source.sendMessage(text("<dark_gray><strikethrough>----------------------"));
            source.sendMessage(text("<dark_aqua>CTCommands <aqua>" + version));
            source.sendMessage(text("<aqua>by " + authors));
            source.sendMessage(text("<dark_gray><strikethrough>----------------------"));
            return;
        }

        // Usage-Hinweis
        source.sendMessage(text("<gray>Verwendung: <yellow>/ctcommands <white><reload|version>"));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        final var source = invocation.source();
        List<String> proposals = new ArrayList<>();

        // Vorschläge abhängig von Permissions
        if (source.hasPermission("ctcommands.reload")) {
            proposals.add("reload");
        }
        if (source.hasPermission("ctcommands.version")) {
            proposals.add("version");
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            return proposals;
        }
        String last = args[args.length - 1].toLowerCase();

        List<String> out = new ArrayList<>();
        for (String p : proposals) {
            if (p.startsWith(last)) out.add(p);
        }
        return out;
    }

    /* ---------- helpers ---------- */

    private Component text(String miniMsg) {
        return mm.deserialize(miniMsg);
    }

    /** versucht loadConfigs() oder loadConfig() per Reflection aufzurufen */
    private boolean callReloadMethod() {
        List<String> candidates = Arrays.asList("loadConfigs", "loadConfig", "reloadConfigs", "reloadConfig");
        for (String name : candidates) {
            try {
                Method m = CTCommands.class.getDeclaredMethod(name);
                m.setAccessible(true);
                m.invoke(plugin);
                return true;
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable t) {
                logger.error("Fehler beim Aufruf von {}()", name, t);
                return false;
            }
        }
        return false;
    }
}
