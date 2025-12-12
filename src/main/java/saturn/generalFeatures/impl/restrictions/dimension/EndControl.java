package saturn.generalFeatures.impl.restrictions.dimension;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class EndControl implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private File configFile;
    private FileConfiguration config;
    private boolean endDisabled = true; // Default to disabled

    public EndControl(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
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
        
        // Load settings with default (disabled)
        endDisabled = config.getBoolean("end.disabled", true);
    }

    public void saveConfig() {
        config.set("end.disabled", endDisabled);
        
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
        
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /end <enable|disable>").color(NamedTextColor.RED));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("enable")) {
            endDisabled = false;
            sender.sendMessage(Component.text("The End has been enabled!").color(NamedTextColor.GREEN));
            Bukkit.broadcast(Component.text("The End has been enabled!").color(NamedTextColor.GREEN), "generalfeatures.endcontrol.notify");
        } else if (args[0].equalsIgnoreCase("disable")) {
            endDisabled = true;
            sender.sendMessage(Component.text("The End has been disabled!").color(NamedTextColor.GREEN));
            Bukkit.broadcast(Component.text("The End has been disabled!").color(NamedTextColor.RED), "generalfeatures.endcontrol.notify");
            
            // Teleport players out of the End if it's being disabled
            teleportPlayersFromEnd();
        } else {
            sender.sendMessage(Component.text("Usage: /end <enable|disable>").color(NamedTextColor.RED));
        }
        
        saveConfig();
        return true;
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
                player.sendMessage(Component.text("You have been teleported out of the End because it has been disabled.").color(NamedTextColor.YELLOW));
            }
        }
    }
    
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!endDisabled) return;
        
        // Check if teleporting to the End
        if (event.getTo() != null && 
            event.getTo().getWorld() != null && 
            event.getTo().getWorld().getEnvironment() == Environment.THE_END) {
            
            // Cancel teleport to the End
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("The End is currently disabled!").color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
        if (!endDisabled) return;

        Entity entity = event.getEntity();

        // Check if it's an end portal
        if (entity.getLocation().getBlock().getType().name().contains("END_PORTAL")) {
            // Handle players with a message
            if (entity instanceof Player player) {
                player.sendMessage(Component.text("The End is currently disabled!").color(NamedTextColor.RED));
            }
            // Remove all entities (players, ender pearls, items, mobs, etc.)
            entity.remove();
        }
    }
}
