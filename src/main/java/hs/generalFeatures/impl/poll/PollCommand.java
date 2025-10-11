package hs.generalFeatures.impl.poll;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class PollCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final PollManager pollManager;

    public PollCommand(JavaPlugin plugin, PollManager pollManager) {
        this.plugin = plugin;
        this.pollManager = pollManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create" -> {
                if (!sender.hasPermission("generalfeatures.poll.create")) {
                    sender.sendMessage(Component.text("You don't have permission to create polls.").color(NamedTextColor.RED));
                    return true;
                }
                return handleCreate(sender, args);
            }
            case "vote" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can vote in polls.").color(NamedTextColor.RED));
                    return true;
                }
                return handleVote(player, args);
            }
            case "results" -> {
                return handleResults(sender);
            }
            case "end" -> {
                if (!sender.hasPermission("generalfeatures.poll.create")) {
                    sender.sendMessage(Component.text("You don't have permission to end polls.").color(NamedTextColor.RED));
                    return true;
                }
                return handleEnd(sender);
            }
            case "status" -> {
                return handleStatus(sender);
            }
            default -> {
                sendUsage(sender);
                return true;
            }
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (pollManager.hasActivePoll()) {
            sender.sendMessage(Component.text("There is already an active poll! End it first with /poll end").color(NamedTextColor.RED));
            return true;
        }

        // Format: /poll create <duration> <question> | <option1> | <option2> | ...
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /poll create <duration> <question> | <option1> | <option2> | ...").color(NamedTextColor.RED));
            sender.sendMessage(Component.text("Example: /poll create 60 What's your favorite food? | Pizza | Tacos | Burgers").color(NamedTextColor.GRAY));
            return true;
        }

        int duration;
        try {
            duration = Integer.parseInt(args[1]);
            if (duration < 10 || duration > 3600) {
                sender.sendMessage(Component.text("Duration must be between 10 and 3600 seconds.").color(NamedTextColor.RED));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid duration. Please provide a number in seconds.").color(NamedTextColor.RED));
            return true;
        }

        // Join remaining args and split by |
        String fullText = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String[] parts = fullText.split("\\|");

        if (parts.length < 3) {
            sender.sendMessage(Component.text("You must provide a question and at least 2 options separated by |").color(NamedTextColor.RED));
            return true;
        }

        String question = parts[0].trim();
        String[] options = new String[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            options[i - 1] = parts[i].trim();
        }

        if (options.length > 10) {
            sender.sendMessage(Component.text("Maximum 10 options allowed.").color(NamedTextColor.RED));
            return true;
        }

        // Create and start the poll
        Player creator = sender instanceof Player ? (Player) sender : null;
        hs.generalFeatures.poll.Poll poll = pollManager.createPoll(question, Arrays.asList(options), duration,
                creator != null ? creator.getUniqueId() : null);

        // Broadcast the poll
        Bukkit.broadcast(poll.createPollMessage());

        // Schedule auto-end
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pollManager.getActivePoll() == poll && !poll.hasEnded()) {
                    poll.end();
                    Bukkit.broadcast(Component.text("The poll has ended!", NamedTextColor.GOLD));
                    Bukkit.broadcast(poll.createResultsMessage());
                    pollManager.clearPoll();
                }
            }
        }.runTaskLater(plugin, duration * 20L);

        sender.sendMessage(Component.text("Poll created successfully!", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleVote(Player player, String[] args) {
        hs.generalFeatures.poll.Poll poll = pollManager.getActivePoll();

        if (poll == null) {
            player.sendMessage(Component.text("There is no active poll.", NamedTextColor.RED));
            return true;
        }

        if (poll.hasEnded()) {
            player.sendMessage(Component.text("This poll has ended.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /poll vote <option number>").color(NamedTextColor.RED));
            return true;
        }

        int optionNumber;
        try {
            optionNumber = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid option number.").color(NamedTextColor.RED));
            return true;
        }

        if (optionNumber < 1 || optionNumber > poll.getOptions().size()) {
            player.sendMessage(Component.text("Invalid option number. Choose between 1 and " + poll.getOptions().size()).color(NamedTextColor.RED));
            return true;
        }

        String option = poll.getOptions().get(optionNumber - 1);
        String previousVote = poll.getPlayerVote(player.getUniqueId());

        if (poll.vote(player.getUniqueId(), option)) {
            if (previousVote != null) {
                player.sendMessage(Component.text("You changed your vote to: ", NamedTextColor.GREEN)
                        .append(Component.text(option, NamedTextColor.YELLOW)));
            } else {
                player.sendMessage(Component.text("You voted for: ", NamedTextColor.GREEN)
                        .append(Component.text(option, NamedTextColor.YELLOW)));
            }
            return true;
        } else {
            player.sendMessage(Component.text("Failed to vote.", NamedTextColor.RED));
            return true;
        }
    }

    private boolean handleResults(CommandSender sender) {
        hs.generalFeatures.poll.Poll poll = pollManager.getActivePoll();

        if (poll == null) {
            sender.sendMessage(Component.text("There is no active poll.", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(poll.createResultsMessage());
        return true;
    }

    private boolean handleEnd(CommandSender sender) {
        hs.generalFeatures.poll.Poll poll = pollManager.getActivePoll();

        if (poll == null) {
            sender.sendMessage(Component.text("There is no active poll.", NamedTextColor.RED));
            return true;
        }

        poll.end();
        Bukkit.broadcast(Component.text("The poll has been ended by " + sender.getName() + "!", NamedTextColor.GOLD));
        Bukkit.broadcast(poll.createResultsMessage());
        pollManager.clearPoll();

        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        hs.generalFeatures.poll.Poll poll = pollManager.getActivePoll();

        if (poll == null) {
            sender.sendMessage(Component.text("There is no active poll.", NamedTextColor.RED));
            return true;
        }

        if (poll.hasEnded()) {
            sender.sendMessage(Component.text("The current poll has ended.", NamedTextColor.YELLOW));
            sender.sendMessage(poll.createResultsMessage());
        } else {
            sender.sendMessage(poll.createPollMessage());
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("=== Poll Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/poll create <duration> <question> | <option1> | <option2> | ...", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Create a new poll (ops only)", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/poll vote <number>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Vote for an option", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/poll results", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  View current poll results", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/poll status", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  View the current poll", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/poll end", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  End the current poll (ops only)", NamedTextColor.GRAY));
    }
}