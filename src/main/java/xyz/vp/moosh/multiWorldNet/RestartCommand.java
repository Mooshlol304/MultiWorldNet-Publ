package xyz.vp.moosh.multiWorldNet;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RestartCommand implements CommandExecutor {

    private MultiWorldNet plugin;

    public RestartCommand(MultiWorldNet plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§cUsage: /worldnetrestart <world>");
            return true;
        }

        String worldName = args[0];
        String fallbackName = plugin.getFallbackWorld();

        World world = Bukkit.getWorld(worldName);
        World fallback = Bukkit.getWorld(fallbackName);

        if (world == null || fallback == null) {
            sender.sendMessage("§cWorld or fallback world not found!");
            return true;
        }

        // Move all players to fallback
        for (Player p : world.getPlayers()) {
            p.teleport(fallback.getSpawnLocation());
            p.sendMessage("§eWorld is restarting. You have been sent to the fallback world.");
        }

        // Unload & reload world
        Bukkit.unloadWorld(world, false);
        Bukkit.createWorld(new org.bukkit.WorldCreator(worldName));

        sender.sendMessage("§aWorld " + worldName + " restarted successfully!");
        return true;
    }
}
