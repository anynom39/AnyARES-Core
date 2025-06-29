package com.anynom39.anyares.history;

import com.anynom39.anyares.AnyARES_Core;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;

public class ChangeSet {
    private final List<Change> changes;
    private final World world;

    public ChangeSet(@NotNull World world) {
        this.world = world;
        this.changes = new ArrayList<>();
    }

    public synchronized void recordChange(@NotNull Block originalBlock, @NotNull BlockData newBlockData) {
        if (!originalBlock.getWorld().equals(this.world)) {
            throw new IllegalArgumentException("Change being recorded is in world '" + originalBlock.getWorld().getName() +
                    "' but ChangeSet is for world '" + this.world.getName() + "'.");
        }
        changes.add(new Change(originalBlock.getLocation(), originalBlock.getBlockData(), newBlockData));
    }

    public synchronized void addConstructedChange(@NotNull Change change) {
        if (!change.location().getWorld().equals(this.world)) {
            throw new IllegalArgumentException("Change being added is in world '" + change.location().getWorld().getName() +
                    "' but ChangeSet is for world '" + this.world.getName() + "'.");
        }
        changes.add(change);
    }


    public List<Change> getChanges() {
        return Collections.unmodifiableList(changes);
    }

    public int getSize() {
        return changes.size();
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    @NotNull
    public World getWorld() {
        return world;
    }

    public CompletableFuture<Void> undo(@NotNull AnyARES_Core core, @Nullable Consumer<Integer> onComplete, @Nullable Consumer<Throwable> onFailure) {
        return applyState(core, true, onComplete, onFailure);
    }

    public CompletableFuture<Void> redo(@NotNull AnyARES_Core core, @Nullable Consumer<Integer> onComplete, @Nullable Consumer<Throwable> onFailure) {
        return applyState(core, false, onComplete, onFailure);
    }

    private CompletableFuture<Void> applyState(@NotNull AnyARES_Core core, boolean isUndo,
                                               @Nullable Consumer<Integer> onComplete, @Nullable Consumer<Throwable> onFailure) {
        if (changes.isEmpty()) {
            if (onComplete != null) Bukkit.getScheduler().runTask(core, () -> onComplete.accept(0));
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> overallFuture = new CompletableFuture<>();
        List<Change> effectiveChanges = new ArrayList<>(changes);
        if (isUndo) {
            Collections.reverse(effectiveChanges);
        }

        Bukkit.getGlobalRegionScheduler().execute(core, () -> {
            try {
                int blocksChangedCount = 0;
                for (Change change : effectiveChanges) {
                    Block block = change.location().getBlock();
                    BlockData targetData = isUndo ? change.oldBlockData() : change.newBlockData();

                    block.setBlockData(targetData, false);
                    blocksChangedCount++;
                }
                final int finalBlocksChangedCount = blocksChangedCount;
                if (onComplete != null) {
                    Bukkit.getScheduler().runTask(core, () -> onComplete.accept(finalBlocksChangedCount));
                }
                overallFuture.complete(null);
            } catch (Exception e) {
                core.getLogger().log(Level.SEVERE, "Error during ChangeSet " + (isUndo ? "undo" : "redo") + ": ", e);
                if (onFailure != null) {
                    Bukkit.getScheduler().runTask(core, () -> onFailure.accept(e));
                }
                overallFuture.completeExceptionally(e);
            }
        });
        return overallFuture;
    }
}