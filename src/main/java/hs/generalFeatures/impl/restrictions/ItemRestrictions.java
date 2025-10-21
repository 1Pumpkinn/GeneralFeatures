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
import org.bukkit.command.TabCompleter;
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
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ItemRestrictions implements CommandExecutor, TabCompleter, Listener {

    private final JavaPlugin plugin;
    private File configFile;
    private FileConfiguration config;

    // Restriction settings
    private boolean enderPearlsRestricted = true;
    private boolean fireworksRestricted = true;

    // Store restricted potion effect types by their key
    private Set<String> restrictedPotionEffects = new HashSet<>();

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
                e.printStackTrace();
                return;
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Load settings
        enderPearlsRestricted = config.getBoolean("restrictions.ender-pearls", true);
        fireworksRestricted = config.getBoolean("restrictions.fireworks", true);

        // Load restricted potion effects
        List<String> loadedEffects = config.getStringList("restrictions.potion-effects");
        restrictedPotionEffects = new HashSet<>(loadedEffects);

        // If no effects are loaded, add defaults
        if (restrictedPotionEffects.isEmpty()) {
            restrictedPotionEffects.add("strength:2");
            restrictedPotionEffects.add("speed:2");
            restrictedPotionEffects.add("poison");
            restrictedPotionEffects.add("slowness");
            restrictedPotionEffects.add("instant_damage");
            restrictedPotionEffects.add("unluck");
            restrictedPotionEffects.add("wither");
            restrictedPotionEffects.add("weakness");
            saveConfig();
        }
    }

    private void saveConfig() {
        config.set("restrictions.ender-pearls", enderPearlsRestricted);
        config.set("restrictions.fireworks", fireworksRestricted);
        config.set("restrictions.potion-effects", new ArrayList<>(restrictedPotionEffects));

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save restrictions.yml file!");
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("generalfeatures.restrictions")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "enable":
            case "disable":
                return handleToggle(sender, args);
            case "potion":
            case "potions":
                return handlePotionCommand(sender, args);
            case "list":
                return handleList(sender);
            default:
                sendUsage(sender);
                return true;
        }
    }

    private boolean handleToggle(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /restrictions <enable|disable> <pearls|fireworks|all>").color(NamedTextColor.RED));
            return true;
        }

        boolean enable = args[0].equalsIgnoreCase("enable");
        String item = args[1].toLowerCase();

        switch (item) {
            case "pearls":
                enderPearlsRestricted = enable;
                sender.sendMessage(Component.text("Ender pearl restrictions " + (enable ? "enabled" : "disabled") + "!").color(NamedTextColor.GREEN));
                break;
            case "fireworks":
                fireworksRestricted = enable;
                sender.sendMessage(Component.text("Firework restrictions " + (enable ? "enabled" : "disabled") + "!").color(NamedTextColor.GREEN));
                break;
            case "all":
                enderPearlsRestricted = enable;
                fireworksRestricted = enable;
                sender.sendMessage(Component.text("All item restrictions " + (enable ? "enabled" : "disabled") + "!").color(NamedTextColor.GREEN));
                break;
            default:
                sender.sendMessage(Component.text("Unknown item type: " + item).color(NamedTextColor.RED));
                sender.sendMessage(Component.text("Available: pearls, fireworks, all").color(NamedTextColor.YELLOW));
                return true;
        }

        saveConfig();
        return true;
    }

    private boolean handlePotionCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /restrictions potion <add|remove|clear|list> [effect] [level]").color(NamedTextColor.RED));
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "add":
                return handlePotionAdd(sender, args);
            case "remove":
                return handlePotionRemove(sender, args);
            case "clear":
                return handlePotionClear(sender);
            case "list":
                return handlePotionList(sender);
            default:
                sender.sendMessage(Component.text("Unknown action: " + action).color(NamedTextColor.RED));
                sender.sendMessage(Component.text("Available: add, remove, clear, list").color(NamedTextColor.YELLOW));
                return true;
        }
    }

    private boolean handlePotionAdd(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /restrictions potion add <effect> [level]").color(NamedTextColor.RED));
            sender.sendMessage(Component.text("Example: /restrictions potion add strength 2").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Example: /restrictions potion add poison (all levels)").color(NamedTextColor.GRAY));
            return true;
        }

        String effectName = args[2].toLowerCase();

        // Validate effect exists
        PotionEffectType effectType = PotionEffectType.getByName(effectName);
        if (effectType == null) {
            sender.sendMessage(Component.text("Unknown potion effect: " + effectName).color(NamedTextColor.RED));
            sender.sendMessage(Component.text("Use tab completion to see available effects").color(NamedTextColor.YELLOW));
            return true;
        }

        String restriction;
        if (args.length >= 4) {
            try {
                int level = Integer.parseInt(args[3]);
                if (level < 1) {
                    sender.sendMessage(Component.text("Level must be 1 or higher!").color(NamedTextColor.RED));
                    return true;
                }
                restriction = effectName + ":" + level;
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid level: " + args[3]).color(NamedTextColor.RED));
                return true;
            }
        } else {
            restriction = effectName;
        }

        if (restrictedPotionEffects.add(restriction)) {
            sender.sendMessage(Component.text("Added restriction: " + restriction).color(NamedTextColor.GREEN));
            saveConfig();
        } else {
            sender.sendMessage(Component.text("This restriction already exists!").color(NamedTextColor.YELLOW));
        }

        return true;
    }

    private boolean handlePotionRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /restrictions potion remove <effect> [level]").color(NamedTextColor.RED));
            sender.sendMessage(Component.text("Example: /restrictions potion remove strength 2").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Example: /restrictions potion remove poison").color(NamedTextColor.GRAY));
            return true;
        }

        String effectName = args[2].toLowerCase();

        String restriction;
        if (args.length >= 4) {
            try {
                int level = Integer.parseInt(args[3]);
                restriction = effectName + ":" + level;
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid level: " + args[3]).color(NamedTextColor.RED));
                return true;
            }
        } else {
            restriction = effectName;
        }

        if (restrictedPotionEffects.remove(restriction)) {
            sender.sendMessage(Component.text("Removed restriction: " + restriction).color(NamedTextColor.GREEN));
            saveConfig();
        } else {
            sender.sendMessage(Component.text("This restriction doesn't exist!").color(NamedTextColor.YELLOW));
        }

        return true;
    }

    private boolean handlePotionClear(CommandSender sender) {
        int count = restrictedPotionEffects.size();
        restrictedPotionEffects.clear();
        sender.sendMessage(Component.text("Cleared " + count + " potion restrictions!").color(NamedTextColor.GREEN));
        saveConfig();
        return true;
    }

    private boolean handlePotionList(CommandSender sender) {
        if (restrictedPotionEffects.isEmpty()) {
            sender.sendMessage(Component.text("No potion restrictions are active.").color(NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(Component.text("=== Restricted Potions ===").color(NamedTextColor.GOLD));
        List<String> sorted = new ArrayList<>(restrictedPotionEffects);
        Collections.sort(sorted);

        for (String restriction : sorted) {
            sender.sendMessage(Component.text("  - " + restriction).color(NamedTextColor.GRAY));
        }

        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(Component.text("=== Current Restrictions ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Ender Pearls: " + (enderPearlsRestricted ? "Restricted" : "Allowed")).color(enderPearlsRestricted ? NamedTextColor.RED : NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Fireworks: " + (fireworksRestricted ? "Restricted" : "Allowed")).color(fireworksRestricted ? NamedTextColor.RED : NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Restricted Potions: " + restrictedPotionEffects.size()).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Use '/restrictions potion list' to see potion details").color(NamedTextColor.GRAY));
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("=== Item Restrictions Commands ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/restrictions enable/disable <pearls|fireworks|all>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/restrictions potion add <effect> [level]").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/restrictions potion remove <effect> [level]").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/restrictions potion clear").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/restrictions potion list").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/restrictions list").color(NamedTextColor.YELLOW));
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

        // Check for splash/lingering potion throwing
        if (item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION) {
            if (isPotionRestricted(item)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("This potion is restricted!").color(NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        Player player = event.getPlayer();

        // Only check regular drinkable potions here
        if (item.getType() == Material.POTION) {
            if (isPotionRestricted(item)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("This potion is restricted!").color(NamedTextColor.RED));
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

        // Check for potion dispensing (all potion types)
        if (item.getType() == Material.POTION ||
                item.getType() == Material.SPLASH_POTION ||
                item.getType() == Material.LINGERING_POTION) {
            if (isPotionRestricted(item)) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isPotionRestricted(ItemStack item) {
        if (restrictedPotionEffects.isEmpty()) {
            return false;
        }

        if (!(item.getItemMeta() instanceof PotionMeta potionMeta)) {
            return false;
        }

        // Check base potion type
        if (potionMeta.getBasePotionType() != null) {
            String potionName = potionMeta.getBasePotionType().name().toLowerCase();

            // Extract effect and amplifier from potion type
            String effectName = extractEffectName(potionName);
            int amplifier = potionName.contains("strong") ? 1 : 0;

            if (isEffectRestricted(effectName, amplifier)) {
                return true;
            }
        }

        // Check custom effects
        if (potionMeta.hasCustomEffects()) {
            for (PotionEffect effect : potionMeta.getCustomEffects()) {
                String effectName = effect.getType().getKey().getKey().toLowerCase();
                int amplifier = effect.getAmplifier();

                if (isEffectRestricted(effectName, amplifier)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String extractEffectName(String potionTypeName) {
        // Remove prefixes/suffixes
        potionTypeName = potionTypeName.replace("strong_", "")
                .replace("long_", "")
                .replace("_potion", "");

        // Map potion type names to effect names
        return switch (potionTypeName) {
            case "strength", "increase_damage" -> "strength";
            case "swiftness" -> "speed";
            case "slowness", "slow" -> "slowness";
            case "harming", "instant_damage" -> "instant_damage";
            case "healing", "instant_health" -> "instant_health";
            default -> potionTypeName;
        };
    }

    private boolean isEffectRestricted(String effectName, int amplifier) {
        effectName = effectName.toLowerCase();
        int level = amplifier + 1; // Amplifier 0 = Level 1

        // Check for specific level restriction
        if (restrictedPotionEffects.contains(effectName + ":" + level)) {
            return true;
        }

        // Check for all-level restriction
        if (restrictedPotionEffects.contains(effectName)) {
            return true;
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("generalfeatures.restrictions")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Arrays.asList("enable", "disable", "potion", "list")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("disable")) {
                return Arrays.asList("pearls", "fireworks", "all")
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("potion")) {
                return Arrays.asList("add", "remove", "clear", "list")
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("potion")) {
            if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove")) {
                return Arrays.stream(PotionEffectType.values())
                        .map(type -> type.getKey().getKey().toLowerCase())
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .sorted()
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("potion")) {
            if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove")) {
                return Arrays.asList("1", "2", "3", "4", "5");
            }
        }

        return Collections.emptyList();
    }
}