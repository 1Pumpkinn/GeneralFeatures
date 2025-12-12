package saturn.generalFeatures.impl.mace;

import org.bukkit.Material;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class MaceCooldown implements Listener {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 45000; // 45s in ms
    private static final int COOLDOWN_TICKS = 800;   // 45s * 20t

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if the damager is a player
        if (!(event.getDamager() instanceof Player damager)) return;

        // Check if they're holding a mace
        ItemStack item = damager.getInventory().getItemInMainHand();
        if (item.getType() != Material.MACE) return;

        // If hitting a dragon → do NOT apply cooldown
        if (event.getEntity() instanceof EnderDragon) {
            return;
        }

        // Ignore already-cancelled events
        if (event.isCancelled()) return;

        UUID playerId = damager.getUniqueId();

        // Cooldown check
        if (cooldowns.containsKey(playerId)) {
            long timeLeft = (cooldowns.get(playerId) + COOLDOWN_TIME) - System.currentTimeMillis();

            if (timeLeft > 0) {
                // Still on cooldown, cancel attack
                event.setCancelled(true);
                return;
            } else {
                cooldowns.remove(playerId);
            }
        }

        // Apply cooldown only if the target is NOT a dragon
        damager.setCooldown(Material.MACE, COOLDOWN_TICKS);
        cooldowns.put(playerId, System.currentTimeMillis());
    }
}
