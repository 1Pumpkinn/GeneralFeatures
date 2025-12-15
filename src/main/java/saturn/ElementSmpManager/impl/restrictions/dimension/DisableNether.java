package saturn.ElementSmpManager.impl.restrictions.dimension;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DisableNether implements CommandExecutor, TabCompleter, Listener {

    private final JavaPlugin plugin;
    private File configFile;
    private FileConfiguration config;

    private boolean netherDisabled = false;
    private BukkitRunnable netherTask = null;
    private long netherEndTime = -1;

    public DisableNether(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        restoreState();
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "nether_control.yml");

        if (!configFile.exists()) {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create nether_control.yml file!");
                return;
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void saveConfig() {
        config.set("nether.disabled", netherDisabled);
        config.set("nether.end-time", netherEndTime);

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save nether_control.yml file!");
        }
    }

    private void restoreState() {
        netherDisabled = config.getBoolean("nether.disabled", false);
        netherEndTime = config.getLong("nether.end-time", -1);

        long currentTime = System.currentTimeMillis();

        if (netherDisabled) {
            if (netherEndTime > currentTime) {
                long remainingTime = netherEndTime - currentTime;
                long remainingMinutes = remainingTime / (60 * 1000);

                startNetherTimer(remainingMinutes);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
                    Bukkit.broadcast(Component.text("🔥 Nether Access Disabled").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
                    Bukkit.broadcast(Component.empty());
                    Bukkit.broadcast(Component.text("Time remaining: " + formatTime(remainingMinutes)).color(NamedTextColor.YELLOW));
                    Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
                });
            } else if (netherEndTime == -1) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
                    Bukkit.broadcast(Component.text("🔥 Nether Access Disabled").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
                    Bukkit.broadcast(Component.empty());
                    Bukkit.broadcast(Component.text("Duration: Indefinite").color(NamedTextColor.YELLOW));
                    Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
                });
            } else {
                netherDisabled = false;
                netherEndTime = -1;
            }
        }

        saveConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("generalfeatures.nether")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "disable":
            case "off":
                return handleDisable(sender, args);
            case "enable":
            case "on":
                return handleEnable(sender);
            case "status":
            case "info":
                return handleStatus(sender);
            default:
                sendUsage(sender);
                return true;
        }
    }

    private boolean handleDisable(CommandSender sender, String[] args) {
        if (netherTask != null) {
            netherTask.cancel();
            netherTask = null;
        }

        long minutes = -1;

        if (args.length >= 2) {
            minutes = parseTime(args[1]);
            if (minutes == -2) {
                sender.sendMessage(Component.text("Invalid time format!").color(NamedTextColor.RED));
                sender.sendMessage(Component.text("Examples: 30m, 1h, 90m, 2h").color(NamedTextColor.YELLOW));
                return true;
            }
        }

        netherDisabled = true;
        teleportPlayersFromNether();

        Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(Component.text("🔥 Nether Access Disabled").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(Component.text("Players cannot enter the Nether").color(NamedTextColor.YELLOW));

        if (minutes > 0) {
            netherEndTime = System.currentTimeMillis() + (minutes * 60 * 1000);
            startNetherTimer(minutes);
            Bukkit.broadcast(Component.text("Duration: " + formatTime(minutes)).color(NamedTextColor.GREEN));
        } else {
            netherEndTime = -1;
            Bukkit.broadcast(Component.text("Duration: Indefinite").color(NamedTextColor.GREEN));
        }

        Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));

        saveConfig();
        return true;
    }

    private boolean handleEnable(CommandSender sender) {
        if (!netherDisabled) {
            sender.sendMessage(Component.text("The Nether is already enabled!").color(NamedTextColor.YELLOW));
            return true;
        }

        if (netherTask != null) {
            netherTask.cancel();
            netherTask = null;
        }

        netherDisabled = false;
        netherEndTime = -1;

        Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(Component.text("🔥 Nether Access Enabled").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(Component.text("Players can now enter the Nether").color(NamedTextColor.YELLOW));
        Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));

        saveConfig();
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        sender.sendMessage(Component.text("═══ Nether Status ═══").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));

        if (netherDisabled) {
            sender.sendMessage(Component.text("Status: ").color(NamedTextColor.GRAY)
                    .append(Component.text("Disabled").color(NamedTextColor.RED)));
            sender.sendMessage(Component.text("Players cannot enter the Nether").color(NamedTextColor.YELLOW));

            if (netherEndTime > 0) {
                long remainingTime = netherEndTime - System.currentTimeMillis();
                if (remainingTime > 0) {
                    long remainingMinutes = remainingTime / (60 * 1000);
                    sender.sendMessage(Component.text("Time remaining: " + formatTime(remainingMinutes)).color(NamedTextColor.GREEN));
                }
            } else {
                sender.sendMessage(Component.text("Duration: Indefinite").color(NamedTextColor.GREEN));
            }
        } else {
            sender.sendMessage(Component.text("Status: ").color(NamedTextColor.GRAY)
                    .append(Component.text("Enabled").color(NamedTextColor.GREEN)));
            sender.sendMessage(Component.text("Players can enter the Nether").color(NamedTextColor.YELLOW));
        }

        return true;
    }

    private void startNetherTimer(long minutes) {
        long ticks = minutes * 60 * 20;

        netherTask = new BukkitRunnable() {
            @Override
            public void run() {
                netherDisabled = false;
                netherEndTime = -1;

                Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
                Bukkit.broadcast(Component.text("🔥 Nether Access Enabled").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
                Bukkit.broadcast(Component.empty());
                Bukkit.broadcast(Component.text("Players can now enter the Nether").color(NamedTextColor.YELLOW));
                Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));

                netherTask = null;
                saveConfig();
            }
        };

        netherTask.runTaskLater(plugin, ticks);
    }

    private void teleportPlayersFromNether() {
        World netherWorld = null;
        World overworld = null;

        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NETHER) {
                netherWorld = world;
            } else if (world.getEnvironment() == World.Environment.NORMAL) {
                overworld = world;
            }
        }

        if (netherWorld != null && overworld != null) {
            Location spawnLocation = overworld.getSpawnLocation();

            for (Player player : netherWorld.getPlayers()) {
                player.teleport(spawnLocation);
                player.sendMessage(Component.text("✖ You were teleported out because the Nether was disabled").color(NamedTextColor.RED));
            }
        }
    }

    private long parseTime(String timeStr) {
        try {
            String numericPart;
            long multiplier = 1;

            if (timeStr.endsWith("m")) {
                numericPart = timeStr.substring(0, timeStr.length() - 1);
                multiplier = 1;
            } else if (timeStr.endsWith("h")) {
                numericPart = timeStr.substring(0, timeStr.length() - 1);
                multiplier = 60;
            } else {
                numericPart = timeStr;
            }

            return Long.parseLong(numericPart) * multiplier;
        } catch (NumberFormatException e) {
            return -2;
        }
    }

    private String formatTime(long minutes) {
        if (minutes >= 60) {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                return hours + " hour" + (hours != 1 ? "s" : "");
            } else {
                return hours + " hour" + (hours != 1 ? "s" : "") + " and " + remainingMinutes + " minute" + (remainingMinutes != 1 ? "s" : "");
            }
        } else {
            return minutes + " minute" + (minutes != 1 ? "s" : "");
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("═══ Nether Control Commands ═══").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("What is Nether Control?").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("Prevents players from entering the Nether").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Commands:").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("  /nether disable [time]").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Block access to the Nether").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    Time is optional (e.g., 30m, 1h, 2h)").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("    No time = indefinite").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  /nether enable").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Allow access to the Nether").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  /nether status").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Check current Nether status").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Examples:").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("  /nether disable").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    → Disable Nether indefinitely").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("  /nether disable 1h").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    → Disable Nether for 1 hour").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("  /nether enable").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    → Enable Nether access").color(NamedTextColor.DARK_GRAY));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!netherDisabled) return;

        if (event.getTo() != null &&
                event.getTo().getWorld() != null &&
                event.getTo().getWorld().getEnvironment() == World.Environment.NETHER) {

            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("✖ The Nether is currently disabled").color(NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
        if (!netherDisabled) return;

        Entity entity = event.getEntity();

        if (entity.getLocation().getBlock().getType().name().contains("NETHER_PORTAL")) {
            if (entity instanceof Player player) {
                player.sendMessage(Component.text("✖ The Nether is currently disabled").color(NamedTextColor.RED));
            } else {
                entity.remove();
            }
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("generalfeatures.nether")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Arrays.asList("disable", "enable", "status")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("disable")) {
            return Arrays.asList("30m", "1h", "2h", "3h");
        }

        return Collections.emptyList();
    }

    public void onDisable() {
        if (netherTask != null) {
            netherTask.cancel();
        }
        saveConfig();
    }

    // Legacy methods for backward compatibility
    public void setNetherDisabled(boolean disabled) {
        if (disabled) {
            handleDisable(Bukkit.getConsoleSender(), new String[]{"disable"});
        } else {
            handleEnable(Bukkit.getConsoleSender());
        }
    }

    public boolean isNetherDisabled() {
        return netherDisabled;
    }
}