package saturn.ElementSmpManager.impl.restrictions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;

public class NetheriteArmorBlocker implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSmithingPrepare(PrepareSmithingEvent event) {
        SmithingInventory inventory = event.getInventory();
        ItemStack result = event.getResult();

        // Check if there's a result
        if (result == null || result.getType() == Material.AIR) {
            return;
        }

        // Check if the result is netherite armor
        if (isNetheriteArmor(result.getType())) {
            // Cancel the smithing by setting result to null
            event.setResult(null);

            // Notify the player
            if (event.getView().getPlayer() != null) {
                event.getView().getPlayer().sendMessage(
                        Component.text("✖ Netherite armor upgrades are disabled!").color(NamedTextColor.RED)
                );
            }
        }
        // Allow netherite tools to be upgraded (do nothing, let it proceed)
    }

    /**
     * Check if a material is netherite armor
     */
    private boolean isNetheriteArmor(Material material) {
        return material == Material.NETHERITE_HELMET ||
                material == Material.NETHERITE_CHESTPLATE ||
                material == Material.NETHERITE_LEGGINGS ||
                material == Material.NETHERITE_BOOTS;
    }
}