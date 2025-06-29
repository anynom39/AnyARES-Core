package com.anynom39.anyares.selection;

import com.anynom39.anyares.selection.hull.ConvexHullAlgorithm;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;


public class ConvexHullSelection implements Selection {

    private final World world;
    private final List<Vector> inputDefiningPoints;
    private final List<Vector> hullVertices;
    private final List<int[]> hullFaces;
    private final List<Plane> geometricPlanes;

    private final BoundingBox boundingBox;
    private final long estimatedVolume;

    private static class Plane {
        final Vector normal;
        final double d;

        Plane(Vector p1, Vector p2, Vector p3, Vector centroid) {
            Vector v1 = p2.clone().subtract(p1);
            Vector v2 = p3.clone().subtract(p1);
            Vector calculatedNormal = v1.crossProduct(v2);

            if (calculatedNormal.lengthSquared() < 0.0001) {
                this.normal = new Vector(0, 0, 0);
                this.d = 0;
                System.err.println("ConvexHullSelection.Plane: Collinear points provided for plane definition.");
                return;
            }
            calculatedNormal.normalize();

            if (calculatedNormal.dot(p1.clone().subtract(centroid)) < 0) {
                calculatedNormal.multiply(-1);
            }
            this.normal = calculatedNormal;
            this.d = this.normal.dot(p1);
        }

        boolean isPointInside(Vector point) {
            return (normal.dot(point) - d) <= 0.001;
        }
    }


    public ConvexHullSelection(@NotNull World world, @NotNull List<Location> definingLocations) {
        this.world = Objects.requireNonNull(world, "World cannot be null");
        Objects.requireNonNull(definingLocations, "Defining locations cannot be null");
        if (definingLocations.size() < 4) {
            throw new IllegalArgumentException("Convex Hull selection requires at least 4 defining points.");
        }

        this.inputDefiningPoints = definingLocations.stream()
                .map(Location::toVector)
                .collect(Collectors.toList());

        ConvexHullAlgorithm.HullResult hullResult;
        try {
            Set<Vector> uniquePoints = new HashSet<>(this.inputDefiningPoints);
            if (uniquePoints.size() < 4) throw new IllegalArgumentException("Not enough unique points for a 3D hull.");
            hullResult = ConvexHullAlgorithm.computeHull(new ArrayList<>(uniquePoints));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to compute convex hull: " + e.getMessage(), e);
        }

        this.hullVertices = hullResult.hullVertices;
        this.hullFaces = hullResult.hullFaces;

        if (this.hullVertices.isEmpty() || this.hullFaces.isEmpty()) {
            throw new IllegalArgumentException("Convex hull computation resulted in an empty hull.");
        }

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;
        for (Vector v : this.hullVertices) {
            minX = Math.min(minX, v.getX());
            minY = Math.min(minY, v.getY());
            minZ = Math.min(minZ, v.getZ());
            maxX = Math.max(maxX, v.getX());
            maxY = Math.max(maxY, v.getY());
            maxZ = Math.max(maxZ, v.getZ());
        }
        this.boundingBox = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);

        this.geometricPlanes = new ArrayList<>();
        Vector hullCentroid = new Vector(0, 0, 0);
        for (Vector v : this.hullVertices) hullCentroid.add(v);
        hullCentroid.multiply(1.0 / this.hullVertices.size());

        for (int[] faceIndices : this.hullFaces) {
            if (faceIndices.length < 3) continue;
            Vector p1 = this.hullVertices.get(faceIndices[0]);
            Vector p2 = this.hullVertices.get(faceIndices[1]);
            Vector p3 = this.hullVertices.get(faceIndices[2]);
            geometricPlanes.add(new Plane(p1, p2, p3, hullCentroid));
        }

        this.estimatedVolume = (long) (this.boundingBox.getWidthX() * this.boundingBox.getHeight() * this.boundingBox.getWidthZ());
    }

    public List<Vector> getHullVertices() {
        return Collections.unmodifiableList(hullVertices);
    }

    public List<int[]> getHullFaces() {
        return Collections.unmodifiableList(hullFaces);
    }

    @NotNull
    @Override
    public World getWorld() {
        return world;
    }

    @NotNull
    @Override
    public Location getMinimumPoint() {
        return new Location(world, boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ());
    }

    @NotNull
    @Override
    public Location getMaximumPoint() {
        return new Location(world, boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ());
    }

    @Override
    public long getVolume() {
        return estimatedVolume;
    }

    @Override
    public boolean contains(@NotNull Location location) {
        Objects.requireNonNull(location, "Location cannot be null");
        if (!this.world.equals(location.getWorld())) return false;
        Vector p = location.toVector().add(new Vector(0.5, 0.5, 0.5)); // Block center
        if (!this.boundingBox.contains(p)) return false; // Quick AABB check

        for (Plane plane : geometricPlanes) {
            if (!plane.isPointInside(p)) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    @Override
    public Iterator<Block> getBlockIterator() {
        return new ConvexHullBlockIterator(this);
    }

    @Override
    public @NotNull String getTypeName() {
        return "";
    }

    @Nullable
    @Override
    public Location getCenter() {
        return new Location(world, boundingBox.getCenterX(), boundingBox.getCenterY(), boundingBox.getCenterZ());
    }

    @NotNull
    @Override
    public Selection clone() {
        List<Location> definingLocs = new ArrayList<>();
        for (Vector v : this.inputDefiningPoints) definingLocs.add(new Location(world, v.getX(), v.getY(), v.getZ()));
        return new ConvexHullSelection(this.world, definingLocs);
    }

    private static class ConvexHullBlockIterator implements Iterator<Block> {
        private final ConvexHullSelection selection;
        private final World world;
        private final int minX, minYClamped, minZ;
        private final int maxX, maxYClamped, maxZ;
        private int currentX, currentY, currentZ;
        private Block nextBlock;

        public ConvexHullBlockIterator(ConvexHullSelection selection) {
            this.selection = selection;
            this.world = selection.getWorld();
            BoundingBox bb = selection.boundingBox;
            this.minX = (int) Math.floor(bb.getMinX());
            this.minYClamped = Math.max((int) Math.floor(bb.getMinY()), world.getMinHeight());
            this.minZ = (int) Math.floor(bb.getMinZ());
            this.maxX = (int) Math.floor(bb.getMaxX());
            this.maxYClamped = Math.min((int) Math.floor(bb.getMaxY()), world.getMaxHeight() - 1);
            this.maxZ = (int) Math.floor(bb.getMaxZ());
            this.currentX = minX;
            this.currentY = minYClamped;
            this.currentZ = minZ;
            advanceToNext();
        }

        private void advanceToNext() {
            nextBlock = null;
            while (currentZ <= maxZ) {
                while (currentY <= maxYClamped) {
                    while (currentX <= maxX) {
                        if (selection.contains(new Location(world, currentX, currentY, currentZ))) {
                            nextBlock = world.getBlockAt(currentX, currentY, currentZ);
                            currentX++;
                            return;
                        }
                        currentX++;
                    }
                    currentX = minX;
                    currentY++;
                }
                currentY = minYClamped;
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
            Block r = nextBlock;
            advanceToNext();
            return r;
        }
    }
}