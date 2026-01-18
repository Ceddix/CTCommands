package de.crafttogether.ctcommands.events;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.event.player.TabCompleteEvent; // Legacy (1.12.2-)
import com.velocitypowered.api.proxy.Player;
import de.crafttogether.CTCommands;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;

public final class CommandsAvailabilityListener {

  private final CTCommands plugin;
  private final Logger log;

  public CommandsAvailabilityListener(CTCommands plugin) {
    this.plugin = plugin;
    this.log = plugin.getLogger();
  }

  /* ============================================================
   * 1) 1.13+ Command-Discovery / Tab (Brigadier Tree)
   * ============================================================ */
  @Subscribe
  public void onAvailableCommands(PlayerAvailableCommandsEvent e) {
    final Player p = e.getPlayer();

    final FilterRules rules = safeRulesFor(p);
    if (rules == null || rules.bypassAll) return;

    final RootCommandNode<?> root = e.getRootNode();

    // Snapshot, weil wir Root mutieren
    final List<CommandNode<?>> snapshot = new ArrayList<>(root.getChildren());

    for (CommandNode<?> cmd : snapshot) {
      final String rawName = cmd.getName();
      final String base = normalizeBase(rawName);

      // Namespaced raus (minecraft:help, bukkit:..., etc.)
      if (rawName.contains(":") || rawName.startsWith(":")) {
        removeRootChild(root, rawName);
        continue;
      }

      // help "entkernen"
      if ("help".equals(base)) {
        cmd.getChildren().clear();
        continue;
      }

      // Whitelist/Blacklist Policy
      if (!rules.bypassWhitelist && !rules.whitelist.contains(base)) {
        removeRootChild(root, rawName);
        continue;
      }

      if (!rules.bypassBlacklist && rules.blacklist.contains(base)) {
        removeRootChild(root, rawName);
      }
    }
  }

  /**
   * Entfernt ein Root-Command-Child per Name.
   * In deiner Velocity/Brigadier Kombi gibt es offenbar removeChildByName().
   */
  private void removeRootChild(RootCommandNode<?> root, String name) {
    try {
      // Primary (dein Hinweis)
      root.removeChildByName(name);
    } catch (Throwable ignored) {
      // Fallback: über children-Collection entfernen (wenn modifizierbar)
      try {
        root.getChildren().removeIf(n -> n.getName().equalsIgnoreCase(name));
      } catch (Throwable t) {
        // only error logs
        log.error("[CTCommands] Failed to remove command node '{}'", name, t);
      }
    }
  }

  /* ============================================================
   * 2) Legacy TabComplete (nur 1.12.2- Clients)
   * ============================================================ */
  @Subscribe
  public void onTabCompleteLegacy(TabCompleteEvent e) {
    final Player p = e.getPlayer();

    final FilterRules rules = safeRulesFor(p);
    if (rules == null || rules.bypassAll) return;

    final List<String> suggestions = e.getSuggestions();
    if (suggestions == null || suggestions.isEmpty()) return;

    suggestions.removeIf(s -> !allowSuggestion(s, rules));
  }

  /* ============================================================
   * 3) Enforcement: Ausführung blocken
   * ============================================================ */
  @Subscribe
  public void onExecute(CommandExecuteEvent e) {
    if (!(e.getCommandSource() instanceof Player p)) return;

    final FilterRules rules = safeRulesFor(p);
    if (rules == null || rules.bypassAll) return;

    final String raw = e.getCommand(); // z.B. "msg Steve Hallo"
    final String base = normalizeBase(extractBaseCommand(raw));

    final boolean allowed =
            (rules.bypassWhitelist || rules.whitelist.contains(base)) &&
                    (rules.bypassBlacklist || !rules.blacklist.contains(base));

    if (!allowed) {
      e.setResult(CommandExecuteEvent.CommandResult.denied());

      if (plugin.getCmdLog() != null) {
        plugin.getCmdLog().write(p.getUsername() + " blocked command: /" + raw);
      }
    }
  }

  /* =========================
   * Helpers / Policy
   * ========================= */

  private boolean allowSuggestion(String s, FilterRules r) {
    final String norm = normalizeBase(s);
    if (!r.bypassWhitelist && !r.whitelist.contains(norm)) return false;
    if (!r.bypassBlacklist && r.blacklist.contains(norm)) return false;
    return true;
  }

  private String extractBaseCommand(String raw) {
    if (raw == null) return "";
    String base = raw.trim();
    int space = base.indexOf(' ');
    if (space >= 0) base = base.substring(0, space);
    return base;
  }

  private String normalizeBase(String in) {
    if (in == null) return "";
    String s = in.startsWith("/") ? in.substring(1) : in;
    int colon = s.indexOf(':');          // "plugin:cmd" -> "cmd"
    if (colon >= 0) s = s.substring(colon + 1);
    return s.toLowerCase(Locale.ROOT);
  }

  private FilterRules safeRulesFor(Player p) {
    try {
      return rulesFor(p);
    } catch (Throwable t) {
      log.error("[CTCommands] Failed to build filter rules for {}", p.getUsername(), t);
      return null;
    }
  }

  /**
   * Whitelist/Blacklist aus Configurate.
   * Gruppenmapping per Permission: ct.group.<group>
   */
  private FilterRules rulesFor(Player p) {
    final boolean bypassAll       = p.hasPermission("ctcommands.bypass.all");
    final boolean bypassWhitelist = p.hasPermission("ctcommands.bypass.whitelist");
    final boolean bypassBlacklist = p.hasPermission("ctcommands.bypass.blacklist");

    final Set<String> wl = new HashSet<>();
    final Set<String> bl = new HashSet<>();

    if (!bypassWhitelist) {
      final ConfigurationNode groups = plugin.getWhitelist().node("groups");

      // default immer erlauben (wenn vorhanden)
      addListLowercase(groups.node("default"), wl);

      // Gruppen via Permission aktivieren
      for (Map.Entry<Object, ? extends ConfigurationNode> entry : groups.childrenMap().entrySet()) {
        final String group = String.valueOf(entry.getKey());
        if ("default".equalsIgnoreCase(group)) continue;

        if (p.hasPermission("ct.group." + group)) {
          addListLowercase(groups.node(group), wl);
        }
      }
    }

    if (!bypassBlacklist) {
      addListLowercase(plugin.getBlacklist().node("blacklist"), bl);
    }

    return new FilterRules(bypassAll, bypassWhitelist, bypassBlacklist, wl, bl);
  }

  private void addListLowercase(ConfigurationNode node, Set<String> out) {
    if (node == null) return;
    List<String> list = safeStringList(node);
    for (String s : list) {
      if (s != null && !s.isBlank()) out.add(normalizeBase(s));
    }
  }
  private List<String> safeStringList(ConfigurationNode node) {
    if (node == null) return Collections.emptyList();
    try {
      return node.getList(String.class, Collections.emptyList());
    } catch (SerializationException ex) {
      log.error("[CTCommands] Invalid list value at node '{}'", node.path(), ex);
      return Collections.emptyList();
    }
  }

  private record FilterRules(
          boolean bypassAll,
          boolean bypassWhitelist,
          boolean bypassBlacklist,
          Set<String> whitelist,
          Set<String> blacklist
  ) {}
}
