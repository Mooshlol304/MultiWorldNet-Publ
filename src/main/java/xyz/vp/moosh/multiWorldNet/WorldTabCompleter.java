package xyz.vp.moosh.multiWorldNet;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class WorldTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        List<String> completions = new ArrayList<>();

        // Only complete first argument
        if (args.length == 1) {
            for (World world : Bukkit.getWorlds()) {
                if (world.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(world.getName());
                }
            }
        }

        return completions;
    }
}
