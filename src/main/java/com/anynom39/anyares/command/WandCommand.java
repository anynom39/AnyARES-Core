package com.anynom39.anyares.command;

import com.anynom39.anyares.AnyARES_Core;
import com.anynom39.anyares.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class WandCommand implements CommandExecutor {

    private final AnyARES_Core plugin;

    public WandCommand(AnyARES_Core plugin) {
        this.plugin = plugin;
    }

    private Material getConfiguredWandItem() {
        String materialName = plugin.getConfig().getString("core-settings.wand-item", "WOODEN_AXE").toUpperCase();
        try {
            Material mat = Material.valueOf(materialName);
            return mat.isItem() ? mat : Material.WOODEN_AXE;
        } catch (IllegalArgumentException e) {
            return Material.WOODEN_AXE;
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "&cThis command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("anyares.selection.wand.get")) {
            MessageUtil.sendMessage(player, "&cYou don't have permission to use this command.");
            return true;
        }

        ItemStack wandItem = new ItemStack(getConfiguredWandItem());
        ItemMeta meta = wandItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.color("&6AnyARES Selection Wand"));
            meta.setLore(Collections.singletonList(MessageUtil.color("&7Left-click for Pos1, Right-click for Pos2")));
            wandItem.setItemMeta(meta);
        }

        if (player.getInventory().addItem(wandItem).isEmpty()) {
            MessageUtil.sendMessage(player, "&aYou have received the AnyARES selection wand!");
        } else {
            MessageUtil.sendMessage(player, "&cYour inventory is full! Could not give you the wand.");
        }


        return true;
    }
}