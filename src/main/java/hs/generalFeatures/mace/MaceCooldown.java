package hs.generalFeatures.mace;

import org.bukkit.ChatColor;
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
    private static final long COOLDOWN_TIME = 60000; // 1 minute in milliseconds
    private static final int COOLDOWN_TICKS = 1200; // 60 seconds * 20 ticks

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() != Material.MACE) return;

        UUID playerId = player.getUniqueId();

        if (cooldowns.containsKey(playerId)) {
            long timeLeft = (cooldowns.get(playerId) + COOLDOWN_TIME) - System.currentTimeMillis();

            if (timeLeft > 0) {
                event.setCancelled(true);
                int secondsLeft = (int) (timeLeft / 1000);
                player.sendMessage(ChatColor.RED + "Mace is on cooldown! " + secondsLeft + " seconds remaining.");
                return;
            } else {
                // Cooldown expired, clean up
                cooldowns.remove(playerId);
            }
        }

        // Set cooldown on the item in hotbar
        player.setCooldown(Material.MACE, COOLDOWN_TICKS);

        // Track cooldown time
        cooldowns.put(playerId, System.currentTimeMillis());
        player.sendMessage(ChatColor.YELLOW + "Mace cooldown activated! 60 seconds until next use.");
    }
}