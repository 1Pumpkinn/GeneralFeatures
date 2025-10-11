package hs.generalFeatures.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ChatReactions implements Listener {

    private static final Map<String, String> EMOJI_MAP = new HashMap<>();
    private static final Map<String, TextColor> EMOJI_COLORS = new HashMap<>();

    static {
        // Emoticons - faces
        EMOJI_MAP.put(":)", "☺");
        EMOJI_MAP.put(":(", "☹");
        EMOJI_MAP.put(":D", "😃");
        EMOJI_MAP.put(":P", "😛");
        EMOJI_MAP.put(";)", "😉");
        EMOJI_MAP.put(":O", "😮");
        EMOJI_MAP.put(":/", "😕");
        EMOJI_MAP.put(":|", "😐");
        EMOJI_MAP.put(":*", "😘");
        EMOJI_MAP.put("<3", "❤");
        EMOJI_MAP.put("</3", "💔");

        // Named emojis
        EMOJI_MAP.put(":heart:", "❤");
        EMOJI_MAP.put(":star:", "⭐");
        EMOJI_MAP.put(":fire:", "🔥");
        EMOJI_MAP.put(":skull:", "💀");
        EMOJI_MAP.put(":thumbsup:", "👍");
        EMOJI_MAP.put(":thumbsdown:", "👎");
        EMOJI_MAP.put(":check:", "✓");
        EMOJI_MAP.put(":x:", "✗");
        EMOJI_MAP.put(":warning:", "⚠");
        EMOJI_MAP.put(":info:", "ℹ");
        EMOJI_MAP.put(":question:", "❓");
        EMOJI_MAP.put(":music:", "♪");
        EMOJI_MAP.put(":sword:", "⚔");
        EMOJI_MAP.put(":bow:", "🏹");
        EMOJI_MAP.put(":shield:", "🛡");
        EMOJI_MAP.put(":crown:", "👑");
        EMOJI_MAP.put(":trophy:", "🏆");
        EMOJI_MAP.put(":gem:", "💎");
        EMOJI_MAP.put(":coin:", "🪙");
        EMOJI_MAP.put(":potion:", "🧪");
        EMOJI_MAP.put(":book:", "📖");
        EMOJI_MAP.put(":pickaxe:", "⛏");
        EMOJI_MAP.put(":axe:", "🪓");
        EMOJI_MAP.put(":apple:", "🍎");
        EMOJI_MAP.put(":bread:", "🍞");
        EMOJI_MAP.put(":meat:", "🍖");
        EMOJI_MAP.put(":cake:", "🎂");
        EMOJI_MAP.put(":rainbow:", "🌈");
        EMOJI_MAP.put(":sun:", "☀");
        EMOJI_MAP.put(":moon:", "🌙");
        EMOJI_MAP.put(":lightning:", "⚡");
        EMOJI_MAP.put(":snowflake:", "❄");
        EMOJI_MAP.put(":tree:", "🌲");
        EMOJI_MAP.put(":flower:", "🌸");
        EMOJI_MAP.put(":eyes:", "👀");
        EMOJI_MAP.put(":wave:", "👋");
        EMOJI_MAP.put(":clap:", "👏");
        EMOJI_MAP.put(":punch:", "👊");
        EMOJI_MAP.put(":shrug:", "🤷");
        EMOJI_MAP.put(":thinking:", "🤔");
        EMOJI_MAP.put(":cool:", "😎");
        EMOJI_MAP.put(":laugh:", "😂");
        EMOJI_MAP.put(":cry:", "😢");
        EMOJI_MAP.put(":angry:", "😠");
        EMOJI_MAP.put(":devil:", "😈");
        EMOJI_MAP.put(":angel:", "😇");
        EMOJI_MAP.put(":ghost:", "👻");
        EMOJI_MAP.put(":alien:", "👽");
        EMOJI_MAP.put(":robot:", "🤖");
        EMOJI_MAP.put(":pig:", "🐷");
        EMOJI_MAP.put(":cow:", "🐮");
        EMOJI_MAP.put(":chicken:", "🐔");
        EMOJI_MAP.put(":sheep:", "🐑");
        EMOJI_MAP.put(":wolf:", "🐺");
        EMOJI_MAP.put(":cat:", "🐱");
        EMOJI_MAP.put(":dog:", "🐶");
        EMOJI_MAP.put(":creeper:", "💥");
        EMOJI_MAP.put(":enderman:", "👾");
        EMOJI_MAP.put(":dragon:", "🐉");

        // Emoji colors
        EMOJI_COLORS.put("❤", TextColor.color(255, 85, 85));
        EMOJI_COLORS.put("💔", TextColor.color(139, 0, 0));
        EMOJI_COLORS.put("⭐", TextColor.color(255, 215, 0));
        EMOJI_COLORS.put("🔥", TextColor.color(255, 140, 0));
        EMOJI_COLORS.put("💀", TextColor.color(200, 200, 200));
        EMOJI_COLORS.put("👍", TextColor.color(255, 215, 0));
        EMOJI_COLORS.put("👎", TextColor.color(200, 0, 0));
        EMOJI_COLORS.put("✓", TextColor.color(85, 255, 85));
        EMOJI_COLORS.put("✗", TextColor.color(255, 85, 85));
        EMOJI_COLORS.put("⚠", TextColor.color(255, 255, 0));
        EMOJI_COLORS.put("ℹ", TextColor.color(85, 170, 255));
        EMOJI_COLORS.put("❓", TextColor.color(255, 255, 85));
        EMOJI_COLORS.put("⚔", TextColor.color(192, 192, 192));
        EMOJI_COLORS.put("👑", TextColor.color(255, 215, 0));
        EMOJI_COLORS.put("🏆", TextColor.color(255, 215, 0));
        EMOJI_COLORS.put("💎", TextColor.color(85, 255, 255));
        EMOJI_COLORS.put("⚡", TextColor.color(255, 255, 85));
        EMOJI_COLORS.put("❄", TextColor.color(175, 238, 238));
        EMOJI_COLORS.put("🌲", TextColor.color(34, 139, 34));
        EMOJI_COLORS.put("🌸", TextColor.color(255, 182, 193));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Component message = event.message();

        // Process emoji replacements
        Component processedMessage = processEmojis(message);

        event.message(processedMessage);
    }

    private Component processEmojis(Component message) {
        Component result = message;

        // Replace each emoji pattern
        for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
            String pattern = entry.getKey();
            String emoji = entry.getValue();
            TextColor color = EMOJI_COLORS.getOrDefault(emoji, NamedTextColor.WHITE);

            // Create the replacement component with color
            Component replacement = Component.text(emoji).color(color);

            // Use exact matching for the pattern
            TextReplacementConfig config = TextReplacementConfig.builder()
                    .matchLiteral(pattern)
                    .replacement(replacement)
                    .build();

            result = result.replaceText(config);
        }

        return result;
    }

    public static Map<String, String> getEmojiMap() {
        return new HashMap<>(EMOJI_MAP);
    }
}