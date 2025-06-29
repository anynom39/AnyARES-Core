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

public class CylinderSelection implements Selection {

    private final World world;
    private final Location baseCenter1;
    private final Location baseCenter2;
    private final double radius;
    private final double radiusSquared;
    private final Vector axisVector;
    private final double height;
    private final Location minBoundingPoint;
    private final Location maxBoundingPoint;
    private final long estimatedVolume;

    public CylinderSelection(@NotNull World world, @NotNull Location base1, @NotNull Location base2, double radius) {
        this.world = Objects.requireNonNull(world, "World cannot be null");
        this.baseCenter1 = Objects.requireNonNull(base1, "Base center 1 cannot be null").clone();
        this.baseCenter2 = Objects.requireNonNull(base2, "Base center 2 cannot be null").clone();

        if (!base1.getWorld().equals(world) || !base2.getWorld().equals(world)) {
            throw new IllegalArgumentException("Cylinder base points must be in the same world as the selection.");
        }
        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be positive.");
        }
        this.radius = radius;
        this.radiusSquared = radius * radius;

        Vector vec1 = baseCenter1.toVector();
        Vector vec2 = baseCenter2.toVector();
        this.axisVector = vec2.clone().subtract(vec1);
        this.height = this.axisVector.length();

        if (this.height < 0.001) {
            if (this.height > 0) this.axisVector.normalize();
            else this.axisVector.setY(1);
        } else {
            this.axisVector.normalize();
        }


        double minX = Math.min(base1.getX(), base2.getX()) - radius;
        double minY = Math.min(base1.getY(), base2.getY()) - radius;
        double minZ = Math.min(base1.getZ(), base2.getZ()) - radius;
        double maxX = Math.max(base1.getX(), base2.getX()) + radius;
        double maxY = Math.max(base1.getY(), base2.getY()) + radius;
        double maxZ = Math.max(base1.getZ(), base2.getZ()) + radius;

        if (Math.abs(axisVector.getX()) < 0.01 && Math.abs(axisVector.getZ()) < 0.01) {
            minY = Math.min(base1.getY(), base2.getY());
            maxY = Math.max(base1.getY(), base2.getY());
        }

        this.minBoundingPoint = new Location(world, Math.floor(minX), Math.floor(minY), Math.floor(minZ));
        this.maxBoundingPoint = new Location(world, Math.ceil(maxX), Math.ceil(maxY), Math.ceil(maxZ));


        this.estimatedVolume = (long) (Math.PI * this.radiusSquared * this.height);
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
        Objects.requireNonNull(location, "Location to check cannot be null");
        if (!world.equals(location.getWorld())) return false;

        Vector p = location.toVector().add(new Vector(0.5, 0.5, 0.5)); // Center of the block
        Vector p1 = baseCenter1.toVector();

        Vector p_minus_p1 = p.clone().subtract(p1);

        double projectionLength = p_minus_p1.dot(axisVector);

        if (projectionLength < 0 || projectionLength > this.height) {
            return false;
        }

        Vector projectionVector = axisVector.clone().multiply(projectionLength);
        Vector perpendicularVector = p_minus_p1.subtract(projectionVector);

        return perpendicularVector.lengthSquared() <= radiusSquared;
    }

    @NotNull
    @Override
    public Iterator<Block> getBlockIterator() {
        return new CylinderBlockIterator(this);
    }

    @NotNull
    @Override
    public String getTypeName() {
        return SelectionType.CYLINDER.getDisplayName();
    }

    @Nullable
    @Override
    public Location getCenter() {
        return baseCenter1.clone().add(baseCenter2).multiply(0.5);
    }

    public Location getBaseCenter1() {
        return baseCenter1.clone();
    }

    public Location getBaseCenter2() {
        return baseCenter2.clone();
    }

    public double getRadius() {
        return radius;
    }

    public double getHeight() {
        return height;
    }


    @NotNull
    @Override
    public Selection clone() {
        return new CylinderSelection(this.world, this.baseCenter1, this.baseCenter2, this.radius);
    }

    private static class CylinderBlockIterator implements Iterator<Block> {
        private final CylinderSelection selection;
        private final World world;
        private final int minX, minY, minZ;
        private final int maxX, maxY, maxZ;
        private int currentX, currentY, currentZ;
        private Block nextBlock;

        public CylinderBlockIterator(CylinderSelection selection) {
            this.selection = selection;
            this.world = selection.getWorld();
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
            advanceToNext();
        }

        private void advanceToNext() {
            nextBlock = null;
            while (currentZ <= maxZ) {
                while (currentY <= maxY) {
                    while (currentX <= maxX) {
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
            if (nextBlock == null) throw new NoSuchElementException();
            Block toReturn = nextBlock;
            advanceToNext();
            return toReturn;
        }
    }
}