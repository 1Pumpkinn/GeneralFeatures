package hs.generalFeatures.listeners;

import org.bukkit.Material;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class DisableMaceDamage implements Listener {

    @EventHandler
    public void onMaceHitEnderDragon(EntityDamageByEntityEvent event) {
        // Check if entity being hit is an ender dragon
        if (!(event.getEntity() instanceof EnderDragon)) return;

        // Check if damager is a player
        if (!(event.getDamager() instanceof Player player)) return;

        // Get item in player's hand
        ItemStack item = player.getInventory().getItemInMainHand();

        // Check if it's a mace
        if (item.getType() == Material.MACE) {
            event.setCancelled(true);
            event.setDamage(0);
        }
    }
}
