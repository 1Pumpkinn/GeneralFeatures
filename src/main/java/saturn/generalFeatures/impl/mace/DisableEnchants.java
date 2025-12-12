package saturn.generalFeatures.impl.mace;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class DisableEnchants implements Listener {

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        if (event.getItem().getType() == Material.MACE) {
            // Check if any of the enchantments being added are not allowed
            for (Enchantment enchant : event.getEnchantsToAdd().keySet()) {
                if (isDisallowedEnchantment(enchant)) {
                    event.setCancelled(true);
                    event.getEnchanter().sendMessage(ChatColor.RED + "You can only enchant a Mace with Unbreaking or Mending!");
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onAnvilUse(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        ItemStack first = event.getInventory().getFirstItem();

        if (first != null && first.getType() == Material.MACE) {
            if (result != null && result.getType() == Material.MACE) {
                // Check all enchantments on the result
                Map<Enchantment, Integer> enchants = result.getEnchantments();

                for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                    Enchantment enchant = entry.getKey();
                    int level = entry.getValue();

                    // Block if enchantment is not allowed
                    if (isDisallowedEnchantment(enchant)) {
                        event.setResult(null);
                        return;
                    }

                    // Block if Unbreaking is above level 3
                    if (enchant.equals(Enchantment.UNBREAKING) && level > 3) {
                        event.setResult(null);
                        return;
                    }
                }
            }
        }
    }

    private boolean isDisallowedEnchantment(Enchantment enchant) {
        return !enchant.equals(Enchantment.UNBREAKING) && !enchant.equals(Enchantment.MENDING);
    }
}