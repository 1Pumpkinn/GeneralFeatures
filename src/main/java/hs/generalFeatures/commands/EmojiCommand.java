package hs.generalFeatures.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class EmojiCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Map<String, String> emojiMap = ChatReactions.getEmojiMap();

        // Group emojis by category
        Map<String, List<Map.Entry<String, String>>> categories = new LinkedHashMap<>();
        categories.put("Faces & Emotions", new ArrayList<>());
        categories.put("Hearts & Symbols", new ArrayList<>());
        categories.put("Gaming & Items", new ArrayList<>());
        categories.put("Nature & Weather", new ArrayList<>());
        categories.put("Animals & Mobs", new ArrayList<>());
        categories.put("Actions & Gestures", new ArrayList<>());

        for (Map.Entry<String, String> entry : emojiMap.entrySet()) {
            String key = entry.getKey();

            if (isFaceEmoji(key)) {
                categories.get("Faces & Emotions").add(entry);
            } else if (isHeartOrSymbol(key)) {
                categories.get("Hearts & Symbols").add(entry);
            } else if (isGamingItem(key)) {
                categories.get("Gaming & Items").add(entry);
            } else if (isNatureWeather(key)) {
                categories.get("Nature & Weather").add(entry);
            } else if (isAnimal(key)) {
                categories.get("Animals & Mobs").add(entry);
            } else if (isAction(key)) {
                categories.get("Actions & Gestures").add(entry);
            }
        }

        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Available Emojis", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("Type these in chat to use them!", NamedTextColor.YELLOW));
        sender.sendMessage(Component.empty());

        for (Map.Entry<String, List<Map.Entry<String, String>>> category : categories.entrySet()) {
            if (category.getValue().isEmpty()) continue;

            sender.sendMessage(Component.text(category.getKey(), NamedTextColor.AQUA, TextDecoration.BOLD));

            StringBuilder line = new StringBuilder();
            for (Map.Entry<String, String> emoji : category.getValue()) {
                line.append(emoji.getKey()).append(" → ").append(emoji.getValue()).append("  ");
                if (line.length() > 50) {
                    sender.sendMessage(Component.text("  " + line.toString(), NamedTextColor.GRAY));
                    line = new StringBuilder();
                }
            }
            if (line.length() > 0) {
                sender.sendMessage(Component.text("  " + line.toString(), NamedTextColor.GRAY));
            }
            sender.sendMessage(Component.empty());
        }

        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));

        return true;
    }

    private boolean isFaceEmoji(String key) {
        return key.matches("[:;][)D(OP/*|]") ||
                key.contains(":laugh:") || key.contains(":cry:") ||
                key.contains(":angry:") || key.contains(":cool:") ||
                key.contains(":thinking:") || key.contains(":devil:") ||
                key.contains(":angel:");
    }

    private boolean isHeartOrSymbol(String key) {
        return key.contains("heart") || key.contains("<3") ||
                key.contains("star") || key.contains("check") ||
                key.contains(":x:") || key.contains("warning") ||
                key.contains("info") || key.contains("question");
    }

    private boolean isGamingItem(String key) {
        return key.contains("sword") || key.contains("bow") ||
                key.contains("shield") || key.contains("crown") ||
                key.contains("trophy") || key.contains("gem") ||
                key.contains("coin") || key.contains("potion") ||
                key.contains("book") || key.contains("pickaxe") ||
                key.contains("axe") || key.contains("apple") ||
                key.contains("bread") || key.contains("meat") ||
                key.contains("cake");
    }

    private boolean isNatureWeather(String key) {
        return key.contains("rainbow") || key.contains("sun") ||
                key.contains("moon") || key.contains("lightning") ||
                key.contains("snowflake") || key.contains("tree") ||
                key.contains("flower") || key.contains("fire");
    }

    private boolean isAnimal(String key) {
        return key.contains("pig") || key.contains("cow") ||
                key.contains("chicken") || key.contains("sheep") ||
                key.contains("wolf") || key.contains("cat") ||
                key.contains("dog") || key.contains("creeper") ||
                key.contains("enderman") || key.contains("dragon") ||
                key.contains("skull") || key.contains("ghost") ||
                key.contains("alien") || key.contains("robot");
    }

    private boolean isAction(String key) {
        return key.contains("eyes") || key.contains("wave") ||
                key.contains("clap") || key.contains("punch") ||
                key.contains("shrug") || key.contains("thumbs") ||
                key.contains("music");
    }
}