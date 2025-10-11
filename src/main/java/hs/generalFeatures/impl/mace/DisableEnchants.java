package hs.generalFeatures.impl.mace;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

public class DisableEnchants implements Listener {

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        if (event.getItem().getType() == Material.MACE) {
            event.setCancelled(true);
            event.getEnchanter().sendMessage(ChatColor.RED + "You cannot enchant a Mace!");
        }
    }

    @EventHandler
    public void onAnvilUse(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        ItemStack first = event.getInventory().getFirstItem();

        if (first != null && first.getType() == Material.MACE) {
            // Check if enchantments are being added
            if (result != null && result.getType() == Material.MACE) {
                if (!result.getEnchantments().isEmpty()) {
                    event.setResult(null);
                }
            }
        }
    }
}