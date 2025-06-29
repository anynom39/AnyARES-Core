package com.anynom39.anyares.command;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.manager.HistoryManager;
import com.anynom39.anyares.manager.TaskEngine;
import com.anynom39.anyares.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AnyAresCommand implements CommandExecutor {

    private final AnyARES_Core plugin;
    private final HistoryManager historyManager;
    private final TaskEngine taskEngine;

    public AnyAresCommand(AnyARES_Core plugin) {
        this.plugin = plugin;
        this.historyManager = plugin.getHistoryManager();
        this.taskEngine = plugin.getTaskEngine();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                sendHelp(sender, label);
                break;
            case "info":
            case "version":
                MessageUtil.sendMessage(sender, "&6AnyARES-Core &ev" + plugin.getDescription().getVersion());
                MessageUtil.sendMessage(sender, "&eDeveloped by: &f" + String.join(", ", plugin.getDescription().getAuthors()));
                break;
            case "reload":
                if (!sender.hasPermission("anyares.core.admin")) {
                    MessageUtil.sendMessage(sender, "&cYou don't have permission to use this subcommand.");
                    return true;
                }
                plugin.reloadPluginConfiguration();
                MessageUtil.sendMessage(sender, "&aAnyARES-Core configuration and components reloaded.");
                break;
            case "status":
                if (!sender.hasPermission("anyares.core.status")) {
                    MessageUtil.sendMessage(sender, "&cYou don't have permission to view plugin status.");
                    return true;
                }
                MessageUtil.sendMessage(sender, "&6AnyARES Core Status:");
                MessageUtil.sendMessage(sender, "&e - Version: &f" + plugin.getDescription().getVersion());
                MessageUtil.sendMessage(sender, "&e - Task Engine Queue: &f" + taskEngine.getQueueSize());
                MessageUtil.sendMessage(sender, "&e - Active Operations: &f" + taskEngine.getActiveOperations());
                break;
            case "queue":
                if (!sender.hasPermission("anyares.queue.manage")) {
                    MessageUtil.sendMessage(sender, "&cYou don't have permission to manage the queue.");
                    return true;
                }
                MessageUtil.sendMessage(sender, "&eTask Engine Queue: &f" + taskEngine.getQueueSize() + " operations pending.");
                MessageUtil.sendMessage(sender, "&eActive Operations: &f" + taskEngine.getActiveOperations());
                break;
            case "history":
                if (!(sender instanceof Player player)) {
                    MessageUtil.sendMessage(sender, "&cThis subcommand is player-specific.");
                    return true;
                }
                if (args.length > 1 && args[1].equalsIgnoreCase("clear")) {
                    if (!player.hasPermission("anyares.history.manage")) {
                        MessageUtil.sendMessage(player, "&cYou don't have permission to clear history.");
                        return true;
                    }
                    historyManager.clearPlayerHistory(player);
                } else {
                    MessageUtil.sendMessage(player, "&eUse &6/" + label + " history clear&e to clear your edit history.");
                }
                break;
            default:
                MessageUtil.sendMessage(sender, "&cUnknown subcommand. Use &e/" + label + " help&c for assistance.");
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        MessageUtil.sendRawMessage(sender, "&6--- AnyARES Core Help (/ " + label + ") ---");
        MessageUtil.sendRawMessage(sender, "&e help &7- Shows this help message.");
        MessageUtil.sendRawMessage(sender, "&e info &7- Shows plugin information.");
        MessageUtil.sendRawMessage(sender, "&e status &7- Shows current plugin and task engine status.");
        if (sender.hasPermission("anyares.queue.manage")) {
            MessageUtil.sendRawMessage(sender, "&e queue &7- View operation queue details.");
        }
        if (sender instanceof Player && sender.hasPermission("anyares.history.manage")) {
            MessageUtil.sendRawMessage(sender, "&e history clear &7- Clears your personal edit history.");
        }
        if (sender.hasPermission("anyares.core.admin")) {
            MessageUtil.sendRawMessage(sender, "&e reload &7- Reloads the plugin configuration.");
        }
        MessageUtil.sendRawMessage(sender, "&6---------------------------------");
        MessageUtil.sendRawMessage(sender, "&7Other commands: /wand, /pos1, /pos2, /set, /replace, /replacenear, /copy, /cut, /paste, /undo, /redo");
    }
}