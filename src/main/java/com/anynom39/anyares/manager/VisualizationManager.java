package com.anynom39.anyares.manager;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.api.AnyAresAPI;
import com.anynom39.anyares.selection.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VisualizationManager {
    private final AnyARES_Core plugin;

    private boolean enabled;
    private final Particle PARTICLE_TYPE = Particle.FLAME;
    private int refreshIntervalTicks;
    private double maxViewDistanceSquared;
    private double particleDensity;

    private final Map<UUID, BukkitTask> activeVisualTasks = new ConcurrentHashMap<>();

    public VisualizationManager(AnyARES_Core plugin) {
        this.plugin = plugin;
        loadConfigValues();
        plugin.getLogger().info("VisualizationManager initialized. Particle type: FLAME. Enabled: " + enabled);
    }

    public void loadConfigValues() {
        this.enabled = plugin.getConfig().getBoolean("core-settings.visualization.enabled", true);
        this.refreshIntervalTicks = plugin.getConfig().getInt("core-settings.visualization.refresh-interval-ticks", 10);
        if (this.refreshIntervalTicks <= 0) this.refreshIntervalTicks = 10;
        double maxDist = plugin.getConfig().getDouble("core-settings.visualization.max-view-distance", 64);
        this.maxViewDistanceSquared = maxDist * maxDist;
        this.particleDensity = plugin.getConfig().getDouble("core-settings.visualization.particle-density", 0.5);
        if (this.particleDensity <= 0) this.particleDensity = 0.5;
    }

    public void reloadConfigValues() {
        plugin.reloadConfig();
        loadConfigValues();
        SelectionManager sm = null;
        if (AnyAresAPI.isAvailable()) {
            try {
                sm = AnyAresAPI.getSelectionManager();
            } catch (IllegalStateException e) {
                plugin.getLogger().warning("VizMan Reload: SM not available.");
            }
        } else {
            plugin.getLogger().warning("VizMan Reload: API not available.");
        }
        if (sm == null) return;
        final SelectionManager finalSm = sm;
        Bukkit.getOnlinePlayers().forEach(player -> {
            Selection activeSelection = finalSm.getActiveSelection(player);
            updateSelectionVisuals(player, activeSelection);
        });
        plugin.getLogger().info("VisualizationManager config reloaded.");
    }

    public void updateSelectionVisuals(Player player, @Nullable Selection activeSelection) {
        if (!enabled) {
            clearSelectionVisuals(player);
            return;
        }
        UUID playerUUID = player.getUniqueId();
        clearTask(playerUUID);
        if (activeSelection != null) {
            startVisualTask(player, activeSelection);
        } else {
            if (AnyAresAPI.isAvailable()) {
                SelectionManager sm = null;
                try {
                    sm = AnyAresAPI.getSelectionManager();
                } catch (IllegalStateException ignored) {
                }
                if (sm != null) {
                    PlayerSelection psState = sm.getPlayerSelectionState(player);
                    if (psState != null) {
                        List<Location> defPts = psState.getDefiningPoints();
                        Location p1 = defPts.size() > 0 ? defPts.get(0) : null;
                        Location p2 = defPts.size() > 1 ? defPts.get(1) : null;
                        startVisualTaskForPoints(player, p1, p2);
                    }
                }
            }
        }
    }

    public void updateSelectionVisuals(Player player, @Nullable Location pos1, @Nullable Location pos2) {
        if (!enabled) {
            clearSelectionVisuals(player);
            return;
        }
        UUID playerUUID = player.getUniqueId();
        clearTask(playerUUID);
        if (pos1 != null || pos2 != null) {
            startVisualTaskForPoints(player, pos1, pos2);
        }
    }

    private void startVisualTask(Player player, @NotNull Selection initialSelection) {
        UUID playerUUID = player.getUniqueId();
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !AnyAresAPI.isAvailable()) {
                    this.cancel();
                    activeVisualTasks.remove(playerUUID);
                    return;
                }
                SelectionManager sm = null;
                try {
                    sm = AnyAresAPI.getSelectionManager();
                } catch (IllegalStateException e) {
                    this.cancel();
                    activeVisualTasks.remove(playerUUID);
                    return;
                }
                Selection curSel = sm.getActiveSelection(player);
                if (curSel == null) {
                    this.cancel();
                    activeVisualTasks.remove(playerUUID);
                    return;
                }
                Location chkPt = curSel.getCenter();
                if (chkPt == null) chkPt = curSel.getMinimumPoint();
                if (chkPt == null || !player.getWorld().equals(curSel.getWorld()) || player.getLocation().distanceSquared(chkPt) > maxViewDistanceSquared)
                    return;
                if (curSel instanceof CuboidSelection c) {
                    drawCuboidOutline(player, c);
                } else if (curSel instanceof SphereSelection s) {
                    drawSphereOutline(player, s);
                } else if (curSel instanceof CylinderSelection c) {
                    drawCylinderOutline(player, c);
                } else if (curSel instanceof PolygonSelection p) {
                    drawPolygonOutline(player, p);
                } else if (curSel instanceof EllipsoidSelection e) {
                    drawEllipsoidOutline(player, e);
                } else if (curSel instanceof PyramidSelection p) {
                    drawPyramidOutline(player, p);
                } else {
                    Location min = curSel.getMinimumPoint();
                    Location max = curSel.getMaximumPoint();
                    if (min != null && max != null && min.getWorld() != null) {
                        CuboidSelection bb = new CuboidSelection(min.getWorld(), min, max);
                        drawCuboidOutline(player, bb);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, refreshIntervalTicks);
        activeVisualTasks.put(playerUUID, task);
    }

    private void startVisualTaskForPoints(Player player, @Nullable Location point1, @Nullable Location point2) {
        UUID playerUUID = player.getUniqueId();
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    activeVisualTasks.remove(playerUUID);
                    return;
                }
                Location p1Draw = point1;
                Location p2Draw = point2;
                if (p1Draw != null && player.getWorld().equals(p1Draw.getWorld()) && player.getLocation().distanceSquared(p1Draw) <= maxViewDistanceSquared)
                    spawnParticleAtPoint(player, p1Draw, 5);
                if (p2Draw != null && player.getWorld().equals(p2Draw.getWorld()) && player.getLocation().distanceSquared(p2Draw) <= maxViewDistanceSquared)
                    spawnParticleAtPoint(player, p2Draw, 5);
                if (p1Draw == null && p2Draw == null) {
                    this.cancel();
                    activeVisualTasks.remove(playerUUID);
                }
            }
        }.runTaskTimer(plugin, 0L, refreshIntervalTicks);
        activeVisualTasks.put(playerUUID, task);
    }

    private void spawnParticleAtPoint(Player viewer, Location point, int count) {
        if (point == null || point.getWorld() == null) return;
        viewer.spawnParticle(PARTICLE_TYPE, point.getX() + 0.5, point.getY() + 0.5, point.getZ() + 0.5, count, 0, 0, 0, 0, null);
    }

    private void drawCuboidOutline(Player viewer, @NotNull CuboidSelection cuboid) {
        World world = cuboid.getWorld();
        Vector min = cuboid.getMinimumPoint().toVector();
        Vector max = cuboid.getMaximumPoint().toVector().add(new Vector(1, 1, 1));
        drawEdge(viewer, world, new Vector(min.getX(), min.getY(), min.getZ()), new Vector(max.getX(), min.getY(), min.getZ()));
        drawEdge(viewer, world, new Vector(min.getX(), max.getY(), min.getZ()), new Vector(max.getX(), max.getY(), min.getZ()));
        drawEdge(viewer, world, new Vector(min.getX(), min.getY(), max.getZ()), new Vector(max.getX(), min.getY(), max.getZ()));
        drawEdge(viewer, world, new Vector(min.getX(), max.getY(), max.getZ()), new Vector(max.getX(), max.getY(), max.getZ()));
        drawEdge(viewer, world, new Vector(min.getX(), min.getY(), min.getZ()), new Vector(min.getX(), max.getY(), min.getZ()));
        drawEdge(viewer, world, new Vector(max.getX(), min.getY(), min.getZ()), new Vector(max.getX(), max.getY(), min.getZ()));
        drawEdge(viewer, world, new Vector(min.getX(), min.getY(), max.getZ()), new Vector(min.getX(), max.getY(), max.getZ()));
        drawEdge(viewer, world, new Vector(max.getX(), min.getY(), max.getZ()), new Vector(max.getX(), max.getY(), max.getZ()));
        drawEdge(viewer, world, new Vector(min.getX(), min.getY(), min.getZ()), new Vector(min.getX(), min.getY(), max.getZ()));
        drawEdge(viewer, world, new Vector(max.getX(), min.getY(), min.getZ()), new Vector(max.getX(), min.getY(), max.getZ()));
        drawEdge(viewer, world, new Vector(min.getX(), max.getY(), min.getZ()), new Vector(min.getX(), max.getY(), max.getZ()));
        drawEdge(viewer, world, new Vector(max.getX(), max.getY(), min.getZ()), new Vector(max.getX(), max.getY(), max.getZ()));
        spawnParticleAtPoint(viewer, cuboid.getMinimumPoint(), 10);
        spawnParticleAtPoint(viewer, cuboid.getMaximumPoint(), 10);
    }

    private void drawSphereOutline(Player viewer, @NotNull SphereSelection sphere) {
        Location center = sphere.getCenter();
        double r = sphere.getRadius();
        if (center == null || center.getWorld() == null) return;
        int pCircle = (int) Math.max(20, (2 * Math.PI * r) * particleDensity * 1.5);
        for (int i = 0; i < pCircle; i++) {
            double angle = (2 * Math.PI * i) / pCircle;
            spawnParticleAtPoint(viewer, center.clone().add(r * Math.cos(angle), 0, r * Math.sin(angle)), 1);
            spawnParticleAtPoint(viewer, center.clone().add(r * Math.cos(angle), r * Math.sin(angle), 0), 1);
            spawnParticleAtPoint(viewer, center.clone().add(0, r * Math.cos(angle), r * Math.sin(angle)), 1);
        }
    }

    private void drawCylinderOutline(Player viewer, @NotNull CylinderSelection cylinder) {
        Location b1 = cylinder.getBaseCenter1();
        Location b2 = cylinder.getBaseCenter2();
        double r = cylinder.getRadius();
        World w = cylinder.getWorld();
        if (b1 == null || b2 == null || w == null) return;
        int pCircle = (int) Math.max(16, (2 * Math.PI * r) * particleDensity * 1.5);
        double h = cylinder.getHeight();
        Vector axis = b2.toVector().subtract(b1.toVector());
        if (h > 0.001) axis.normalize();
        else axis = new Vector(0, 1, 0);
        Vector p1R = axis.clone().crossProduct(new Vector(0, 1, 0));
        if (p1R.lengthSquared() < 0.001) p1R = axis.clone().crossProduct(new Vector(1, 0, 0));
        if (p1R.lengthSquared() < 0.001) p1R = new Vector(1, 0, 0);
        p1R.normalize().multiply(r);
        Vector p2R = axis.clone().crossProduct(p1R).normalize().multiply(r);
        for (int i = 0; i < pCircle; i++) {
            double angle = (2 * Math.PI * i) / pCircle;
            Vector rOff = p1R.clone().multiply(Math.cos(angle)).add(p2R.clone().multiply(Math.sin(angle)));
            spawnParticleAtPoint(viewer, b1.clone().add(rOff), 1);
            spawnParticleAtPoint(viewer, b2.clone().add(rOff), 1);
        }
        int lines = 4;
        for (int i = 0; i < lines; i++) {
            double angle = (2 * Math.PI * i) / lines;
            Vector rOff = p1R.clone().multiply(Math.cos(angle)).add(p2R.clone().multiply(Math.sin(angle)));
            drawEdge(viewer, w, b1.clone().add(rOff).toVector(), b2.clone().add(rOff).toVector());
        }
    }

    private void drawPolygonOutline(Player viewer, @NotNull PolygonSelection polygon) {
        World w = polygon.getWorld();
        List<PolygonSelection.Vector2D> verts = polygon.getPolygonVertices();
        int minY = polygon.getMinY();
        int maxY = polygon.getMaxY();
        if (verts.size() < 2) return;
        for (int i = 0; i < verts.size(); i++) {
            PolygonSelection.Vector2D v1_2D = verts.get(i);
            PolygonSelection.Vector2D v2_2D = verts.get((i + 1) % verts.size());
            Vector v1_b = new Vector(v1_2D.x, minY + 0.5, v1_2D.z);
            Vector v2_b = new Vector(v2_2D.x, minY + 0.5, v2_2D.z);
            drawEdge(viewer, w, v1_b, v2_b);
            Vector v1_t = new Vector(v1_2D.x, maxY + 0.5, v1_2D.z);
            Vector v2_t = new Vector(v2_2D.x, maxY + 0.5, v2_2D.z);
            drawEdge(viewer, w, v1_t, v2_t);
        }
        for (PolygonSelection.Vector2D v2D : verts) {
            Vector bV = new Vector(v2D.x, minY + 0.5, v2D.z);
            Vector tV = new Vector(v2D.x, maxY + 0.5, v2D.z);
            if (maxY > minY) drawEdge(viewer, w, bV, tV);
            spawnParticleAtPoint(viewer, new Location(w, v2D.x, minY, v2D.z), 3);
            spawnParticleAtPoint(viewer, new Location(w, v2D.x, maxY, v2D.z), 3);
        }
    }

    private void drawEllipsoidOutline(Player viewer, @NotNull EllipsoidSelection ellipsoid) {
        Location center = ellipsoid.getCenter();
        if (center == null || center.getWorld() == null) return;
        double rx = ellipsoid.getRadiusX(), ry = ellipsoid.getRadiusY(), rz = ellipsoid.getRadiusZ();
        int pE = (int) Math.max(20, (Math.PI * (rx + rz)) * particleDensity * 1.5);
        for (int i = 0; i < pE; i++) {
            double angle = (2 * Math.PI * i) / pE;
            spawnParticleAtPoint(viewer, center.clone().add(rx * Math.cos(angle), 0, rz * Math.sin(angle)), 1);
        }
        pE = (int) Math.max(20, (Math.PI * (rx + ry)) * particleDensity * 1.5);
        for (int i = 0; i < pE; i++) {
            double angle = (2 * Math.PI * i) / pE;
            spawnParticleAtPoint(viewer, center.clone().add(rx * Math.cos(angle), ry * Math.sin(angle), 0), 1);
        }
        pE = (int) Math.max(20, (Math.PI * (ry + rz)) * particleDensity * 1.5);
        for (int i = 0; i < pE; i++) {
            double angle = (2 * Math.PI * i) / pE;
            spawnParticleAtPoint(viewer, center.clone().add(0, ry * Math.cos(angle), rz * Math.sin(angle)), 1);
        }
    }

    private void drawPyramidOutline(Player viewer, @NotNull PyramidSelection pyramid) {
        World world = pyramid.getWorld();
        Location bc1 = pyramid.getBaseCorner1Input();
        Location bc2 = pyramid.getBaseCorner2Input();
        Location apex = pyramid.getApexInput();

        if (bc1 == null || bc2 == null || apex == null) return;

        double baseY = Math.min(bc1.getY(), bc2.getY());
        Vector v1 = new Vector(Math.min(bc1.getX(), bc2.getX()), baseY, Math.min(bc1.getZ(), bc2.getZ()));
        Vector v2 = new Vector(Math.max(bc1.getX(), bc2.getX()), baseY, Math.min(bc1.getZ(), bc2.getZ()));
        Vector v3 = new Vector(Math.max(bc1.getX(), bc2.getX()), baseY, Math.max(bc1.getZ(), bc2.getZ()));
        Vector v4 = new Vector(Math.min(bc1.getX(), bc2.getX()), baseY, Math.max(bc1.getZ(), bc2.getZ()));
        Vector apexV = apex.toVector();

        Vector yOffset = new Vector(0, 0.5, 0);
        drawEdge(viewer, world, v1.clone().add(yOffset), v2.clone().add(yOffset));
        drawEdge(viewer, world, v2.clone().add(yOffset), v3.clone().add(yOffset));
        drawEdge(viewer, world, v3.clone().add(yOffset), v4.clone().add(yOffset));
        drawEdge(viewer, world, v4.clone().add(yOffset), v1.clone().add(yOffset));

        Vector apexVis = apexV.clone().add(yOffset);
        drawEdge(viewer, world, v1.clone().add(yOffset), apexVis);
        drawEdge(viewer, world, v2.clone().add(yOffset), apexVis);
        drawEdge(viewer, world, v3.clone().add(yOffset), apexVis);
        drawEdge(viewer, world, v4.clone().add(yOffset), apexVis);
    }

    private void drawEdge(Player viewer, World world, Vector start, Vector end) {
        double length = start.distance(end);
        if (length == 0) return;
        Vector dir = end.clone().subtract(start).normalize();
        int numP = (int) Math.max(1, length * particleDensity);
        for (int i = 0; i < numP; i++) {
            double step = (numP == 1 || (numP - 1) == 0) ? 0 : (length / (numP - 1.0)) * i;
            Vector pPos = start.clone().add(dir.clone().multiply(step));
            viewer.spawnParticle(PARTICLE_TYPE, pPos.getX(), pPos.getY(), pPos.getZ(), 1, 0, 0, 0, 0, null);
        }
    }

    public void clearSelectionVisuals(Player player) {
        clearTask(player.getUniqueId());
    }

    private void clearTask(UUID playerUUID) {
        BukkitTask task = activeVisualTasks.remove(playerUUID);
        if (task != null && !task.isCancelled()) task.cancel();
    }

    public void clearAllVisuals() {
        activeVisualTasks.values().forEach(task -> {
            if (!task.isCancelled()) task.cancel();
        });
        activeVisualTasks.clear();
        plugin.getLogger().info("Cleared all active selection visuals.");
    }
}