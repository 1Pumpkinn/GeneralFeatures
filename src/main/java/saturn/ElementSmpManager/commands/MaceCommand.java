package saturn.ElementSmpManager.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MaceCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private File configFile;
    private FileConfiguration config;

    // Default values
    private long cooldownSeconds = 45;
    private boolean dragonExempt = true;
    private boolean cooldownEnabled = true;
    private boolean enchantRestrictionsEnabled = true;
    private boolean dragonDamageDisabled = true;
    private boolean maceDisabled = false; // NEW: complete mace disable

    public MaceCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "mace_config.yml");

        if (!configFile.exists()) {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create mace_config.yml file!");
                return;
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        cooldownSeconds = config.getLong("mace.cooldown-seconds", 45);
        dragonExempt = config.getBoolean("mace.dragon-exempt", true);
        cooldownEnabled = config.getBoolean("mace.cooldown-enabled", true);
        enchantRestrictionsEnabled = config.getBoolean("mace.enchant-restrictions-enabled", true);
        dragonDamageDisabled = config.getBoolean("mace.dragon-damage-disabled", true);
        maceDisabled = config.getBoolean("mace.disabled", false); // NEW

        saveConfig();
    }

    private void saveConfig() {
        config.set("mace.cooldown-seconds", cooldownSeconds);
        config.set("mace.dragon-exempt", dragonExempt);
        config.set("mace.cooldown-enabled", cooldownEnabled);
        config.set("mace.enchant-restrictions-enabled", enchantRestrictionsEnabled);
        config.set("mace.dragon-damage-disabled", dragonDamageDisabled);
        config.set("mace.disabled", maceDisabled); // NEW

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save mace_config.yml file!");
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("generalfeatures.mace")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "cooldown" -> handleCooldown(sender, args);
            case "dragon" -> handleDragon(sender, args);
            case "enchants" -> handleEnchants(sender, args);
            case "disable", "off" -> handleDisable(sender); // NEW
            case "enable", "on" -> handleEnable(sender); // NEW
            case "info", "status" -> handleInfo(sender);
            case "reload" -> handleReload(sender);
            case "reset" -> handleReset(sender);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    // NEW: Handle complete mace disable
    private boolean handleDisable(CommandSender sender) {
        maceDisabled = true;
        saveConfig();
        sender.sendMessage(Component.text("✓ Mace has been completely disabled").color(NamedTextColor.GREEN));
        sender.sendMessage(Component.text("  Players cannot use maces at all").color(NamedTextColor.GRAY));
        return true;
    }

    // NEW: Handle mace enable
    private boolean handleEnable(CommandSender sender) {
        maceDisabled = false;
        saveConfig();
        sender.sendMessage(Component.text("✓ Mace has been enabled").color(NamedTextColor.GREEN));
        sender.sendMessage(Component.text("  Players can now use maces").color(NamedTextColor.GRAY));
        return true;
    }

    private boolean handleCooldown(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /mace cooldown <enable|disable|set> [seconds]").color(NamedTextColor.RED));
            return true;
        }

        String action = args[1].toLowerCase();

        return switch (action) {
            case "enable", "on" -> {
                cooldownEnabled = true;
                saveConfig();
                sender.sendMessage(Component.text("✓ Mace cooldown enabled (" + cooldownSeconds + "s)").color(NamedTextColor.GREEN));
                yield true;
            }
            case "disable", "off" -> {
                cooldownEnabled = false;
                saveConfig();
                sender.sendMessage(Component.text("✓ Mace cooldown disabled").color(NamedTextColor.GREEN));
                yield true;
            }
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /mace cooldown set <seconds>").color(NamedTextColor.RED));
                    yield true;
                }

                try {
                    long seconds = Long.parseLong(args[2]);
                    if (seconds < 0) {
                        sender.sendMessage(Component.text("Cooldown must be 0 or higher!").color(NamedTextColor.RED));
                        yield true;
                    }

                    cooldownSeconds = seconds;
                    saveConfig();
                    sender.sendMessage(Component.text("✓ Mace cooldown set to " + seconds + " seconds").color(NamedTextColor.GREEN));
                    yield true;
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid number: " + args[2]).color(NamedTextColor.RED));
                    yield true;
                }
            }
            default -> {
                sender.sendMessage(Component.text("Unknown action: " + action).color(NamedTextColor.RED));
                sender.sendMessage(Component.text("Use: enable, disable, or set").color(NamedTextColor.YELLOW));
                yield true;
            }
        };
    }

    private boolean handleDragon(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /mace dragon <exempt|damage> <enable|disable>").color(NamedTextColor.RED));
            return true;
        }

        String action = args[1].toLowerCase();

        if (action.equals("exempt")) {
            if (args.length < 3) {
                sender.sendMessage(Component.text("Usage: /mace dragon exempt <enable|disable>").color(NamedTextColor.RED));
                return true;
            }

            String state = args[2].toLowerCase();
            if (state.equals("enable") || state.equals("on")) {
                dragonExempt = true;
                saveConfig();
                sender.sendMessage(Component.text("✓ Ender Dragon is now exempt from mace cooldown").color(NamedTextColor.GREEN));
                return true;
            } else if (state.equals("disable") || state.equals("off")) {
                dragonExempt = false;
                saveConfig();
                sender.sendMessage(Component.text("✓ Ender Dragon is no longer exempt from mace cooldown").color(NamedTextColor.GREEN));
                return true;
            }
        } else if (action.equals("damage")) {
            if (args.length < 3) {
                sender.sendMessage(Component.text("Usage: /mace dragon damage <enable|disable>").color(NamedTextColor.RED));
                return true;
            }

            String state = args[2].toLowerCase();
            if (state.equals("enable") || state.equals("on")) {
                dragonDamageDisabled = false;
                saveConfig();
                sender.sendMessage(Component.text("✓ Mace can now damage the Ender Dragon").color(NamedTextColor.GREEN));
                return true;
            } else if (state.equals("disable") || state.equals("off")) {
                dragonDamageDisabled = true;
                saveConfig();
                sender.sendMessage(Component.text("✓ Mace damage to Ender Dragon is now disabled").color(NamedTextColor.GREEN));
                return true;
            }
        }

        sender.sendMessage(Component.text("Unknown action: " + action).color(NamedTextColor.RED));
        sender.sendMessage(Component.text("Use: exempt or damage").color(NamedTextColor.YELLOW));
        return true;
    }

    private boolean handleEnchants(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /mace enchants <enable|disable>").color(NamedTextColor.RED));
            return true;
        }

        String action = args[1].toLowerCase();

        return switch (action) {
            case "enable", "on" -> {
                enchantRestrictionsEnabled = true;
                saveConfig();
                sender.sendMessage(Component.text("✓ Mace enchantment restrictions enabled").color(NamedTextColor.GREEN));
                sender.sendMessage(Component.text("Only Unbreaking and Mending allowed").color(NamedTextColor.GRAY));
                yield true;
            }
            case "disable", "off" -> {
                enchantRestrictionsEnabled = false;
                saveConfig();
                sender.sendMessage(Component.text("✓ Mace enchantment restrictions disabled").color(NamedTextColor.GREEN));
                sender.sendMessage(Component.text("All enchantments now allowed").color(NamedTextColor.GRAY));
                yield true;
            }
            default -> {
                sender.sendMessage(Component.text("Unknown action: " + action).color(NamedTextColor.RED));
                sender.sendMessage(Component.text("Use: enable or disable").color(NamedTextColor.YELLOW));
                yield true;
            }
        };
    }

    private boolean handleInfo(CommandSender sender) {
        sender.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("⚔ Mace Configuration").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.empty());

        // NEW: Show mace disabled status
        sender.sendMessage(Component.text("Mace Status:").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("  Completely Disabled: ").color(NamedTextColor.GRAY)
                .append(Component.text(maceDisabled ? "✓ Yes" : "✗ No")
                        .color(maceDisabled ? NamedTextColor.RED : NamedTextColor.GREEN)));

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Cooldown Settings:").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("  Enabled: ").color(NamedTextColor.GRAY)
                .append(Component.text(cooldownEnabled ? "✓ Yes" : "✗ No")
                        .color(cooldownEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
        sender.sendMessage(Component.text("  Duration: ").color(NamedTextColor.GRAY)
                .append(Component.text(cooldownSeconds + " seconds").color(NamedTextColor.WHITE)));

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Dragon Settings:").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("  Cooldown Exempt: ").color(NamedTextColor.GRAY)
                .append(Component.text(dragonExempt ? "✓ Yes" : "✗ No")
                        .color(dragonExempt ? NamedTextColor.GREEN : NamedTextColor.RED)));
        sender.sendMessage(Component.text("  Damage Disabled: ").color(NamedTextColor.GRAY)
                .append(Component.text(dragonDamageDisabled ? "✓ Yes" : "✗ No")
                        .color(dragonDamageDisabled ? NamedTextColor.GREEN : NamedTextColor.RED)));

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Enchantment Settings:").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("  Restrictions: ").color(NamedTextColor.GRAY)
                .append(Component.text(enchantRestrictionsEnabled ? "✓ Enabled" : "✗ Disabled")
                        .color(enchantRestrictionsEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
        if (enchantRestrictionsEnabled) {
            sender.sendMessage(Component.text("  Allowed: ").color(NamedTextColor.GRAY)
                    .append(Component.text("Unbreaking, Mending only").color(NamedTextColor.AQUA)));
        }

        sender.sendMessage(Component.empty());
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        loadConfig();
        sender.sendMessage(Component.text("✓ Mace configuration reloaded").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleReset(CommandSender sender) {
        cooldownSeconds = 45;
        dragonExempt = true;
        cooldownEnabled = true;
        enchantRestrictionsEnabled = true;
        dragonDamageDisabled = true;
        maceDisabled = false; // NEW
        saveConfig();

        sender.sendMessage(Component.text("✓ Mace configuration reset to defaults").color(NamedTextColor.GREEN));
        sender.sendMessage(Component.text("  Mace: Enabled").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Cooldown: 45s (enabled)").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Dragon exempt: Yes").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Dragon damage: Disabled").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Enchant restrictions: Enabled").color(NamedTextColor.GRAY));
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("⚔ Mace Commands").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.empty());

        // NEW: Mace Enable/Disable
        sender.sendMessage(Component.text("Mace Control:").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("  /mace enable").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Enable mace usage").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /mace disable").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Completely disable mace usage").color(NamedTextColor.GRAY));

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Cooldown Management:").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("  /mace cooldown enable").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Enable mace attack cooldown").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /mace cooldown disable").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Disable mace attack cooldown").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /mace cooldown set <seconds>").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Set cooldown duration").color(NamedTextColor.GRAY));

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Dragon Settings:").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("  /mace dragon exempt <enable|disable>").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Toggle cooldown exemption for dragon").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /mace dragon damage <enable|disable>").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Toggle mace damage to dragon").color(NamedTextColor.GRAY));

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Enchantment Settings:").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("  /mace enchants <enable|disable>").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Toggle enchantment restrictions").color(NamedTextColor.GRAY));

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Other Commands:").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("  /mace info").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    View current mace settings").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /mace reload").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Reload configuration from file").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /mace reset").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Reset to default settings").color(NamedTextColor.GRAY));

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Examples:").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("  /mace disable").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    → Completely disable mace").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("  /mace cooldown set 30").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    → Set 30 second cooldown").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("  /mace dragon exempt disable").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    → Apply cooldown to dragon hits").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("  /mace enchants disable").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    → Allow all enchantments").color(NamedTextColor.DARK_GRAY));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("generalfeatures.mace")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Arrays.asList("enable", "disable", "cooldown", "dragon", "enchants", "info", "reload", "reset") // NEW: added enable/disable
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("cooldown")) {
                return Arrays.asList("enable", "disable", "set")
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (subCmd.equals("dragon")) {
                return Arrays.asList("exempt", "damage")
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (subCmd.equals("enchants")) {
                return Arrays.asList("enable", "disable")
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("cooldown") && args[1].equalsIgnoreCase("set")) {
                return Arrays.asList("15", "30", "45", "60");
            }
            if (subCmd.equals("dragon")) {
                return Arrays.asList("enable", "disable")
                        .stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    // Getter methods for other classes to access settings
    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public boolean isDragonExempt() {
        return dragonExempt;
    }

    public boolean isCooldownEnabled() {
        return cooldownEnabled;
    }

    public boolean isEnchantRestrictionsEnabled() {
        return enchantRestrictionsEnabled;
    }

    public boolean isDragonDamageDisabled() {
        return dragonDamageDisabled;
    }

    public boolean isMaceDisabled() {
        return maceDisabled;
    }
}