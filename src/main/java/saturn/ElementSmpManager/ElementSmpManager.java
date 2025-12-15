package saturn.ElementSmpManager;

import saturn.ElementSmpManager.commands.MaceCommand;
import saturn.ElementSmpManager.impl.InvisibilityNameHider;
import saturn.ElementSmpManager.impl.grace.GracePeriod;
import saturn.ElementSmpManager.impl.mace.DisableEnchants;
import saturn.ElementSmpManager.impl.mace.MaceCooldown;
import saturn.ElementSmpManager.impl.restrictions.ItemRestrictions;
import saturn.ElementSmpManager.impl.restrictions.dimension.DisableNether;
import saturn.ElementSmpManager.impl.restrictions.dimension.EndControl;
import saturn.ElementSmpManager.listeners.DisableMaceDamage;
import saturn.ElementSmpManager.listeners.PlayerDeathListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ElementSmpManager extends JavaPlugin {

    private EndControl endControl;
    private DisableNether netherControl;
    private GracePeriod gracePeriod;
    private MaceCommand maceCommand;
    private MaceCooldown maceCooldown;
    private DisableEnchants disableEnchants;
    private DisableMaceDamage disableMaceDamage;

    @Override
    public void onEnable() {
        // Initialize mace command first (so settings are loaded)
        maceCommand = new MaceCommand(this);
        if (getCommand("mace") != null) {
            getCommand("mace").setExecutor(maceCommand);
            getCommand("mace").setTabCompleter(maceCommand);
        }

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

        // Register mace features with config reference
        maceCooldown = new MaceCooldown(maceCommand);
        getServer().getPluginManager().registerEvents(maceCooldown, this);

        disableEnchants = new DisableEnchants(maceCommand);
        getServer().getPluginManager().registerEvents(disableEnchants, this);

        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);

        disableMaceDamage = new DisableMaceDamage(maceCommand);
        getServer().getPluginManager().registerEvents(disableMaceDamage, this);

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