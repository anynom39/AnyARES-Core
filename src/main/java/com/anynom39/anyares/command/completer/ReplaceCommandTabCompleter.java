package com.anynom39.anyares.command.completer;

// Imports similar to SetCommandTabCompleter

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ReplaceCommandTabCompleter implements TabCompleter {

    private final SetCommandTabCompleter setCompleterLogic = new SetCommandTabCompleter();

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 || args.length == 2) {
            String[] currentPatternArgArray = {args[args.length - 1]};
            return setCompleterLogic.onTabComplete(sender, command, alias, currentPatternArgArray);
        }
        return new ArrayList<>();
    }
}