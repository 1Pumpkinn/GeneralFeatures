package hs.generalFeatures.restrictions;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.ItemStack;

public class DisableFireworks implements Listener {

    private boolean enabled = false;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @EventHandler
    public void onDispense(BlockDispenseEvent event) {
        if (!enabled) return; // If restriction is disabled, allow everything

        Block block = event.getBlock();
        ItemStack item = event.getItem();

        // Check if it's a dispenser or dropper dispensing fireworks
        if ((block.getState() instanceof Dispenser || block.getState() instanceof Dropper)
                && item.getType() == Material.FIREWORK_ROCKET) {
            event.setCancelled(true);
        }
    }
}