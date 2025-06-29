package com.anynom39.anyares;

import com.anynom39.anyares.api.AnyAresAPI;
import com.anynom39.anyares.command.*;
import com.anynom39.anyares.command.completer.*;
import com.anynom39.anyares.listener.WandListener;
import com.anynom39.anyares.manager.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

public final class AnyARES_Core extends JavaPlugin {

    private static AnyARES_Core instance;

    private SelectionManager selectionManager;
    private TaskEngine taskEngine;
    private HistoryManager historyManager;
    private ClipboardManager clipboardManager;
    private VisualizationManager visualizationManager;
    private AddonManager addonManager;

    private WandListener wandListener;

    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();

        getLogger().info("Initializing AnyARES-Core v" + getDescription().getVersion() + "...");

        saveDefaultConfig();

        getLogger().info("Initializing managers...");
        try {
            visualizationManager = new VisualizationManager(this);

            selectionManager = new SelectionManager(this, visualizationManager);
            getServer().getPluginManager().registerEvents(selectionManager, this);

            historyManager = new HistoryManager(this);
            getServer().getPluginManager().registerEvents(historyManager, this);

            clipboardManager = new ClipboardManager(this);
            getServer().getPluginManager().registerEvents(clipboardManager, this);

            taskEngine = new TaskEngine(this);

            addonManager = new AddonManager(this);

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize core managers! Disabling plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Managers initialized.");

        getLogger().info("Registering event listeners...");
        this.wandListener = new WandListener(this);
        getServer().getPluginManager().registerEvents(this.wandListener, this);
        getLogger().info("Event listeners registered.");

        getLogger().info("Registering commands and tab completers...");
        try {
            Objects.requireNonNull(getCommand("anyares")).setExecutor(new AnyAresCommand(this));
            Objects.requireNonNull(getCommand("anyares")).setTabCompleter(new AnyAresTabCompleter());

            Objects.requireNonNull(getCommand("wand")).setExecutor(new WandCommand(this));

            Objects.requireNonNull(getCommand("pos1")).setExecutor(new PosCommand(this, 0));
            Objects.requireNonNull(getCommand("pos2")).setExecutor(new PosCommand(this, 1));

            Objects.requireNonNull(getCommand("undo")).setExecutor(new UndoCommand(this));
            Objects.requireNonNull(getCommand("redo")).setExecutor(new RedoCommand(this));

            Objects.requireNonNull(getCommand("set")).setExecutor(new SetCommand(this));
            Objects.requireNonNull(getCommand("set")).setTabCompleter(new SetCommandTabCompleter());

            Objects.requireNonNull(getCommand("copy")).setExecutor(new CopyCommand(this));
            Objects.requireNonNull(getCommand("cut")).setExecutor(new CutCommand(this));
            Objects.requireNonNull(getCommand("paste")).setExecutor(new PasteCommand(this));
            Objects.requireNonNull(getCommand("paste")).setTabCompleter(new PasteCommandTabCompleter());

            Objects.requireNonNull(getCommand("replace")).setExecutor(new ReplaceCommand(this));
            Objects.requireNonNull(getCommand("replace")).setTabCompleter(new ReplaceCommandTabCompleter());
            Objects.requireNonNull(getCommand("replacenear")).setExecutor(new ReplaceNearCommand(this));
            Objects.requireNonNull(getCommand("replacenear")).setTabCompleter(new ReplaceNearCommandTabCompleter());

        } catch (NullPointerException e) {
            getLogger().log(Level.SEVERE, "Could not register a core command! Check plugin.yml and command setup.", e);
        }
        getLogger().info("Commands and tab completers registered.");

        try {
            AnyAresAPI.initialize(this);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize AnyAresAPI!", e);
        }

        long endTime = System.currentTimeMillis();
        getLogger().info("AnyARES-Core has been enabled successfully! (Took " + (endTime - startTime) + "ms)");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling AnyARES-Core...");

        AnyAresAPI.shutdown();

        if (taskEngine != null) {
            getLogger().info("Shutting down Task Engine...");
            taskEngine.shutdown();
        }

        if (visualizationManager != null) {
            visualizationManager.clearAllVisuals();
        }

        instance = null;
        getLogger().info("AnyARES-Core has been disabled.");
    }

    public void reloadPluginConfiguration() {
        getLogger().info("Reloading AnyARES-Core configuration...");
        reloadConfig();

        if (wandListener != null) {
            wandListener.loadConfiguredWandItem();
        }
        if (historyManager != null) {
            historyManager.reloadConfigValues();
        }
        if (taskEngine != null) {
            taskEngine.reloadConfigValues();
        }
        if (visualizationManager != null) {
            visualizationManager.reloadConfigValues();
        }
        getLogger().info("Configuration reloaded.");
    }

    ;

    public static AnyARES_Core getInstance() {
        return instance;
    }

    public SelectionManager getSelectionManager() {
        if (selectionManager == null) throw new IllegalStateException("SelectionManager is not initialized!");
        return selectionManager;
    }

    public TaskEngine getTaskEngine() {
        if (taskEngine == null) throw new IllegalStateException("TaskEngine is not initialized!");
        return taskEngine;
    }

    public HistoryManager getHistoryManager() {
        if (historyManager == null) throw new IllegalStateException("HistoryManager is not initialized!");
        return historyManager;
    }

    public ClipboardManager getClipboardManager() {
        if (clipboardManager == null) throw new IllegalStateException("ClipboardManager is not initialized!");
        return clipboardManager;
    }

    public VisualizationManager getVisualizationManager() {
        if (visualizationManager == null) throw new IllegalStateException("VisualizationManager is not initialized!");
        return visualizationManager;
    }

    public AddonManager getAddonManager() {
        if (addonManager == null) throw new IllegalStateException("AddonManager is not initialized!");
        return addonManager;
    }
}