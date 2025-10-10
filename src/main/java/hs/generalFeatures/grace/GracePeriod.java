package hs.generalFeatures.grace;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class GracePeriod implements CommandExecutor, Listener {

    private boolean pvpEnabled = true;
    private BukkitRunnable graceTask = null;
    private final JavaPlugin plugin;

    public GracePeriod(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("generalfeatures.grace")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /grace <on|off> [time]").color(NamedTextColor.RED));
            sender.sendMessage(Component.text("Examples:").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("  /grace on - Enable grace period indefinitely").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /grace on 30m - Enable grace period for 30 minutes").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /grace on 1h - Enable grace period for 1 hour").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /grace off - Disable grace period").color(NamedTextColor.GRAY));
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            // Cancel any existing grace period timer
            if (graceTask != null) {
                graceTask.cancel();
                graceTask = null;
            }

            pvpEnabled = false;

            // Check if a time duration was specified
            if (args.length >= 2) {
                String timeArg = args[1].toLowerCase();
                long minutes = parseTime(timeArg);

                if (minutes == -1) {
                    sender.sendMessage(Component.text("Invalid time format! Use format like '30m' or '1h'").color(NamedTextColor.RED));
                    return true;
                }

                // Start timer
                startGraceTimer(minutes);

                Bukkit.broadcast(Component.text("Grace period activated for " + formatTime(minutes) + "!").color(NamedTextColor.GOLD));
                Bukkit.broadcast(Component.text("Players are protected from PvP damage.").color(NamedTextColor.YELLOW));
            } else {
                // Infinite grace period
                Bukkit.broadcast(Component.text("Grace period activated! PvP is now disabled.").color(NamedTextColor.GOLD));
                Bukkit.broadcast(Component.text("Players are protected from PvP damage.").color(NamedTextColor.YELLOW));
            }

        } else if (args[0].equalsIgnoreCase("off")) {
            // Cancel any existing grace period timer
            if (graceTask != null) {
                graceTask.cancel();
                graceTask = null;
            }

            pvpEnabled = true;
            Bukkit.broadcast(Component.text("Grace period ended! PvP is now enabled.").color(NamedTextColor.RED));
            Bukkit.broadcast(Component.text("Players can now damage each other.").color(NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("Usage: /grace <on|off> [time]").color(NamedTextColor.RED));
        }

        return true;
    }

    private void startGraceTimer(long minutes) {
        long ticks = minutes * 60 * 20; // Convert minutes to ticks (20 ticks per second)

        graceTask = new BukkitRunnable() {
            @Override
            public void run() {
                pvpEnabled = true;
                Bukkit.broadcast(Component.text("Grace period has ended! PvP is now enabled.").color(NamedTextColor.RED));
                Bukkit.broadcast(Component.text("Players can now damage each other.").color(NamedTextColor.YELLOW));
                graceTask = null;
            }
        };

        graceTask.runTaskLater(plugin, ticks);
    }

    private long parseTime(String timeStr) {
        try {
            String numericPart;
            long multiplier = 1;

            if (timeStr.endsWith("m")) {
                // Minutes
                numericPart = timeStr.substring(0, timeStr.length() - 1);
                multiplier = 1;
            } else if (timeStr.endsWith("h")) {
                // Hours (convert to minutes)
                numericPart = timeStr.substring(0, timeStr.length() - 1);
                multiplier = 60;
            } else {
                // Try parsing as plain minutes
                numericPart = timeStr;
            }

            return Long.parseLong(numericPart) * multiplier;
        } catch (NumberFormatException e) {
            return -1;
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

    @EventHandler
    public void onEntityDamage(@NotNull EntityDamageByEntityEvent event) {
        if (!pvpEnabled && event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            event.setCancelled(true);
            event.getDamager().sendMessage(Component.text("Grace period is active! PvP is disabled.").color(NamedTextColor.RED));
        }
    }
}