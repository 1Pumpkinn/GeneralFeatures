package hs.generalFeatures;

import hs.generalFeatures.grace.GracePeriod;
import hs.generalFeatures.mace.DisableEnchants;
import hs.generalFeatures.mace.MaceCooldown;
import org.bukkit.plugin.java.JavaPlugin;

public final class GeneralFeatures extends JavaPlugin {

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

        getLogger().info("GeneralFeatures plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("GeneralFeatures plugin has been disabled!");
    }
}