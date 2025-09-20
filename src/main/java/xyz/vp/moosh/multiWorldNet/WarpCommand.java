package xyz.vp.moosh.multiWorldNet;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarpCommand implements CommandExecutor {

    private final MultiWorldNet plugin;

    public WarpCommand(MultiWorldNet plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp() && !player.hasPermission("worldnet.warp")) {
            player.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage: /worldnetwarp <world>");
            return true;
        }

        String targetWorldName = args[0];
        World targetWorld = Bukkit.getWorld(targetWorldName);

        if (targetWorld == null) {
            player.sendMessage("§cWorld not found!");
            return true;
        }

        // 🔹 Check Op-only worlds
        if (plugin.getOpWorlds().contains(targetWorldName.toLowerCase()) && !player.isOp()) {
            player.sendMessage("§cYou cannot warp to this world. OP only.");
            return true;
        }

        // 🔹 Maintenance check
        if (plugin.isUnderMaintenance(targetWorldName)) {
            if (!(player.isOp() || player.hasPermission("worldnet.maintenance.bypass"))) {
                String reason = plugin.getMaintenanceReason(targetWorldName);
                player.sendMessage("§cWorld '" + targetWorldName + "' is currently under maintenance: §f" + reason);
                return true;
            }
        }

        player.teleport(targetWorld.getSpawnLocation());
        player.sendMessage("§aTeleported to world §e" + targetWorldName);
        return true;
    }
}
