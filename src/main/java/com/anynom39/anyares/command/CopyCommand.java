package com.anynom39.anyares.command;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.api.AnyAresAPI;
import com.anynom39.anyares.clipboard.ClipboardObject;
import com.anynom39.anyares.manager.ClipboardManager;
import com.anynom39.anyares.manager.SelectionManager;
import com.anynom39.anyares.manager.TaskEngine;
import com.anynom39.anyares.operation.CopyOperation;
import com.anynom39.anyares.selection.Selection;
import com.anynom39.anyares.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class CopyCommand implements CommandExecutor {

    private final AnyARES_Core plugin;

    public CopyCommand(AnyARES_Core plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "&cThis command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("anyares.clipboard.copy")) {
            MessageUtil.sendMessage(player, "&cYou don't have permission to use this command.");
            return true;
        }

        if (!AnyAresAPI.isAvailable()) {
            MessageUtil.sendMessage(player, "&cAnyARES-Core is not available.");
            plugin.getLogger().warning("/copy used by " + player.getName() + " but API is unavailable.");
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
            plugin.getLogger().log(Level.SEVERE, "Failed to access Core components for /copy by " + player.getName(), e);
            return true;
        }


        Selection selection = selectionManager.getActiveSelection(player);
        if (selection == null) {
            MessageUtil.sendMessage(player, "&cYou must make a complete selection first.");
            return true;
        }

        CopyOperation copyOp = new CopyOperation(player, selection, clipboardManager, false);
        taskEngine.submitOperation(copyOp)
                .whenComplete((changeSet, throwable) -> {
                    if (throwable == null) {
                        ClipboardObject clipboard = clipboardManager.getPlayerClipboard(player);
                        if (clipboard != null) {
                            MessageUtil.sendMessage(player, "&aCopied " + clipboard.getVolume() + " blocks (" + selection.getTypeName() + ") to your clipboard.");
                        } else {
                            MessageUtil.sendMessage(player, "&cCopy operation completed, but clipboard seems empty.");
                        }
                    }
                });

        return true;
    }
}