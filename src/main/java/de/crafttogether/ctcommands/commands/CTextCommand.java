package de.crafttogether.ctcommands.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.crafttogether.CTCommands;
import de.crafttogether.ctcommands.text.Texts;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public final class CTextCommand implements SimpleCommand {
  private final CTCommands plugin;
  private final ProxyServer server;
  private final Logger logger;
  private final Path dataDir;


  public CTextCommand(CTCommands plugin) {
    this.plugin = plugin;
    this.server = plugin.getServer();
    this.logger = plugin.getLogger();
    this.dataDir = plugin.getDataDir();
  }

  @Override
  public void execute(Invocation invocation) {
    final var source = invocation.source();
    final String[] args = invocation.arguments();

    if (args.length < 1) {
      source.sendMessage(Texts.parse("/ctext <fileName> [player|all]"));
      return;
    }

    final String fileName = args[0];
    final Path file = dataDir.resolve("ctext").resolve(fileName + ".txt");

    if (!Files.exists(file)) {
      source.sendMessage(Texts.error("Es konnte keine Datei mit dem Namen &e" + fileName + " &cgefunden werden."));
      return;
    }

    // Zeilen laden (asynchron, um I/O zu entkoppeln)
    server.getScheduler().buildTask(plugin, () -> {
      List<String> lines;
      try {
        lines = Files.readAllLines(file, StandardCharsets.UTF_8);
      } catch (IOException e) {
        logger.error("Fehler beim Lesen von {}", file, e);
        source.sendMessage(Texts.error("Fehler beim Lesen der Datei."));
        return;
      }

      // Ziel bestimmen
      if (args.length == 1) {
        // Kein Ziel angegeben -> an Ausführenden (falls Spieler), sonst Hinweis
        if (source instanceof Player p) {
          sendLines(p, lines);
        } else {
          source.sendMessage(Texts.info("Konsole benötigt ein Ziel: &e/ctext " + fileName + " <player|all>"));
        }
        return;
      }

      // Ziel explizit
      String target = args[1];
      if (target.equalsIgnoreCase("all")) {
        for (Player p : server.getAllPlayers()) sendLines(p, lines);
        return;
      }

      // Spieler per Name
      Optional<Player> opt = server.getAllPlayers().stream()
              .filter(p -> p.getUsername().equalsIgnoreCase(target))
              .findFirst();
      if (opt.isEmpty()) {
        source.sendMessage(Texts.error("Spieler &e" + target + "&e ist nicht online."));
        return;
      }
      sendLines(opt.get(), lines);
    }).schedule();
  }

  private void sendLines(Player player, List<String> lines) {
    for (String line : lines) {
      player.sendMessage(Texts.parse(line,player));
    }
  }

  @Override
  public List<String> suggest(Invocation invocation) {
    String[] args = invocation.arguments();

    // Vorschläge für Dateinamen beim ersten Argument
    if (args.length <= 1) {
      String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
      return listCTextFiles().stream()
              .filter(n -> n.startsWith(prefix))
              .sorted()
              .collect(Collectors.toList());
    }

    // Vorschläge für Ziel beim zweiten Argument: "all" + Spielernamen
    if (args.length == 2) {
      String prefix = args[1].toLowerCase(Locale.ROOT);
      List<String> out = new ArrayList<>();
      if ("all".startsWith(prefix)) out.add("all");
      for (Player p : server.getAllPlayers()) {
        if (p.getUsername().toLowerCase(Locale.ROOT).startsWith(prefix)) {
          out.add(p.getUsername());
        }
      }
      Collections.sort(out);
      return out;
    }

    return Collections.emptyList();
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
