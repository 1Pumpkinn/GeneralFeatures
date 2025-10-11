package hs.generalFeatures.impl;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffectType;

public class InvisibilityNameHider implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // Check if player has invisibility effect
        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            // Cancel the original chat event
            event.setCancelled(true);

            // Send the message without showing the player's name
            Component hiddenMessage = Component.text("[???] ", NamedTextColor.GRAY)
                    .append(event.message());

            // Broadcast the anonymous message to all players
            event.viewers().forEach(viewer -> {
                if (viewer instanceof Player) {
                    viewer.sendMessage(hiddenMessage);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Check if the killer exists and has invisibility
        if (killer != null && killer.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            // Get the original death message
            Component originalMessage = event.deathMessage();

            if (originalMessage != null) {
                // Replace killer's name with "???" in the death message
                String originalText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(originalMessage);

                String killerName = killer.getName();
                String modifiedText = originalText.replace(killerName, "???");

                // Set the new death message
                event.deathMessage(Component.text(modifiedText));
            }
        }

        // Also check if the victim was invisible (hide their name too)
        if (victim.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            Component originalMessage = event.deathMessage();

            if (originalMessage != null) {
                String originalText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(originalMessage);

                String victimName = victim.getName();
                String modifiedText = originalText.replace(victimName, "???");

                event.deathMessage(Component.text(modifiedText));
            }
        }
    }
}