package com.anynom39.anyares.command.completer;

// Imports

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReplaceNearCommandTabCompleter implements TabCompleter {

    private final SetCommandTabCompleter patternCompleter = new SetCommandTabCompleter(); // Reuse for patterns

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> radii = Arrays.asList("5", "10", "15", "20");
            return StringUtil.copyPartialMatches(args[0], radii, new ArrayList<>());
        } else if (args.length == 2 || args.length == 3) {
            String[] currentPatternArgArray = {args[args.length - 1]};
            return patternCompleter.onTabComplete(sender, command, alias, currentPatternArgArray);
        }
        return new ArrayList<>();
    }
}