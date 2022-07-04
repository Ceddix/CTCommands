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

        if (p == null)
            return;

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!p.hasPermission("ctcommands.reload")) {
                p.sendMessage(new TextComponent(new MineDown("&cDazu hast du keine Berechtigung.").toComponent()));
                return;
            }

            plugin.loadConfig();
            sender.sendMessage(new TextComponent(new MineDown("&aConfigurations reloaded!").toComponent()));
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        ArrayList<String> newList = new ArrayList<>();
        ArrayList<String> proposals = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("ctcommands.reload"))
                proposals.add("reload");
        }

        if (args.length < 1 || args[args.length - 1].equals("")) {
            newList = proposals;
        } else {
            for (String value : proposals) {
                if (value.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                    newList.add(value);
            }
        }

        return newList;

    }
}
