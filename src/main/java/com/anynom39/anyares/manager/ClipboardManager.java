package com.anynom39.anyares.manager;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.clipboard.ClipboardObject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClipboardManager implements Listener {
    private final AnyARES_Core plugin;
    private final Map<UUID, ClipboardObject> playerClipboards = new ConcurrentHashMap<>();

    public ClipboardManager(AnyARES_Core plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("ClipboardManager initialized.");
    }

    public void setPlayerClipboard(Player player, ClipboardObject clipboard) {
        playerClipboards.put(player.getUniqueId(), clipboard);
    }

    @Nullable
    public ClipboardObject getPlayerClipboard(Player player) {
        return playerClipboards.get(player.getUniqueId());
    }

    public void clearPlayerClipboard(Player player) {
        playerClipboards.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerClipboards.remove(event.getPlayer().getUniqueId());
    }
}