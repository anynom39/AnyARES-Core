package com.anynom39.anyares.operation;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.history.ChangeSet;
import com.anynom39.anyares.selection.Selection;
import com.anynom39.anyares.util.BlockPatternParser;
import org.bukkit.Bukkit;
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

public class SetOperation implements BlockOperation {

    private final Player player;
    private final Selection selection;
    private final List<BlockPatternParser.WeightedBlockData> patternList;
    private final String operationName;

    public SetOperation(@Nullable Player player, @NotNull Selection selection,
                        @NotNull List<BlockPatternParser.WeightedBlockData> patternList, @NotNull String rawPatternString) {
        this.player = player;
        this.selection = Objects.requireNonNull(selection, "Selection cannot be null");
        this.patternList = Objects.requireNonNull(patternList, "Pattern list cannot be null");
        if (patternList.isEmpty()) {
            throw new IllegalArgumentException("Pattern list cannot be empty for SetOperation.");
        }
        this.operationName = "Set Blocks to pattern: " + (rawPatternString.length() > 30 ? rawPatternString.substring(0, 27) + "..." : rawPatternString)
                + " in " + selection.getTypeName();
    }

    @Override
    @Nullable
    public Player getPlayer() {
        return player;
    }

    @Override
    @NotNull
    public Selection getSelection() { // Return generic Selection
        return selection;
    }

    @Override
    public String getOperationName() {
        return operationName;
    }

    @Override
    public long getEstimatedBlocks() {
        return selection.getVolume();
    }


    @Override
    public CompletableFuture<ChangeSet> execute(@NotNull AnyARES_Core core) {
        CompletableFuture<ChangeSet> future = new CompletableFuture<>();
        World world = selection.getWorld();
        ChangeSet changeSet = new ChangeSet(world);

        if (patternList.isEmpty()) {
            future.completeExceptionally(new IllegalStateException("Cannot execute SetOperation with an empty pattern list."));
            return future;
        }

        Bukkit.getGlobalRegionScheduler().execute(core, () -> {
            try {
                Iterator<Block> blockIterator = selection.getBlockIterator();
                while (blockIterator.hasNext()) {
                    Block currentBlock = blockIterator.next();

                    BlockData targetBlockData = BlockPatternParser.selectRandomBlockData(patternList);
                    if (targetBlockData == null) {
                        core.getLogger().warning("selectRandomBlockData returned null in SetOperation. Skipping block.");
                        continue;
                    }

                    BlockData oldBlockData = currentBlock.getBlockData();
                    if (!oldBlockData.matches(targetBlockData)) {
                        changeSet.recordChange(currentBlock, targetBlockData);
                        currentBlock.setBlockData(targetBlockData, false);
                    }
                }
                future.complete(changeSet);
            } catch (Exception e) {
                core.getLogger().log(Level.SEVERE, "Error during SetOperation execution for " + selection.getTypeName() + ": ", e);
                future.completeExceptionally(new RuntimeException("Failed to set blocks: " + e.getMessage(), e));
            }
        });
        return future;
    }
}