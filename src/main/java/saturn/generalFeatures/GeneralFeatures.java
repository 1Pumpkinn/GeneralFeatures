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