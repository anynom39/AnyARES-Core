package com.anynom39.anyares.selection;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class EllipsoidSelection implements Selection {

    private final World world;
    private final Location center;
    private final double radiusX, radiusY, radiusZ;
    private final double radiusXSq, radiusYSq, radiusZSq;

    private final Location minBoundingPoint;
    private final Location maxBoundingPoint;
    private final long estimatedVolume;

    public EllipsoidSelection(@NotNull World world, @NotNull Location center, double rx, double ry, double rz) {
        this.world = Objects.requireNonNull(world, "World cannot be null");
        this.center = Objects.requireNonNull(center, "Center location cannot be null").clone();

        if (rx <= 0 || ry <= 0 || rz <= 0) {
            throw new IllegalArgumentException("Radii (rx, ry, rz) must be positive.");
        }
        this.radiusX = rx;
        this.radiusY = ry;
        this.radiusZ = rz;
        this.radiusXSq = rx * rx;
        this.radiusYSq = ry * ry;
        this.radiusZSq = rz * rz;

        this.minBoundingPoint = new Location(world, center.getX() - rx, center.getY() - ry, center.getZ() - rz);
        this.maxBoundingPoint = new Location(world, center.getX() + rx, center.getY() + ry, center.getZ() + rz);

        this.estimatedVolume = (long) ((4.0 / 3.0) * Math.PI * rx * ry * rz);
    }

    @NotNull
    @Override
    public World getWorld() {
        return world;
    }

    @NotNull
    @Override
    public Location getMinimumPoint() {
        return minBoundingPoint.clone();
    }

    @NotNull
    @Override
    public Location getMaximumPoint() {
        return maxBoundingPoint.clone();
    }

    @Override
    public long getVolume() {
        return estimatedVolume;
    }

    @Override
    public boolean contains(@NotNull Location location) {
        Objects.requireNonNull(location, "Location cannot be null");
        if (!this.world.equals(location.getWorld())) return false;

        double dx = (location.getBlockX() + 0.5) - center.getX();
        double dy = (location.getBlockY() + 0.5) - center.getY();
        double dz = (location.getBlockZ() + 0.5) - center.getZ();

        return (dx * dx / radiusXSq) + (dy * dy / radiusYSq) + (dz * dz / radiusZSq) <= 1.0001;
    }

    @NotNull
    @Override
    public Iterator<Block> getBlockIterator() {
        return new EllipsoidBlockIterator(this);
    }

    @NotNull
    @Override
    public String getTypeName() {
        return SelectionType.ELLIPSOID.getDisplayName();
    }

    @Nullable
    @Override
    public Location getCenter() {
        return center.clone();
    }

    public double getRadiusX() {
        return radiusX;
    }

    public double getRadiusY() {
        return radiusY;
    }

    public double getRadiusZ() {
        return radiusZ;
    }

    @NotNull
    @Override
    public Selection clone() {
        return new EllipsoidSelection(this.world, this.center, this.radiusX, this.radiusY, this.radiusZ);
    }

    private static class EllipsoidBlockIterator implements Iterator<Block> {
        private final EllipsoidSelection selection;
        private final World world;
        private final int bbMinX, bbMinYClamped, bbMinZ;
        private final int bbMaxX, bbMaxYClamped, bbMaxZ;
        private int currentX, currentY, currentZ;
        private Block nextBlock;

        public EllipsoidBlockIterator(EllipsoidSelection selection) {
            this.selection = selection;
            this.world = selection.getWorld();
            Location bbMin = selection.getMinimumPoint();
            Location bbMax = selection.getMaximumPoint();

            this.bbMinX = (int) Math.floor(bbMin.getX());
            this.bbMinYClamped = Math.max((int) Math.floor(bbMin.getY()), world.getMinHeight());
            this.bbMinZ = (int) Math.floor(bbMin.getZ());
            this.bbMaxX = (int) Math.ceil(bbMax.getX()) - 1;
            this.bbMaxYClamped = Math.min((int) Math.ceil(bbMax.getY()) - 1, world.getMaxHeight() - 1);
            this.bbMaxZ = (int) Math.ceil(bbMax.getZ()) - 1;


            this.currentX = this.bbMinX;
            this.currentY = this.bbMinYClamped;
            this.currentZ = this.bbMinZ;
            advanceToNext();
        }

        private void advanceToNext() {
            nextBlock = null;
            while (currentZ <= bbMaxZ) {
                while (currentY <= bbMaxYClamped) {
                    while (currentX <= bbMaxX) {
                        Location testLoc = new Location(world, currentX, currentY, currentZ);
                        if (selection.contains(testLoc)) {
                            if (currentY >= world.getMinHeight() && currentY < world.getMaxHeight()) {
                                nextBlock = world.getBlockAt(currentX, currentY, currentZ);
                                currentX++;
                                return;
                            }
                        }
                        currentX++;
                    }
                    currentX = bbMinX;
                    currentY++;
                }
                currentY = bbMinYClamped;
                currentZ++;
            }
        }

        @Override
        public boolean hasNext() {
            return nextBlock != null;
        }

        @Override
        public Block next() {
            if (nextBlock == null) throw new NoSuchElementException();
            Block toReturn = nextBlock;
            advanceToNext();
            return toReturn;
        }
    }
}