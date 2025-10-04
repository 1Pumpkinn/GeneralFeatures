package hs.generalFeatures.restrictions;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
        if (!enabled) return; // If restriction is disabled, allow everything

        ItemStack item = event.getItem();
        if (item.getType() != Material.POTION && item.getType() != Material.SPLASH_POTION &&
                item.getType() != Material.LINGERING_POTION) {
            return;
        }

        if (!(item.getItemMeta() instanceof PotionMeta)) return;

        PotionMeta meta = (PotionMeta) item.getItemMeta();

        // Check custom potion effects
        for (PotionEffect effect : meta.getCustomEffects()) {
            if (effect.getAmplifier() >= 1) { // Amplifier 1 = Level 2
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "Level 2+ potions are disabled!");
                return;
            }
        }

        // Check base potion data
        if (meta.getBasePotionType() != null) {
            // For 1.20.5+ use getBasePotionType()
            // Potions with amplifier 1+ (level 2+) have upgraded versions
            String potionName = meta.getBasePotionType().toString().toLowerCase();
            if (potionName.contains("strong") || potionName.contains("upgraded")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "Level 2+ potions are disabled!");
            }
        }
    }
}