package com.anynom39.anyares.selection.hull;

import com.github.quickhull3d.Point3d;
import com.github.quickhull3d.QuickHull3D;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConvexHullAlgorithm {
    private static final Logger LOGGER = Logger.getLogger(ConvexHullAlgorithm.class.getSimpleName());

    public static class HullResult {
        public final List<Vector> hullVertices;
        public final List<int[]> hullFaces;

        public HullResult(List<Vector> hullVertices, List<int[]> hullFaces) {
            this.hullVertices = hullVertices;
            this.hullFaces = hullFaces;
        }
    }

    public static HullResult computeHull(List<Vector> inputBukkitVectors) throws IllegalArgumentException {
        if (inputBukkitVectors == null) {
            throw new IllegalArgumentException("Input points list cannot be null for hull computation.");
        }

        Set<Vector> uniqueBukkitVectors = new HashSet<>(inputBukkitVectors);

        if (uniqueBukkitVectors.size() < 4) {
            throw new IllegalArgumentException("Convex hull computation requires at least 4 unique non-coplanar points. Found " + uniqueBukkitVectors.size() + " unique points.");
        }

        Point3d[] qhPoints = new Point3d[uniqueBukkitVectors.size()];
        int idx = 0;
        for (Vector v : uniqueBukkitVectors) {
            qhPoints[idx++] = new Point3d(v.getX(), v.getY(), v.getZ());
        }

        QuickHull3D hullBuilder = new QuickHull3D();
        try {
            hullBuilder.build(qhPoints);

        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "QuickHull3D algorithm argument error (likely degenerate input points): " + e.getMessage());
            throw new IllegalArgumentException("Input points are likely degenerate (e.g., collinear or coplanar). QuickHull3D failed: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during QuickHull3D build process: " + e.getMessage(), e);
            throw new RuntimeException("Failed to compute convex hull due to an internal QuickHull3D error: " + e.getMessage(), e);
        }

        Point3d[] qhHullVerticesArray = hullBuilder.getVertices();
        if (qhHullVerticesArray == null || qhHullVerticesArray.length == 0) {
            throw new IllegalArgumentException("Hull computation by QuickHull3D resulted in no hull vertices. Input points might be degenerate.");
        }

        List<Vector> finalHullVertices = new ArrayList<>(qhHullVerticesArray.length);
        for (Point3d pv : qhHullVerticesArray) {
            finalHullVertices.add(new Vector(pv.x, pv.y, pv.z));
        }

        int[][] qhFacesArray = hullBuilder.getFaces();
        if (qhFacesArray == null || qhFacesArray.length == 0) {
            throw new IllegalArgumentException("Hull computation by QuickHull3D resulted in no hull faces, despite having vertices.");
        }

        List<int[]> finalHullFaces = new ArrayList<>(qhFacesArray.length);
        for (int[] faceIndices : qhFacesArray) {
            if (faceIndices != null && faceIndices.length == 3) {
                finalHullFaces.add(faceIndices.clone());
            } else {
                String indicesStr = faceIndices != null ? Arrays.toString(faceIndices) : "null";
                LOGGER.warning("ConvexHullAlgorithm: QuickHull3D returned a non-triangular or null face. Length: " +
                        (faceIndices != null ? faceIndices.length : "N/A") + ". Indices: " + indicesStr + ". This face will be skipped.");
            }
        }

        if (finalHullVertices.isEmpty() || finalHullFaces.isEmpty()) {
            throw new IllegalArgumentException("Hull computation resulted in an empty set of valid (triangular) faces or vertices.");
        }

        LOGGER.fine("Convex Hull computed via QuickHull3D: " + finalHullVertices.size() + " vertices, " + finalHullFaces.size() + " faces.");
        return new HullResult(finalHullVertices, finalHullFaces);
    }
}