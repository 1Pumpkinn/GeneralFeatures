package hs.generalFeatures.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BroadcastCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("generalfeatures.broadcast")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        // Parse options and message
        BroadcastOptions options = new BroadcastOptions();
        StringBuilder messageBuilder = new StringBuilder();

        int i = 0;
        while (i < args.length) {
            String arg = args[i];

            // Check for flags
            if (arg.startsWith("-")) {
                switch (arg.toLowerCase()) {
                    case "-c", "-color" -> {
                        if (i + 1 >= args.length) {
                            sender.sendMessage(Component.text("Missing color value after " + arg).color(NamedTextColor.RED));
                            return true;
                        }
                        options.color = parseColor(args[++i]);
                        if (options.color == null) {
                            sender.sendMessage(Component.text("Invalid color: " + args[i]).color(NamedTextColor.RED));
                            return true;
                        }
                    }
                    case "-p", "-prefix" -> {
                        if (i + 1 >= args.length) {
                            sender.sendMessage(Component.text("Missing prefix after " + arg).color(NamedTextColor.RED));
                            return true;
                        }
                        options.prefix = args[++i];
                    }
                    case "-b", "-bold" -> options.bold = true;
                    case "-i", "-italic" -> options.italic = true;
                    case "-u", "-underline" -> options.underlined = true;
                    case "-s", "-sound" -> {
                        if (i + 1 >= args.length) {
                            sender.sendMessage(Component.text("Missing sound after " + arg).color(NamedTextColor.RED));
                            return true;
                        }
                        options.sound = parseSound(args[++i]);
                        if (options.sound == null) {
                            sender.sendMessage(Component.text("Invalid sound: " + args[i]).color(NamedTextColor.RED));
                            return true;
                        }
                    }
                    case "-t", "-title" -> options.displayAsTitle = true;
                    case "-a", "-actionbar" -> options.displayAsActionBar = true;
                    case "-subtitle" -> {
                        if (i + 1 >= args.length) {
                            sender.sendMessage(Component.text("Missing subtitle after " + arg).color(NamedTextColor.RED));
                            return true;
                        }
                        options.subtitle = args[++i];
                    }
                    case "-fadein" -> {
                        if (i + 1 >= args.length) {
                            sender.sendMessage(Component.text("Missing fade in time after " + arg).color(NamedTextColor.RED));
                            return true;
                        }
                        options.fadeIn = parseSeconds(args[++i]);
                    }
                    case "-stay" -> {
                        if (i + 1 >= args.length) {
                            sender.sendMessage(Component.text("Missing stay time after " + arg).color(NamedTextColor.RED));
                            return true;
                        }
                        options.stay = parseSeconds(args[++i]);
                    }
                    case "-fadeout" -> {
                        if (i + 1 >= args.length) {
                            sender.sendMessage(Component.text("Missing fade out time after " + arg).color(NamedTextColor.RED));
                            return true;
                        }
                        options.fadeOut = parseSeconds(args[++i]);
                    }
                    case "-center" -> options.centered = true;
                    default -> {
                        sender.sendMessage(Component.text("Unknown flag: " + arg).color(NamedTextColor.RED));
                        return true;
                    }
                }
            } else {
                // This is part of the message
                if (!messageBuilder.isEmpty()) {
                    messageBuilder.append(" ");
                }
                messageBuilder.append(arg);
            }
            i++;
        }

        String message = messageBuilder.toString();
        if (message.isEmpty()) {
            sender.sendMessage(Component.text("You must provide a message to broadcast!").color(NamedTextColor.RED));
            return true;
        }

        // Process color codes in message (&c, &#hex, etc.)
        message = translateColorCodes(message);

        // Send broadcast
        sendBroadcast(message, options);
        sender.sendMessage(Component.text("Broadcast sent!").color(NamedTextColor.GREEN));

        return true;
    }

    private void sendBroadcast(String message, BroadcastOptions options) {
        Component messageComponent = buildComponent(message, options);

        if (options.displayAsTitle) {
            // Send as title
            Component titleComponent = messageComponent;
            Component subtitleComponent = options.subtitle != null ?
                    buildComponent(options.subtitle, options) : Component.empty();

            Title title = Title.title(
                    titleComponent,
                    subtitleComponent,
                    Title.Times.times(
                            Duration.ofMillis(options.fadeIn),
                            Duration.ofMillis(options.stay),
                            Duration.ofMillis(options.fadeOut)
                    )
            );

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showTitle(title);
                if (options.sound != null) {
                    player.playSound(player.getLocation(), options.sound, 1.0f, 1.0f);
                }
            }
        } else if (options.displayAsActionBar) {
            // Send as action bar
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendActionBar(messageComponent);
                if (options.sound != null) {
                    player.playSound(player.getLocation(), options.sound, 1.0f, 1.0f);
                }
            }
        } else {
            // Send as chat message
            Bukkit.broadcast(messageComponent);

            if (options.sound != null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), options.sound, 1.0f, 1.0f);
                }
            }
        }
    }

    private Component buildComponent(String message, BroadcastOptions options) {
        Component component;

        // Add prefix if specified
        if (options.prefix != null && !options.prefix.isEmpty()) {
            String translatedPrefix = translateColorCodes(options.prefix);
            component = Component.text(translatedPrefix + " ").append(Component.text(message));
        } else {
            component = Component.text(message);
        }

        // Apply color
        if (options.color != null) {
            component = component.color(options.color);
        }

        // Apply text decorations
        if (options.bold) {
            component = component.decorate(TextDecoration.BOLD);
        }
        if (options.italic) {
            component = component.decorate(TextDecoration.ITALIC);
        }
        if (options.underlined) {
            component = component.decorate(TextDecoration.UNDERLINED);
        }

        return component;
    }

    private String translateColorCodes(String text) {
        // Replace & color codes with § for Minecraft formatting
        text = text.replaceAll("&([0-9a-fk-or])", "§$1");
        return text;
    }

    private TextColor parseColor(String colorStr) {
        // Try hex color first
        if (colorStr.startsWith("#")) {
            try {
                return TextColor.fromHexString(colorStr);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        // Try named colors
        return switch (colorStr.toLowerCase()) {
            case "black", "dark_black" -> NamedTextColor.BLACK;
            case "dark_blue", "blue" -> NamedTextColor.DARK_BLUE;
            case "dark_green", "green" -> NamedTextColor.DARK_GREEN;
            case "dark_aqua", "aqua" -> NamedTextColor.DARK_AQUA;
            case "dark_red", "red" -> NamedTextColor.DARK_RED;
            case "dark_purple", "purple" -> NamedTextColor.DARK_PURPLE;
            case "gold", "orange" -> NamedTextColor.GOLD;
            case "gray", "grey" -> NamedTextColor.GRAY;
            case "dark_gray", "dark_grey" -> NamedTextColor.DARK_GRAY;
            case "light_blue" -> NamedTextColor.BLUE;
            case "light_green", "lime" -> NamedTextColor.GREEN;
            case "cyan", "light_aqua" -> NamedTextColor.AQUA;
            case "light_red", "pink" -> NamedTextColor.RED;
            case "light_purple", "magenta" -> NamedTextColor.LIGHT_PURPLE;
            case "yellow" -> NamedTextColor.YELLOW;
            case "white" -> NamedTextColor.WHITE;
            default -> null;
        };
    }

    private Sound parseSound(String soundStr) {
        // Try common sound names first
        Sound commonSound = switch (soundStr.toLowerCase()) {
            case "bell", "ding" -> Sound.BLOCK_NOTE_BLOCK_BELL;
            case "pling", "ping" -> Sound.BLOCK_NOTE_BLOCK_PLING;
            case "bass" -> Sound.BLOCK_NOTE_BLOCK_BASS;
            case "explosion", "explode" -> Sound.ENTITY_GENERIC_EXPLODE;
            case "levelup", "level" -> Sound.ENTITY_PLAYER_LEVELUP;
            case "orb", "xp" -> Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
            case "anvil" -> Sound.BLOCK_ANVIL_LAND;
            case "portal" -> Sound.BLOCK_PORTAL_TRAVEL;
            case "thunder" -> Sound.ENTITY_LIGHTNING_BOLT_THUNDER;
            case "wither" -> Sound.ENTITY_WITHER_SPAWN;
            case "dragon" -> Sound.ENTITY_ENDER_DRAGON_GROWL;
            default -> null;
        };

        if (commonSound != null) {
            return commonSound;
        }

        // Try matching exact Sound enum name
        try {
            String enumName = soundStr.toUpperCase().replace(" ", "_");
            for (Sound sound : Sound.values()) {
                if (sound.name().equals(enumName)) {
                    return sound;
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    private long parseSeconds(String timeStr) {
        try {
            double seconds = Double.parseDouble(timeStr);
            return (long) (seconds * 1000); // Convert to milliseconds
        } catch (NumberFormatException e) {
            return 1000; // Default 1 second
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("=== Broadcast Command Usage ===").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("Usage: /broadcast [options] <message>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Options:").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("  -c/-color <color>     Set text color (name or #hex)").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  -p/-prefix <text>     Add prefix to message").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  -b/-bold              Make text bold").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  -i/-italic            Make text italic").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  -u/-underline         Make text underlined").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  -s/-sound <sound>     Play sound (e.g., bell, levelup)").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  -t/-title             Display as title").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  -a/-actionbar         Display as action bar").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  -subtitle <text>      Add subtitle (with -title)").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  -fadein <seconds>     Title fade in time").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  -stay <seconds>       Title stay time").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  -fadeout <seconds>    Title fade out time").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Examples:").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("  /broadcast -c gold -b Server restarting in 5 minutes!").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /broadcast -t -subtitle \"by Admin\" -s levelup Welcome to the server!").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /broadcast -c #FF5733 -p [ALERT] -s bell Important announcement!").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /broadcast -a -c yellow -i PvP enabled in 30 seconds").color(NamedTextColor.GRAY));
    }

    private static class BroadcastOptions {
        TextColor color = null;
        String prefix = null;
        boolean bold = false;
        boolean italic = false;
        boolean underlined = false;
        Sound sound = null;
        boolean displayAsTitle = false;
        boolean displayAsActionBar = false;
        String subtitle = null;
        long fadeIn = 500;  // milliseconds
        long stay = 3000;   // milliseconds
        long fadeOut = 1000; // milliseconds
        boolean centered = false;
    }
}