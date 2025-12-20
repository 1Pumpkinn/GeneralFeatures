package saturn.ElementSmpManager.impl.restrictions;

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
import org.bukkit.potion.PotionType;
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
    private boolean potionsRestricted = false;
    private Set<String> restrictedPotionTypes = new HashSet<>();

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
        potionsRestricted = config.getBoolean("restrictions.potions-disabled", false);

        List<String> loadedPotions = config.getStringList("restrictions.potion-types");
        restrictedPotionTypes = new HashSet<>(loadedPotions);

        if (restrictedPotionTypes.isEmpty()) {
            // Default restricted potions
            restrictedPotionTypes.add("strong_strength");
            restrictedPotionTypes.add("strong_harming");
            restrictedPotionTypes.add("strong_poison");
            restrictedPotionTypes.add("strong_slowness");
            restrictedPotionTypes.add("turtle_master");
            restrictedPotionTypes.add("strong_turtle_master");

            saveConfig();
        }
    }

    private void saveConfig() {
        config.set("restrictions.ender-pearls", enderPearlsRestricted);
        config.set("restrictions.fireworks", fireworksRestricted);
        config.set("restrictions.potions-disabled", potionsRestricted);
        config.set("restrictions.potion-types", new ArrayList<>(restrictedPotionTypes));

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
            sender.sendMessage(Component.text("Usage: /restrictions disable <pearls|fireworks|potions|all>").color(NamedTextColor.RED));
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
            case "potions", "potion" -> {
                potionsRestricted = true;
                sender.sendMessage(Component.text("✓ All potions are now completely disabled").color(NamedTextColor.GREEN));
                sender.sendMessage(Component.text("  Players cannot use any potions").color(NamedTextColor.GRAY));
                saveConfig();
                yield true;
            }
            case "all" -> {
                enderPearlsRestricted = false;
                fireworksRestricted = false;
                potionsRestricted = false;
                sender.sendMessage(Component.text("✓ All items are now allowed").color(NamedTextColor.GREEN));
                saveConfig();
                yield true;
            }
            default -> {
                sender.sendMessage(Component.text("Unknown item: " + item).color(NamedTextColor.RED));
                sender.sendMessage(Component.text("Available: pearls, fireworks, potions, all").color(NamedTextColor.YELLOW));
                yield true;
            }
        };
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /restrictions add <potion_type> or <potion1,potion2,...>").color(NamedTextColor.RED));
            sender.sendMessage(Component.text("Examples:").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("  /restrictions add turtle_master").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /restrictions add strong_strength").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /restrictions add turtle_master,strong_strength,strong_harming").color(NamedTextColor.GRAY));
            return true;
        }

        String potionArg = args[1].toLowerCase();

        // Check if multiple potions (comma-separated)
        if (potionArg.contains(",")) {
            String[] potions = potionArg.split(",");
            int added = 0;
            List<String> failed = new ArrayList<>();

            for (String potion : potions) {
                potion = potion.trim();
                if (isValidPotionType(potion)) {
                    if (restrictedPotionTypes.add(potion)) {
                        added++;
                    }
                } else {
                    failed.add(potion);
                }
            }

            if (added > 0) {
                sender.sendMessage(Component.text("✓ Added " + added + " potion restriction(s)").color(NamedTextColor.GREEN));
                saveConfig();
            }
            if (!failed.isEmpty()) {
                sender.sendMessage(Component.text("✗ Invalid potion types: " + String.join(", ", failed)).color(NamedTextColor.RED));
            }
            return true;
        }

        // Single potion
        String potionType = potionArg;

        if (!isValidPotionType(potionType)) {
            sender.sendMessage(Component.text("Unknown potion type: " + potionType).color(NamedTextColor.RED));
            sender.sendMessage(Component.text("Use tab completion to see available potion types").color(NamedTextColor.YELLOW));
            return true;
        }

        if (restrictedPotionTypes.add(potionType)) {
            sender.sendMessage(Component.text("✓ Added restriction: " + potionType).color(NamedTextColor.GREEN));
            saveConfig();
        } else {
            sender.sendMessage(Component.text("This potion is already restricted!").color(NamedTextColor.YELLOW));
        }

        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /restrictions remove <potion_type> or <potion1,potion2,...>").color(NamedTextColor.RED));
            sender.sendMessage(Component.text("Examples:").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("  /restrictions remove turtle_master").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /restrictions remove strong_strength").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /restrictions remove turtle_master,strong_strength").color(NamedTextColor.GRAY));
            return true;
        }

        String potionArg = args[1].toLowerCase();

        // Check if multiple potions (comma-separated)
        if (potionArg.contains(",")) {
            String[] potions = potionArg.split(",");
            int removed = 0;

            for (String potion : potions) {
                potion = potion.trim();
                if (restrictedPotionTypes.remove(potion)) {
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

        // Single potion
        String potionType = potionArg;

        if (restrictedPotionTypes.remove(potionType)) {
            sender.sendMessage(Component.text("✓ Removed restriction: " + potionType).color(NamedTextColor.GREEN));
            saveConfig();
        } else {
            sender.sendMessage(Component.text("This potion is not restricted!").color(NamedTextColor.YELLOW));
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
                int count = restrictedPotionTypes.size();
                restrictedPotionTypes.clear();
                sender.sendMessage(Component.text("✓ Cleared " + count + " potion restriction(s)").color(NamedTextColor.GREEN));
                saveConfig();
                yield true;
            }
            case "all" -> {
                int count = restrictedPotionTypes.size();
                restrictedPotionTypes.clear();
                enderPearlsRestricted = false;
                fireworksRestricted = false;
                potionsRestricted = false;
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
        restrictedPotionTypes.clear();

        // Default restricted potions
        restrictedPotionTypes.add("strong_strength");
        restrictedPotionTypes.add("strong_harming");
        restrictedPotionTypes.add("strong_poison");
        restrictedPotionTypes.add("strong_slowness");
        restrictedPotionTypes.add("turtle_master");
        restrictedPotionTypes.add("strong_turtle_master");

        enderPearlsRestricted = true;
        fireworksRestricted = true;
        potionsRestricted = false;

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
        sender.sendMessage(Component.text("Potions:").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  All Potions: " + (potionsRestricted ? "✗ Completely Disabled" : "✓ Enabled"))
                .color(potionsRestricted ? NamedTextColor.RED : NamedTextColor.GREEN));

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Restricted Potion Types (" + restrictedPotionTypes.size() + " total):").color(NamedTextColor.YELLOW));

        if (restrictedPotionTypes.isEmpty()) {
            sender.sendMessage(Component.text("  None").color(NamedTextColor.GRAY));
        } else {
            List<String> sorted = new ArrayList<>(restrictedPotionTypes);
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
        sender.sendMessage(Component.text("  /restrictions add <potion_type>").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Add potion type restriction").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /restrictions remove <potion_type>").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Remove potion type restriction").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /restrictions list").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Show all restrictions").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Item Controls:").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /restrictions enable <pearls|fireworks|all>").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  /restrictions disable <pearls|fireworks|potions|all>").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    Note: 'disable potions' blocks ALL potions").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Bulk Operations:").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /restrictions clear <potions|all>").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  /restrictions reset").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Examples:").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /restrictions add turtle_master").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    → Ban turtle master potions").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("  /restrictions add strong_strength,strong_harming").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    → Ban multiple potion types").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("  /restrictions disable potions").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("    → Completely disable all potions").color(NamedTextColor.DARK_GRAY));
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
            // Check if all potions are disabled
            if (potionsRestricted) {
                event.setCancelled(true);
                player.sendMessage(Component.text("All potions are disabled!").color(NamedTextColor.RED));
                return;
            }

            // Check specific potion type restrictions
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
            // Check if all potions are disabled
            if (potionsRestricted) {
                event.setCancelled(true);
                player.sendMessage(Component.text("All potions are disabled!").color(NamedTextColor.RED));
                return;
            }

            // Check specific potion type restrictions
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
            // Check if all potions are disabled
            if (potionsRestricted) {
                event.setCancelled(true);
                return;
            }

            // Check specific potion type restrictions
            if (isPotionRestricted(item)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Checks if a potion is restricted based on its base potion type.
     */
    private boolean isPotionRestricted(ItemStack item) {
        if (restrictedPotionTypes.isEmpty()) {
            return false;
        }

        if (!(item.getItemMeta() instanceof PotionMeta potionMeta)) {
            return false;
        }

        if (potionMeta.getBasePotionType() != null) {
            String potionTypeName = potionMeta.getBasePotionType().name().toLowerCase();

            // Check if this exact potion type is restricted
            if (restrictedPotionTypes.contains(potionTypeName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Validates if a string is a valid PotionType
     */
    private boolean isValidPotionType(String potionType) {
        try {
            PotionType.valueOf(potionType.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
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
            if (subCmd.equals("enable")) {
                return Arrays.asList("pearls", "fireworks", "all")
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (subCmd.equals("disable")) {
                return Arrays.asList("pearls", "fireworks", "potions", "all")
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
                // Show all available potion types
                return Arrays.stream(PotionType.values())
                        .map(type -> type.name().toLowerCase())
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .sorted()
                        .collect(Collectors.toList());
            }
            if (subCmd.equals("remove") || subCmd.equals("delete")) {
                // Only show restricted potion types for removal
                return restrictedPotionTypes.stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .sorted()
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}