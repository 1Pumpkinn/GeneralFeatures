package saturn.ElementSmpManager.impl.grace;

import saturn.ElementSmpManager.impl.restrictions.dimension.DisableNether;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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

public class GracePeriod implements CommandExecutor, TabCompleter, Listener {

    private boolean pvpEnabled = true;
    private BukkitRunnable graceTask = null;
    private final JavaPlugin plugin;
    private final DisableNether netherControl;

    private File configFile;
    private FileConfiguration config;

    private long graceEndTime = -1; // -1 means not active

    public GracePeriod(JavaPlugin plugin, DisableNether netherControl) {
        this.plugin = plugin;
        this.netherControl = netherControl;
        loadConfig();
        restoreTimers();
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "grace_period.yml");

        if (!configFile.exists()) {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create grace_period.yml file!");
                return;
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void saveConfig() {
        config.set("grace.pvp-enabled", pvpEnabled);
        config.set("grace.grace-end-time", graceEndTime);

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save grace_period.yml file!");
        }
    }

    private void restoreTimers() {
        pvpEnabled = config.getBoolean("grace.pvp-enabled", true);
        graceEndTime = config.getLong("grace.grace-end-time", -1);

        long currentTime = System.currentTimeMillis();

        // Restore grace period timer if it hasn't expired
        if (graceEndTime > currentTime && !pvpEnabled) {
            long remainingTime = graceEndTime - currentTime;
            long remainingSeconds = remainingTime / 1000;

            startGraceTimer(remainingSeconds);

            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                Bukkit.broadcast(Component.text("⚔ Grace Period Active").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                Bukkit.broadcast(Component.empty());
                Bukkit.broadcast(Component.text("Players are protected from PvP damage").color(NamedTextColor.YELLOW));
                Bukkit.broadcast(Component.text("Time remaining: " + formatTime(remainingSeconds)).color(NamedTextColor.GREEN));
                Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            });
        } else if (!pvpEnabled) {
            // Grace period was infinite, keep it disabled
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                Bukkit.broadcast(Component.text("⚔ Grace Period Active").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                Bukkit.broadcast(Component.empty());
                Bukkit.broadcast(Component.text("Players are protected from PvP damage").color(NamedTextColor.YELLOW));
                Bukkit.broadcast(Component.text("Duration: Indefinite").color(NamedTextColor.GREEN));
                Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            });
        } else {
            graceEndTime = -1;
        }

        saveConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("generalfeatures.grace")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "enable":
            case "on":
                return handleEnable(sender, args);
            case "disable":
            case "off":
                return handleDisable(sender);
            case "status":
            case "info":
                return handleStatus(sender);
            default:
                sendUsage(sender);
                return true;
        }
    }

    private boolean handleEnable(CommandSender sender, String[] args) {
        // Cancel any existing timer
        if (graceTask != null) {
            graceTask.cancel();
            graceTask = null;
        }

        long seconds = -1; // -1 means infinite

        if (args.length >= 2) {
            seconds = parseTime(args[1]);
            if (seconds == -2) { // -2 means parse error
                sender.sendMessage(Component.text("Invalid time format!").color(NamedTextColor.RED));
                sender.sendMessage(Component.text("Examples: 30s, 5m, 30m, 1h, 90m, 2h").color(NamedTextColor.YELLOW));
                return true;
            }
        }

        pvpEnabled = false;

        // Broadcast to all players
        Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(Component.text("⚔ Grace Period Enabled").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(Component.text("Players are now protected from PvP damage").color(NamedTextColor.YELLOW));

        if (seconds > 0) {
            graceEndTime = System.currentTimeMillis() + (seconds * 1000);
            startGraceTimer(seconds);
            Bukkit.broadcast(Component.text("Duration: " + formatTime(seconds)).color(NamedTextColor.GREEN));
        } else {
            graceEndTime = -1;
            Bukkit.broadcast(Component.text("Duration: Indefinite").color(NamedTextColor.GREEN));
        }

        Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));

        saveConfig();
        return true;
    }

    private boolean handleDisable(CommandSender sender) {
        if (pvpEnabled) {
            sender.sendMessage(Component.text("Grace period is already disabled!").color(NamedTextColor.YELLOW));
            return true;
        }

        // Cancel any existing timer
        if (graceTask != null) {
            graceTask.cancel();
            graceTask = null;
        }

        // End the grace period
        endGracePeriod();
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        sender.sendMessage(Component.text("═══ Grace Period Status ═══").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));

        if (pvpEnabled) {
            sender.sendMessage(Component.text("Status: ").color(NamedTextColor.GRAY)
                    .append(Component.text("Disabled").color(NamedTextColor.RED)));
            sender.sendMessage(Component.text("PvP is currently enabled").color(NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("Status: ").color(NamedTextColor.GRAY)
                    .append(Component.text("Active").color(NamedTextColor.GREEN)));
            sender.sendMessage(Component.text("Players are protected from PvP").color(NamedTextColor.YELLOW));

            if (graceEndTime > 0) {
                long remainingTime = graceEndTime - System.currentTimeMillis();
                if (remainingTime > 0) {
                    long remainingSeconds = remainingTime / 1000;
                    sender.sendMessage(Component.text("Time remaining: " + formatTime(remainingSeconds)).color(NamedTextColor.GREEN));
                }
            } else {
                sender.sendMessage(Component.text("Duration: Indefinite").color(NamedTextColor.GREEN));
            }
        }

        return true;
    }

    private void startGraceTimer(long seconds) {
        long ticks = seconds * 20;

        graceTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Automatically disable grace period when timer expires
                endGracePeriod();
            }
        };

        graceTask.runTaskLater(plugin, ticks);
    }

    private void endGracePeriod() {
        pvpEnabled = true;
        graceEndTime = -1;

        // Notify nether control that grace period ended
        if (netherControl != null) {
            netherControl.onGracePeriodEnd();
        }

        Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(Component.text("⚔ Grace Period Ended").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(Component.text("PvP is now enabled!").color(NamedTextColor.YELLOW));
        Bukkit.broadcast(Component.text("Players can now damage each other").color(NamedTextColor.GRAY));
        Bukkit.broadcast(Component.text("═══════════════════════════════").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));

        graceTask = null;
        saveConfig();
    }

    private long parseTime(String timeStr) {
        try {
            String numericPart;
            long multiplier = 1;

            if (timeStr.endsWith("s")) {
                numericPart = timeStr.substring(0, timeStr.length() - 1);
                multiplier = 1;
                // Convert seconds to minutes (fractional)
                long seconds = Long.parseLong(numericPart);
                return seconds; // Return seconds, will be converted properly
            } else if (timeStr.endsWith("m")) {
                numericPart = timeStr.substring(0, timeStr.length() - 1);
                multiplier = 60; // minutes to seconds
            } else if (timeStr.endsWith("h")) {
                numericPart = timeStr.substring(0, timeStr.length() - 1);
                multiplier = 3600; // hours to seconds
            } else {
                numericPart = timeStr;
                multiplier = 60; // default to minutes
            }

            long value = Long.parseLong(numericPart) * multiplier;
            return value; // Return total seconds
        } catch (NumberFormatException e) {
            return -2;
        }
    }

    private String formatTime(long seconds) {
        if (seconds >= 3600) {
            long hours = seconds / 3600;
            long remainingMinutes = (seconds % 3600) / 60;
            long remainingSeconds = seconds % 60;

            if (remainingMinutes == 0 && remainingSeconds == 0) {
                return hours + " hour" + (hours != 1 ? "s" : "");
            } else if (remainingSeconds == 0) {
                return hours + " hour" + (hours != 1 ? "s" : "") + " and " + remainingMinutes + " minute" + (remainingMinutes != 1 ? "s" : "");
            } else if (remainingMinutes == 0) {
                return hours + " hour" + (hours != 1 ? "s" : "") + " and " + remainingSeconds + " second" + (remainingSeconds != 1 ? "s" : "");
            } else {
                return hours + "h " + remainingMinutes + "m " + remainingSeconds + "s";
            }
        } else if (seconds >= 60) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            if (remainingSeconds == 0) {
                return minutes + " minute" + (minutes != 1 ? "s" : "");
            } else {
                return minutes + " minute" + (minutes != 1 ? "s" : "") + " and " + remainingSeconds + " second" + (remainingSeconds != 1 ? "s" : "");
            }
        } else {
            return seconds + " second" + (seconds != 1 ? "s" : "");
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("═══ Grace Period Commands ═══").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("What is Grace Period?").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("Disables PvP damage between players").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Commands:").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("  /grace enable [time]").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Enable PvP protection").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    Time is optional (e.g., 30s, 5m, 30m, 1h, 2h)").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("    No time = indefinite").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  /grace disable").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Disable PvP protection (enable PvP)").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  /grace status").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Check current grace period status").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Examples:").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("  /grace enable").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    → Enable grace indefinitely").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("  /grace enable 30m").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    → Enable grace for 30 minutes").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("  /grace enable 1h").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    → Enable grace for 1 hour").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("  /grace disable").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    → Disable grace (enable PvP)").color(NamedTextColor.DARK_GRAY));
    }

    @EventHandler
    public void onEntityDamage(@NotNull EntityDamageByEntityEvent event) {
        if (!pvpEnabled && event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            event.setCancelled(true);
            event.getDamager().sendMessage(Component.text("✖ Grace period is active! PvP is disabled.").color(NamedTextColor.RED));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("generalfeatures.grace")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Arrays.asList("enable", "disable", "status")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("enable")) {
            return Arrays.asList("30s", "1m", "5m", "30m", "1h", "2h", "3h");
        }

        return Collections.emptyList();
    }

    public void onDisable() {
        if (graceTask != null) {
            graceTask.cancel();
        }
        saveConfig();
    }
}