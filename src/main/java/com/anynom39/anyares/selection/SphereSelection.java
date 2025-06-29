package com.anynom39.anyares.selection;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class SphereSelection implements Selection {

    private final World world;
    private final Location center;
    private final double radius;
    private final double radiusSquared;

    private final Location minBoundingPoint;
    private final Location maxBoundingPoint;
    private final long estimatedVolume;

    public SphereSelection(@NotNull World world, @NotNull Location center, double radius) {
        this.world = Objects.requireNonNull(world, "World cannot be null");
        this.center = Objects.requireNonNull(center, "Center location cannot be null").clone();
        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be positive.");
        }
        this.radius = radius;
        this.radiusSquared = radius * radius;

        int minX = (int) Math.floor(center.getX() - radius);
        int minY = (int) Math.floor(center.getY() - radius);
        int minZ = (int) Math.floor(center.getZ() - radius);
        int maxX = (int) Math.ceil(center.getX() + radius);
        int maxY = (int) Math.ceil(center.getY() + radius);
        int maxZ = (int) Math.ceil(center.getZ() + radius);

        this.minBoundingPoint = new Location(world, minX, minY, minZ);
        this.maxBoundingPoint = new Location(world, maxX, maxY, maxZ);

        this.estimatedVolume = (long) ((4.0 / 3.0) * Math.PI * this.radius * this.radius * this.radius);
    }

    @NotNull
    @Override
    public World getWorld() {
        return world;
    }

    @NotNull
    @Override
    public Location getMinimumPoint() { // Bounding box min
        return minBoundingPoint.clone();
    }

    @NotNull
    @Override
    public Location getMaximumPoint() { // Bounding box max
        return maxBoundingPoint.clone();
    }

    @Override
    public long getVolume() {
        return estimatedVolume;
    }

    @Override
    public boolean contains(@NotNull Location location) {
        Objects.requireNonNull(location, "Location to check cannot be null");
        if (!world.equals(location.getWorld())) {
            return false;
        }
        return location.toVector().add(new Vector(0.5, 0.5, 0.5))
                .distanceSquared(center.toVector()) <= radiusSquared;
    }

    @NotNull
    @Override
    public Iterator<Block> getBlockIterator() {
        return new SphereBlockIterator(this);
    }

    @NotNull
    @Override
    public String getTypeName() {
        return SelectionType.SPHERE.getDisplayName();
    }

    @Nullable
    @Override
    public Location getCenter() {
        return center.clone();
    }

    public double getRadius() {
        return radius;
    }

    @NotNull
    @Override
    public Selection clone() {
        return new SphereSelection(this.world, this.center, this.radius);
    }

    private static class SphereBlockIterator implements Iterator<Block> {
        private final SphereSelection selection;
        private final World world;
        private final Location center;
        private final double radiusSquared;

        private final int minX, minY, minZ;
        private final int maxX, maxY, maxZ;

        private int currentX, currentY, currentZ;
        private Block nextBlock;

        public SphereBlockIterator(SphereSelection selection) {
            this.selection = selection;
            this.world = selection.getWorld();
            this.center = selection.getCenter();
            this.radiusSquared = selection.radiusSquared;

            Location bbMin = selection.getMinimumPoint();
            Location bbMax = selection.getMaximumPoint();

            this.minX = bbMin.getBlockX();
            this.minY = Math.max(bbMin.getBlockY(), world.getMinHeight());
            this.minZ = bbMin.getBlockZ();
            this.maxX = bbMax.getBlockX();
            this.maxY = Math.min(bbMax.getBlockY(), world.getMaxHeight() - 1);
            this.maxZ = bbMax.getBlockZ();

            this.currentX = minX;
            this.currentY = minY;
            this.currentZ = minZ;
            this.nextBlock = null;
            advanceToNext();
        }

        private void advanceToNext() {
            nextBlock = null;
            while (currentZ <= maxZ) {
                while (currentY <= maxY) {
                    while (currentX <= maxX) {
                        Location blockCenterLocation = new Location(world, currentX + 0.5, currentY + 0.5, currentZ + 0.5);
                        if (blockCenterLocation.distanceSquared(center) <= radiusSquared) {
                            if (currentY >= world.getMinHeight() && currentY < world.getMaxHeight()) {
                                nextBlock = world.getBlockAt(currentX, currentY, currentZ);
                                currentX++;
                                return;
                            }
                        }
                        currentX++;
                    }
                    currentX = minX;
                    currentY++;
                }
                currentY = minY;
                currentZ++;
            }
        }

        @Override
        public boolean hasNext() {
            return nextBlock != null;
        }

        @Override
        public Block next() {
            if (nextBlock == null) {
                throw new NoSuchElementException("No more blocks in sphere selection iterator.");
            }
            Block toReturn = nextBlock;
            advanceToNext();
            return toReturn;
        }
    }
}