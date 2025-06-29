package com.anynom39.anyares.selection;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

public interface Selection {

    @NotNull
    World getWorld();

    @NotNull
    Location getMinimumPoint();

    @NotNull
    Location getMaximumPoint();

    long getVolume();

    boolean contains(@NotNull Location location);

    @NotNull
    Iterator<Block> getBlockIterator();

    @NotNull
    String getTypeName();

    @Nullable
    Location getCenter();

    @NotNull
    Selection clone();
}