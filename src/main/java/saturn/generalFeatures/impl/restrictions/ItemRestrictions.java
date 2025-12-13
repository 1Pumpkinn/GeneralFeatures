package saturn.generalFeatures.impl.restrictions;

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

    private boolean enderPearlsRestricted = true;
    private boolean fireworksRestricted = true;
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

        enderPearlsRestricted = config.getBoolean("restrictions.ender-pearls", true);
        fireworksRestricted = config.getBoolean("restrictions.fireworks", true);

        List<String> loadedEffects = config.getStringList("restrictions.potion-effects");
        restrictedPotionEffects = new HashSet<>(loadedEffects);

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

        return switch (subCommand) {
            case "enable", "on" -> handleEnable(sender, args);
            case "disable", "off" -> handleDisable(sender, args);
            case "add" -> handleAdd(sender, args);
            case "remove", "delete" -> handleRemove(sender, args);
            case "clear" -> handleClear(sender, args);
            case "list", "show" -> handleList(sender);
            case "reset" -> handleReset(sender);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleEnable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /restrictions enable <pearls|fireworks|all>").color(NamedTextColor.RED));
            return true;
        }

        String item = args[1].toLowerCase();
        return switch (item) {
            case "pearls", "pearl" -> {
                enderPearlsRestricted = true;
                sender.sendMessage(Component.text("✓ Ender pearls are now restricted").color(NamedTextColor.GREEN));
                saveConfig();
                yield true;
            }
            case "fireworks", "firework" -> {
                fireworksRestricted = true;
                sender.sendMessage(Component.text("✓ Fireworks are now restricted").color(NamedTextColor.GREEN));
                saveConfig();
                yield true;
            }
            case "all" -> {
                enderPearlsRestricted = true;
                fireworksRestricted = true;
                sender.sendMessage(Component.text("✓ All items are now restricted").color(NamedTextColor.GREEN));
                saveConfig();
                yield true;
            }
            default -> {
                sender.sendMessage(Component.text("Unknown item: " + item).color(NamedTextColor.RED));
                sender.sendMessage(Component.text("Available: pearls, fireworks, all").color(NamedTextColor.YELLOW));
                yield true;
            }
        };
    }

    private boolean handleDisable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /restrictions disable <pearls|fireworks|all>").color(NamedTextColor.RED));
            return true;
        }

        String item = args[1].toLowerCase();
        return switch (item) {
            case "pearls", "pearl" -> {
                enderPearlsRestricted = false;
                sender.sendMessage(Component.text("✓ Ender pearls are now allowed").color(NamedTextColor.GREEN));
                saveConfig();
                yield true;
            }
            case "fireworks", "firework" -> {
                fireworksRestricted = false;
                sender.sendMessage(Component.text("✓ Fireworks are now allowed").color(NamedTextColor.GREEN));
                saveConfig();
                yield true;
            }
            case "all" -> {
                enderPearlsRestricted = false;
                fireworksRestricted = false;
                sender.sendMessage(Component.text("✓ All items are now allowed").color(NamedTextColor.GREEN));
                saveConfig();
                yield true;
            }
            default -> {
                sender.sendMessage(Component.text("Unknown item: " + item).color(NamedTextColor.RED));
                sender.sendMessage(Component.text("Available: pearls, fireworks, all").color(NamedTextColor.YELLOW));
                yield true;
            }
        };
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /restrictions add <effect> [level] or <effect1,effect2,...>").color(NamedTextColor.RED));
            sender.sendMessage(Component.text("Examples:").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("  /restrictions add strength 2").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /restrictions add poison").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /restrictions add poison,weakness,wither").color(NamedTextColor.GRAY));
            return true;
        }

        String effectArg = args[1].toLowerCase();

        // Check if multiple effects (comma-separated)
        if (effectArg.contains(",")) {
            String[] effects = effectArg.split(",");
            int added = 0;
            List<String> failed = new ArrayList<>();

            for (String effect : effects) {
                effect = effect.trim();
                if (PotionEffectType.getByName(effect) != null) {
                    if (restrictedPotionEffects.add(effect)) {
                        added++;
                    }
                } else {
                    failed.add(effect);
                }
            }

            if (added > 0) {
                sender.sendMessage(Component.text("✓ Added " + added + " potion restriction(s)").color(NamedTextColor.GREEN));
                saveConfig();
            }
            if (!failed.isEmpty()) {
                sender.sendMessage(Component.text("✗ Invalid effects: " + String.join(", ", failed)).color(NamedTextColor.RED));
            }
            return true;
        }

        // Single effect
        String effectName = effectArg;
        PotionEffectType effectType = PotionEffectType.getByName(effectName);

        if (effectType == null) {
            sender.sendMessage(Component.text("Unknown potion effect: " + effectName).color(NamedTextColor.RED));
            sender.sendMessage(Component.text("Use tab completion to see available effects").color(NamedTextColor.YELLOW));
            return true;
        }

        String restriction;
        if (args.length >= 3) {
            try {
                int level = Integer.parseInt(args[2]);
                if (level < 1) {
                    sender.sendMessage(Component.text("Level must be 1 or higher!").color(NamedTextColor.RED));
                    return true;
                }
                restriction = effectName + ":" + level;
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid level: " + args[2]).color(NamedTextColor.RED));
                return true;
            }
        } else {
            restriction = effectName;
        }

        if (restrictedPotionEffects.add(restriction)) {
            sender.sendMessage(Component.text("✓ Added restriction: " + restriction).color(NamedTextColor.GREEN));
            saveConfig();
        } else {
            sender.sendMessage(Component.text("This restriction already exists!").color(NamedTextColor.YELLOW));
        }

        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /restrictions remove <effect> [level] or <effect1,effect2,...>").color(NamedTextColor.RED));
            sender.sendMessage(Component.text("Examples:").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("  /restrictions remove strength 2").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /restrictions remove poison").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /restrictions remove poison,weakness,wither").color(NamedTextColor.GRAY));
            return true;
        }

        String effectArg = args[1].toLowerCase();

        // Check if multiple effects (comma-separated)
        if (effectArg.contains(",")) {
            String[] effects = effectArg.split(",");
            int removed = 0;

            for (String effect : effects) {
                effect = effect.trim();
                if (restrictedPotionEffects.remove(effect)) {
                    removed++;
                }
            }

            if (removed > 0) {
                sender.sendMessage(Component.text("✓ Removed " + removed + " potion restriction(s)").color(NamedTextColor.GREEN));
                saveConfig();
            } else {
                sender.sendMessage(Component.text("No matching restrictions found").color(NamedTextColor.YELLOW));
            }
            return true;
        }

        // Single effect
        String effectName = effectArg;
        String restriction;

        if (args.length >= 3) {
            try {
                int level = Integer.parseInt(args[2]);
                restriction = effectName + ":" + level;
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid level: " + args[2]).color(NamedTextColor.RED));
                return true;
            }
        } else {
            restriction = effectName;
        }

        if (restrictedPotionEffects.remove(restriction)) {
            sender.sendMessage(Component.text("✓ Removed restriction: " + restriction).color(NamedTextColor.GREEN));
            saveConfig();
        } else {
            sender.sendMessage(Component.text("This restriction doesn't exist!").color(NamedTextColor.YELLOW));
        }

        return true;
    }

    private boolean handleClear(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /restrictions clear <potions|all>").color(NamedTextColor.RED));
            return true;
        }

        String target = args[1].toLowerCase();
        return switch (target) {
            case "potions", "potion" -> {
                int count = restrictedPotionEffects.size();
                restrictedPotionEffects.clear();
                sender.sendMessage(Component.text("✓ Cleared " + count + " potion restriction(s)").color(NamedTextColor.GREEN));
                saveConfig();
                yield true;
            }
            case "all" -> {
                int count = restrictedPotionEffects.size();
                restrictedPotionEffects.clear();
                enderPearlsRestricted = false;
                fireworksRestricted = false;
                sender.sendMessage(Component.text("✓ Cleared all restrictions (" + count + " potions + items)").color(NamedTextColor.GREEN));
                saveConfig();
                yield true;
            }
            default -> {
                sender.sendMessage(Component.text("Unknown target: " + target).color(NamedTextColor.RED));
                sender.sendMessage(Component.text("Available: potions, all").color(NamedTextColor.YELLOW));
                yield true;
            }
        };
    }

    private boolean handleReset(CommandSender sender) {
        restrictedPotionEffects.clear();
        restrictedPotionEffects.add("strength:2");
        restrictedPotionEffects.add("speed:2");
        restrictedPotionEffects.add("poison");
        restrictedPotionEffects.add("slowness");
        restrictedPotionEffects.add("instant_damage");
        restrictedPotionEffects.add("unluck");
        restrictedPotionEffects.add("wither");
        restrictedPotionEffects.add("weakness");

        enderPearlsRestricted = true;
        fireworksRestricted = true;

        sender.sendMessage(Component.text("✓ Reset all restrictions to defaults").color(NamedTextColor.GREEN));
        saveConfig();
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(Component.text("═══ Current Restrictions ═══").color(NamedTextColor.GOLD));

        sender.sendMessage(Component.text("Items:").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Ender Pearls: " + (enderPearlsRestricted ? "✗ Restricted" : "✓ Allowed"))
                .color(enderPearlsRestricted ? NamedTextColor.RED : NamedTextColor.GREEN));
        sender.sendMessage(Component.text("  Fireworks: " + (fireworksRestricted ? "✗ Restricted" : "✓ Allowed"))
                .color(fireworksRestricted ? NamedTextColor.RED : NamedTextColor.GREEN));

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Potions (" + restrictedPotionEffects.size() + " total):").color(NamedTextColor.YELLOW));

        if (restrictedPotionEffects.isEmpty()) {
            sender.sendMessage(Component.text("  None").color(NamedTextColor.GRAY));
        } else {
            List<String> sorted = new ArrayList<>(restrictedPotionEffects);
            Collections.sort(sorted);
            for (String restriction : sorted) {
                sender.sendMessage(Component.text("  • " + restriction).color(NamedTextColor.GRAY));
            }
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("═══ Item Restrictions ═══").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Quick Commands:").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /restrictions add <effect> [level]").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Add potion restriction").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /restrictions remove <effect> [level]").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Remove potion restriction").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /restrictions list").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Show all restrictions").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Item Controls:").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /restrictions enable <pearls|fireworks|all>").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  /restrictions disable <pearls|fireworks|all>").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Bulk Operations:").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /restrictions clear <potions|all>").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  /restrictions reset").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Examples:").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /restrictions add poison,weakness,wither").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /restrictions remove strength 2").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /restrictions enable pearls").color(NamedTextColor.GRAY));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        if (enderPearlsRestricted && item.getType() == Material.ENDER_PEARL) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Ender pearls are restricted!").color(NamedTextColor.RED));
            return;
        }

        if (fireworksRestricted && item.getType() == Material.FIREWORK_ROCKET) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Fireworks are restricted!").color(NamedTextColor.RED));
            return;
        }

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

        if (!(block.getState() instanceof Dispenser) && !(block.getState() instanceof Dropper)) {
            return;
        }

        if (enderPearlsRestricted && item.getType() == Material.ENDER_PEARL) {
            event.setCancelled(true);
            return;
        }

        if (fireworksRestricted && item.getType() == Material.FIREWORK_ROCKET) {
            event.setCancelled(true);
            return;
        }

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

        if (potionMeta.getBasePotionType() != null) {
            String potionName = potionMeta.getBasePotionType().name().toLowerCase();
            String effectName = extractEffectName(potionName);
            int amplifier = potionName.contains("strong") ? 1 : 0;

            if (isEffectRestricted(effectName, amplifier)) {
                return true;
            }
        }

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
        potionTypeName = potionTypeName.replace("strong_", "")
                .replace("long_", "")
                .replace("_potion", "");

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
        int level = amplifier + 1;

        if (restrictedPotionEffects.contains(effectName + ":" + level)) {
            return true;
        }

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
            return Arrays.asList("add", "remove", "enable", "disable", "clear", "list", "reset")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("enable") || subCmd.equals("disable")) {
                return Arrays.asList("pearls", "fireworks", "all")
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (subCmd.equals("clear")) {
                return Arrays.asList("potions", "all")
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (subCmd.equals("add")) {
                return Arrays.stream(PotionEffectType.values())
                        .map(type -> type.getKey().getKey().toLowerCase())
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .sorted()
                        .collect(Collectors.toList());
            }
            if (subCmd.equals("remove") || subCmd.equals("delete")) {
                // Only show restricted effects for removal
                return restrictedPotionEffects.stream()
                        .map(restriction -> {
                            // Extract base effect name from "effect:level" format
                            if (restriction.contains(":")) {
                                return restriction.split(":")[0];
                            }
                            return restriction;
                        })
                        .distinct()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .sorted()
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("add")) {
                return Arrays.asList("1", "2", "3", "4", "5");
            }
            if (subCmd.equals("remove") || subCmd.equals("delete")) {
                // Show only the levels that are restricted for this effect
                String effectName = args[1].toLowerCase();
                List<String> levels = new ArrayList<>();

                for (String restriction : restrictedPotionEffects) {
                    if (restriction.contains(":")) {
                        String[] parts = restriction.split(":");
                        if (parts[0].equals(effectName)) {
                            levels.add(parts[1]);
                        }
                    }
                }

                return levels.stream()
                        .sorted()
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}