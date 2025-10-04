package hs.generalFeatures.grace;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

public class GracePeriod implements CommandExecutor, Listener {

    private boolean pvpEnabled = true;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("generalfeatures.grace")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /grace <on|off>");
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            pvpEnabled = true;
            Bukkit.broadcastMessage(ChatColor.GREEN + "PvP has been enabled!");
        } else if (args[0].equalsIgnoreCase("off")) {
            pvpEnabled = false;
            Bukkit.broadcastMessage(ChatColor.RED + "PvP has been disabled!");
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /grace <on|off>");
        }

        return true;
    }

    @EventHandler
    public void onEntityDamage(@NotNull EntityDamageByEntityEvent event) {
        if (!pvpEnabled && event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            event.setCancelled(true);
            event.getDamager().sendMessage(ChatColor.RED + "PvP is currently disabled!");
        }
    }
}