package de.crafttogether.ctcommands.events;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import de.crafttogether.CTCommands;
import dev.simplix.protocolize.api.Direction;
import dev.simplix.protocolize.api.listener.AbstractPacketListener;
import dev.simplix.protocolize.api.listener.PacketReceiveEvent;
import dev.simplix.protocolize.api.listener.PacketSendEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.protocol.packet.Commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CommandsPacketListener extends AbstractPacketListener<Commands>
{
  private CTCommands plugin;
  
  public CommandsPacketListener() {
    super(Commands.class, Direction.UPSTREAM, 0);
    this.plugin = CTCommands.getInstance();
  }

  @Override
  public void packetReceive(PacketReceiveEvent<Commands> event) { }

  @Override
  public void packetSend(PacketSendEvent<Commands> event) {
        ProxiedPlayer player = CTCommands.getInstance().getProxy().getPlayer(event.player().uniqueId());

    if (player == null)
      return;

    boolean bypassAll = player.hasPermission("ctcommands.bypass.all");
    boolean bypassWhitelist = player.hasPermission("ctcommands.bypass.whitelist");
    boolean bypassBlacklist = player.hasPermission("ctcommands.bypass.blacklist");

    if (bypassAll) {
      return;
    }

    Commands packet = event.packet();
    RootCommandNode<?> root = packet.getRoot();
    Collection<?> children = root.getChildren();
    List<Object> blockedCmds = new ArrayList<Object>();
    List<String> whitelist = new ArrayList<String>();
    List<String> blacklist = new ArrayList<String>();
    Configuration groups = this.plugin.getWhitelist().getSection("groups");

    if (!bypassWhitelist) {
      for (String pGroup : player.getGroups()) {
        for (String group : groups.getKeys()) {
          if (group.equalsIgnoreCase(pGroup)) {
            List<String> commands = this.plugin.getWhitelist().getStringList("groups." + group);

            for (String cmdName : commands) {
              if (!whitelist.contains(cmdName)) {
                whitelist.add(cmdName);
              }
            }
          }
        }
      }
    }
    else if (!bypassBlacklist) {
      blacklist = this.plugin.getBlacklist().getStringList("blacklist");
    }

    for (Object obj : children) {
      if (obj instanceof CommandNode) {
        CommandNode<?> cmd = (CommandNode<?>)obj;

        String cmdName = cmd.getName();
        if (cmdName.contains(":") || cmd.getName().startsWith(":")) {
          blockedCmds.add(obj);
        }

        if (cmd.getName().equalsIgnoreCase("help")) {
          cmd.getChildren().clear();
        }
        else if (!bypassWhitelist && !whitelist.contains(cmd.getName())) {
          //System.out.println("NOT WHITELISTED COMMAND FOUND: " + cmd.getName());
          blockedCmds.add(obj);
        }
        else if (!bypassBlacklist && (blacklist.contains(cmdName))) {
          //System.out.println("BLACKLISTED COMMAND FOUND: " + cmd.getName());
          blockedCmds.add(obj);
        }
      } 
    }

    for (Object obj : blockedCmds) {
      if (!root.getChildren().contains(obj))
        continue;  root.getChildren().remove(obj);
    } 
    
    packet.setRoot(root);
    event.packet(packet);
  }
}
