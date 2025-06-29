package com.anynom39.anyares.command;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.api.AnyAresAPI;
import com.anynom39.anyares.manager.SelectionManager;
import com.anynom39.anyares.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class PosCommand implements CommandExecutor {

    private final AnyARES_Core plugin;
    private final int pointIndexToSet;

    public PosCommand(AnyARES_Core plugin, int pointIndexToSet) {
        this.plugin = plugin;
        if (pointIndexToSet != 0 && pointIndexToSet != 1) {
            throw new IllegalArgumentException("Point index must be 0 or 1 for PosCommand");
        }
        this.pointIndexToSet = pointIndexToSet;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "&cThis command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("anyares.selection.pos")) {
            MessageUtil.sendMessage(player, "&cYou don't have permission to use this command.");
            return true;
        }

        if (!AnyAresAPI.isAvailable()) {
            MessageUtil.sendMessage(player, "&cAnyARES-Core is not available. Please contact an administrator.");
            plugin.getLogger().warning("PosCommand used by " + player.getName() + " but AnyAresAPI is not available.");
            return true;
        }

        SelectionManager selectionManager;
        try {
            selectionManager = AnyAresAPI.getSelectionManager();
        } catch (IllegalStateException e) {
            MessageUtil.sendMessage(player, "&cError accessing AnyARES-Core: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "Failed to access SelectionManager for /pos command by " + player.getName(), e);
            return true;
        }

        Location targetLocation;
        if (args.length == 0) {
            Block targetBlock = player.getTargetBlockExact(5);
            if (targetBlock != null) {
                targetLocation = targetBlock.getLocation();
            } else {
                targetLocation = player.getLocation().getBlock().getLocation();
            }
        } else if (args.length == 3) {
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                targetLocation = new Location(player.getWorld(), x, y, z);
            } catch (NumberFormatException e) {
                MessageUtil.sendMessage(player, "&cInvalid coordinates. Usage: /" + label + " [x y z]");
                return true;
            }
        } else {
            MessageUtil.sendMessage(player, "&cInvalid arguments. Usage: /" + label + " [x y z]");
            return true;
        }

        selectionManager.setPlayerDefiningPoint(player, pointIndexToSet, targetLocation);

        return true;
    }
}