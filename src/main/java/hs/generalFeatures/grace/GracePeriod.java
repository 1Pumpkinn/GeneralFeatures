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
import org.jetbrains.annotations.NotNull;

public class GracePeriod implements CommandExecutor, Listener {

    private boolean pvpEnabled = true;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("generalfeatures.grace")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /grace <on|off>").color(NamedTextColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            pvpEnabled = false;
            Bukkit.broadcast(Component.text("Grace period activated! PvP is now disabled.").color(NamedTextColor.GOLD));
            Bukkit.broadcast(Component.text("Players are protected from PvP damage.").color(NamedTextColor.YELLOW));
        } else if (args[0].equalsIgnoreCase("off")) {
            pvpEnabled = true;
            Bukkit.broadcast(Component.text("Grace period ended! PvP is now enabled.").color(NamedTextColor.RED));
            Bukkit.broadcast(Component.text("Players can now damage each other.").color(NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("Usage: /grace <on|off>").color(NamedTextColor.RED));
        }

        return true;
    }

    @EventHandler
    public void onEntityDamage(@NotNull EntityDamageByEntityEvent event) {
        if (!pvpEnabled && event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            event.setCancelled(true);
            event.getDamager().sendMessage(Component.text("Grace period is active! PvP is disabled.").color(NamedTextColor.RED));
        }
    }
}