package de.crafttogether.ctcommands.commands;

import de.crafttogether.CTCommands;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class Commands extends Command implements TabExecutor {
    private CTCommands plugin;

    public Commands() {
        super("ctcommands");
        plugin = CTCommands.getInstance();
        plugin.getProxy().getPluginManager().registerCommand(plugin, this);
    }

    public void execute(final CommandSender sender, final String[] args) {
        ProxiedPlayer p = null;

        if (sender instanceof ProxiedPlayer)
            p = (ProxiedPlayer) sender;
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
