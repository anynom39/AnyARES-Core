package com.anynom39.anyares.command;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.api.AnyAresAPI;
import com.anynom39.anyares.clipboard.ClipboardObject;
import com.anynom39.anyares.manager.ClipboardManager;
import com.anynom39.anyares.manager.TaskEngine;
import com.anynom39.anyares.operation.PasteOperation;
import com.anynom39.anyares.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class PasteCommand implements CommandExecutor {

    private final AnyARES_Core plugin;

    public PasteCommand(AnyARES_Core plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "&cThis command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("anyares.clipboard.paste")) {
            MessageUtil.sendMessage(player, "&cYou don't have permission to use this command.");
            return true;
        }

        if (!AnyAresAPI.isAvailable()) {
            MessageUtil.sendMessage(player, "&cAnyARES-Core is not available.");
            plugin.getLogger().warning("/paste used by " + player.getName() + " but API is unavailable.");
            return true;
        }

        ClipboardManager clipboardManager;
        TaskEngine taskEngine;
        try {
            clipboardManager = AnyAresAPI.getClipboardManager();
            taskEngine = AnyAresAPI.getTaskEngine();
        } catch (IllegalStateException e) {
            MessageUtil.sendMessage(player, "&cError accessing AnyARES-Core components: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "Failed to access Core components for /paste by " + player.getName(), e);
            return true;
        }

        ClipboardObject clipboard = clipboardManager.getPlayerClipboard(player);
        if (clipboard == null) {
            MessageUtil.sendMessage(player, "&cYour clipboard is empty. Use /copy or /cut first.");
            return true;
        }

        boolean pasteAir = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-a")) {
                pasteAir = true;
            }
        }

        PasteOperation pasteOp = new PasteOperation(player, clipboard, player.getLocation(), pasteAir);
        taskEngine.submitOperation(pasteOp);

        return true;
    }
}