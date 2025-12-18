package saturn.ElementSmpManager.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import saturn.ElementSmpManager.commands.MaceCommand;

public class DisableMaceUsage implements Listener {

    private final MaceCommand maceCommand;

    public DisableMaceUsage(MaceCommand maceCommand) {
        this.maceCommand = maceCommand;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMaceAttack(EntityDamageByEntityEvent event) {
        // Check if mace is disabled
        if (!maceCommand.isMaceDisabled()) {
            return;
        }

        // Check if damager is a player
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        // Check if player is holding a mace
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.MACE) {
            event.setCancelled(true);
            player.sendMessage(Component.text("✖ Maces are currently disabled!").color(NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMaceInteract(PlayerInteractEvent event) {
        // Check if mace is disabled
        if (!maceCommand.isMaceDisabled()) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if player is trying to use a mace
        if (item != null && item.getType() == Material.MACE) {
            event.setCancelled(true);
            player.sendMessage(Component.text("✖ Maces are currently disabled!").color(NamedTextColor.RED));
        }
    }
}