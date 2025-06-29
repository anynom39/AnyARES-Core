package com.anynom39.anyares.command.completer;

// ... (imports: Bukkit, Command, CommandSender, TabCompleter, Player, StringUtil, NotNull, Nullable, ArrayList, Arrays, List, Collectors)

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AnyAresTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS_INFO = Arrays.asList("help", "info", "version", "status");
    private static final List<String> SUBCOMMANDS_PLAYER_HISTORY = Arrays.asList("history");
    private static final List<String> SUBCOMMANDS_QUEUE_MANAGEMENT = Arrays.asList("queue");
    private static final List<String> SUBCOMMANDS_ADMIN = Arrays.asList("reload", "debug");

    private static final List<String> HISTORY_ARGS = Arrays.asList("clear");
    private static final List<String> QUEUE_ARGS = Arrays.asList("list", "clear");

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> availableSubcommands = new ArrayList<>(SUBCOMMANDS_INFO);

            if (sender instanceof Player && sender.hasPermission("anyares.history.manage")) {
                availableSubcommands.addAll(SUBCOMMANDS_PLAYER_HISTORY);
            }
            if (sender.hasPermission("anyares.queue.manage")) {
                availableSubcommands.addAll(SUBCOMMANDS_QUEUE_MANAGEMENT);
            }
            if (sender.hasPermission("anyares.core.admin")) {
                availableSubcommands.addAll(SUBCOMMANDS_ADMIN);
            }
            StringUtil.copyPartialMatches(args[0], availableSubcommands, completions);
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "history":
                    if (sender instanceof Player && sender.hasPermission("anyares.history.manage")) {
                        StringUtil.copyPartialMatches(args[1], HISTORY_ARGS, completions);
                    }
                    break;
                case "queue":
                    if (sender.hasPermission("anyares.queue.manage")) {
                        List<String> currentQueueArgs = new ArrayList<>(QUEUE_ARGS);
                        StringUtil.copyPartialMatches(args[1], currentQueueArgs, completions);
                    }
                    break;
            }
        }
        return completions.stream().distinct().sorted().collect(Collectors.toList());
    }
}