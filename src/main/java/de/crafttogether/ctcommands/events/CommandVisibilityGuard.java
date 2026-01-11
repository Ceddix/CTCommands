package de.crafttogether.ctcommands.events;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.TabCompleteEvent; // legacy (falls vorhanden)
import com.velocitypowered.api.proxy.Player;
import de.crafttogether.CTCommands;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;
import java.util.stream.Collectors;

public final class CommandVisibilityGuard {
  private final CTCommands plugin;
  private final Logger log;

  public CommandVisibilityGuard(CTCommands plugin) {
    this.plugin = plugin;
    this.log = plugin.getLogger();
  }

  /* ====== 1) SUGGESTIONS filtern (Legacy API) ====== */
  @Subscribe
  public void onTabCompleteLegacy(TabCompleteEvent e) throws SerializationException {
    // Falls die Klasse in deiner Velocity-Version fehlt, wird dieser Handler einfach nie registriert.
    if (!(e.getPlayer() instanceof Player)) return;
    Player p = (Player) e.getPlayer();

    FilterRules rules = rulesFor(p);
    if (rules.bypassAll) return;

    List<String> suggestions = e.getSuggestions();
    if (suggestions == null) return;

    List<String> filtered = suggestions.stream()
            .filter(s -> allowSuggestion(s, rules))
            .collect(Collectors.toList());
    suggestions.clear();
    suggestions.addAll(filtered);
  }

  /* ====== 2) AUSFÜHRUNG blocken ====== */
  @Subscribe
  public void onExecute(CommandExecuteEvent e) throws SerializationException {
    if (!(e.getCommandSource() instanceof Player p)) return;

    // kompletter Command inkl. Args, aber ohne führenden Slash
    String raw = e.getCommand();          // z.B. "msg Steve Hallo"
    String full = "/" + raw;

    FilterRules rules = rulesFor(p);       // deine Whitelist/Blacklist-Ermittlung
    if (rules.bypassAll) return;

    // Basis-Name extrahieren (erstes Wort vor Leerzeichen / Namespace entfernen)
    String base = raw.trim();
    int space = base.indexOf(' ');
    if (space >= 0) base = base.substring(0, space);
    int colon = base.indexOf(':');         // "plugin:cmd" -> "cmd"
    if (colon >= 0) base = base.substring(colon + 1);
    base = base.toLowerCase(Locale.ROOT);

    boolean allowed =
            (rules.bypassWhitelist || rules.whitelist.contains(base)) &&
                    (rules.bypassBlacklist || !rules.blacklist.contains(base));

    if (!allowed) {
      // Ausführung blockieren
      e.setResult(CommandExecuteEvent.CommandResult.denied());
      if (plugin.getCmdLog() != null) plugin.getCmdLog().write(p.getUsername() + " versuchte verbotenen Befehl: " + full);
    }
  }

  /* ====== Hilfen ====== */

  private boolean allowSuggestion(String s, FilterRules r) {
    // Vorschläge können mit "/" kommen oder Namespace enthalten – normalize:
    String norm = s.startsWith("/") ? s.substring(1) : s;
    int colon = norm.indexOf(':');
    if (colon >= 0) norm = norm.substring(colon + 1);
    norm = norm.toLowerCase(Locale.ROOT);

    if (!r.bypassWhitelist && !r.whitelist.contains(norm)) return false;
    if (!r.bypassBlacklist && r.blacklist.contains(norm)) return false;
    return true;
  }

  private FilterRules rulesFor(Player p) throws SerializationException {
    boolean bypassAll       = p.hasPermission("ctcommands.bypass.all");
    boolean bypassWhitelist = p.hasPermission("ctcommands.bypass.whitelist");
    boolean bypassBlacklist = p.hasPermission("ctcommands.bypass.blacklist");

    Set<String> wl = new HashSet<>();
    Set<String> bl = new HashSet<>();

    if (!bypassWhitelist) {
      // whitelist.yml:
      // groups:
      //   default: [help, rules, ...]
      //   spieler: [tpa, home, ...]
      ConfigurationNode groups = plugin.getWhitelist().node("groups");
      for (String group : groups.childrenMap().keySet().stream().map(Object::toString).collect(Collectors.toList())) {
        // Deine Gruppenlogik: falls du echte Permissions/Groups hast, mappe sie hier.
        // Als einfaches Beispiel: wenn der Spieler die Permission "ct.group.<group>" hat
        if (p.hasPermission("ct.group." + group)) {
          List<String> cmds = groups.node(group).getList(String.class);
          if (cmds != null) cmds.forEach(c -> wl.add(c.toLowerCase(Locale.ROOT)));
        }
      }
      // Fallback: immer "default" erlauben
      List<String> def = groups.node("default").getList(String.class);
      if (def != null) def.forEach(c -> wl.add(c.toLowerCase(Locale.ROOT)));
    }

    if (!bypassBlacklist) {
      // blacklist.yml:
      // blacklist:
      //   - worldedit
      //   - worldguard
      List<String> list = plugin.getBlacklist().node("blacklist").getList(String.class);
      if (list != null) list.forEach(c -> bl.add(c.toLowerCase(Locale.ROOT)));
    }

    return new FilterRules(bypassAll, bypassWhitelist, bypassBlacklist, wl, bl);
  }

  private record FilterRules(boolean bypassAll, boolean bypassWhitelist, boolean bypassBlacklist,
                             Set<String> whitelist, Set<String> blacklist) {}
}
