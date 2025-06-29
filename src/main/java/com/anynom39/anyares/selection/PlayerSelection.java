package com.anynom39.anyares.selection;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PlayerSelection {
    private final List<Location> definingPoints;
    private SelectionType currentSelectionType;
    private Selection activeSelection;
    private World world;

    private Integer customPolygonMinY = null;
    private Integer customPolygonMaxY = null;

    private static final Logger LOGGER = Logger.getLogger(PlayerSelection.class.getSimpleName());

    public PlayerSelection() {
        this.definingPoints = new ArrayList<>();
        this.currentSelectionType = SelectionType.CUBOID;
        this.activeSelection = null;
        this.world = null;
    }

    @NotNull
    public List<Location> getDefiningPoints() {
        List<Location> pointsCopy = new ArrayList<>();
        for (Location loc : definingPoints) {
            if (loc != null) {
                pointsCopy.add(loc.clone());
            }
        }
        return pointsCopy;
    }

    @Nullable
    public World getSelectionWorld() {
        return world;
    }

    public void setSelectionWorld(@Nullable World newWorld) {
        if (!Objects.equals(this.world, newWorld)) {
            LOGGER.finer("Selection world changing from " + (this.world != null ? this.world.getName() : "null") +
                    " to " + (newWorld != null ? newWorld.getName() : "null") + ". Clearing points.");
            clearPointsAndSelectionInternal(true);
            this.world = newWorld;
        }
    }

    public void addDefiningPoint(@NotNull Location point) {
        Objects.requireNonNull(point, "Defining point cannot be null");
        Location clonedPoint = point.clone();
        Objects.requireNonNull(clonedPoint.getWorld(), "Defining point's world cannot be null");

        if (this.world == null) {
            setSelectionWorld(clonedPoint.getWorld());
        } else if (!this.world.equals(clonedPoint.getWorld())) {
            LOGGER.info("Player selected a point in a new world (" + clonedPoint.getWorld().getName() +
                    "). Current selection world (" + (this.world != null ? this.world.getName() : "null") + "). Clearing previous selection.");
            setSelectionWorld(clonedPoint.getWorld());
        }

        int maxPoints = getMaxDefiningPointsForType(this.currentSelectionType);

        if (this.currentSelectionType == SelectionType.POLYGON) {
            if (definingPoints.size() < maxPoints) {
                definingPoints.add(clonedPoint);
            } else {
                LOGGER.info("Max points (" + maxPoints + ") for " + this.currentSelectionType.getDisplayName() + ". Point not added: " + clonedPoint.toString());
                return;
            }
        } else {
            if (definingPoints.size() < maxPoints) {
                definingPoints.add(clonedPoint);
            } else {
                if (maxPoints > 0) {
                    for (int i = 0; i < maxPoints - 1; i++) {
                        definingPoints.set(i, definingPoints.get(i + 1));
                    }
                    definingPoints.set(maxPoints - 1, clonedPoint);
                } else {
                    definingPoints.clear();
                    if (maxPoints > 0) definingPoints.add(clonedPoint);
                }
            }
        }
        recalculateActiveSelection();
    }

    public void setDefiningPoint(int index, @NotNull Location point) {
        Objects.requireNonNull(point, "Defining point cannot be null");
        Location clonedPoint = point.clone();
        Objects.requireNonNull(clonedPoint.getWorld(), "Defining point's world cannot be null");

        if (this.world == null) {
            setSelectionWorld(clonedPoint.getWorld());
        } else if (!this.world.equals(clonedPoint.getWorld())) {
            LOGGER.info("Player set point in new world (" + clonedPoint.getWorld().getName() +
                    "). Current (" + (this.world != null ? this.world.getName() : "null") + "). Clearing previous selection.");
            setSelectionWorld(clonedPoint.getWorld());
        }

        int maxPoints = getMaxDefiningPointsForType(this.currentSelectionType);
        if (index < 0 || index >= maxPoints) {
            LOGGER.warning("Invalid index " + index + " for type " + this.currentSelectionType + " (max " + maxPoints + "). Ignoring.");
            return;
        }

        while (definingPoints.size() <= index) {
            definingPoints.add(null);
        }
        definingPoints.set(index, clonedPoint);
        recalculateActiveSelection();
    }

    public boolean removeLastDefiningPoint() {
        if (!definingPoints.isEmpty()) {
            definingPoints.remove(definingPoints.size() - 1);
            recalculateActiveSelection();
            LOGGER.finer("Removed last defining point. Points remaining: " + definingPoints.size());
            return true;
        }
        LOGGER.finer("Attempted to remove last point, but list empty.");
        return false;
    }

    @NotNull
    public SelectionType getCurrentSelectionType() {
        return currentSelectionType;
    }

    public void setCurrentSelectionType(@NotNull SelectionType newType) {
        Objects.requireNonNull(newType, "SelectionType cannot be null");
        if (this.currentSelectionType != newType) {
            LOGGER.finer("Player selection type changing from " + this.currentSelectionType + " to " + newType);
            SelectionType oldType = this.currentSelectionType;
            this.currentSelectionType = newType;

            this.customPolygonMinY = null;
            this.customPolygonMaxY = null;

            int maxPointsForNewType = getMaxDefiningPointsForType(newType);
            boolean needsPointPruning = ((oldType == SelectionType.POLYGON) &&
                    (newType != SelectionType.POLYGON)
            ) || (definingPoints.size() > maxPointsForNewType);

            if (needsPointPruning) {
                LOGGER.finer("Pruning defining points from " + definingPoints.size() + " to fit max " + maxPointsForNewType + " for " + newType);
                while (definingPoints.size() > maxPointsForNewType) {
                    definingPoints.remove(0);
                }
            }
        }
    }

    public void setCustomPolygonYLevels(int minY, int maxY) {
        this.customPolygonMinY = Math.min(minY, maxY);
        this.customPolygonMaxY = Math.max(minY, maxY);
        LOGGER.finer("Custom polygon Y levels set: " + this.customPolygonMinY + " to " + this.customPolygonMaxY);
        if (this.currentSelectionType == SelectionType.POLYGON) {
            recalculateActiveSelection();
        }
    }

    public void clearCustomPolygonYLevels() {
    }

    @Nullable
    public Selection getActiveSelection() {
        if (activeSelection != null) {
            if (world == null || !world.equals(activeSelection.getWorld())) {
                LOGGER.finer("Active selection world (" + (activeSelection.getWorld() != null ? activeSelection.getWorld().getName() : "null") +
                        ") mismatches PlayerSelection world (" + (world != null ? world.getName() : "null") + "). Invalidating.");
                recalculateActiveSelection();
            }
        }
        return activeSelection;
    }

    public void clearPointsAndSelectionInternal(boolean clearWorldAlso) {
    }

    public void clearSelection() {
    }

    private int getMaxDefiningPointsForType(SelectionType type) {
        return switch (type) {
            case CUBOID, SPHERE -> 2;
            case CYLINDER -> 3;
            case ELLIPSOID -> 4;
            case POLYGON -> 50;
        };
    }

    public void recalculateActiveSelection() {
        if (world == null) {
            activeSelection = null;
            LOGGER.finer("Recalc skipped: World null.");
            return;
        }

        int requiredPoints = 0;
        switch (currentSelectionType) {
            case CUBOID:
                requiredPoints = 2;
                break;
            case SPHERE:
                requiredPoints = 1;
                break;
            case CYLINDER:
                requiredPoints = 2;
                break;
            case ELLIPSOID:
                requiredPoints = 1;
                break;
            case POLYGON:
                requiredPoints = 3;
                break;
        }

        if (definingPoints.size() < requiredPoints) {
            activeSelection = null;
            LOGGER.finer("Recalc: Not enough points (" + definingPoints.size() + "/" + requiredPoints + ") for " + currentSelectionType);
            return;
        }
        for (int i = 0; i < definingPoints.size(); i++) {
            Location p = definingPoints.get(i);
            if (p == null) {
                if (i < requiredPoints) {
                    activeSelection = null;
                    LOGGER.finer("Recalc: Essential point " + i + " null for " + currentSelectionType);
                    return;
                }
            } else if (!world.equals(p.getWorld())) {
                activeSelection = null;
                LOGGER.warning("Recalc: Mismatched world for point " + i + ". Active selection cleared.");
                return;
            }
        }

        try {
            switch (currentSelectionType) {
                case CUBOID:
                    activeSelection = new CuboidSelection(world, definingPoints.get(0), definingPoints.get(1));
                    break;
                case SPHERE:
                    Location centerS = definingPoints.get(0);
                    double radiusS = 5.0;
                    if (definingPoints.size() >= 2 && definingPoints.get(1) != null) {
                        radiusS = centerS.distance(definingPoints.get(1));
                        if (radiusS < 0.5) radiusS = 0.5;
                    }
                    activeSelection = new SphereSelection(world, centerS, radiusS);
                    break;
                case CYLINDER:
                    Location base1C = definingPoints.get(0);
                    Location base2C = definingPoints.get(1);
                    double cylRad = 5.0;
                    if (definingPoints.size() >= 3 && definingPoints.get(2) != null) {
                        Location radPtC = definingPoints.get(2);
                        Vector pT = radPtC.toVector();
                        Vector pAS = base1C.toVector();
                        Vector axLDir = base2C.toVector().subtract(pAS);
                        if (axLDir.lengthSquared() < 0.0001) {
                            cylRad = pT.distance(pAS);
                        } else {
                            axLDir.normalize();
                            Vector vFSTT = pT.subtract(pAS);
                            cylRad = vFSTT.subtract(axLDir.multiply(vFSTT.dot(axLDir))).length();
                        }
                        if (cylRad < 0.5) cylRad = 0.5;
                    }
                    activeSelection = new CylinderSelection(world, base1C, base2C, cylRad);
                    break;
                case ELLIPSOID:
                    Location centerE = definingPoints.get(0);
                    double rX = 5, rY = 5, rZ = 5;
                    if (definingPoints.size() >= 2 && definingPoints.get(1) != null) {
                        Location p1E = definingPoints.get(1);
                        rX = Math.max(0.5, Math.abs(centerE.getX() - p1E.getX()));
                        if (definingPoints.size() == 2) {
                            rY = Math.max(0.5, Math.abs(centerE.getY() - p1E.getY()));
                            rZ = Math.max(0.5, Math.abs(centerE.getZ() - p1E.getZ()));
                        }
                    }
                    if (definingPoints.size() >= 3 && definingPoints.get(2) != null) {
                        Location p2E = definingPoints.get(2);
                        rY = Math.max(0.5, Math.abs(centerE.getY() - p2E.getY()));
                        if (definingPoints.size() == 3) rZ = rX;
                    } // If 3 points, p2 for Y, Z defaults to X
                    if (definingPoints.size() >= 4 && definingPoints.get(3) != null) {
                        Location p3E = definingPoints.get(3);
                        rZ = Math.max(0.5, Math.abs(centerE.getZ() - p3E.getZ()));
                    }
                    activeSelection = new EllipsoidSelection(world, centerE, rX, rY, rZ);
                    break;
                case POLYGON:
                    int polyMinY, polyMaxY;
                    if (customPolygonMinY != null && customPolygonMaxY != null) {
                        polyMinY = customPolygonMinY;
                        polyMaxY = customPolygonMaxY;
                    } else {
                        polyMinY = definingPoints.get(0).getBlockY();
                        polyMaxY = definingPoints.get(0).getBlockY();
                        for (Location p : definingPoints) {
                            if (p == null) continue;
                            polyMinY = Math.min(polyMinY, p.getBlockY());
                            polyMaxY = Math.max(polyMaxY, p.getBlockY());
                        }
                    }
                    List<Location> validPolyPoints = definingPoints.stream().filter(Objects::nonNull).collect(Collectors.toList());
                    if (validPolyPoints.size() < 3) {
                        activeSelection = null;
                        LOGGER.finer("Not enough valid points for Polygon.");
                        break;
                    }
                    activeSelection = new PolygonSelection(world, validPolyPoints, polyMinY, polyMaxY);
                    break;
                default:
                    LOGGER.warning("Unknown selection type for recalculation: " + currentSelectionType);
                    activeSelection = null;
                    break;
            }
            if (activeSelection != null) {
                LOGGER.finer("Recalculated: " + activeSelection.getTypeName() + ", Vol: " + activeSelection.getVolume());
            } else {
                LOGGER.finer("Recalc resulted in no active selection (type: " + currentSelectionType + ").");
            }
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Error creating " + currentSelectionType + ": " + e.getMessage());
            activeSelection = null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error recalculating " + currentSelectionType, e);
            activeSelection = null;
        }
    }

    @Nullable
    public Location getPos1() {
        return definingPoints.isEmpty() ? null : definingPoints.get(0);
    }

    @Nullable
    public Location getPos2() {
        return definingPoints.size() < 2 ? null : definingPoints.get(1);
    }

    @Nullable
    public Location getPos3() {
        return definingPoints.size() < 3 ? null : definingPoints.get(2);
    }

    @Nullable
    public Location getPos4() {
        return definingPoints.size() < 4 ? null : definingPoints.get(3);
    }
}