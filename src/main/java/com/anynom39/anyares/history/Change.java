package com.anynom39.anyares.history;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a single block change, storing its location,
 * old BlockData, and new BlockData.
 * This class is immutable.
 */
public record Change(
        @NotNull Location location,
        @NotNull BlockData oldBlockData,
        @NotNull BlockData newBlockData
) {
    public Change(@NotNull Location location, @NotNull BlockData oldBlockData, @NotNull BlockData newBlockData) {
        this.location = location.clone();
        this.oldBlockData = oldBlockData.clone();
        this.newBlockData = newBlockData.clone();
    }
}