package saturn.generalFeatures.impl.restrictions.dimension;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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

public class EndControl implements CommandExecutor, TabCompleter, Listener {

    private final JavaPlugin plugin;
    private File configFile;
    private FileConfiguration config;

    private boolean endDisabled = true;
    private BukkitRunnable endTask = null;
    private long endEndTime = -1;

    public EndControl(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        restoreState();
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "end_control.yml");

        if (!configFile.exists()) {
            if (plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().info("Created plugin directory");
            }
            try {
                if (configFile.createNewFile()) {
                    plugin.getLogger().info("Created end_control.yml file");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create end_control.yml file!");
                return;
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        endDisabled = config.getBoolean("end.disabled", true);
        endEndTime = config.getLong("end.end-time", -1);
    }

    private void restoreState() {
        long currentTime = System.currentTimeMillis();

        if (endDisabled) {
            if (endEndTime > currentTime) {
                long remainingTime = endEndTime - currentTime;
                long remainingMinutes = remainingTime / (60 * 1000);

                startEndTimer(remainingMinutes);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
                    Bukkit.broadcast(Component.text("🌌 End Access Disabled").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
                    Bukkit.broadcast(Component.empty());
                    Bukkit.broadcast(Component.text("Time remaining: " + formatTime(remainingMinutes)).color(NamedTextColor.LIGHT_PURPLE));
                    Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
                });
            } else if (endEndTime == -1) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
                    Bukkit.broadcast(Component.text("🌌 End Access Disabled").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
                    Bukkit.broadcast(Component.empty());
                    Bukkit.broadcast(Component.text("Duration: Indefinite").color(NamedTextColor.LIGHT_PURPLE));
                    Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
                });
            } else {
                endDisabled = false;
                endEndTime = -1;
            }
        }

        saveConfig();
    }

    public void saveConfig() {
        config.set("end.disabled", endDisabled);
        config.set("end.end-time", endEndTime);

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save end_control.yml file!");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("generalfeatures.endcontrol")) {
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
        if (endTask != null) {
            endTask.cancel();
            endTask = null;
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

        endDisabled = true;
        teleportPlayersFromEnd();

        Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(Component.text("🌌 End Access Disabled").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(Component.text("Players cannot enter the End").color(NamedTextColor.LIGHT_PURPLE));

        if (minutes > 0) {
            endEndTime = System.currentTimeMillis() + (minutes * 60 * 1000);
            startEndTimer(minutes);
            Bukkit.broadcast(Component.text("Duration: " + formatTime(minutes)).color(NamedTextColor.GREEN));
        } else {
            endEndTime = -1;
            Bukkit.broadcast(Component.text("Duration: Indefinite").color(NamedTextColor.GREEN));
        }

        Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));

        saveConfig();
        return true;
    }

    private boolean handleEnable(CommandSender sender) {
        if (!endDisabled) {
            sender.sendMessage(Component.text("The End is already enabled!").color(NamedTextColor.YELLOW));
            return true;
        }

        if (endTask != null) {
            endTask.cancel();
            endTask = null;
        }

        endDisabled = false;
        endEndTime = -1;

        Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(Component.text("🌌 End Access Enabled").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(Component.text("Players can now enter the End").color(NamedTextColor.YELLOW));
        Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));

        saveConfig();
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        sender.sendMessage(Component.text("═══ End Status ═══").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));

        if (endDisabled) {
            sender.sendMessage(Component.text("Status: ").color(NamedTextColor.GRAY)
                    .append(Component.text("Disabled").color(NamedTextColor.RED)));
            sender.sendMessage(Component.text("Players cannot enter the End").color(NamedTextColor.YELLOW));

            if (endEndTime > 0) {
                long remainingTime = endEndTime - System.currentTimeMillis();
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
            sender.sendMessage(Component.text("Players can enter the End").color(NamedTextColor.YELLOW));
        }

        return true;
    }

    private void startEndTimer(long minutes) {
        long ticks = minutes * 60 * 20;

        endTask = new BukkitRunnable() {
            @Override
            public void run() {
                endDisabled = false;
                endEndTime = -1;

                Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
                Bukkit.broadcast(Component.text("🌌 End Access Enabled").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
                Bukkit.broadcast(Component.empty());
                Bukkit.broadcast(Component.text("Players can now enter the End").color(NamedTextColor.YELLOW));
                Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));

                endTask = null;
                saveConfig();
            }
        };

        endTask.runTaskLater(plugin, ticks);
    }

    private void teleportPlayersFromEnd() {
        World endWorld = null;
        World overworld = null;

        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == Environment.THE_END) {
                endWorld = world;
            } else if (world.getEnvironment() == Environment.NORMAL) {
                overworld = world;
            }
        }

        if (endWorld != null && overworld != null) {
            Location spawnLocation = overworld.getSpawnLocation();

            for (Player player : endWorld.getPlayers()) {
                player.teleport(spawnLocation);
                player.sendMessage(Component.text("✖ You were teleported out because the End was disabled").color(NamedTextColor.DARK_PURPLE));
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
        sender.sendMessage(Component.text("═══ End Control Commands ═══").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("What is End Control?").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("Prevents players from entering the End").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Commands:").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("  /end disable [time]").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Block access to the End").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    Time is optional (e.g., 30m, 1h, 2h)").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("    No time = indefinite").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  /end enable").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Allow access to the End").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  /end status").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Check current End status").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Examples:").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("  /end disable").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    → Disable End indefinitely").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("  /end disable 2h").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    → Disable End for 2 hours").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("  /end enable").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    → Enable End access").color(NamedTextColor.DARK_GRAY));
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!endDisabled) return;

        if (event.getTo() != null &&
                event.getTo().getWorld() != null &&
                event.getTo().getWorld().getEnvironment() == Environment.THE_END) {

            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("✖ The End is currently disabled").color(NamedTextColor.DARK_PURPLE));
        }
    }

    @EventHandler
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
        if (!endDisabled) return;

        Entity entity = event.getEntity();

        if (entity.getLocation().getBlock().getType().name().contains("END_PORTAL")) {
            if (entity instanceof Player player) {
                player.sendMessage(Component.text("✖ The End is currently disabled").color(NamedTextColor.DARK_PURPLE));
            }
            entity.remove();
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("generalfeatures.endcontrol")) {
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
        if (endTask != null) {
            endTask.cancel();
        }
        saveConfig();
    }
}