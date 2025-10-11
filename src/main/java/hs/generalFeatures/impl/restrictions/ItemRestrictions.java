package hs.generalFeatures.impl.restrictions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class ItemRestrictions implements CommandExecutor, Listener {
    
    private final JavaPlugin plugin;
    private File configFile;
    private FileConfiguration config;
    
    // Restriction settings - default to enabled (true)
    private boolean enderPearlsRestricted = true;
    private boolean potionsRestricted = true;
    private boolean fireworksRestricted = true;
    
    public ItemRestrictions(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "restrictions.yml");
        
        if (!configFile.exists()) {
            if (!plugin.getDataFolder().mkdirs() && !plugin.getDataFolder().exists()) {
                plugin.getLogger().warning("Failed to create plugin directory!");
            }
            try {
                if (!configFile.createNewFile()) {
                    plugin.getLogger().warning("Failed to create restrictions.yml file!");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create restrictions.yml file!");
                return;
            }
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Load settings with defaults (enabled by default)
        enderPearlsRestricted = config.getBoolean("restrictions.ender-pearls", true);
        potionsRestricted = config.getBoolean("restrictions.potions", true);
        fireworksRestricted = config.getBoolean("restrictions.fireworks", true);
    }
    
    private void saveConfig() {
        config.set("restrictions.ender-pearls", enderPearlsRestricted);
        config.set("restrictions.potions", potionsRestricted);
        config.set("restrictions.fireworks", fireworksRestricted);
        
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save restrictions.yml file!");
        }
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("generalfeatures.restrictions")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /restrictions <enable|disable> <potions|fireworks|pearls|all>").color(NamedTextColor.RED));
            return true;
        }
        
        String action = args[0].toLowerCase();
        String item = args[1].toLowerCase();
        
        if (!action.equals("enable") && !action.equals("disable")) {
            sender.sendMessage(Component.text("Usage: /restrictions <enable|disable> <potions|fireworks|pearls|all>").color(NamedTextColor.RED));
            return true;
        }
        
        boolean enable = action.equals("enable");
        
        switch (item) {
            case "pearls":
                enderPearlsRestricted = enable;
                sender.sendMessage(Component.text("Ender pearl restrictions " + (enable ? "enabled" : "disabled") + "!").color(NamedTextColor.GREEN));
                break;
            case "potions":
                potionsRestricted = enable;
                sender.sendMessage(Component.text("Potion restrictions " + (enable ? "enabled" : "disabled") + "!").color(NamedTextColor.GREEN));
                break;
            case "fireworks":
                fireworksRestricted = enable;
                sender.sendMessage(Component.text("Firework restrictions " + (enable ? "enabled" : "disabled") + "!").color(NamedTextColor.GREEN));
                break;
            case "all":
                enderPearlsRestricted = enable;
                potionsRestricted = enable;
                fireworksRestricted = enable;
                sender.sendMessage(Component.text("All restrictions " + (enable ? "enabled" : "disabled") + "!").color(NamedTextColor.GREEN));
                break;
            default:
                sender.sendMessage(Component.text("Usage: /restrictions <enable|disable> <potions|fireworks|pearls|all>").color(NamedTextColor.RED));
                return true;
        }
        
        saveConfig();
        return true;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null) return;
        
        // Check for ender pearl usage
        if (enderPearlsRestricted && item.getType() == Material.ENDER_PEARL) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Ender pearls are restricted!").color(NamedTextColor.RED));
            return;
        }
        
        // Check for firework usage
        if (fireworksRestricted && item.getType() == Material.FIREWORK_ROCKET) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Fireworks are restricted!").color(NamedTextColor.RED));
            return;
        }
        
        // Check for potion usage (splash and lingering potions)
        if (potionsRestricted && (item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION)) {
            if (isPlusTwo(item)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("Strength 2, Speed 2, and debuff potions are restricted!").color(NamedTextColor.RED));
            }
        }
    }
    
    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (!potionsRestricted) return;
        
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        
        if (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION) {
            if (isPlusTwo(item)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("Strength 2, Speed 2, and debuff potions are restricted!").color(NamedTextColor.RED));
            }
        }
    }
    

    
    @EventHandler
    public void onBlockDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        ItemStack item = event.getItem();
        
        // Check if it's a dispenser or dropper
        if (!(block.getState() instanceof Dispenser) && !(block.getState() instanceof Dropper)) {
            return;
        }
        
        // Check for ender pearl dispensing
        if (enderPearlsRestricted && item.getType() == Material.ENDER_PEARL) {
            event.setCancelled(true);
            return;
        }
        
        // Check for firework dispensing
        if (fireworksRestricted && item.getType() == Material.FIREWORK_ROCKET) {
            event.setCancelled(true);
            return;
        }
        
        // Check for potion dispensing
        if (potionsRestricted && (item.getType() == Material.POTION || 
                                 item.getType() == Material.SPLASH_POTION || 
                                 item.getType() == Material.LINGERING_POTION)) {
            if (isPlusTwo(item)) {
                event.setCancelled(true);
            }
        }
    }
    
    private boolean isPlusTwo(ItemStack item) {
        if (!(item.getItemMeta() instanceof PotionMeta potionMeta)) {
            return false;
        }
        
        // Check base potion type for specific banned potions
        if (potionMeta.getBasePotionType() != null) {
            String potionName = potionMeta.getBasePotionType().name();
            
            // Ban strength 2 and speed 2 (strong variants)
            if (potionName.contains("STRONG") && 
                (potionName.contains("STRENGTH") || potionName.contains("SPEED"))) {
                return true;
            }
            
            // Ban turtle master (all levels)
            if (potionName.contains("TURTLE_MASTER")) {
                return true;
            }
            
            // Ban debuff potions (all levels)
            if (potionName.contains("POISON") || 
                potionName.contains("WEAKNESS") || 
                potionName.contains("SLOWNESS") || 
                potionName.contains("HARMING") || 
                potionName.contains("DECAY") ||
                potionName.contains("UNLUCK")) {
                return true;
            }
        }
        
        // Check custom effects for specific banned effects at level 2+
        if (potionMeta.hasCustomEffects()) {
            for (PotionEffect effect : potionMeta.getCustomEffects()) {
                String effectType = effect.getType().getKey().getKey().toUpperCase();
                
                // Ban strength 2 and speed 2 (amplifier >= 1 means level 2+)
                if (effect.getAmplifier() >= 1 && 
                    (effectType.equals("INCREASE_DAMAGE") || effectType.equals("SPEED"))) {
                    return true;
                }
                
                
                // Ban debuff effects (all levels)
                if (effectType.equals("POISON") || 
                    effectType.equals("WEAKNESS") || 
                    effectType.equals("SLOW") || 
                    effectType.equals("HARM") || 
                    effectType.equals("UNLUCK")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
}
