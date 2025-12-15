package saturn.ElementSmpManager.impl.mace;

import saturn.ElementSmpManager.commands.MaceCommand;
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
    private final MaceCommand maceCommand;

    public MaceCooldown(MaceCommand maceCommand) {
        this.maceCommand = maceCommand;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if cooldown is enabled
        if (!maceCommand.isCooldownEnabled()) {
            return;
        }

        // Check if the damager is a player
        if (!(event.getDamager() instanceof Player damager)) return;

        // Check if they're holding a mace
        ItemStack item = damager.getInventory().getItemInMainHand();
        if (item.getType() != Material.MACE) return;

        // If hitting a dragon and dragon is exempt → do NOT apply cooldown
        if (event.getEntity() instanceof EnderDragon && maceCommand.isDragonExempt()) {
            return;
        }

        // Ignore already-cancelled events
        if (event.isCancelled()) return;

        UUID playerId = damager.getUniqueId();

        // Get cooldown time from config (convert seconds to milliseconds)
        long cooldownTime = maceCommand.getCooldownSeconds() * 1000;
        int cooldownTicks = (int) (maceCommand.getCooldownSeconds() * 20);

        // Cooldown check
        if (cooldowns.containsKey(playerId)) {
            long timeLeft = (cooldowns.get(playerId) + cooldownTime) - System.currentTimeMillis();

            if (timeLeft > 0) {
                // Still on cooldown, cancel attack
                event.setCancelled(true);
                return;
            } else {
                cooldowns.remove(playerId);
            }
        }

        // Apply cooldown
        damager.setCooldown(Material.MACE, cooldownTicks);
        cooldowns.put(playerId, System.currentTimeMillis());
    }
}