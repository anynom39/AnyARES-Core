package com.anynom39.anyares.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageUtil {

    private static final String PREFIX = ChatColor.GOLD + "[AnyARES] " + ChatColor.YELLOW;

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + color(message));
    }

    public static void sendRawMessage(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(color(message));
    }
}