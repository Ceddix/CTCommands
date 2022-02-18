package de.crafttogether.ctcommands.events;

import de.crafttogether.CTCommands;
import dev.simplix.protocolize.api.Direction;
import dev.simplix.protocolize.api.listener.AbstractPacketListener;
import dev.simplix.protocolize.api.listener.PacketReceiveEvent;
import dev.simplix.protocolize.api.listener.PacketSendEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.protocol.packet.Chat;

import java.util.concurrent.RejectedExecutionException;

public class ChatPacketListener extends AbstractPacketListener<Chat>
{
  private CTCommands plugin;
  
  public ChatPacketListener() {
    super(Chat.class, Direction.UPSTREAM, 0);
    this.plugin = CTCommands.getInstance();
  }

  @Override
  public void packetReceive(PacketReceiveEvent<Chat> event) {
    final ProxiedPlayer player = plugin.getProxy().getPlayer(event.player().uniqueId());
    final String message = event.packet().getMessage();

    if (player == null)
      return;

    try {
      plugin.getProxy().getScheduler().runAsync(plugin, () -> {
          if (message.startsWith("/")) {
            LogFile cmdLog = plugin.getCmdLog();
            if (cmdLog != null)
              cmdLog.write(player.getName() + " sendet Befehl: " + message);
          } else {
            LogFile chatLog = plugin.getChatLog();
            if (chatLog != null)
              chatLog.write(player.getName() + ": " + message);
          }
      });
    }
    catch (RejectedExecutionException e) {

    }
  }

  @Override
  public void packetSend(PacketSendEvent<Chat> packetSendEvent) {

  }
}
