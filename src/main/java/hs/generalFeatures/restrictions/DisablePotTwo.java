package hs.generalFeatures.restrictions;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

public class DisablePotTwo implements Listener {

    private boolean enabled = false;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @EventHandler
    public void onPotionConsume(PlayerItemConsumeEvent event) {
        if (!enabled) return;

        ItemStack item = event.getItem();
        if (item.getType() != Material.POTION) {
            return;
        }

        if (hasLevel2Potion(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Level 2+ potions are disabled!");
        }
    }

    @EventHandler
    public void onPotionThrow(ProjectileLaunchEvent event) {
        if (!enabled) return;

        if (!(event.getEntity() instanceof ThrownPotion thrownPotion)) return;
        if (!(thrownPotion.getShooter() instanceof Player player)) return;

        ItemStack item = thrownPotion.getItem();
        if (item.getType() != Material.SPLASH_POTION && item.getType() != Material.LINGERING_POTION) {
            return;
        }

        if (hasLevel2Potion(item)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Level 2+ potions are disabled!");
        }
    }

    private boolean hasLevel2Potion(ItemStack item) {
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return false;

        // Check custom potion effects
        for (PotionEffect effect : meta.getCustomEffects()) {
            if (effect.getAmplifier() >= 1) { // Amplifier 1 = Level 2
                return true;
            }
        }

        // Check base potion effects
        if (meta.hasBasePotionType() && meta.getBasePotionType() != null) {
            for (PotionEffect effect : meta.getBasePotionType().getPotionEffects()) {
                if (effect.getAmplifier() >= 1) { // Amplifier 1 = Level 2
                    return true;
                }
            }
        }

        return false;
    }
}