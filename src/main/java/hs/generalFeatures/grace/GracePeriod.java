package hs.generalFeatures.grace;

import hs.generalFeatures.dimension.DisableNether;
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
    private BukkitRunnable netherTask = null;
    private final JavaPlugin plugin;
    private final DisableNether netherControl;

    public GracePeriod(JavaPlugin plugin, DisableNether netherControl) {
        this.plugin = plugin;
        this.netherControl = netherControl;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("generalfeatures.grace")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /grace <on|off> [grace-time] [nether-time]").color(NamedTextColor.RED));
            sender.sendMessage(Component.text("Examples:").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("  /grace on - Enable grace period and nether disable indefinitely").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /grace on 30m - Enable grace period for 30 minutes, nether disabled indefinitely").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /grace on 30m 1h - Grace for 30 minutes, nether disabled for 1 hour").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /grace on 0 1h - No grace period, only nether disabled for 1 hour").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /grace off - Disable grace period and enable nether").color(NamedTextColor.GRAY));
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            // Cancel any existing timers
            if (graceTask != null) {
                graceTask.cancel();
                graceTask = null;
            }
            if (netherTask != null) {
                netherTask.cancel();
                netherTask = null;
            }

            // Parse grace period time
            long graceMinutes = -1; // -1 means infinite
            if (args.length >= 2) {
                graceMinutes = parseTime(args[1]);
                if (graceMinutes == -2) { // -2 means parse error
                    sender.sendMessage(Component.text("Invalid grace time format! Use format like '30m' or '1h'").color(NamedTextColor.RED));
                    return true;
                }
            }

            // Parse nether disable time
            long netherMinutes = -1; // -1 means infinite
            if (args.length >= 3) {
                netherMinutes = parseTime(args[2]);
                if (netherMinutes == -2) { // -2 means parse error
                    sender.sendMessage(Component.text("Invalid nether time format! Use format like '30m' or '1h'").color(NamedTextColor.RED));
                    return true;
                }
            }

            // Enable grace period (unless 0)
            if (graceMinutes != 0) {
                pvpEnabled = false;
                if (graceMinutes > 0) {
                    startGraceTimer(graceMinutes);
                    Bukkit.broadcast(Component.text("Grace period activated for " + formatTime(graceMinutes) + "!").color(NamedTextColor.GOLD));
                } else {
                    Bukkit.broadcast(Component.text("Grace period activated indefinitely!").color(NamedTextColor.GOLD));
                }
                Bukkit.broadcast(Component.text("Players are protected from PvP damage.").color(NamedTextColor.YELLOW));
            }

            // Enable nether disable (unless 0)
            if (netherMinutes != 0) {
                netherControl.setNetherDisabled(true);
                if (netherMinutes > 0) {
                    startNetherTimer(netherMinutes);
                    Bukkit.broadcast(Component.text("The Nether has been disabled for " + formatTime(netherMinutes) + "!").color(NamedTextColor.YELLOW));
                } else {
                    Bukkit.broadcast(Component.text("The Nether has been disabled indefinitely!").color(NamedTextColor.YELLOW));
                }
            }

            // If both are 0, inform user
            if (graceMinutes == 0 && netherMinutes == 0) {
                sender.sendMessage(Component.text("Both grace period and nether disable are set to 0. Nothing was changed.").color(NamedTextColor.RED));
            }

        } else if (args[0].equalsIgnoreCase("off")) {
            // Cancel any existing timers
            if (graceTask != null) {
                graceTask.cancel();
                graceTask = null;
            }
            if (netherTask != null) {
                netherTask.cancel();
                netherTask = null;
            }

            pvpEnabled = true;
            netherControl.setNetherDisabled(false);

            Bukkit.broadcast(Component.text("Grace period ended! PvP is now enabled.").color(NamedTextColor.RED));
            Bukkit.broadcast(Component.text("Players can now damage each other.").color(NamedTextColor.YELLOW));
            Bukkit.broadcast(Component.text("The Nether has been enabled.").color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Usage: /grace <on|off> [grace-time] [nether-time]").color(NamedTextColor.RED));
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

    private void startNetherTimer(long minutes) {
        long ticks = minutes * 60 * 20; // Convert minutes to ticks (20 ticks per second)

        netherTask = new BukkitRunnable() {
            @Override
            public void run() {
                netherControl.setNetherDisabled(false);

                Bukkit.broadcast(Component.text("The Nether has been enabled.").color(NamedTextColor.GREEN));
                netherTask = null;
            }
        };

        netherTask.runTaskLater(plugin, ticks);
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
                // Try parsing as plain number (minutes)
                numericPart = timeStr;
            }

            long value = Long.parseLong(numericPart) * multiplier;
            return value; // Return actual value, 0 is valid (means don't enable that feature)
        } catch (NumberFormatException e) {
            return -2; // Return -2 for parse error
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