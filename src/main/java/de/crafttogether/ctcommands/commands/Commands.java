package de.crafttogether.ctcommands.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.crafttogether.CTCommands;
import de.crafttogether.ctcommands.text.Texts;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;


public final class Commands implements SimpleCommand {

    private final CTCommands plugin;
    private final Logger logger;
    private final Path dataDir;

    public Commands(CTCommands plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dataDir = plugin.getDataDir();
    }

    @Override
    public void execute(Invocation invocation) {
        final CommandSource source = invocation.source();

        // Im Original nur für Spieler erlaubt – wir übernehmen das Verhalten.
        if (!(source instanceof Player player)) {
            source.sendMessage(Texts.error("Dieser Befehl ist nur für Spieler."));
            logger.warn("Dieser Befehl ist nur für Spieler. {}",source);
            return;
        }
        String[] args = invocation.arguments();

        if (args.length == 0) {
            sendUsage(player);
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "version" -> {
                if (!player.hasPermission("ctcommands.version")) {
                    player.sendMessage(Texts.error("Dazu hast du keine Berechtigung."));
                    return;
                }
                plugin.getVersion(player);
            }

            case "reload" -> {
                if (!player.hasPermission("ctcommands.reload")) {
                    player.sendMessage(Texts.error("Dazu hast du keine Berechtigung."));
                    return;
                }
                boolean ok = plugin.ReloaddConfig(); // deine Methode
                if (ok) player.sendMessage(Texts.ok("Configurations reloaded!"));
                else player.sendMessage(Texts.error("Konnte Konfiguration nicht neu laden."));
            }

            case "welcome" -> {
                if (!player.hasPermission("ctcommands.welcome")) {
                    player.sendMessage(Texts.error("Dazu hast du keine Berechtigung."));
                    return;
                }
                if (args.length < 2) {
                    player.sendMessage(Texts.info("Verwendung: /ctcommands welcome <filename>"));
                    return;
                }

                String fileName = normalizeTxt(args[1]);
                plugin.getConfig().setPrivateServerJoinFile(fileName);
                try {
                    plugin.getConfig().save();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                boolean ok = plugin.ReloaddConfig(); // deine Methode
                if (ok) {
                    player.sendMessage(Texts.ok("Welcome-Datei gesetzt: " + fileName));
                    player.sendMessage(Texts.ok("Configurations reloaded!"));
                }
                else player.sendMessage(Texts.error("Konnte Konfiguration nicht neu laden."));
            }

            default -> sendUsage(player);
        }
    }

    private void sendUsage(CommandSource source) {
        source.sendMessage(Texts.info("Verwendung: /ctcommands <version|reload|welcome>"));
        source.sendMessage(Texts.info(" - /ctcommands version"));
        source.sendMessage(Texts.info(" - /ctcommands reload"));
        source.sendMessage(Texts.info(" - /ctcommands welcome <filename>"));
        source.sendMessage(Texts.info("Zusatz: /ctext <filename> <playername>"));
    }

private String normalizeTxt(String fileName) {
    if (!fileName.toLowerCase(Locale.ROOT).endsWith(".txt")) return fileName + ".txt";
    return fileName;
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
                proposals.add("version");
                proposals.add("welcome");
            }
        } else if (args.length == 1) {
            // Filtern nach Prefix (wie bei dir)
            String prefix = args[0].toLowerCase();

            if (sender.hasPermission("ctcommands.reload") && "reload".startsWith(prefix)) {
                proposals.add("reload");
            }
            if (sender.hasPermission("ctcommands.version") && "version".startsWith(prefix)) {
                proposals.add("version");
            }
            if (sender.hasPermission("ctcommands.welcome") && "welcome".startsWith(prefix)) {
                proposals.add("welcome");
            }
        } else if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            return listCTextFiles().stream()
                    .filter(n -> n.startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        } else {
            // keine weiteren Vorschläge
            return Collections.emptyList();
        }

        return proposals;
    }

    private List<String> listCTextFiles() {
        Path dir = dataDir.resolve("ctext");
        if (!Files.isDirectory(dir)) return Collections.emptyList();

        try {
            try (var stream = Files.list(dir)) {
                return stream
                        .filter(p -> !Files.isDirectory(p) && p.getFileName().toString().endsWith(".txt"))
                        .map(p -> {
                            String n = p.getFileName().toString();
                            return n.substring(0, n.length() - 4); // ohne ".txt"
                        })
                        .sorted()
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            logger.warn("Konnte ctext-Verzeichnis nicht lesen: {}", dir, e);
            return Collections.emptyList();
        }
    }

}
