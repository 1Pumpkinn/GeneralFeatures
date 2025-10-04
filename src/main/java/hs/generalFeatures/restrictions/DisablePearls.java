package hs.generalFeatures.restrictions;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.ItemStack;

public class DisablePearls implements Listener {

    private boolean enabled = false;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @EventHandler
    public void onDispense(BlockDispenseEvent event) {
        if (!enabled) return;

        Block block = event.getBlock();
        ItemStack item = event.getItem();

        // Check if it's a dispenser or dropper dispensing ender pearls
        if ((block.getState() instanceof Dispenser || block.getState() instanceof Dropper)
                && item.getType() == Material.ENDER_PEARL) {
            event.setCancelled(true);
        }
    }
}