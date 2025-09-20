package xyz.vp.moosh.multiWorldNet;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UtiliseCommand implements CommandExecutor {

    private MultiWorldNet plugin;

    public UtiliseCommand(MultiWorldNet plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§cUsage: /worldnetutilise <world>");
            return true;
        }

        World world = Bukkit.getWorld(args[0]);
        if (world == null) {
            sender.sendMessage("§cWorld not found!");
            return true;
        }

// Approximate memory usage per world: entities + loaded chunks
        int loadedChunks = world.getLoadedChunks().length;
        int entityCount = world.getEntities().size();

        sender.sendMessage("§aWorld: " + world.getName());
        sender.sendMessage("§aPlayers: " + world.getPlayers().size());
        sender.sendMessage("§aLoaded Chunks: " + loadedChunks);
        sender.sendMessage("§aEntities: " + entityCount);

        return true;
    }
}
