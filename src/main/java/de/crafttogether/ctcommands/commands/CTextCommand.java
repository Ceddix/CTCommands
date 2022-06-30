package de.crafttogether.ctcommands.commands;

import de.crafttogether.CTCommands;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import de.themoep.minedown.MineDown;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class CTextCommand
  extends Command {
  private CTCommands plugin;
  
  public CTextCommand() {
    super("ctext");
    this.plugin = CTCommands.getInstance();
  }

  
  public void execute(final CommandSender sender, final String[] args) {
    ProxiedPlayer p = null;
    
    if (args.length < 1) {
      sender.sendMessage(new TextComponent("/ctext <fileName> [player|all]"));
      return;
    }

    if (args.length > 1) {
      if (!args[1].equalsIgnoreCase("all")) {
        p = this.plugin.getProxy().getPlayer(args[1]);
      }
      if (!args[1].equalsIgnoreCase("all") && p == null) {
        sender.sendMessage(new TextComponent("/ctext <fileName> [player]"));
        return;
      }

    }
    final ProxiedPlayer player = p;
    
    this.plugin.getProxy().getScheduler().runAsync(this.plugin, new Runnable() {
          List<String> lines = new ArrayList<String>();
          
          public void run() {
            File file = new File(CTextCommand.this.plugin.getDataFolder(), "/ctext/" + args[0] + ".txt");
            Scanner sc = null;
            
            try {
              sc = new Scanner(file);
            } catch (FileNotFoundException e) {
              sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&cEs konnte keine Datei mit dem Namen &e" + file.getName() + " &cgefunden werden.")));
            }
            
            if (sc != null) {
              while (sc.hasNextLine()) {
                this.lines.add(sc.nextLine());
              }
            }
            if (args[1] != null) {
              for (String line : this.lines) {
                sender.sendMessage(new TextComponent(new MineDown(line).toComponent()));
              }
            }
            else if (player != null) {
              for (String line : this.lines) {
                player.sendMessage(new TextComponent(new MineDown(line).toComponent()));
              }
            }
            else if (args[1].equalsIgnoreCase("all")) {
              Collection<ProxiedPlayer> players = CTextCommand.this.plugin.getProxy().getPlayers();
              
              for (ProxiedPlayer p : players) {
                for (String line : this.lines)
                  p.sendMessage(new TextComponent(new MineDown(line).toComponent()));
              } 
            } 
          }
        });
  }
}
