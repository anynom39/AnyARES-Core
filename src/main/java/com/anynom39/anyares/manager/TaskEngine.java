package com.anynom39.anyares.manager;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.history.ChangeSet;
import com.anynom39.anyares.operation.BlockOperation;
import com.anynom39.anyares.util.MessageUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.logging.Level;

public class TaskEngine {
    private final AnyARES_Core plugin;
    private final HistoryManager historyManager;

    private final Queue<QueuedOperation> operationQueue;
    private final ConcurrentHashMap<Object, Object> playerOperationCount;

    private ScheduledTask processingTask;
    private final Semaphore concurrencySemaphore;
    private int maxConcurrentOperations;

    private record QueuedOperation(BlockOperation operation, CompletableFuture<ChangeSet> future) {
    }

    public TaskEngine(AnyARES_Core plugin) {
        this.plugin = plugin;
        this.historyManager = plugin.getHistoryManager();
        this.operationQueue = new ConcurrentLinkedQueue<>();
        this.playerOperationCount = new ConcurrentHashMap<>();

        loadConfig();
        this.concurrencySemaphore = new Semaphore(maxConcurrentOperations, true);

        plugin.getLogger().info("TaskEngine initialized. Max concurrent operations: " + maxConcurrentOperations);
        startProcessingLoop();
    }

    private void loadConfig() {
        this.maxConcurrentOperations = plugin.getConfig().getInt("core-settings.task-engine.max-concurrent-operations", 1);
        if (this.maxConcurrentOperations <= 0) this.maxConcurrentOperations = 1;
    }

    public void reloadConfigValues() {
        plugin.reloadConfig();
        loadConfig();

        if (this.processingTask != null) {
            this.processingTask.cancel();
            this.processingTask = null;
        }

        this.concurrencySemaphore.drainPermits();
        this.concurrencySemaphore.release(maxConcurrentOperations);
        startProcessingLoop();
        plugin.getLogger().info("TaskEngine config reloaded. Max concurrent operations: " + maxConcurrentOperations);
    }

    public CompletableFuture<ChangeSet> submitOperation(@NotNull BlockOperation operation) {
        Player player = operation.getPlayer();
        CompletableFuture<ChangeSet> future = new CompletableFuture<>();
        operationQueue.add(new QueuedOperation(operation, future));

        String estimatedBlocksMsg = operation.getEstimatedBlocks() > 0 ? "&7(&f" + operation.getEstimatedBlocks() + " blocks&7)" : "";
        if (player != null) {
            MessageUtil.sendMessage(player, "&7Queueing operation: &e" + operation.getOperationName() + " " + estimatedBlocksMsg);
        } else {
            plugin.getLogger().info("Queueing console operation: " + operation.getOperationName() + " " + estimatedBlocksMsg);
        }
        return future;
    }

    private void startProcessingLoop() {
        if (processingTask != null) {
            processingTask.cancel();
            processingTask = null;
        }

        processingTask = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (task) -> {

            if (operationQueue.isEmpty() || !concurrencySemaphore.tryAcquire()) {
                return;
            }

            QueuedOperation queuedOp = operationQueue.poll();
            if (queuedOp == null) {
                concurrencySemaphore.release();
                return;
            }

            BlockOperation operation = queuedOp.operation();
            CompletableFuture<ChangeSet> operationFuture = queuedOp.future();
            Player player = operation.getPlayer();

            if (player != null && player.isOnline()) {
                MessageUtil.sendActionBar(player, "&bProcessing: &e" + operation.getOperationName() + "&b...");
            } else if (player == null) {
                plugin.getLogger().info("Processing: " + operation.getOperationName() + "...");
            }

            long startTime = System.currentTimeMillis();
            CompletableFuture<ChangeSet> executionResultFuture = operation.execute(plugin);

            executionResultFuture.whenCompleteAsync((changeSet, throwable) -> {
                concurrencySemaphore.release();
                long duration = System.currentTimeMillis() - startTime;

                if (throwable != null) {
                    plugin.getLogger().log(Level.SEVERE, "Operation " + operation.getOperationName() + " failed for " +
                            (player != null ? player.getName() : "CONSOLE"), throwable);
                    if (player != null && player.isOnline()) {
                        MessageUtil.sendMessage(player, "&cOperation &e" + operation.getOperationName() + "&c failed after " + duration + "ms: &7" + throwable.getMessage());
                    }
                    operationFuture.completeExceptionally(throwable);
                } else {
                    if (player != null && player.isOnline()) {
                        if (changeSet != null && !changeSet.isEmpty()) {
                            historyManager.recordChangeSet(player, changeSet);
                            MessageUtil.sendMessage(player, "&aOperation &e" + operation.getOperationName() + "&a completed in " + duration + "ms. Modified " + changeSet.getSize() + " blocks.");
                        } else {
                            MessageUtil.sendMessage(player, "&aOperation &e" + operation.getOperationName() + "&a completed in " + duration + "ms. No blocks were changed.");
                        }
                    } else if (player == null) {
                        plugin.getLogger().info("Operation " + operation.getOperationName() + " completed in " + duration + "ms. Modified " +
                                (changeSet != null ? changeSet.getSize() : 0) + " blocks.");
                    }
                    operationFuture.complete(changeSet);
                }
            }, Bukkit.getScheduler().getMainThreadExecutor(plugin));

        }, 0L, 1L, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        if (processingTask != null) {
            processingTask.cancel();
            processingTask = null;
        }
        plugin.getLogger().info("TaskEngine shutting down. " + operationQueue.size() + " operations in queue will be dropped.");
        operationQueue.forEach(queuedOp -> {
            if (!queuedOp.future().isDone()) {
                queuedOp.future().completeExceptionally(new IllegalStateException("Plugin shutting down"));
            }
        });
        operationQueue.clear();
    }

    public int getQueueSize() {
        return operationQueue.size();
    }

    public int getActiveOperations() {
        return maxConcurrentOperations - concurrencySemaphore.availablePermits();
    }
}