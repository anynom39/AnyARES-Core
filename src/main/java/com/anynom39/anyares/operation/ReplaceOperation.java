package com.anynom39.anyares.operation;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.history.ChangeSet;
import com.anynom39.anyares.selection.Selection;
import com.anynom39.anyares.util.BlockPatternParser;
import com.anynom39.anyares.util.BlockPatternParser.MaskEntry;
import com.anynom39.anyares.util.BlockPatternParser.WeightedBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ReplaceOperation implements BlockOperation {

    private final Player player;
    private final Selection selection;
    private final List<MaskEntry> fromMaskRules;
    private final List<WeightedBlockData> toPatternList;
    private final String operationName;
    private final Location sphereCenter;
    private final double radiusSquared;

    public ReplaceOperation(@Nullable Player player, @NotNull Selection selection,
                            @NotNull List<MaskEntry> fromMaskRules,
                            @NotNull List<WeightedBlockData> toPatternList,
                            @NotNull String rawFromPattern, @NotNull String rawToPattern) {
        this(player, selection, fromMaskRules, toPatternList, rawFromPattern, rawToPattern, null, -1);
    }

    public ReplaceOperation(@Nullable Player player, @NotNull Selection selectionBoundary,
                            @NotNull List<MaskEntry> fromMaskRules,
                            @NotNull List<WeightedBlockData> toPatternList,
                            @NotNull String rawFromPattern, @NotNull String rawToPattern,
                            @Nullable Location sphereCenter, double radius) {
        this.player = player;
        this.selection = Objects.requireNonNull(selectionBoundary, "Selection boundary cannot be null");
        this.fromMaskRules = Objects.requireNonNull(fromMaskRules, "FromMaskRules list cannot be null.");
        this.toPatternList = Objects.requireNonNull(toPatternList, "ToPattern list cannot be null");

        if (toPatternList.isEmpty()) {
            throw new IllegalArgumentException("ToPattern list cannot be empty for ReplaceOperation.");
        }

        this.sphereCenter = sphereCenter;
        this.radiusSquared = (radius > 0 && sphereCenter != null) ? radius * radius : -1;

        String shapeInfo = (this.sphereCenter != null) ? " (Spherical)" : " in " + selection.getTypeName();
        this.operationName = "Replace " + (rawFromPattern.length() > 15 ? rawFromPattern.substring(0, 12) + "..." : rawFromPattern) +
                " with " + (rawToPattern.length() > 15 ? rawToPattern.substring(0, 12) + "..." : rawToPattern) + shapeInfo;
    }

    @Override
    @Nullable
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
        return operationName;
    }

    @Override
    public long getEstimatedBlocks() {
        if (sphereCenter != null && radiusSquared > 0) {
            double radius = Math.sqrt(radiusSquared);
            return (long) ((4.0 / 3.0) * Math.PI * radius * radius * radius);
        }
        return selection.getVolume();
    }

    @Override
    public CompletableFuture<ChangeSet> execute(@NotNull AnyARES_Core core) {
        CompletableFuture<ChangeSet> future = new CompletableFuture<>();
        World world = selection.getWorld();
        ChangeSet changeSet = new ChangeSet(world);

        Bukkit.getGlobalRegionScheduler().execute(core, () -> {
            try {
                Iterator<Block> blockIterator = selection.getBlockIterator();

                while (blockIterator.hasNext()) {
                    Block currentBlock = blockIterator.next();
                    Location currentBlockCenter = currentBlock.getLocation().add(0.5, 0.5, 0.5);

                    if (sphereCenter != null && radiusSquared > 0) {
                        if (currentBlockCenter.getWorld().equals(sphereCenter.getWorld()) &&
                                currentBlockCenter.distanceSquared(sphereCenter) > radiusSquared) {
                            continue;
                        }
                    }

                    BlockData oldBlockData = currentBlock.getBlockData();

                    if (BlockPatternParser.matchesMask(oldBlockData, fromMaskRules)) {
                        BlockData newBlockData = BlockPatternParser.selectRandomBlockData(toPatternList);
                        if (newBlockData == null) {
                            core.getLogger().warning("selectRandomBlockData (toPattern) returned null in ReplaceOperation.");
                            continue;
                        }
                        if (!oldBlockData.matches(newBlockData)) {
                            changeSet.recordChange(currentBlock, newBlockData);
                            currentBlock.setBlockData(newBlockData, false);
                        }
                    }
                }
                future.complete(changeSet);
            } catch (Exception e) {
                core.getLogger().log(Level.SEVERE, "Error during ReplaceOperation (" + getOperationName() + ") execution: ", e);
                future.completeExceptionally(new RuntimeException("Failed to replace blocks: " + e.getMessage(), e));
            }
        });
        return future;
    }
}