package hs.generalFeatures;

import hs.generalFeatures.commands.BroadcastCommand;
import hs.generalFeatures.commands.DimensionTeleporter;
import hs.generalFeatures.impl.InvisibilityNameHider;
import hs.generalFeatures.impl.grace.GracePeriod;
import hs.generalFeatures.impl.mace.DisableEnchants;
import hs.generalFeatures.impl.mace.MaceCooldown;
import hs.generalFeatures.impl.restrictions.ItemRestrictions;
import hs.generalFeatures.impl.restrictions.dimension.DisableNether;
import hs.generalFeatures.impl.restrictions.dimension.EndControl;
import hs.generalFeatures.listeners.DisableMaceDamage;
import hs.generalFeatures.managers.listeners.PlayerDeathListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class GeneralFeatures extends JavaPlugin {

    private EndControl endControl;
    private DisableNether netherControl;

    @Override
    public void onEnable() {
        // Initialize nether control
        netherControl = new DisableNether();
        getServer().getPluginManager().registerEvents(netherControl, this);

        // Register grace period command and listener (with nether control)
        GracePeriod gracePeriod = new GracePeriod(this, netherControl);
        if (getCommand("grace") != null) {
            getCommand("grace").setExecutor(gracePeriod);
        }
        getServer().getPluginManager().registerEvents(gracePeriod, this);

        getServer().getPluginManager().registerEvents(new InvisibilityNameHider(), this);
        getServer().getPluginManager().registerEvents(new MaceCooldown(), this);
        getServer().getPluginManager().registerEvents(new DisableEnchants(), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);

        // Register teleport end command
        if (getCommand("teleportend") != null) {
            getCommand("teleportend").setExecutor(new DimensionTeleporter());
        }

        if (getCommand("broadcast") != null) {
            getCommand("broadcast").setExecutor(new BroadcastCommand());
        }

        // Register item restrictions command and listener
        ItemRestrictions itemRestrictions = new ItemRestrictions(this);
        if (getCommand("restrictions") != null) {
            getCommand("restrictions").setExecutor(itemRestrictions);
            getCommand("restrictions").setTabCompleter(itemRestrictions); // Add this line
        }
        getServer().getPluginManager().registerEvents(itemRestrictions, this);

        // Register end control command and listener
        endControl = new EndControl(this);
        if (getCommand("end") != null) {
            getCommand("end").setExecutor(endControl);
            getServer().getPluginManager().registerEvents(new DisableMaceDamage(), this);

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