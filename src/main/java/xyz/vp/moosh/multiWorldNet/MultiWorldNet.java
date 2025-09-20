package xyz.vp.moosh.multiWorldNet;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public final class MultiWorldNet extends JavaPlugin implements Listener {

    private JsonObject configJson;
    private final Gson gson = new Gson();
    private final Map<String, World> worldCache = new HashMap<>();

    // 🔹 Maintenance
    private final Map<String, Long> maintenanceWorlds = new HashMap<>();
    private final Map<String, String> maintenanceReasons = new HashMap<>();
    private BossBar maintenanceBar;
    private BukkitRunnable maintenanceTask;

    @Override
    public void onEnable() {
        loadConfig();

        // Register commands
        this.getCommand("worldnetwarp").setExecutor(new WarpCommand(this));
        this.getCommand("worldnetrestart").setExecutor(new RestartCommand(this));
        this.getCommand("worldnetutilise").setExecutor(new UtiliseCommand(this));
        this.getCommand("worldnetcreate").setExecutor(new CreateCommand(this));
        this.getCommand("worldnetcheck").setExecutor(new CheckCommand(this));
        this.getCommand("worldnetmaintenance").setExecutor(new MaintenanceCommand(this));

        // Register tab completers
        WorldTabCompleter tabCompleter = new WorldTabCompleter();
        this.getCommand("worldnetwarp").setTabCompleter(tabCompleter);
        this.getCommand("worldnetrestart").setTabCompleter(tabCompleter);
        this.getCommand("worldnetutilise").setTabCompleter(tabCompleter);
        this.getCommand("worldnetcreate").setTabCompleter(tabCompleter);
        this.getCommand("worldnetmaintenance").setTabCompleter(new MaintenanceTabCompleter());

        // Register events
        Bukkit.getPluginManager().registerEvents(this, this);

        // Schedule tablist refresh every 5 seconds
        Bukkit.getScheduler().runTaskTimer(this, this::refreshTablists, 0L, 100L);
    }

    @Override
    public void onDisable() {
        stopMaintenance();
    }

    // 🔹 Config helpers
    public String getFallbackWorld() {
        return configJson.has("Fallback_World") ? configJson.get("Fallback_World").getAsString() : "world";
    }

    public String getJoinWorld() {
        return configJson.has("JoinWorld") ? configJson.get("JoinWorld").getAsString() : getFallbackWorld();
    }

    public Set<String> getOpWorlds() {
        Set<String> result = new HashSet<>();
        if (configJson.has("OpWorlds")) {
            JsonArray arr = configJson.getAsJsonArray("OpWorlds");
            for (int i = 0; i < arr.size(); i++) result.add(arr.get(i).getAsString().toLowerCase());
        }
        return result;
    }

    public Set<String> getUniqueWorlds() {
        Set<String> result = new HashSet<>();
        if (configJson.has("UniqueWorlds")) {
            JsonArray arr = configJson.getAsJsonArray("UniqueWorlds");
            for (int i = 0; i < arr.size(); i++) result.add(arr.get(i).getAsString().toLowerCase());
        }
        return result;
    }

    private void loadConfig() {
        try {
            java.io.File file = new java.io.File(getDataFolder(), "world.json");
            if (!file.exists()) {
                getDataFolder().mkdirs();
                configJson = new JsonObject();
                configJson.addProperty("Fallback_World", "world");
                configJson.addProperty("JoinWorld", "world");
                configJson.add("OpWorlds", new JsonArray());
                configJson.add("UniqueWorlds", new JsonArray());
                saveConfigJson();
            } else {
                try (FileReader reader = new FileReader(file)) {
                    configJson = gson.fromJson(reader, JsonObject.class);
                    if (!configJson.has("OpWorlds")) configJson.add("OpWorlds", new JsonArray());
                    if (!configJson.has("UniqueWorlds")) configJson.add("UniqueWorlds", new JsonArray());
                    saveConfigJson();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveConfigJson() {
        try (FileWriter writer = new FileWriter(new java.io.File(getDataFolder(), "world.json"))) {
            gson.toJson(configJson, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 🔹 Maintenance
    public void setMaintenance(String world, double minutes, String reason) {
        long expiry = System.currentTimeMillis() + (long) (minutes * 60_000);
        maintenanceWorlds.put(world.toLowerCase(), expiry);
        maintenanceReasons.put(world.toLowerCase(), reason);
    }

    public void removeMaintenance(String world) {
        maintenanceWorlds.remove(world.toLowerCase());
        maintenanceReasons.remove(world.toLowerCase());
    }

    public boolean isUnderMaintenance(String world) {
        Long expiry = maintenanceWorlds.get(world.toLowerCase());
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            removeMaintenance(world);
            return false;
        }
        return true;
    }

    public void startMaintenance(String worldName, double minutes, String reason) {
        setMaintenance(worldName, minutes, reason);

        // Kick all non-ops from the world
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            for (Player p : world.getPlayers()) {
                if (!p.isOp() && !p.hasPermission("worldnet.maintenance.bypass")) {
                    World fallback = Bukkit.getWorld(getFallbackWorld());
                    if (fallback != null) {
                        p.teleport(fallback.getSpawnLocation());
                        p.sendMessage("§cThis world is under maintenance: §f" + reason);
                    }
                }
            }
        }

        // BossBar
        if (maintenanceBar != null) maintenanceBar.removeAll();
        if (maintenanceTask != null) maintenanceTask.cancel();

        long totalTicks = (long) (minutes * 60); // seconds
        final long[] remaining = {totalTicks};

        maintenanceBar = Bukkit.createBossBar("§c[Maintenance] §f" + reason, BarColor.RED, BarStyle.SOLID);
        maintenanceBar.setProgress(1.0);
        for (Player p : Bukkit.getOnlinePlayers()) maintenanceBar.addPlayer(p);

        maintenanceTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (remaining[0] <= 0) {
                    stopMaintenance();
                    cancel();
                    return;
                }
                double progress = (double) remaining[0] / totalTicks;
                maintenanceBar.setProgress(progress);
                long minutesLeft = remaining[0] / 60;
                long secondsLeft = remaining[0] % 60;
                maintenanceBar.setTitle("§c[Maintenance] §f" + reason + " §7(" + minutesLeft + "m " + secondsLeft + "s left)");
                remaining[0]--;
            }
        };
        maintenanceTask.runTaskTimer(this, 0L, 20L);
    }

    public void stopMaintenance() {
        if (maintenanceTask != null) {
            maintenanceTask.cancel();
            maintenanceTask = null;
        }
        if (maintenanceBar != null) {
            maintenanceBar.removeAll();
            maintenanceBar = null;
        }
    }

    public String getMaintenanceReason(String world) {
        return maintenanceReasons.getOrDefault(world.toLowerCase(), "No reason specified");
    }

    // 🔹 Player join/quit
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String joinWorldName = getJoinWorld();
        World joinWorld = Bukkit.getWorld(joinWorldName);

        if (joinWorld != null) {
            player.teleport(joinWorld.getSpawnLocation());
        } else {
            getLogger().warning("JoinWorld '" + joinWorldName + "' not found! Defaulting to Fallback_World.");
            World fallback = Bukkit.getWorld(getFallbackWorld());
            if (fallback != null) player.teleport(fallback.getSpawnLocation());
        }

        if (maintenanceBar != null) maintenanceBar.addPlayer(player);
        refreshTablists();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        refreshTablists();
    }

    // 🔹 UniqueWorlds chat isolation
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        if (getUniqueWorlds().contains(sender.getWorld().getName().toLowerCase())) {
            event.getRecipients().clear();
            event.getRecipients().addAll(sender.getWorld().getPlayers());
        }
    }

    // 🔹 Tablist isolation (refresh every 5 seconds via scheduler)
    private void refreshTablists() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            String worldName = p.getWorld().getName().toLowerCase();
            Set<String> uniqueWorlds = getUniqueWorlds();

            for (Player other : Bukkit.getOnlinePlayers()) {
                if (p == other) continue;
                String otherWorld = other.getWorld().getName().toLowerCase();

                if (uniqueWorlds.contains(worldName) && uniqueWorlds.contains(otherWorld)) {
                    if (!worldName.equals(otherWorld)) p.hidePlayer(this, other);
                    else p.showPlayer(this, other);
                } else {
                    p.showPlayer(this, other);
                }
            }
        }
    }
}
