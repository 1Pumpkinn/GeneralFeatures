package hs.generalFeatures;

import hs.generalFeatures.commands.DimensionTeleporter;
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
        GracePeriod gracePeriod = new GracePeriod(this);
        if (getCommand("grace") != null) {
            getCommand("grace").setExecutor(gracePeriod);
        }
        getServer().getPluginManager().registerEvents(gracePeriod, this);
        getServer().getPluginManager().registerEvents(new InvisibilityNameHider(), this);
        getServer().getPluginManager().registerEvents(new MaceCooldown(), this);
        getServer().getPluginManager().registerEvents(new DisableEnchants(), this);

        // Register teleport end command
        if (getCommand("teleportend") != null) {
            getCommand("teleportend").setExecutor(new DimensionTeleporter());
        }

        // Register item restrictions command and listener
        ItemRestrictions itemRestrictions = new ItemRestrictions(this);
        if (getCommand("restrictions") != null) {
            getCommand("restrictions").setExecutor(itemRestrictions);
        }
        getServer().getPluginManager().registerEvents(itemRestrictions, this);

        // Register end control command and listener
        endControl = new EndControl(this);
        if (getCommand("end") != null) {
            getCommand("end").setExecutor(endControl);
        }
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