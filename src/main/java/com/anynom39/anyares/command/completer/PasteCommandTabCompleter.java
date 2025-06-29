package com.anynom39.anyares.command.completer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PasteCommandTabCompleter implements TabCompleter {

    private static final List<String> FLAGS = Arrays.asList("-a");

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length > 0) {
            String currentArg = args[args.length - 1];
            List<String> availableFlags = new ArrayList<>(FLAGS);
            StringUtil.copyPartialMatches(currentArg, availableFlags, completions);
        }
        return completions.stream().sorted().collect(Collectors.toList());
    }
}