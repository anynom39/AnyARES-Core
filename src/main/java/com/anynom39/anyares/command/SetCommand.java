package com.anynom39.anyares.command;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.api.AnyAresAPI;
import com.anynom39.anyares.manager.SelectionManager;
import com.anynom39.anyares.manager.TaskEngine;
import com.anynom39.anyares.operation.SetOperation;
import com.anynom39.anyares.selection.Selection;
import com.anynom39.anyares.util.BlockPatternParser;
import com.anynom39.anyares.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Level;

public class SetCommand implements CommandExecutor {

    private final AnyARES_Core plugin;

    public SetCommand(AnyARES_Core plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "&cThis command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("anyares.operation.set")) {
            MessageUtil.sendMessage(player, "&cYou don't have permission to use this command.");
            return true;
        }

        if (!AnyAresAPI.isAvailable()) {
            MessageUtil.sendMessage(player, "&cAnyARES-Core is not available. Please contact an administrator.");
            plugin.getLogger().warning("SetCommand used by " + player.getName() + " but AnyAresAPI is not available.");
            return true;
        }

        SelectionManager selectionManager;
        TaskEngine taskEngine;
        try {
            selectionManager = AnyAresAPI.getSelectionManager();
            taskEngine = AnyAresAPI.getTaskEngine();
        } catch (IllegalStateException e) {
            MessageUtil.sendMessage(player, "&cError accessing AnyARES-Core components: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "Failed to access Core components for /set by " + player.getName(), e);
            return true;
        }

        Selection selection = selectionManager.getActiveSelection(player);
        if (selection == null) {
            MessageUtil.sendMessage(player, "&cYou must make a complete selection first.");
            return true;
        }

        if (args.length == 0) {
            MessageUtil.sendMessage(player, "&cUsage: /" + label + " <pattern>");
            MessageUtil.sendMessage(player, "&cExample: /" + label + " oak_log[axis=y]");
            MessageUtil.sendMessage(player, "&cExample: /" + label + " 50%stone,50%dirt");
            return true;
        }

        String patternString = String.join("", args);
        List<BlockPatternParser.WeightedBlockData> patternList;

        try {
            patternList = BlockPatternParser.parseComplexPatternForReplacement(patternString);
            if (patternList.isEmpty()) {
                MessageUtil.sendMessage(player, "&cPattern resulted in no valid blocks: &e" + patternString);
                return true;
            }
        } catch (IllegalArgumentException e) {
            MessageUtil.sendMessage(player, "&cInvalid block pattern: &e" + patternString);
            MessageUtil.sendMessage(player, "&cError: &7" + e.getMessage());
            return true;
        }

        SetOperation setOperation = new SetOperation(player, selection, patternList, patternString);
        taskEngine.submitOperation(setOperation);

        return true;
    }
}