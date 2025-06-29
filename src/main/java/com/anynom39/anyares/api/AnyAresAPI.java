package com.anynom39.anyares.api;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.manager.*;
import com.anynom39.anyares.selection.PlayerSelection;
import com.anynom39.anyares.selection.Selection;
import com.anynom39.anyares.selection.SelectionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Public API for interacting with the AnyARES-Core plugin.
 * Addon developers should use this class to access core functionalities
 * and managers.
 */
public class AnyAresAPI {

    private static AnyARES_Core corePluginInstance = null;

    private AnyAresAPI() {
    } // Private constructor

    public static synchronized void initialize(@NotNull AnyARES_Core plugin) {
        if (corePluginInstance != null) {
            plugin.getLogger().warning("AnyAresAPI already initialized. Ignoring duplicate initialization call.");
            return;
        }
        AnyAresAPI.corePluginInstance = plugin;
        plugin.getLogger().info("AnyAresAPI initialized and linked with AnyARES-Core.");
    }

    public static synchronized void shutdown() {
        if (corePluginInstance != null) {
            corePluginInstance.getLogger().info("AnyAresAPI shutting down.");
        }
        corePluginInstance = null;
    }

    public static boolean isAvailable() {
        if (corePluginInstance == null) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("AnyARES-Core");
            if (plugin instanceof AnyARES_Core && plugin.isEnabled()) {
                corePluginInstance = (AnyARES_Core) plugin;
            }
        }
        return corePluginInstance != null && corePluginInstance.isEnabled();
    }

    private static void ensureAvailable() {
        if (!isAvailable()) {
            throw new IllegalStateException("AnyARES-Core is not loaded/enabled, or API not initialized.");
        }
    }

    // --- Core Manager Getters ---

    @NotNull
    public static SelectionManager getSelectionManager() {
        ensureAvailable();
        return corePluginInstance.getSelectionManager();
    }

    @NotNull
    public static TaskEngine getTaskEngine() {
        ensureAvailable();
        return corePluginInstance.getTaskEngine();
    }

    @NotNull
    public static HistoryManager getHistoryManager() {
        ensureAvailable();
        return corePluginInstance.getHistoryManager();
    }

    @NotNull
    public static ClipboardManager getClipboardManager() {
        ensureAvailable();
        return corePluginInstance.getClipboardManager();
    }

    @NotNull
    public static VisualizationManager getVisualizationManager() {
        ensureAvailable();
        return corePluginInstance.getVisualizationManager();
    }

    @NotNull
    public static AddonManager getAddonManager() { // Assuming AddonManager exists for future use
        ensureAvailable();
        return corePluginInstance.getAddonManager();
    }

    @NotNull
    public static AnyARES_Core getCorePlugin() {
        ensureAvailable();
        return corePluginInstance;
    }

    // --- New Selection System API Methods ---

    /**
     * Gets the player's currently active and calculated {@link Selection} object.
     * This could be a {@link com.anynom39.anyares.selection.CuboidSelection},
     * or other types like SphereSelection if implemented and selected.
     *
     * @param player The player.
     * @return The active {@link Selection}, or null if the selection is incomplete or invalid.
     */
    @Nullable
    public static Selection getActiveSelection(@NotNull Player player) {
        ensureAvailable();
        return getSelectionManager().getActiveSelection(player);
    }

    /**
     * Gets the {@link PlayerSelection} state object for a player, which holds their
     * defining points and current selection type. Useful for addons that need to
     * directly inspect or manipulate raw selection parameters.
     *
     * @param player The player.
     * @return The {@link PlayerSelection} state, or null if none exists.
     */
    @Nullable
    public static PlayerSelection getPlayerSelectionState(@NotNull Player player) {
        ensureAvailable();
        return getSelectionManager().getPlayerSelectionState(player);
    }

    /**
     * Sets the type of selection the player will make next (e.g., CUBOID, SPHERE).
     * This will also attempt to recalculate the active selection based on existing defining points.
     *
     * @param player The player.
     * @param type   The {@link SelectionType} to set.
     */
    public static void setPlayerSelectionType(@NotNull Player player, @NotNull SelectionType type) {
        ensureAvailable();
        getSelectionManager().setPlayerSelectionType(player, type);
    }

    /**
     * Sets a specific defining point for the player's selection.
     * For CUBOID, index 0 is Pos1, index 1 is Pos2.
     * Other selection types may interpret indices differently.
     *
     * @param player     The player.
     * @param pointIndex The index of the defining point to set.
     * @param location   The location for the point.
     */
    public static void setPlayerDefiningPoint(@NotNull Player player, int pointIndex, @NotNull Location location) {
        ensureAvailable();
        getSelectionManager().setPlayerDefiningPoint(player, pointIndex, location);
    }

    /**
     * Adds a defining point to the player's current selection process.
     * How this point is used depends on the current {@link SelectionType}.
     * For CUBOID, it typically cycles through Pos1 and Pos2.
     *
     * @param player   The player.
     * @param location The location to add as a defining point.
     */
    public static void addPlayerDefiningPoint(@NotNull Player player, @NotNull Location location) {
        ensureAvailable();
        getSelectionManager().addPlayerDefiningPoint(player, location);
    }

    /**
     * Clears all defining points and the active selection for the specified player.
     *
     * @param player The player whose selection should be cleared.
     */
    public static void clearPlayerSelection(@NotNull Player player) {
        ensureAvailable();
        getSelectionManager().clearPlayerSelection(player);
    }

    // --- Future API methods for Addon Registration ---
    // Example:
    // public static void registerSelectionType(String name, Class<? extends Selection> selectionClass, SelectionFactory factory, ShapeVisualizer visualizer) {
    //    ensureAvailable();
    //    // corePluginInstance.getSelectionTypeRegistry().register(...);
    // }
}