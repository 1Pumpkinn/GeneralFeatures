package hs.generalFeatures.poll;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;

public class Poll {
    private final String question;
    private final List<String> options;
    private final Map<String, Set<UUID>> votes; // option -> set of player UUIDs
    private final long endTime;
    private final UUID creatorId;
    private boolean ended = false;

    public Poll(String question, List<String> options, int durationSeconds, UUID creatorId) {
        this.question = question;
        this.options = options;
        this.votes = new HashMap<>();
        this.creatorId = creatorId;

        // Initialize vote tracking for each option
        for (String option : options) {
            votes.put(option, new HashSet<>());
        }

        this.endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getOptions() {
        return options;
    }

    public UUID getCreatorId() {
        return creatorId;
    }

    public boolean hasEnded() {
        return ended || System.currentTimeMillis() >= endTime;
    }

    public void end() {
        this.ended = true;
    }

    public long getTimeRemaining() {
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    public boolean hasVoted(UUID playerId) {
        for (Set<UUID> voters : votes.values()) {
            if (voters.contains(playerId)) {
                return true;
            }
        }
        return false;
    }

    public boolean vote(UUID playerId, String option) {
        if (hasEnded()) {
            return false;
        }

        if (!options.contains(option)) {
            return false;
        }

        // Remove previous vote if exists
        for (Set<UUID> voters : votes.values()) {
            voters.remove(playerId);
        }

        // Add new vote
        votes.get(option).add(playerId);
        return true;
    }

    public String getPlayerVote(UUID playerId) {
        for (Map.Entry<String, Set<UUID>> entry : votes.entrySet()) {
            if (entry.getValue().contains(playerId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Map<String, Integer> getResults() {
        Map<String, Integer> results = new HashMap<>();
        for (Map.Entry<String, Set<UUID>> entry : votes.entrySet()) {
            results.put(entry.getKey(), entry.getValue().size());
        }
        return results;
    }

    public int getTotalVotes() {
        int total = 0;
        for (Set<UUID> voters : votes.values()) {
            total += voters.size();
        }
        return total;
    }

    public Component createPollMessage() {
        Component message = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD)
                .append(Component.newline())
                .append(Component.text("📊 POLL: ", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(question, NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.newline());

        // Add options with click functionality
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            String optionNumber = String.valueOf(i + 1);

            Component optionComponent = Component.text("  [" + optionNumber + "] ", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .append(Component.text(option, NamedTextColor.WHITE))
                    .clickEvent(ClickEvent.runCommand("/poll vote " + optionNumber))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to vote for: " + option, NamedTextColor.GREEN)));

            message = message.append(optionComponent).append(Component.newline());
        }

        message = message.append(Component.newline())
                .append(Component.text("Click an option to vote or use /poll vote <number>", NamedTextColor.GRAY, TextDecoration.ITALIC))
                .append(Component.newline())
                .append(Component.text("Time remaining: ", NamedTextColor.GRAY))
                .append(Component.text(formatTimeRemaining(), NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));

        return message;
    }

    public Component createResultsMessage() {
        Map<String, Integer> results = getResults();
        int totalVotes = getTotalVotes();

        Component message = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD)
                .append(Component.newline())
                .append(Component.text("📊 POLL RESULTS: ", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(question, NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.newline());

        // Sort options by vote count
        List<Map.Entry<String, Integer>> sortedResults = new ArrayList<>(results.entrySet());
        sortedResults.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (Map.Entry<String, Integer> entry : sortedResults) {
            String option = entry.getKey();
            int voteCount = entry.getValue();
            double percentage = totalVotes > 0 ? (voteCount * 100.0 / totalVotes) : 0;

            // Create progress bar
            int barLength = 20;
            int filledBars = (int) Math.round(percentage / 5.0); // 5% per bar
            String progressBar = "█".repeat(Math.max(0, filledBars)) +
                    "░".repeat(Math.max(0, barLength - filledBars));

            message = message.append(Component.text("  " + option, NamedTextColor.WHITE, TextDecoration.BOLD))
                    .append(Component.newline())
                    .append(Component.text("  ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(progressBar, NamedTextColor.GREEN))
                    .append(Component.text(String.format(" %.1f%% (%d votes)", percentage, voteCount), NamedTextColor.GRAY))
                    .append(Component.newline());
        }

        message = message.append(Component.newline())
                .append(Component.text("Total votes: ", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(totalVotes), NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));

        return message;
    }

    private String formatTimeRemaining() {
        long seconds = getTimeRemaining() / 1000;
        if (seconds >= 60) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return minutes + "m " + remainingSeconds + "s";
        }
        return seconds + "s";
    }
}