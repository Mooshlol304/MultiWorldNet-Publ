package xyz.vp.moosh.multiWorldNet;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MaintenanceCommand implements CommandExecutor {

    private final MultiWorldNet plugin;

    public MaintenanceCommand(MultiWorldNet plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("multiworldnet.maintenance")) {
            sender.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /worldnetmaintenance <world> <minutes> <on/off> [reason]");
            return true;
        }

        String worldName = args[0];
        World targetWorld = Bukkit.getWorld(worldName);
        if (targetWorld == null) {
            sender.sendMessage("§cWorld '" + worldName + "' not found!");
            return true;
        }

        double minutes;
        try {
            minutes = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number format for minutes: " + args[1]);
            return true;
        }

        String status = args[2];
        String reason = (args.length > 3)
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length))
                : "Maintenance";

        if (status.equalsIgnoreCase("on")) {
            plugin.startMaintenance(worldName, minutes, reason);
            sender.sendMessage("§aStarted maintenance on §e" + worldName +
                    "§a for §e" + minutes + " minutes§a. Reason: §f" + reason);
        } else if (status.equalsIgnoreCase("off")) {
            plugin.stopMaintenance();
            plugin.removeMaintenance(worldName);
            sender.sendMessage("§cMaintenance ended for world §e" + worldName);
        } else {
            sender.sendMessage("§cInvalid status! Use 'on' or 'off'.");
        }

        return true;
    }
}
