package com.anynom39.anyares.command;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.api.AnyAresAPI;
import com.anynom39.anyares.clipboard.ClipboardObject;
import com.anynom39.anyares.manager.ClipboardManager;
import com.anynom39.anyares.manager.SelectionManager;
import com.anynom39.anyares.manager.TaskEngine;
import com.anynom39.anyares.operation.CopyOperation;
import com.anynom39.anyares.operation.SetOperation;
import com.anynom39.anyares.selection.Selection;
import com.anynom39.anyares.util.BlockPatternParser;
import com.anynom39.anyares.util.BlockPatternParser.WeightedBlockData;
import com.anynom39.anyares.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Level;

public class CutCommand implements CommandExecutor {
    private final AnyARES_Core plugin;

    public CutCommand(AnyARES_Core plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "&cThis command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("anyares.clipboard.cut")) {
            MessageUtil.sendMessage(player, "&cYou don't have permission to use this command.");
            return true;
        }

        if (!AnyAresAPI.isAvailable()) {
            MessageUtil.sendMessage(player, "&cAnyARES-Core is not available.");
            plugin.getLogger().warning("/cut used by " + player.getName() + " but API is unavailable.");
            return true;
        }

        SelectionManager selectionManager;
        ClipboardManager clipboardManager;
        TaskEngine taskEngine;
        try {
            selectionManager = AnyAresAPI.getSelectionManager();
            clipboardManager = AnyAresAPI.getClipboardManager();
            taskEngine = AnyAresAPI.getTaskEngine();
        } catch (IllegalStateException e) {
            MessageUtil.sendMessage(player, "&cError accessing AnyARES-Core components: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "Failed to access Core components for /cut by " + player.getName(), e);
            return true;
        }

        Selection selection = selectionManager.getActiveSelection(player);
        if (selection == null) {
            MessageUtil.sendMessage(player, "&cYou must make a complete selection first.");
            return true;
        }

        CopyOperation copyOp = new CopyOperation(player, selection, clipboardManager, true);
        taskEngine.submitOperation(copyOp)
                .thenAcceptAsync(emptyChangeSet -> {
                    ClipboardObject clipboard = clipboardManager.getPlayerClipboard(player);
                    if (clipboard != null) {
                        MessageUtil.sendMessage(player, "&aCut " + clipboard.getVolume() + " blocks (" + selection.getTypeName() + ") to clipboard. Clearing original area...");

                        String airPatternString = Material.AIR.getKey().toString();
                        try {
                            List<WeightedBlockData> airPatternList =
                                    BlockPatternParser.parseComplexPatternForReplacement(airPatternString);
                            if (airPatternList.isEmpty()) {
                                MessageUtil.sendMessage(player, "&cInternal error: Could not parse 'air' for cut. Area not cleared.");
                                plugin.getLogger().warning("CutCommand: Failed to parse 'minecraft:air' pattern.");
                                return;
                            }
                            SetOperation clearOp = new SetOperation(player, selection, airPatternList, airPatternString);
                            taskEngine.submitOperation(clearOp);
                        } catch (IllegalArgumentException e) {
                            MessageUtil.sendMessage(player, "&cInternal error during cut (clear phase): " + e.getMessage());
                            plugin.getLogger().log(Level.SEVERE, "CutCommand: Error parsing 'air' pattern for SetOperation.", e);
                        }
                    } else {
                        MessageUtil.sendMessage(player, "&cFailed to copy selection for cut. Original area not cleared.");
                    }
                }, plugin.getServer().getScheduler().getMainThreadExecutor(plugin))
                .exceptionally(throwable -> {
                    MessageUtil.sendMessage(player, "&cError during copy phase of cut: " + throwable.getMessage());
                    plugin.getLogger().log(Level.WARNING, "CutCommand: CopyOperation failed.", throwable);
                    return null;
                });

        return true;
    }
}