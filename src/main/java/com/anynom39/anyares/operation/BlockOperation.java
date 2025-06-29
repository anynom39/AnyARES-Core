package com.anynom39.anyares.operation;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.history.ChangeSet;
import com.anynom39.anyares.selection.Selection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public interface BlockOperation {

    @Nullable
    Player getPlayer();

    @NotNull
    Selection getSelection();

    CompletableFuture<ChangeSet> execute(@NotNull AnyARES_Core core);

    String getOperationName();

    long getEstimatedBlocks();
}