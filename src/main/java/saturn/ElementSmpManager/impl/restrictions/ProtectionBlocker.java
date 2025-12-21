package saturn.ElementSmpManager.impl.restrictions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class ProtectionBlocker implements Listener {

    private static final int MAX_PROTECTION_LEVEL = 3;

    /**
     * Automatically downgrade Protection IV to Protection III at enchantment table
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnchantItem(EnchantItemEvent event) {
        Map<Enchantment, Integer> enchants = event.getEnchantsToAdd();

        if (enchants.containsKey(Enchantment.PROTECTION) &&
                enchants.get(Enchantment.PROTECTION) > MAX_PROTECTION_LEVEL) {
            // Downgrade to Protection III
            enchants.put(Enchantment.PROTECTION, MAX_PROTECTION_LEVEL);

            event.getEnchanter().sendMessage(
                    Component.text("⚠ Protection IV downgraded to Protection III").color(NamedTextColor.YELLOW)
            );
        }
    }

    /**
     * Automatically downgrade Protection IV to Protection III in anvil results
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnvilCombine(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();

        if (result == null || !result.hasItemMeta()) {
            return;
        }

        ItemMeta meta = result.getItemMeta();

        // Check if result has Protection IV
        if (meta.hasEnchant(Enchantment.PROTECTION) &&
                meta.getEnchantLevel(Enchantment.PROTECTION) > MAX_PROTECTION_LEVEL) {

            // Downgrade to Protection III
            meta.removeEnchant(Enchantment.PROTECTION);
            meta.addEnchant(Enchantment.PROTECTION, MAX_PROTECTION_LEVEL, true);
            result.setItemMeta(meta);
            event.setResult(result);

            // Notify player if they're viewing the anvil
            if (event.getView().getPlayer() instanceof Player player) {
                player.sendMessage(
                        Component.text("⚠ Protection IV downgraded to Protection III").color(NamedTextColor.YELLOW)
                );
            }
        }
    }

    /**
     * Downgrade Protection IV to Protection III when trying to wear armor
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // Check the item being moved into armor slot
        if (item != null && isArmorSlot(event.getSlot(), event.getSlotType())) {
            downgradeProtectionIfNeeded(item, player);
        }

        // Check cursor item being placed in armor slot
        if (cursor != null && isArmorSlot(event.getSlot(), event.getSlotType())) {
            downgradeProtectionIfNeeded(cursor, player);
        }
    }

    /**
     * Downgrade Protection IV when equipping armor via right-click
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !isArmor(item.getType())) {
            return;
        }

        // Check if this interaction would equip the armor
        if (event.getAction().toString().contains("RIGHT_CLICK")) {
            downgradeProtectionIfNeeded(item, player);
        }
    }

    /**
     * Downgrade Protection IV when placing on armor stands
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        ItemStack item = event.getPlayerItem();

        if (item != null) {
            downgradeProtectionIfNeeded(item, event.getPlayer());
        }
    }

    /**
     * Helper method to downgrade Protection IV to Protection III
     */
    private void downgradeProtectionIfNeeded(ItemStack item, Player player) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (!isArmor(item.getType())) {
            return;
        }

        // Check if item has Protection IV
        if (meta.hasEnchant(Enchantment.PROTECTION) &&
                meta.getEnchantLevel(Enchantment.PROTECTION) > MAX_PROTECTION_LEVEL) {

            // Downgrade to Protection III
            meta.removeEnchant(Enchantment.PROTECTION);
            meta.addEnchant(Enchantment.PROTECTION, MAX_PROTECTION_LEVEL, true);
            item.setItemMeta(meta);
        }
    }

    /**
     * Check if a material is armor
     */
    private boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") ||
                name.endsWith("_CHESTPLATE") ||
                name.endsWith("_LEGGINGS") ||
                name.endsWith("_BOOTS");
    }

    /**
     * Check if a slot is an armor slot
     */
    private boolean isArmorSlot(int slot, org.bukkit.event.inventory.InventoryType.SlotType slotType) {
        // Armor slots are 36-39 in player inventory (boots, leggings, chestplate, helmet)
        return (slot >= 36 && slot <= 39) ||
                slotType == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR;
    }
}