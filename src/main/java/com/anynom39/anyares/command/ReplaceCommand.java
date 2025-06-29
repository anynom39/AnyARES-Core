package com.anynom39.anyares.command;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.api.AnyAresAPI;
import com.anynom39.anyares.manager.SelectionManager;
import com.anynom39.anyares.manager.TaskEngine;
import com.anynom39.anyares.operation.ReplaceOperation;
import com.anynom39.anyares.selection.Selection;
import com.anynom39.anyares.util.BlockPatternParser;
import com.anynom39.anyares.util.BlockPatternParser.MaskEntry;
import com.anynom39.anyares.util.BlockPatternParser.WeightedBlockData;
import com.anynom39.anyares.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Level;

public class ReplaceCommand implements CommandExecutor {

    private final AnyARES_Core plugin;

    public ReplaceCommand(AnyARES_Core plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "&cThis command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("anyares.operation.replace")) {
            MessageUtil.sendMessage(player, "&cYou don't have permission to use this command.");
            return true;
        }

        if (!AnyAresAPI.isAvailable()) {
            MessageUtil.sendMessage(player, "&cAnyARES-Core is not available. Please contact an administrator.");
            plugin.getLogger().warning("/replace used by " + player.getName() + " but AnyAresAPI is unavailable.");
            return true;
        }

        SelectionManager selectionManager;
        TaskEngine taskEngine;
        try {
            selectionManager = AnyAresAPI.getSelectionManager();
            taskEngine = AnyAresAPI.getTaskEngine();
        } catch (IllegalStateException e) {
            MessageUtil.sendMessage(player, "&cError accessing AnyARES-Core components: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "Failed to access Core components for /replace by " + player.getName(), e);
            return true;
        }

        Selection selection = selectionManager.getActiveSelection(player);
        if (selection == null) {
            MessageUtil.sendMessage(player, "&cYou must make a complete selection first.");
            return true;
        }

        if (args.length < 2) {
            MessageUtil.sendMessage(player, "&cUsage: /" + label + " <from_pattern> <to_pattern>");
            MessageUtil.sendMessage(player, "&cExample: /" + label + " grass_block stone");
            MessageUtil.sendMessage(player, "&cExample: /" + label + " \"*,!water\" dirt");
            return true;
        }

        String fromPatternStr = args[0];
        String toPatternStr = args[1];

        List<MaskEntry> fromMaskRules;
        List<WeightedBlockData> toPatternList;

        try {
            fromMaskRules = BlockPatternParser.parseMaskPattern(fromPatternStr);
        } catch (IllegalArgumentException e) {
            MessageUtil.sendMessage(player, "&cInvalid 'from' pattern: &e" + fromPatternStr);
            MessageUtil.sendMessage(player, "&cError: &7" + e.getMessage());
            return true;
        }

        try {
            toPatternList = BlockPatternParser.parseComplexPatternForReplacement(toPatternStr);
            if (toPatternList.isEmpty()) {
                MessageUtil.sendMessage(player, "&c'To' pattern resulted in no valid blocks: &e" + toPatternStr);
                return true;
            }
        } catch (IllegalArgumentException e) {
            MessageUtil.sendMessage(player, "&cInvalid 'to' pattern: &e" + toPatternStr);
            MessageUtil.sendMessage(player, "&cError: &7" + e.getMessage());
            return true;
        }

        ReplaceOperation replaceOp = new ReplaceOperation(
                player,
                selection,
                fromMaskRules,
                toPatternList,
                fromPatternStr,
                toPatternStr
        );
        taskEngine.submitOperation(replaceOp);

        return true;
    }
}