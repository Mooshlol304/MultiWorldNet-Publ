package xyz.vp.moosh.multiWorldNet;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateCommand implements CommandExecutor {

    private MultiWorldNet plugin;

    public CreateCommand(MultiWorldNet plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cYou do not have permission!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /worldnetcreate <world> [overworld/nether/end]");
            return true;
        }

        String worldName = args[0];
        String type = args.length > 1 ? args[1].toLowerCase() : "overworld";

        // Check if world already exists
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            sender.sendMessage("§cWorld Already Exists!");
            return true;
        }

        // Determine environment type
        World.Environment env;
        switch (type) {
            case "nether" -> env = World.Environment.NETHER;
            case "end" -> env = World.Environment.THE_END;
            default -> env = World.Environment.NORMAL;
        }

        // Create world
        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(env);
        Bukkit.createWorld(creator);

        sender.sendMessage("§aWorld " + worldName + " created successfully as " + type + "!");
        return true;
    }
}
