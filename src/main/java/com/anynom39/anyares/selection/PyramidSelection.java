package com.anynom39.anyares.selection;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class PyramidSelection implements Selection {


    private static final boolean DEBUG_PYRAMID_CONSTRUCTOR = true;
    private static final boolean DEBUG_PYRAMID_CONTAINS = true;
    private static final boolean DEBUG_PYRAMID_ITERATOR = true;

    private static final double EPSILON = 0.001;

    private final World world;
    private final Location baseCorner1Input;
    private final Location baseCorner2Input;
    private final Location apexInput;

    private final BoundingBox boundingBox;
    private final Plane geometricBasePlane;
    private final List<Plane> sidePlanes;

    private static class Plane {
        final Vector normal;
        final double dValue;

        Plane(String planeName, Vector p1, Vector p2, Vector p3Defining, Vector pTestInsideHull) {
            Vector v1 = p2.clone().subtract(p1);
            Vector v2 = p3Defining.clone().subtract(p1);
            Vector calcNormal = v1.crossProduct(v2);

            if (calcNormal.lengthSquared() < EPSILON * EPSILON) {
                if (DEBUG_PYRAMID_CONSTRUCTOR)
                    System.out.println("Pyramid.Plane[" + planeName + "]: Degenerate plane for normal calc: p1=" + p1 + " p2=" + p2 + " p3Def=" + p3Defining);
                this.normal = new Vector(0, 0, 0);
                this.dValue = 0;
                return;
            }
            calcNormal.normalize();

            if (calcNormal.dot(pTestInsideHull.clone().subtract(p1)) > 0) {
                if (DEBUG_PYRAMID_CONSTRUCTOR)
                    System.out.println("Pyramid.Plane[" + planeName + "]: Flipping normal. Original normal pointed towards test_inside_hull.");
                calcNormal.multiply(-1);
            }
            this.normal = calcNormal;
            this.dValue = this.normal.dot(p1);
            if (DEBUG_PYRAMID_CONSTRUCTOR)
                System.out.println("Pyramid.Plane[" + planeName + "] CREATED: Normal=" + this.normal + ", D=" + this.dValue);
        }

        Plane(String planeName, Vector pointOnPlane, Vector alreadyOrientedNormal) {
            this.normal = alreadyOrientedNormal.clone().normalize();
            this.dValue = this.normal.dot(pointOnPlane);
            if (DEBUG_PYRAMID_CONSTRUCTOR)
                System.out.println("Pyramid.Plane[" + planeName + "] CREATED (Oriented): Normal=" + this.normal + ", D=" + this.dValue);
        }

        double signedDistance(Vector point) {
            if (this.normal.lengthSquared() < EPSILON * EPSILON) {
                return Double.POSITIVE_INFINITY;
            }
            return this.normal.dot(point) - this.dValue;
        }
    }

    public PyramidSelection(@NotNull World world, @NotNull Location baseC1In, @NotNull Location baseC2In, @NotNull Location apexPointIn) {
        if (DEBUG_PYRAMID_CONSTRUCTOR)
            System.out.println("\n[CONSTRUCTOR] PyramidSelection: Constructing with baseC1=" + baseC1In + ", baseC2=" + baseC2In + ", apex=" + apexPointIn);
        this.world = Objects.requireNonNull(world, "World cannot be null");
        this.baseCorner1Input = baseC1In.clone();
        this.baseCorner2Input = baseC2In.clone();
        this.apexInput = apexPointIn.clone();

        if (!world.equals(baseCorner1Input.getWorld()) || !world.equals(baseCorner2Input.getWorld()) || !world.equals(apexInput.getWorld())) {
            throw new IllegalArgumentException("All pyramid defining points must be in the same world.");
        }

        double baseYLevel = Math.min(baseCorner1Input.getY(), baseCorner2Input.getY());
        Vector bV0 = new Vector(Math.min(baseCorner1Input.getX(), baseCorner2Input.getX()), baseYLevel, Math.min(baseCorner1Input.getZ(), baseCorner2Input.getZ()));
        Vector bV1 = new Vector(Math.max(baseCorner1Input.getX(), baseCorner2Input.getX()), baseYLevel, Math.min(baseCorner1Input.getZ(), baseCorner2Input.getZ()));
        Vector bV2 = new Vector(Math.max(baseCorner1Input.getX(), baseCorner2Input.getX()), baseYLevel, Math.max(baseCorner1Input.getZ(), baseCorner2Input.getZ()));
        Vector bV3 = new Vector(Math.min(baseCorner1Input.getX(), baseCorner2Input.getX()), baseYLevel, Math.max(baseCorner1Input.getZ(), baseCorner2Input.getZ()));
        Vector apexV = apexInput.toVector();
        if (DEBUG_PYRAMID_CONSTRUCTOR) {
            System.out.println("[CONSTRUCTOR] Base Vertices (CCW from minX,minZ): V0=" + bV0 + ", V1=" + bV1 + ", V2=" + bV2 + ", V3=" + bV3 + " | Apex: " + apexV);
        }

        BoundingBox tempBox = BoundingBox.of(bV0, apexV);
        tempBox.expand(bV1);
        tempBox.expand(bV2);
        tempBox.expand(bV3);
        this.boundingBox = tempBox;
        if (DEBUG_PYRAMID_CONSTRUCTOR)
            System.out.println("[CONSTRUCTOR] BoundingBox: Min=" + boundingBox.getMin() + ", Max=" + boundingBox.getMax());

        Vector basePlaneCalcNormal;
        Vector baseEdge1 = bV1.clone().subtract(bV0);
        Vector baseEdge2 = bV3.clone().subtract(bV0);
        basePlaneCalcNormal = baseEdge1.crossProduct(baseEdge2);
        if (basePlaneCalcNormal.lengthSquared() < EPSILON * EPSILON) {
            basePlaneCalcNormal = new Vector(0, 1, 0);
            if (DEBUG_PYRAMID_CONSTRUCTOR)
                System.out.println("[CONSTRUCTOR] Base plane degenerate, defaulting normal to Y-up.");
        }
        basePlaneCalcNormal.normalize();
        if (basePlaneCalcNormal.dot(apexV.clone().subtract(bV0)) < 0) {
            basePlaneCalcNormal.multiply(-1);
            if (DEBUG_PYRAMID_CONSTRUCTOR)
                System.out.println("[CONSTRUCTOR] Base plane normal flipped to point towards apex half-space.");
        }
        this.geometricBasePlane = new Plane("Base", bV0, basePlaneCalcNormal);
        if (DEBUG_PYRAMID_CONSTRUCTOR)
            System.out.println("[CONSTRUCTOR] Final Base Plane: Normal=" + geometricBasePlane.normal + ", D=" + geometricBasePlane.dValue);

        this.sidePlanes = new ArrayList<>(4);
        Vector pyramidCentroid = bV0.clone().add(bV1).add(bV2).add(bV3).add(apexV).multiply(0.2);
        if (DEBUG_PYRAMID_CONSTRUCTOR)
            System.out.println("[CONSTRUCTOR] Pyramid Centroid (for side plane orientation): " + pyramidCentroid);

        addSidePlaneToList("Side01", bV0, bV1, apexV, pyramidCentroid);
        addSidePlaneToList("Side12", bV1, bV2, apexV, pyramidCentroid);
        addSidePlaneToList("Side23", bV2, bV3, apexV, pyramidCentroid);
        addSidePlaneToList("Side30", bV3, bV0, apexV, pyramidCentroid);
        if (DEBUG_PYRAMID_CONSTRUCTOR)
            System.out.println("[CONSTRUCTOR] PyramidSelection: Construction complete. Sides defined: " + sidePlanes.size());
    }

    private void addSidePlaneToList(String name, Vector p1_base, Vector p2_base, Vector apex, Vector centroid_test) {
        if (DEBUG_PYRAMID_CONSTRUCTOR)
            System.out.println("Adding side plane [" + name + "] for base edge: " + p1_base + " -> " + p2_base + " and apex: " + apex);
        Plane plane = new Plane(name, p1_base, p2_base, apex, centroid_test);
        if (plane.normal.lengthSquared() > EPSILON * EPSILON) {
            this.sidePlanes.add(plane);
        } else {
            if (DEBUG_PYRAMID_CONSTRUCTOR) System.out.println("SKIPPED adding degenerate side plane [" + name + "].");
        }
    }

    @NotNull
    public Location getBaseCorner1Input() {
        return baseCorner1Input.clone();
    }

    @NotNull
    public Location getBaseCorner2Input() {
        return baseCorner2Input.clone();
    }

    @NotNull
    public Location getApexInput() {
        return apexInput.clone();
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
        double bWidth = Math.abs(baseCorner1Input.getX() - baseCorner2Input.getX());
        double bLength = Math.abs(baseCorner1Input.getZ() - baseCorner2Input.getZ());
        if (bWidth < 1 && Math.abs(bWidth) > EPSILON) bWidth = 1;
        else if (Math.abs(bWidth) < EPSILON) bWidth = 0;
        if (bLength < 1 && Math.abs(bLength) > EPSILON) bLength = 1;
        else if (Math.abs(bLength) < EPSILON) bLength = 0;
        double bArea = bWidth * bLength;
        double h = Math.abs(geometricBasePlane.signedDistance(apexInput.toVector()));
        if (DEBUG_PYRAMID_CONSTRUCTOR && h < EPSILON && geometricBasePlane.normal.lengthSquared() > EPSILON) {
            System.out.println("[VOLUME CALC] Apex appears to be on the base plane (height near zero). Dist: " + geometricBasePlane.signedDistance(apexInput.toVector()));
        }
        return (long) (bArea * h / 3.0);
    }

    @Override
    public boolean contains(@NotNull Location location) {
        Objects.requireNonNull(location, "Location cannot be null");
        if (!this.world.equals(location.getWorld())) return false;

        Vector p = location.toVector().add(new Vector(0.5, 0.5, 0.5));

        boolean targetDebugBlock = false;

        if (DEBUG_PYRAMID_CONTAINS && targetDebugBlock) {
            System.out.println("\n--- Pyramid.contains() for TARGET BLOCK: " + location + " (center: " + p + ") ---");
            System.out.println("BoundingBox: Min=" + boundingBox.getMin() + ", Max=" + boundingBox.getMax());
        }

        if (!this.boundingBox.contains(p)) {
            if (DEBUG_PYRAMID_CONTAINS && targetDebugBlock) System.out.println("Result: FAILED BoundingBox check.");
            return false;
        }
        if (DEBUG_PYRAMID_CONTAINS && targetDebugBlock) System.out.println("Result: Passed BoundingBox check.");

        double baseDist = geometricBasePlane.signedDistance(p);
        if (DEBUG_PYRAMID_CONTAINS && targetDebugBlock)
            System.out.println("BasePlane (" + geometricBasePlane.normal + "): dist=" + String.format("%.4f", baseDist));
        if (baseDist < -EPSILON) {
            if (DEBUG_PYRAMID_CONTAINS && targetDebugBlock)
                System.out.println("Result: FAILED Base Plane check (point is 'below' base relative to apex).");
            return false;
        }
        if (DEBUG_PYRAMID_CONTAINS && targetDebugBlock) System.out.println("Result: Passed Base Plane check.");

        for (int i = 0; i < sidePlanes.size(); i++) {
            Plane sidePlane = sidePlanes.get(i);
            if (sidePlane.normal.lengthSquared() < EPSILON * EPSILON) {
                if (DEBUG_PYRAMID_CONTAINS && targetDebugBlock)
                    System.out.println("SidePlane " + i + ": DEGENERATE (normal is zero). Skipping this plane check.");
                continue;
            }
            double sideDist = sidePlane.signedDistance(p);
            if (DEBUG_PYRAMID_CONTAINS && targetDebugBlock)
                System.out.println("SidePlane " + i + " (" + sidePlane.normal + "): dist=" + String.format("%.4f", sideDist));
            if (sideDist > EPSILON) {
                if (DEBUG_PYRAMID_CONTAINS && targetDebugBlock)
                    System.out.println("Result: FAILED Side Plane " + i + " check (point is 'outside').");
                return false;
            }
        }
        if (DEBUG_PYRAMID_CONTAINS && targetDebugBlock)
            System.out.println("Result: PASSED ALL CHECKS. Point is INSIDE.");
        return true;
    }

    @NotNull
    @Override
    public Iterator<Block> getBlockIterator() {
        return new PyramidBlockIterator(this);
    }

    @Nullable
    @Override
    public Location getCenter() {
        double baseYLevel = Math.min(baseCorner1Input.getY(), baseCorner2Input.getY());
        Vector bV0 = new Vector(Math.min(baseCorner1Input.getX(), baseCorner2Input.getX()), baseYLevel, Math.min(baseCorner1Input.getZ(), baseCorner2Input.getZ()));
        Vector bV1 = new Vector(Math.max(baseCorner1Input.getX(), baseCorner2Input.getX()), baseYLevel, Math.min(baseCorner1Input.getZ(), baseCorner2Input.getZ()));
        Vector bV2 = new Vector(Math.max(baseCorner1Input.getX(), baseCorner2Input.getX()), baseYLevel, Math.max(baseCorner1Input.getZ(), baseCorner2Input.getZ()));
        Vector bV3 = new Vector(Math.min(baseCorner1Input.getX(), baseCorner2Input.getX()), baseYLevel, Math.max(baseCorner1Input.getZ(), baseCorner2Input.getZ()));
        Vector baseCentroid = bV0.add(bV1).add(bV2).add(bV3).multiply(0.25);
        Vector apexV = apexInput.toVector();
        Vector centerV = baseCentroid.multiply(0.75).add(apexV.multiply(0.25));
        return new Location(world, centerV.getX(), centerV.getY(), centerV.getZ());
    }

    @NotNull
    @Override
    public Selection clone() {
        return new PyramidSelection(this.world, this.baseCorner1Input, this.baseCorner2Input, this.apexInput) {
            @Override
            public @NotNull String getTypeName() {
                return "";
            }
        };
    }

    private static class PyramidBlockIterator implements Iterator<Block> {
        private final PyramidSelection selection;
        private final World world;
        private final int minX, minYClamped, minZ;
        private final int maxX, maxYClamped, maxZ;
        private int currentX, currentY, currentZ;
        private Block nextBlock;

        public PyramidBlockIterator(PyramidSelection selection) {
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
            if (DEBUG_PYRAMID_ITERATOR)
                System.out.println("[ITERATOR] PyramidBlockIterator: Range X(" + minX + "-" + maxX + "), Y(" + minYClamped + "-" + maxYClamped + "), Z(" + minZ + "-" + maxZ + ")");
            advanceToNext();
        }

        private void advanceToNext() {
            nextBlock = null;
            while (currentZ <= maxZ) {
                while (currentY <= maxYClamped) {
                    while (currentX <= maxX) {
                        Location testLoc = new Location(world, currentX, currentY, currentZ);
                        if (selection.contains(testLoc)) {
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