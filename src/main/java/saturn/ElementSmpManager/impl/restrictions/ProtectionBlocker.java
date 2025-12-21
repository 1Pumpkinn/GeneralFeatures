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
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class ProtectionBlocker implements Listener {

    private static final int MAX_PROTECTION_LEVEL = 3;

    /**
     * Prevents enchanting items with Protection IV at enchantment table
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnchantItem(EnchantItemEvent event) {
        Map<Enchantment, Integer> enchants = event.getEnchantsToAdd();

        if (enchants.containsKey(Enchantment.PROTECTION) &&
                enchants.get(Enchantment.PROTECTION) > MAX_PROTECTION_LEVEL) {
            event.setCancelled(true);
            event.getEnchanter().sendMessage(
                    Component.text("✖ Protection IV is not allowed!").color(NamedTextColor.RED)
            );
            event.getEnchanter().sendMessage(
                    Component.text("Maximum allowed: Protection III").color(NamedTextColor.GRAY)
            );
        }
    }

    /**
     * Prevents combining items with Protection IV in anvil
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
            event.setResult(null);

            // Notify player if they're viewing the anvil
            if (event.getView().getPlayer() instanceof Player player) {
                player.sendMessage(
                        Component.text("✖ Cannot create items with Protection IV!").color(NamedTextColor.RED)
                );
                player.sendMessage(
                        Component.text("Maximum allowed: Protection III").color(NamedTextColor.GRAY)
                );
            }
        }
    }

    /**
     * Prevents wearing armor with Protection IV
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // Check the item being moved
        if (isArmorWithProtectionIV(item) && isArmorSlot(event.getSlot(), event.getSlotType())) {
            event.setCancelled(true);
            player.sendMessage(
                    Component.text("✖ You cannot wear armor with Protection IV!").color(NamedTextColor.RED)
            );
            return;
        }

        // Check cursor item being placed in armor slot
        if (isArmorWithProtectionIV(cursor) && isArmorSlot(event.getSlot(), event.getSlotType())) {
            event.setCancelled(true);
            player.sendMessage(
                    Component.text("✖ You cannot wear armor with Protection IV!").color(NamedTextColor.RED)
            );
        }
    }

    /**
     * Prevents equipping armor with Protection IV via right-click
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) {
            return;
        }

        // Check if player is trying to equip armor with Protection IV
        if (isArmorWithProtectionIV(item) && isArmor(item.getType())) {
            // Check if this interaction would equip the armor
            if (event.getAction().toString().contains("RIGHT_CLICK")) {
                event.setCancelled(true);
                player.sendMessage(
                        Component.text("✖ You cannot wear armor with Protection IV!").color(NamedTextColor.RED)
                );
            }
        }
    }

    /**
     * Prevents placing armor with Protection IV on armor stands
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        ItemStack item = event.getPlayerItem();

        if (isArmorWithProtectionIV(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(
                    Component.text("✖ Cannot place armor with Protection IV on armor stands!").color(NamedTextColor.RED)
            );
        }
    }

    /**
     * Check if an item is armor with Protection IV
     */
    private boolean isArmorWithProtectionIV(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        return isArmor(item.getType()) &&
                meta.hasEnchant(Enchantment.PROTECTION) &&
                meta.getEnchantLevel(Enchantment.PROTECTION) > MAX_PROTECTION_LEVEL;
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