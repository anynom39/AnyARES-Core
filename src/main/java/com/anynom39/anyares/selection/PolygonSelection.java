package com.anynom39.anyares.selection;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;

public class PolygonSelection implements Selection {

    private static final Logger LOGGER = Logger.getLogger(PolygonSelection.class.getSimpleName());

    private final World world;
    private final List<Vector2D> polygonVertices;
    private final int minY;
    private final int maxY;

    private final Location minBoundingPoint;
    private final Location maxBoundingPoint;
    private final long estimatedVolume;

    public static class Vector2D {
        public final double x;
        public final double z;

        public Vector2D(double x, double z) {
            this.x = x;
            this.z = z;
        }

        public double distanceSq(Vector2D other) {
            double dx = this.x - other.x;
            double dz = this.z - other.z;
            return dx * dx + dz * dz;
        }
    }

    public PolygonSelection(@NotNull World world, @NotNull List<Location> definingPoints, int explicitMinY, int explicitMaxY) {
        this.world = Objects.requireNonNull(world, "World cannot be null");
        Objects.requireNonNull(definingPoints, "Defining points list cannot be null");
        if (definingPoints.size() < 3) {
            throw new IllegalArgumentException("Polygon selection requires at least 3 defining points.");
        }

        this.polygonVertices = new ArrayList<>(definingPoints.size());
        for (Location loc : definingPoints) {
            if (loc == null || !world.equals(loc.getWorld())) {
                throw new IllegalArgumentException("All defining points must be non-null and in the same world as the selection.");
            }
            this.polygonVertices.add(new Vector2D(loc.getBlockX() + 0.5, loc.getBlockZ() + 0.5)); // Use block centers for polygon vertices
        }

        this.minY = Math.min(explicitMinY, explicitMaxY);
        this.maxY = Math.max(explicitMinY, explicitMaxY);
        if (this.minY > this.maxY) {
            throw new IllegalArgumentException("MinY cannot be greater than MaxY for polygon extrusion.");
        }
        if (this.minY < world.getMinHeight() || this.maxY >= world.getMaxHeight()) {
            LOGGER.warning("Polygon Y-levels (" + this.minY + " to " + this.maxY + ") are partially outside world height. Clamping may occur in iterator.");
        }


        double minPolyX = polygonVertices.get(0).x;
        double maxPolyX = polygonVertices.get(0).x;
        double minPolyZ = polygonVertices.get(0).z;
        double maxPolyZ = polygonVertices.get(0).z;

        for (int i = 1; i < polygonVertices.size(); i++) {
            Vector2D vertex = polygonVertices.get(i);
            minPolyX = Math.min(minPolyX, vertex.x);
            maxPolyX = Math.max(maxPolyX, vertex.x);
            minPolyZ = Math.min(minPolyZ, vertex.z);
            maxPolyZ = Math.max(maxPolyZ, vertex.z);
        }

        this.minBoundingPoint = new Location(world, Math.floor(minPolyX), this.minY, Math.floor(minPolyZ));
        this.maxBoundingPoint = new Location(world, Math.ceil(maxPolyX - 1), this.maxY, Math.ceil(maxPolyZ - 1)); // -1 because block coords are inclusive

        long bbWidth = (long) (maxBoundingPoint.getX() - minBoundingPoint.getX()) + 1;
        long bbLength = (long) (maxBoundingPoint.getZ() - minBoundingPoint.getZ()) + 1;
        long height = (long) (this.maxY - this.minY) + 1;
        this.estimatedVolume = bbWidth * bbLength * height;
        LOGGER.finer("PolygonSelection created. BB: " + minBoundingPoint + " to " + maxBoundingPoint + ". Vertices: " + polygonVertices.size());
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

        int blockY = location.getBlockY();
        if (blockY < minY || blockY > maxY) {
            return false;
        }

        double testX = location.getBlockX() + 0.5;
        double testZ = location.getBlockZ() + 0.5;
        int intersections = 0;
        int n = polygonVertices.size();

        for (int i = 0; i < n; i++) {
            Vector2D p1 = polygonVertices.get(i);
            Vector2D p2 = polygonVertices.get((i + 1) % n);

            if (((p1.z <= testZ && testZ < p2.z) || (p2.z <= testZ && testZ < p1.z)) &&
                    (testX < (p2.x - p1.x) * (testZ - p1.z) / (p2.z - p1.z) + p1.x)) {
                intersections++;
            }
        }
        return (intersections % 2 == 1);
    }

    @NotNull
    @Override
    public Iterator<Block> getBlockIterator() {
        return new PolygonBlockIterator(this);
    }

    @NotNull
    @Override
    public String getTypeName() {
        return SelectionType.POLYGON.getDisplayName();
    }

    @Nullable
    @Override
    public Location getCenter() {
        return new Location(world,
                (minBoundingPoint.getX() + maxBoundingPoint.getX()) / 2.0,
                (minBoundingPoint.getY() + maxBoundingPoint.getY()) / 2.0,
                (minBoundingPoint.getZ() + maxBoundingPoint.getZ()) / 2.0
        );
    }

    public List<Vector2D> getPolygonVertices() {
        return Collections.unmodifiableList(polygonVertices);
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }


    @NotNull
    @Override
    public Selection clone() {
        List<Location> definingPointsForClone = new ArrayList<>();
        for (Vector2D v2d : polygonVertices) {
            definingPointsForClone.add(new Location(world, v2d.x, this.minY, v2d.z));
        }
        return new PolygonSelection(this.world, definingPointsForClone, this.minY, this.maxY);
    }

    private static class PolygonBlockIterator implements Iterator<Block> {
        private final PolygonSelection selection;
        private final World world;
        private final int bbMinX, bbMinYClamped, bbMinZ;
        private final int bbMaxX, bbMaxYClamped, bbMaxZ;

        private int currentX, currentY, currentZ;
        private Block nextBlock;

        public PolygonBlockIterator(PolygonSelection selection) {
            this.selection = selection;
            this.world = selection.getWorld();

            Location bbMin = selection.getMinimumPoint();
            Location bbMax = selection.getMaximumPoint();

            this.bbMinX = bbMin.getBlockX();
            this.bbMinYClamped = Math.max(selection.getMinY(), world.getMinHeight());
            this.bbMinZ = bbMin.getBlockZ();
            this.bbMaxX = bbMax.getBlockX();
            this.bbMaxYClamped = Math.min(selection.getMaxY(), world.getMaxHeight() - 1);
            this.bbMaxZ = bbMax.getBlockZ();

            this.currentX = bbMinX;
            this.currentY = bbMinYClamped;
            this.currentZ = bbMinZ;
            advanceToNext();
        }

        private void advanceToNext() {
            nextBlock = null;
            while (currentZ <= bbMaxZ) {
                while (currentY <= bbMaxYClamped) {
                    while (currentX <= bbMaxX) {
                        Location testLoc = new Location(world, currentX, currentY, currentZ);
                        if (selection.contains(testLoc)) {
                            nextBlock = world.getBlockAt(currentX, currentY, currentZ);
                            currentX++;
                            return;
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