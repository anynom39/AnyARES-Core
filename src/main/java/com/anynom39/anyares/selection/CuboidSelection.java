package com.anynom39.anyares.selection; // Ensure it's in this package

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class CuboidSelection implements Selection {
    private final World world;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    private final Location definingPoint1;
    private final Location definingPoint2;


    public CuboidSelection(@NotNull World world, @NotNull Location loc1, @NotNull Location loc2) {
        if (!Objects.requireNonNull(world, "World cannot be null").equals(Objects.requireNonNull(loc1, "Location 1 cannot be null").getWorld()) ||
                !world.equals(Objects.requireNonNull(loc2, "Location 2 cannot be null").getWorld())) {
            throw new IllegalArgumentException("Locations must be in the same world as the selection world (" + world.getName() + ")");
        }

        this.world = world;
        this.definingPoint1 = loc1.clone();
        this.definingPoint2 = loc2.clone();

        this.minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        this.minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        this.minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        this.maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        this.maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        this.maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
    }

    @Override
    @NotNull
    public World getWorld() {
        return world;
    }

    @Override
    @NotNull
    public Location getMinimumPoint() {
        return new Location(world, minX, minY, minZ);
    }

    @Override
    @NotNull
    public Location getMaximumPoint() {
        return new Location(world, maxX, maxY, maxZ);
    }

    @NotNull
    public Location getDefiningPoint1() {
        return definingPoint1.clone();
    }

    @NotNull
    public Location getDefiningPoint2() {
        return definingPoint2.clone();
    }


    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public int getWidth() {
        return maxX - minX + 1;
    }

    public int getHeight() {
        return maxY - minY + 1;
    }

    public int getLength() {
        return maxZ - minZ + 1;
    }

    @Override
    public long getVolume() {
        return (long) getWidth() * getHeight() * getLength();
    }

    @Override
    public boolean contains(@NotNull Location location) {
        if (!world.equals(location.getWorld())) return false;
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ;
    }

    @Override
    @NotNull
    public Iterator<Block> getBlockIterator() {
        return new CuboidBlockIterator(world, minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    @NotNull
    public String getTypeName() {
        return SelectionType.CUBOID.getDisplayName();
    }

    @Override
    @org.jetbrains.annotations.Nullable
    public Location getCenter() {
        return new Location(world,
                minX + (getWidth() / 2.0),
                minY + (getHeight() / 2.0),
                minZ + (getLength() / 2.0)
        );
    }

    @Override
    @NotNull
    public Selection clone() {
        return new CuboidSelection(this.world, this.definingPoint1, this.definingPoint2);
    }

    public static class CuboidBlockIterator implements Iterator<Block> {
        private final World world;
        private final int minX, minY, minZ;
        private final int maxX, maxY, maxZ;
        private int currentX, currentY, currentZ;

        public CuboidBlockIterator(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.world = world;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.currentX = minX;
            this.currentY = minY;
            this.currentZ = minZ;
        }

        @Override
        public boolean hasNext() {
            return currentY >= world.getMinHeight() && currentY < world.getMaxHeight() &&
                    currentX <= maxX && currentY <= maxY && currentZ <= maxZ;
        }

        @Override
        public Block next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more blocks in selection iterator");
            }
            Block block = world.getBlockAt(currentX, currentY, currentZ);
            currentX++;
            if (currentX > maxX) {
                currentX = minX;
                currentY++;
                if (currentY > maxY) {
                    currentY = minY;
                    currentZ++;
                }
            }
            return block;
        }
    }
}