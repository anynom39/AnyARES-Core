package com.anynom39.anyares.manager;

import com.anynom39.anyares.AnyARES_Core;

public class AddonManager {
    private final AnyARES_Core plugin;

    public AddonManager(AnyARES_Core plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("AddonManager initialized.");
    }
}