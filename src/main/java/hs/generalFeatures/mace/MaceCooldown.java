package hs.generalFeatures.mace;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.UUID;

public class MaceCooldown implements Listener {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 60000; // 1 minute in milliseconds
    private static final int COOLDOWN_TICKS = 1200; // 60 seconds * 20 ticks

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;

        ItemStack item = damager.getInventory().getItemInMainHand();

        if (item.getType() != Material.MACE) return;

        UUID playerId = damager.getUniqueId();

        if (cooldowns.containsKey(playerId)) {
            long timeLeft = (cooldowns.get(playerId) + COOLDOWN_TIME) - System.currentTimeMillis();

            if (timeLeft > 0) {
                event.setCancelled(true);
                return;
            } else {
                // Cooldown expired, clean up
                cooldowns.remove(playerId);
            }
        }

        // Check if target is using a shield
        if (event.getEntity() instanceof Player target) {
            PlayerInventory targetInv = target.getInventory();
            
            // Check if target is holding a shield in either hand
            if (isShield(targetInv.getItemInMainHand()) || isShield(targetInv.getItemInOffHand())) {
                // Set cooldown on the item in hotbar
                damager.setCooldown(Material.MACE, COOLDOWN_TICKS);

                // Track cooldown time
                cooldowns.put(playerId, System.currentTimeMillis());
                return;
            }
        }

        // Set cooldown on the item in hotbar
        damager.setCooldown(Material.MACE, COOLDOWN_TICKS);

        // Track cooldown time
        cooldowns.put(playerId, System.currentTimeMillis());
    }
    
    private boolean isShield(ItemStack item) {
        return item != null && item.getType() == Material.SHIELD;
    }
}