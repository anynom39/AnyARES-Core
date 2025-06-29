package com.anynom39.anyares.manager;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.history.ChangeSet;
import com.anynom39.anyares.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;

public class HistoryManager implements Listener {
    private final AnyARES_Core plugin;
    private final Map<UUID, Deque<ChangeSet>> undoStacks = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<ChangeSet>> redoStacks = new ConcurrentHashMap<>();
    private int maxHistorySize;

    public HistoryManager(AnyARES_Core plugin) {
        this.plugin = plugin;
        loadConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("HistoryManager initialized. Max history size: " + maxHistorySize);
    }

    private void loadConfig() {
        this.maxHistorySize = plugin.getConfig().getInt("core-settings.history.max-size", 50);
        if (maxHistorySize <= 0) maxHistorySize = 1;
    }

    public void reloadConfigValues() {
        plugin.reloadConfig();
        loadConfig();
        plugin.getLogger().info("HistoryManager config reloaded. Max history size: " + maxHistorySize);
    }


    public void recordChangeSet(@NotNull Player player, @NotNull ChangeSet changeSet) {
        if (changeSet.isEmpty()) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        Deque<ChangeSet> undoStack = undoStacks.computeIfAbsent(playerUUID, k -> new LinkedBlockingDeque<>(maxHistorySize));
        Deque<ChangeSet> redoStack = redoStacks.computeIfAbsent(playerUUID, k -> new LinkedBlockingDeque<>(maxHistorySize));

        while (undoStack.size() >= maxHistorySize) {
            undoStack.pollLast();
        }
        undoStack.push(changeSet);
        redoStack.clear();
    }

    public void performUndo(@NotNull Player player) {
        Deque<ChangeSet> undoStack = undoStacks.get(player.getUniqueId());
        if (undoStack == null || undoStack.isEmpty()) {
            MessageUtil.sendMessage(player, "&cNothing to undo.");
            return;
        }

        ChangeSet lastChangeSet = undoStack.pop();
        MessageUtil.sendMessage(player, "&7Undoing last operation (" + lastChangeSet.getSize() + " blocks)... Please wait.");

        lastChangeSet.undo(plugin,
                (count) -> {
                    Deque<ChangeSet> redoStack = redoStacks.computeIfAbsent(player.getUniqueId(), k -> new LinkedBlockingDeque<>(maxHistorySize));
                    while (redoStack.size() >= maxHistorySize) {
                        redoStack.pollLast();
                    }
                    redoStack.push(lastChangeSet);
                    MessageUtil.sendMessage(player, "&aSuccessfully undid " + count + " block changes. Use &e//redo&a to reapply.");
                },
                (error) -> {
                    plugin.getLogger().log(Level.SEVERE, "Error during undo for " + player.getName(), error);
                    MessageUtil.sendMessage(player, "&cAn error occurred during undo: &7" + error.getMessage() + "&c. Check console. Operation state might be inconsistent.");
                }
        );
    }

    public void performRedo(@NotNull Player player) {
        Deque<ChangeSet> redoStack = redoStacks.get(player.getUniqueId());
        if (redoStack == null || redoStack.isEmpty()) {
            MessageUtil.sendMessage(player, "&cNothing to redo.");
            return;
        }

        ChangeSet nextChangeSet = redoStack.pop();
        MessageUtil.sendMessage(player, "&7Redoing last undone operation (" + nextChangeSet.getSize() + " blocks)... Please wait.");

        nextChangeSet.redo(plugin,
                (count) -> {
                    Deque<ChangeSet> undoStack = undoStacks.computeIfAbsent(player.getUniqueId(), k -> new LinkedBlockingDeque<>(maxHistorySize));
                    while (undoStack.size() >= maxHistorySize) {
                        undoStack.pollLast();
                    }
                    undoStack.push(nextChangeSet);
                    MessageUtil.sendMessage(player, "&aSuccessfully redid " + count + " block changes. Use &e//undo&a to revert again.");
                },
                (error) -> {
                    plugin.getLogger().log(Level.SEVERE, "Error during redo for " + player.getName(), error);
                    MessageUtil.sendMessage(player, "&cAn error occurred during redo: &7" + error.getMessage() + "&c. Check console. Operation state might be inconsistent.");
                }
        );
    }

    public void clearPlayerHistory(Player player) {
        undoStacks.remove(player.getUniqueId());
        redoStacks.remove(player.getUniqueId());
        MessageUtil.sendMessage(player, "&7Your edit history has been cleared.");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        undoStacks.remove(playerUUID);
        redoStacks.remove(playerUUID);
    }
}