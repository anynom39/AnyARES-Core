package com.anynom39.anyares.listener;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.api.AnyAresAPI;
import com.anynom39.anyares.manager.SelectionManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class WandListener implements Listener {

    private final AnyARES_Core plugin;
    private Material wandItemMaterial;

    public WandListener(AnyARES_Core plugin) {
        this.plugin = plugin;
        loadConfiguredWandItem();
    }

    public void loadConfiguredWandItem() {
        String materialName = plugin.getConfig().getString("core-settings.wand-item", "WOODEN_AXE").toUpperCase();
        try {
            this.wandItemMaterial = Material.valueOf(materialName);
            if (!this.wandItemMaterial.isItem()) {
                plugin.getLogger().warning("Configured wand-item '" + materialName + "' is not a valid item! Defaulting to WOODEN_AXE.");
                this.wandItemMaterial = Material.WOODEN_AXE;
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material name for wand-item in config: '" + materialName + "'. Defaulting to WOODEN_AXE.");
            this.wandItemMaterial = Material.WOODEN_AXE;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() == wandItemMaterial) {
            if (!player.hasPermission("anyares.selection.wand")) {
                return;
            }

            if (!AnyAresAPI.isAvailable()) {
                plugin.getLogger().warning("Wand used by " + player.getName() + " but AnyAresAPI is not available.");
                return;
            }
            SelectionManager selectionManager = AnyAresAPI.getSelectionManager();


            Action action = event.getAction();
            Location clickedBlockLocation = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : null;

            if (clickedBlockLocation == null) {
                return;
            }

            event.setCancelled(true);
            if (action == Action.LEFT_CLICK_BLOCK) {
                selectionManager.setPlayerDefiningPoint(player, 0, clickedBlockLocation);
            } else if (action == Action.RIGHT_CLICK_BLOCK) {
                selectionManager.setPlayerDefiningPoint(player, 1, clickedBlockLocation);
            }
        }
    }
}