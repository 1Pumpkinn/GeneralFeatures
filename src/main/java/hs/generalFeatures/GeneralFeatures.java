package hs.generalFeatures;

import hs.generalFeatures.end.EndControl;
import hs.generalFeatures.grace.GracePeriod;
import hs.generalFeatures.mace.DisableEnchants;
import hs.generalFeatures.mace.MaceCooldown;
import hs.generalFeatures.restrictions.ItemRestrictions;
import org.bukkit.plugin.java.JavaPlugin;

public final class GeneralFeatures extends JavaPlugin {

    private EndControl endControl;

    @Override
    public void onEnable() {
        // Register grace period command and listener
        GracePeriod gracePeriod = new GracePeriod();
        getCommand("grace").setExecutor(gracePeriod);
        getServer().getPluginManager().registerEvents(gracePeriod, this);

        // Register mace cooldown listener
        getServer().getPluginManager().registerEvents(new MaceCooldown(), this);

        // Register disable mace enchants listener
        getServer().getPluginManager().registerEvents(new DisableEnchants(), this);

        // Register item restrictions command and listener
        ItemRestrictions itemRestrictions = new ItemRestrictions(this);
        getCommand("restrictions").setExecutor(itemRestrictions);
        getServer().getPluginManager().registerEvents(itemRestrictions, this);
        
        // Register end control command and listener
        endControl = new EndControl(this);
        getCommand("end").setExecutor(endControl);
        getServer().getPluginManager().registerEvents(endControl, this);

        getLogger().info("GeneralFeatures plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save end control state
        if (endControl != null) {
            endControl.saveConfig();
        }
        
        getLogger().info("GeneralFeatures plugin has been disabled!");
    }
}