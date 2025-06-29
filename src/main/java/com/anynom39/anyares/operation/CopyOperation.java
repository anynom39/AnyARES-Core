package com.anynom39.anyares.operation;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.clipboard.ClipboardObject;
import com.anynom39.anyares.history.ChangeSet;
import com.anynom39.anyares.manager.ClipboardManager;
import com.anynom39.anyares.selection.Selection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class CopyOperation implements BlockOperation {

    private final Player player;
    private final Selection selection;
    private final ClipboardManager clipboardManager;
    private final boolean forCutOperation;

    public CopyOperation(@NotNull Player player, @NotNull Selection selection, @NotNull ClipboardManager clipboardManager, boolean forCutOperation) {
        this.player = player;
        this.selection = selection;
        this.clipboardManager = clipboardManager;
        this.forCutOperation = forCutOperation;
    }

    @Override
    @NotNull
    public Player getPlayer() {
        return player;
    }

    @Override
    @NotNull
    public Selection getSelection() {
        return selection;
    }

    @Override
    public String getOperationName() {
        return (forCutOperation ? "Cut (" : "Copy (") + selection.getTypeName() + ")";
    }

    @Override
    public long getEstimatedBlocks() {
        return selection.getVolume();
    }

    @Override
    public CompletableFuture<ChangeSet> execute(@NotNull AnyARES_Core core) {
        CompletableFuture<ChangeSet> future = new CompletableFuture<>();
        World world = selection.getWorld();

        Bukkit.getGlobalRegionScheduler().execute(core, () -> {
            try {
                Location minSelPoint = selection.getMinimumPoint();
                Location maxSelPoint = selection.getMaximumPoint();


                int width = maxSelPoint.getBlockX() - minSelPoint.getBlockX() + 1;
                int height = maxSelPoint.getBlockY() - minSelPoint.getBlockY() + 1;
                int length = maxSelPoint.getBlockZ() - minSelPoint.getBlockZ() + 1;

                if (width <= 0 || height <= 0 || length <= 0) {
                    throw new IllegalArgumentException("Selection has zero or negative dimension for copy.");
                }

                BlockData[][][] copiedBlocks = new BlockData[height][length][width];

                Iterator<Block> blockIterator = selection.getBlockIterator();
                while (blockIterator.hasNext()) {
                    Block block = blockIterator.next();
                    Location loc = block.getLocation();

                    int relX = loc.getBlockX() - minSelPoint.getBlockX();
                    int relY = loc.getBlockY() - minSelPoint.getBlockY();
                    int relZ = loc.getBlockZ() - minSelPoint.getBlockZ();

                    if (relY >= 0 && relY < height && relZ >= 0 && relZ < length && relX >= 0 && relX < width) {
                        if (loc.getBlockY() < world.getMinHeight() || loc.getBlockY() >= world.getMaxHeight()) {
                            copiedBlocks[relY][relZ][relX] = Bukkit.createBlockData(Material.AIR); // Treat out-of-bounds as air
                        } else {
                            copiedBlocks[relY][relZ][relX] = block.getBlockData().clone();
                        }
                    } else {
                        core.getLogger().warning("CopyOperation: Block " + loc + " from iterator was outside calculated bounding box relative coords. MinSel: " + minSelPoint);
                    }
                }
                for (int y = 0; y < height; ++y) {
                    for (int z = 0; z < length; ++z) {
                        for (int x = 0; x < width; ++x) {
                            if (copiedBlocks[y][z][x] == null) {
                                Location checkLoc = new Location(world, minSelPoint.getBlockX() + x, minSelPoint.getBlockY() + y, minSelPoint.getBlockZ() + z);
                                if (selection.contains(checkLoc)) {
                                    core.getLogger().warning("CopyOperation: Bounding box cell [" + y + "][" + z + "][" + x + "] was null but selection.contains() is true. Filling with AIR.");
                                    copiedBlocks[y][z][x] = Bukkit.createBlockData(Material.AIR);
                                } else {
                                    copiedBlocks[y][z][x] = Bukkit.createBlockData(Material.AIR);
                                }
                            }
                        }
                    }
                }


                Vector playerLocationVec = player.getLocation().toVector();
                Vector selectionMinVec = minSelPoint.toVector();
                Vector relativeOrigin = playerLocationVec.subtract(selectionMinVec);

                ClipboardObject clipboard = new ClipboardObject(
                        copiedBlocks,
                        new Vector(width, height, length),
                        relativeOrigin,
                        world
                );
                clipboardManager.setPlayerClipboard(player, clipboard);
                future.complete(new ChangeSet(world));

            } catch (Exception e) {
                core.getLogger().log(Level.SEVERE, "Error during " + getOperationName() + " execution: ", e);
                future.completeExceptionally(new RuntimeException("Failed to copy selection: " + e.getMessage(), e));
            }
        });
        return future;
    }
}