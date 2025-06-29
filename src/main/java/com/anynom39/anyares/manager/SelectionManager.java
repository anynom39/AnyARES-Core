package com.anynom39.anyares.manager;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.selection.CuboidSelection;
import com.anynom39.anyares.selection.PlayerSelection;
import com.anynom39.anyares.selection.Selection;
import com.anynom39.anyares.selection.SelectionType;
import com.anynom39.anyares.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SelectionManager implements Listener {
    private final AnyARES_Core plugin;
    private final Map<UUID, PlayerSelection> playerSelectionsMap = new ConcurrentHashMap<>();
    private final VisualizationManager visualizationManager;

    public SelectionManager(AnyARES_Core plugin, VisualizationManager visualizationManager) {
        this.plugin = plugin;
        this.visualizationManager = visualizationManager;
    }

    @NotNull
    public PlayerSelection getOrCreatePlayerSelection(Player player) {
        return playerSelectionsMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerSelection());
    }

    @Nullable
    public PlayerSelection getPlayerSelectionState(Player player) {
        return playerSelectionsMap.get(player.getUniqueId());
    }

    @Nullable
    public Selection getActiveSelection(Player player) {
        PlayerSelection ps = getPlayerSelectionState(player);
        return (ps != null) ? ps.getActiveSelection() : null;
    }

    public void setPlayerSelectionType(Player player, SelectionType type) {
        PlayerSelection ps = getOrCreatePlayerSelection(player);
        ps.setCurrentSelectionType(type);
        MessageUtil.sendMessage(player, "&7Selection type set to: &e" + type.getDisplayName());
        updateVisualsAndNotify(player, ps);
    }

    public void setPlayerDefiningPoint(Player player, int pointIndex, Location location) {
        PlayerSelection ps = getOrCreatePlayerSelection(player);
        if (ps.getSelectionWorld() == null) {
            ps.setSelectionWorld(location.getWorld());
        } else if (!ps.getSelectionWorld().equals(location.getWorld())) {
            MessageUtil.sendMessage(player, "&cNew point is in a different world. Clearing old selection.");
            ps.setSelectionWorld(location.getWorld());
        }

        ps.setDefiningPoint(pointIndex, location);

        String pointName = "Point " + (pointIndex + 1);
        if (ps.getCurrentSelectionType() == SelectionType.CUBOID) {
            pointName = (pointIndex == 0) ? "Position 1" : "Position 2";
        }

        MessageUtil.sendMessage(player, "&a" + pointName + " set to: &e" + formatLocation(location));
        updateVisualsAndNotify(player, ps);
    }

    public void addPlayerDefiningPoint(Player player, Location location) {
        PlayerSelection ps = getOrCreatePlayerSelection(player);
        if (ps.getSelectionWorld() == null) {
            ps.setSelectionWorld(location.getWorld());
        } else if (!ps.getSelectionWorld().equals(location.getWorld())) {
            MessageUtil.sendMessage(player, "&cNew point is in a different world. Clearing old selection.");
            ps.setSelectionWorld(location.getWorld());
        }

        ps.addDefiningPoint(location);

        String pointMessage;
        if (ps.getCurrentSelectionType() == SelectionType.CUBOID) {
            pointMessage = "&aPosition 2 set to: &e" + formatLocation(location) + " (Primary set to previous Pos2)";
            if (ps.getPos1() != null && ps.getPos1().equals(location)) {
                pointMessage = "&aPosition 1 set to: &e" + formatLocation(location);
            }
        } else {
            pointMessage = "&aPoint " + ps.getDefiningPoints().size() + " set to: &e" + formatLocation(location);
        }
        MessageUtil.sendMessage(player, pointMessage);
        updateVisualsAndNotify(player, ps);
    }


    public void clearPlayerSelection(Player player) {
        PlayerSelection ps = getPlayerSelectionState(player);
        if (ps != null) {
            ps.clearSelection();
            MessageUtil.sendMessage(player, "&aSelection cleared.");
            visualizationManager.clearSelectionVisuals(player);
        } else {
            MessageUtil.sendMessage(player, "&7No selection to clear.");
        }
    }

    private void updateVisualsAndNotify(Player player, PlayerSelection ps) {
        Selection activeSel = ps.getActiveSelection();
        if (activeSel != null) {
            MessageUtil.sendMessage(player, "&7Selection Type: &f" + activeSel.getTypeName() + "&7, Volume: &f" + activeSel.getVolume() + " blocks");
            visualizationManager.updateSelectionVisuals(player, activeSel);
        } else {
            Location p1 = ps.getPos1();
            Location p2 = ps.getPos2();
            if (p1 != null && p2 != null && p1.equals(p2) && ps.getDefiningPoints().size() == 1) {
                visualizationManager.updateSelectionVisuals(player, p1, null);
            } else {
                visualizationManager.updateSelectionVisuals(player, p1, p2);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerSelectionsMap.remove(event.getPlayer().getUniqueId());
        visualizationManager.clearSelectionVisuals(event.getPlayer());
    }

    private String formatLocation(Location loc) {
        if (loc == null) return "N/A";
        return String.format("%s (%d, %d, %d)",
                loc.getWorld() != null ? loc.getWorld().getName() : "UnknownWorld",
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public void setPlayerPositionOne(Player player, Location location) {
        setPlayerDefiningPoint(player, 0, location);
    }

    public void setPlayerPositionTwo(Player player, Location location) {
        setPlayerDefiningPoint(player, 1, location);
    }

    @Nullable
    @Deprecated
    public CuboidSelection getCuboidSelection(Player player) {
        Selection activeSel = getActiveSelection(player);
        if (activeSel instanceof CuboidSelection) {
            return (CuboidSelection) activeSel;
        }
        return null;
    }
}