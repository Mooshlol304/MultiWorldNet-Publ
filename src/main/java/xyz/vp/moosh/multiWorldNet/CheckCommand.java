package xyz.vp.moosh.multiWorldNet;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CheckCommand implements CommandExecutor {

    private final MultiWorldNet plugin;

    public CheckCommand(MultiWorldNet plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // If no args → check sender's world (must be a player)
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cConsole must specify a player: /worldnetcheck <player>");
                return true;
            }
            sender.sendMessage("§aYou are currently in world: §e" + player.getWorld().getName());
            return true;
        }

        // If 1 arg → check target player's world
        if (args.length == 1) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage("§cPlayer '" + args[0] + "' is not online.");
                return true;
            }

            sender.sendMessage("§aPlayer §e" + target.getName() + " §ais currently in world: §e" + target.getWorld().getName());
            return true;
        }

        // Invalid usage
        sender.sendMessage("§cUsage: /worldnetcheck [player]");
        return true;
    }
}
