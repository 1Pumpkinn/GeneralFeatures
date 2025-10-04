package hs.generalFeatures;

import hs.generalFeatures.grace.GracePeriod;
import hs.generalFeatures.mace.DisableEnchants;
import hs.generalFeatures.mace.MaceCooldown;
import hs.generalFeatures.restrictions.DisableFireworks;
import hs.generalFeatures.restrictions.DisablePearls;
import hs.generalFeatures.restrictions.DisablePotTwo;
import hs.generalFeatures.restrictions.EnableNDisableCommand;
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

        // Register restriction listeners (default: disabled)
        DisablePotTwo disablePotTwo = new DisablePotTwo();
        DisableFireworks disableFireworks = new DisableFireworks();
        DisablePearls disablePearls = new DisablePearls();

        getServer().getPluginManager().registerEvents(disablePotTwo, this);
        getServer().getPluginManager().registerEvents(disableFireworks, this);
        getServer().getPluginManager().registerEvents(disablePearls, this);

        // Register restrictions command
        EnableNDisableCommand restrictionsCommand = new EnableNDisableCommand(disablePotTwo, disableFireworks, disablePearls);
        getCommand("restrictions").setExecutor(restrictionsCommand);

        getLogger().info("GeneralFeatures plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("GeneralFeatures plugin has been disabled!");
    }
}