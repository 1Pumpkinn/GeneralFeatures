package saturn.generalFeatures;

import saturn.generalFeatures.commands.BroadcastCommand;
import saturn.generalFeatures.commands.DimensionTeleporter;
import saturn.generalFeatures.impl.InvisibilityNameHider;
import saturn.generalFeatures.impl.grace.GracePeriod;
import saturn.generalFeatures.impl.mace.DisableEnchants;
import saturn.generalFeatures.impl.mace.MaceCooldown;
import saturn.generalFeatures.impl.restrictions.ItemRestrictions;
import saturn.generalFeatures.impl.restrictions.dimension.DisableNether;
import saturn.generalFeatures.impl.restrictions.dimension.EndControl;
import saturn.generalFeatures.listeners.DisableMaceDamage;
import saturn.generalFeatures.listeners.PlayerDeathListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class GeneralFeatures extends JavaPlugin {

    private EndControl endControl;
    private DisableNether netherControl;
    private GracePeriod gracePeriod;

    @Override
    public void onEnable() {
        // Initialize dimension controls
        netherControl = new DisableNether(this);
        getServer().getPluginManager().registerEvents(netherControl, this);

        endControl = new EndControl(this);
        getServer().getPluginManager().registerEvents(endControl, this);

        // Register grace period command and listener
        gracePeriod = new GracePeriod(this, netherControl);
        if (getCommand("grace") != null) {
            getCommand("grace").setExecutor(gracePeriod);
            getCommand("grace").setTabCompleter(gracePeriod);
        }

        getServer().getPluginManager().registerEvents(gracePeriod, this);

        // Register nether command
        if (getCommand("nether") != null) {
            getCommand("nether").setExecutor(netherControl);
            getCommand("nether").setTabCompleter(netherControl);
        }

        // Register end command
        if (getCommand("end") != null) {
            getCommand("end").setExecutor(endControl);
            getCommand("end").setTabCompleter(endControl);
        }

        // Register other features
        getServer().getPluginManager().registerEvents(new InvisibilityNameHider(), this);
        getServer().getPluginManager().registerEvents(new MaceCooldown(), this);
        getServer().getPluginManager().registerEvents(new DisableEnchants(), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);
        getServer().getPluginManager().registerEvents(new DisableMaceDamage(), this);

        // Register teleport end command
        if (getCommand("teleportend") != null) {
            getCommand("teleportend").setExecutor(new DimensionTeleporter());
        }

        // Register broadcast command
        if (getCommand("broadcast") != null) {
            getCommand("broadcast").setExecutor(new BroadcastCommand());
        }

        // Register item restrictions command and listener
        ItemRestrictions itemRestrictions = new ItemRestrictions(this);
        if (getCommand("restrictions") != null) {
            getCommand("restrictions").setExecutor(itemRestrictions);
            getCommand("restrictions").setTabCompleter(itemRestrictions);
        }
        getServer().getPluginManager().registerEvents(itemRestrictions, this);

        getLogger().info("GeneralFeatures plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save states
        if (endControl != null) {
            endControl.onDisable();
        }
        if (netherControl != null) {
            netherControl.onDisable();
        }
        if (gracePeriod != null) {
            gracePeriod.onDisable();
        }

        getLogger().info("GeneralFeatures plugin has been disabled!");
    }
}