package hs.generalFeatures.restrictions;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class EnableNDisableCommand implements CommandExecutor {

    private final DisablePotTwo disablePotTwo;
    private final DisableFireworks disableFireworks;
    private final DisablePearls disablePearls;

    public EnableNDisableCommand(DisablePotTwo disablePotTwo, DisableFireworks disableFireworks, DisablePearls disablePearls) {
        this.disablePotTwo = disablePotTwo;
        this.disableFireworks = disableFireworks;
        this.disablePearls = disablePearls;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("generalfeatures.restrictions")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /restrictions <enable|disable> <potions|fireworks|pearls|all>");
            return true;
        }

        String action = args[0].toLowerCase();
        String restriction = args[1].toLowerCase();

        if (!action.equals("enable") && !action.equals("disable")) {
            sender.sendMessage(ChatColor.RED + "Usage: /restrictions <enable|disable> <potions|fireworks|pearls|all>");
            return true;
        }

        boolean enable = action.equals("enable");

        switch (restriction) {
            case "potions":
            case "potion":
            case "pots":
                disablePotTwo.setEnabled(enable);
                Bukkit.broadcastMessage(ChatColor.GOLD + "Level 2+ potions restriction has been " +
                        (enable ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.GOLD + "!");
                break;

            case "fireworks":
            case "firework":
                disableFireworks.setEnabled(enable);
                Bukkit.broadcastMessage(ChatColor.GOLD + "Firework dispenser/dropper restriction has been " +
                        (enable ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.GOLD + "!");
                break;

            case "pearls":
            case "pearl":
            case "enderpearls":
            case "enderpearl":
                disablePearls.setEnabled(enable);
                Bukkit.broadcastMessage(ChatColor.GOLD + "Ender pearl dispenser/dropper restriction has been " +
                        (enable ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.GOLD + "!");
                break;

            case "all":
                disablePotTwo.setEnabled(enable);
                disableFireworks.setEnabled(enable);
                disablePearls.setEnabled(enable);
                Bukkit.broadcastMessage(ChatColor.GOLD + "All restrictions have been " +
                        (enable ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.GOLD + "!");
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Invalid restriction type! Use: potions, fireworks, pearls, or all");
                return true;
        }

        return true;
    }
}