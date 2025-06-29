package com.anynom39.anyares.clipboard;

import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a collection of blocks copied to the clipboard.
 * Stores block data relative to an origin point.
 * <p>
 * For simplicity, this initial version will only store BlockData.
 * Future enhancements: entities, biomes, tile entity data.
 */
public class ClipboardObject {

    // BlockData is stored in a 3D array representing the shape.
    // The array indices are [y][z][x] for somewhat intuitive access if visualized.
    // (Height first, then depth, then width)
    private final BlockData[][][] blocks;
    private final Vector dimensions; // Width (x), Height (y), Length (z)
    private final Vector relativeOrigin; // Player's position relative to the min point of the copied selection.
    // This helps in pasting relative to where the player was standing.
    private final World originalWorld; // For context, though paste can be cross-world.

    /**
     * Creates a new ClipboardObject.
     *
     * @param blocks         The 3D array of BlockData. blocks[y][z][x].
     * @param dimensions     The dimensions (width, height, length) of the copied area.
     * @param relativeOrigin The player's offset from the selection's minimum corner at the time of copy.
     * @param originalWorld  The world from which the data was copied.
     */
    public ClipboardObject(@NotNull BlockData[][][] blocks, @NotNull Vector dimensions, @NotNull Vector relativeOrigin, @NotNull World originalWorld) {
        this.blocks = blocks; // Assume the input array is correctly structured and deep-copied if needed before passing.
        this.dimensions = dimensions.clone();
        this.relativeOrigin = relativeOrigin.clone();
        this.originalWorld = originalWorld;
    }

    /**
     * Gets the BlockData at a relative coordinate within the clipboard.
     *
     * @param x Relative X (0 to width-1)
     * @param y Relative Y (0 to height-1)
     * @param z Relative Z (0 to length-1)
     * @return The BlockData, or null if out of bounds.
     */
    public BlockData getBlockData(int x, int y, int z) {
        if (y < 0 || y >= dimensions.getBlockY() ||
                z < 0 || z >= dimensions.getBlockZ() ||
                x < 0 || x >= dimensions.getBlockX()) {
            return null; // Out of bounds
        }
        return blocks[y][z][x];
    }

    @NotNull
    public Vector getDimensions() {
        return dimensions.clone();
    }

    public int getWidth() {
        return dimensions.getBlockX();
    }

    public int getHeight() {
        return dimensions.getBlockY();
    }

    public int getLength() {
        return dimensions.getBlockZ();
    }

    @NotNull
    public Vector getRelativeOrigin() {
        return relativeOrigin.clone();
    }

    @NotNull
    public World getOriginalWorld() {
        return originalWorld;
    }

    public long getVolume() {
        return (long) getWidth() * getHeight() * getLength();
    }
}