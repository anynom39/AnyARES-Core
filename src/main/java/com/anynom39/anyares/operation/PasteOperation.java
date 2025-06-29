package com.anynom39.anyares.operation;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.clipboard.ClipboardObject;
import com.anynom39.anyares.history.ChangeSet;
import com.anynom39.anyares.selection.CuboidSelection;
import com.anynom39.anyares.selection.Selection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PasteOperation implements BlockOperation {

    private final Player player;
    private final ClipboardObject clipboard;
    private final Location pasteOriginPlayerLocation;
    private final boolean pasteAir;
    private final Selection contextualSelection;

    public PasteOperation(@NotNull Player player, @NotNull ClipboardObject clipboard, @NotNull Location pasteOriginPlayerLocation, boolean pasteAir) {
        this.player = player;
        this.clipboard = Objects.requireNonNull(clipboard, "ClipboardObject cannot be null");
        this.pasteOriginPlayerLocation = Objects.requireNonNull(pasteOriginPlayerLocation, "Paste origin location cannot be null");
        this.pasteAir = pasteAir;

        Vector dimensions = clipboard.getDimensions();
        Location pasteMinCorner = pasteOriginPlayerLocation.clone().subtract(clipboard.getRelativeOrigin());
        Location pasteMaxCorner = pasteMinCorner.clone().add(dimensions).subtract(new Vector(1, 1, 1));

        World worldForSelection = pasteOriginPlayerLocation.getWorld();
        if (worldForSelection == null) {
            throw new IllegalArgumentException("Paste origin location must have a valid world.");
        }
        this.contextualSelection = new CuboidSelection(worldForSelection, pasteMinCorner, pasteMaxCorner);
    }

    @Override
    @NotNull
    public Player getPlayer() {
        return player;
    }

    @Override
    @NotNull
    public Selection getSelection() {
        return contextualSelection;
    }

    @Override
    public String getOperationName() {
        return "Paste Clipboard";
    }

    @Override
    public long getEstimatedBlocks() {
        return clipboard.getVolume();
    }

    @Override
    public CompletableFuture<ChangeSet> execute(@NotNull AnyARES_Core core) {
        CompletableFuture<ChangeSet> future = new CompletableFuture<>();
        World targetWorld = pasteOriginPlayerLocation.getWorld();
        if (targetWorld == null) {
            future.completeExceptionally(new IllegalStateException("Paste target world is null!"));
            return future;
        }
        ChangeSet changeSet = new ChangeSet(targetWorld);
        Location pasteStartLocation = pasteOriginPlayerLocation.clone().subtract(clipboard.getRelativeOrigin());

        Bukkit.getGlobalRegionScheduler().execute(core, () -> {
            try {
                for (int y = 0; y < clipboard.getHeight(); y++) {
                    for (int z = 0; z < clipboard.getLength(); z++) {
                        for (int x = 0; x < clipboard.getWidth(); x++) {
                            BlockData clipboardBlockData = clipboard.getBlockData(x, y, z);
                            if (clipboardBlockData == null) continue;

                            if (!pasteAir && clipboardBlockData.getMaterial().isAir()) {
                                continue;
                            }

                            Location worldLoc = pasteStartLocation.clone().add(x, y, z);
                            if (worldLoc.getBlockY() < targetWorld.getMinHeight() || worldLoc.getBlockY() >= targetWorld.getMaxHeight()) {
                                continue;
                            }

                            Block worldBlock = targetWorld.getBlockAt(worldLoc);
                            BlockData oldBlockData = worldBlock.getBlockData();
                            if (!oldBlockData.matches(clipboardBlockData)) {
                                changeSet.recordChange(worldBlock, clipboardBlockData);
                                worldBlock.setBlockData(clipboardBlockData, false);
                            }
                        }
                    }
                }
                future.complete(changeSet);
            } catch (Exception e) {
                core.getLogger().log(Level.SEVERE, "Error during PasteOperation execution: ", e);
                future.completeExceptionally(new RuntimeException("Failed to paste clipboard: " + e.getMessage(), e));
            }
        });
        return future;
    }
}