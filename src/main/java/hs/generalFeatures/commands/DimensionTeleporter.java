package hs.generalFeatures.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DimensionTeleporter implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!").color(NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("generalfeatures.teleportend")) {
            player.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        // Find the End world
        World endWorld = null;
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.THE_END) {
                endWorld = world;
                break;
            }
        }

        if (endWorld == null) {
            player.sendMessage(Component.text("The End world could not be found!").color(NamedTextColor.RED));
            return true;
        }

        // Get the spawn location in the End
        Location endSpawn = endWorld.getSpawnLocation();

        // Teleport the player
        player.teleport(endSpawn);
        player.sendMessage(Component.text("You have been teleported to the End!").color(NamedTextColor.GREEN));

        return true;
    }
}