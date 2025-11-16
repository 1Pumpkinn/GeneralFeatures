package hs.generalFeatures.impl.mace;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class MaceCooldown implements Listener {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 35000; // 35 seconds in milliseconds
    private static final int COOLDOWN_TICKS = 700;   // 35 seconds * 20 ticks

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if the DAMAGER is a player (not the victim)
        if (!(event.getDamager() instanceof Player damager)) return;

        // Check if the damager is holding a mace
        ItemStack item = damager.getInventory().getItemInMainHand();
        if (item.getType() != Material.MACE) return;

        // Check if the event is already cancelled (miss/no damage dealt)
        if (event.isCancelled()) return;

        UUID playerId = damager.getUniqueId();

        // Check if player is on cooldown
        if (cooldowns.containsKey(playerId)) {
            long timeLeft = (cooldowns.get(playerId) + COOLDOWN_TIME) - System.currentTimeMillis();

            if (timeLeft > 0) {
                // Still on cooldown, cancel the damage
                event.setCancelled(true);
                return;
            } else {
                // Cooldown expired, clean up
                cooldowns.remove(playerId);
            }
        }

        // Apply cooldown regardless of target
        damager.setCooldown(Material.MACE, COOLDOWN_TICKS);
        cooldowns.put(playerId, System.currentTimeMillis());
    }
}
