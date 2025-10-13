package hs.generalFeatures.impl.restrictions.dimension;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class DisableNether implements Listener {

    private boolean netherDisabled = false;

    public void setNetherDisabled(boolean disabled) {
        this.netherDisabled = disabled;

        // Only teleport players out when ENABLING the restriction (grace on)
        // Don't teleport them when disabling (grace off)
        if (disabled) {
            teleportPlayersFromNether();
        }
    }

    public boolean isNetherDisabled() {
        return netherDisabled;
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
                player.sendMessage(Component.text("You have been teleported out of the Nether because it has been disabled.").color(NamedTextColor.YELLOW));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!netherDisabled) return;

        // Check if teleporting to the Nether
        if (event.getTo() != null &&
                event.getTo().getWorld() != null &&
                event.getTo().getWorld().getEnvironment() == World.Environment.NETHER) {

            // Cancel teleport to the Nether
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("The Nether is currently disabled").color(NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
        if (!netherDisabled) return;

        Entity entity = event.getEntity();

        // Check if it's a nether portal
        if (entity.getLocation().getBlock().getType().name().contains("NETHER_PORTAL")) {
            // Handle players - just send a message, teleport is cancelled by onPlayerTeleport
            if (entity instanceof Player player) {
                player.sendMessage(Component.text("The Nether is currently disabled!").color(NamedTextColor.RED));
            } else {
                // For non-player entities (ender pearls, items, mobs), remove them
                entity.remove();
            }
        }
    }
}