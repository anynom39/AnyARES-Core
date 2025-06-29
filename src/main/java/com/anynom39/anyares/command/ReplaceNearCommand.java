package com.anynom39.anyares.command;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.api.AnyAresAPI;
import com.anynom39.anyares.manager.TaskEngine;
import com.anynom39.anyares.operation.ReplaceOperation;
import com.anynom39.anyares.selection.CuboidSelection;
import com.anynom39.anyares.util.BlockPatternParser;
import com.anynom39.anyares.util.BlockPatternParser.MaskEntry;
import com.anynom39.anyares.util.BlockPatternParser.WeightedBlockData;
import com.anynom39.anyares.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Level;

public class ReplaceNearCommand implements CommandExecutor {

    private final AnyARES_Core plugin;

    public ReplaceNearCommand(AnyARES_Core plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "&cThis command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("anyares.operation.replacenear")) {
            MessageUtil.sendMessage(player, "&cYou don't have permission to use this command.");
            return true;
        }

        if (!AnyAresAPI.isAvailable()) {
            MessageUtil.sendMessage(player, "&cAnyARES-Core is not available.");
            plugin.getLogger().warning("/replacenear used by " + player.getName() + " but API is unavailable.");
            return true;
        }

        TaskEngine taskEngine;
        try {
            taskEngine = AnyAresAPI.getTaskEngine();
        } catch (IllegalStateException e) {
            MessageUtil.sendMessage(player, "&cError accessing AnyARES-Core TaskEngine: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "Failed to access TaskEngine for /replacenear by " + player.getName(), e);
            return true;
        }


        if (args.length < 3) {
            MessageUtil.sendMessage(player, "&cUsage: /" + label + " <radius> <from_pattern> <to_pattern>");
            return true;
        }

        double radius;
        try {
            radius = Double.parseDouble(args[0]);
            if (radius <= 0 || radius > 64) {
                MessageUtil.sendMessage(player, "&cRadius must be > 0 and <= 64.");
                return true;
            }
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(player, "&cInvalid radius: &e" + args[0]);
            return true;
        }

        String fromPatternStr = args[1];
        String toPatternStr = args[2];
        List<MaskEntry> fromMaskRules;
        List<WeightedBlockData> toPatternList;

        try {
            fromMaskRules = BlockPatternParser.parseMaskPattern(fromPatternStr);
        } catch (IllegalArgumentException e) {
            MessageUtil.sendMessage(player, "&cInvalid 'from' pattern: &e" + fromPatternStr + "&c. " + e.getMessage());
            return true;
        }
        try {
            toPatternList = BlockPatternParser.parseComplexPatternForReplacement(toPatternStr);
            if (toPatternList.isEmpty()) {
                MessageUtil.sendMessage(player, "&c'To' pattern resulted in no valid blocks: &e" + toPatternStr);
                return true;
            }
        } catch (IllegalArgumentException e) {
            MessageUtil.sendMessage(player, "&cInvalid 'to' pattern: &e" + toPatternStr + "&c. " + e.getMessage());
            return true;
        }

        Location sphereCenter = player.getLocation();
        int intRadius = (int) Math.ceil(radius);
        Location pos1 = sphereCenter.clone().subtract(intRadius, intRadius, intRadius);
        Location pos2 = sphereCenter.clone().add(intRadius, intRadius, intRadius);
        CuboidSelection sphereBoundingBox = new CuboidSelection(player.getWorld(), pos1, pos2);

        ReplaceOperation replaceNearOp = new ReplaceOperation(
                player, sphereBoundingBox, fromMaskRules, toPatternList,
                fromPatternStr, toPatternStr, sphereCenter, radius
        );
        taskEngine.submitOperation(replaceNearOp);
        return true;
    }
}