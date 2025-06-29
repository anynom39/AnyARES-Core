package com.anynom39.anyares.command.completer;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SetCommandTabCompleter implements TabCompleter {

    private static List<String> BLOCK_MATERIALS_CACHE = null;
    private static final Pattern PERCENTAGE_PREFIX_PATTERN = Pattern.compile("^\\d{1,3}(?:\\.\\d+)?$");
    private static final Pattern COMPLETE_BLOCK_ENTRY_PATTERN = Pattern.compile("^(?:!|%\\d{1,3}(?:\\.\\d+)?%?)?[a-zA-Z0-9_:]+(?:\\[[^\\]]*])?$");


    private List<String> getBlockMaterials() {
        if (BLOCK_MATERIALS_CACHE == null) {
            BLOCK_MATERIALS_CACHE = Arrays.stream(Material.values())
                    .filter(Material::isBlock)
                    .map(m -> m.name().toLowerCase())
                    .sorted()
                    .collect(Collectors.toList());
        }
        return BLOCK_MATERIALS_CACHE;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> finalCompletions = new ArrayList<>();
        if (args.length == 0) return finalCompletions;
        String currentTypingFragment = args[args.length - 1].toLowerCase();
        String[] patternEntries = currentTypingFragment.split(",", -1);
        String activeEntryFragment = patternEntries.length > 0 ? patternEntries[patternEntries.length - 1].trim() : "";
        List<String> activeEntrySuggestions = new ArrayList<>();
        if (activeEntryFragment.contains("[")) {
            int bracketIndex = activeEntryFragment.indexOf('[');
            String materialPart = activeEntryFragment.substring(0, bracketIndex);
            String cleanMaterialPart = materialPart.replaceFirst("!", "").replaceFirst("^\\d*\\.?\\d*%", "");
            String statesPart = activeEntryFragment.substring(bracketIndex + 1);
            Material material = Material.matchMaterial(cleanMaterialPart);
            if (material != null && material.isBlock()) {
                BlockData defaultBlockData = material.createBlockData();
                Set<String> existingStatesInArg = Arrays.stream(statesPart.split(","))
                        .map(s -> s.split("=")[0].trim())
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toSet());

                String lastStateToken = statesPart.endsWith(",") || statesPart.isEmpty() ? "" :
                        Arrays.stream(statesPart.split(","))
                                .reduce((first, second) -> second).orElse("");

                List<String> stateBasedCompletionsSource = new ArrayList<>();
                if (lastStateToken.contains("=") && !lastStateToken.endsWith("=")) {
                } else if (lastStateToken.contains("=") && lastStateToken.endsWith("=")) { // Completing a value: e.g. "axis="
                    String stateKeyToComplete = lastStateToken.substring(0, lastStateToken.length() - 1);
                    List<String> stateValues = getValidValuesForState(defaultBlockData, stateKeyToComplete);
                    for (String val : stateValues) {
                        stateBasedCompletionsSource.add(lastStateToken + val);
                    }
                } else {
                    List<String> stateKeys = getPossibleStateKeys(defaultBlockData, existingStatesInArg);
                    for (String key : stateKeys) {
                        stateBasedCompletionsSource.add(key + "=");
                    }
                }
                StringUtil.copyPartialMatches(lastStateToken, stateBasedCompletionsSource, activeEntrySuggestions);
            }
        } else {
            if (!activeEntryFragment.startsWith("!") && !activeEntryFragment.startsWith("*") && !PERCENTAGE_PREFIX_PATTERN.matcher(activeEntryFragment).matches() && !getBlockMaterials().contains(activeEntryFragment)) {
                activeEntrySuggestions.add("!");
            }

            if (activeEntryFragment.isEmpty() || "*".startsWith(activeEntryFragment)) {
                activeEntrySuggestions.add("*");
            }

            if (PERCENTAGE_PREFIX_PATTERN.matcher(activeEntryFragment).matches() && !activeEntryFragment.endsWith("%")) {
                activeEntrySuggestions.add(activeEntryFragment + "%");
            }

            String materialSearchFragment = activeEntryFragment;
            String prefixForMaterial = "";

            if (activeEntryFragment.startsWith("!")) {
                prefixForMaterial = "!";
                materialSearchFragment = activeEntryFragment.substring(1);
            }

            Matcher percentMatcher = Pattern.compile("^(\\d*\\.?\\d*%)").matcher(materialSearchFragment);
            if (percentMatcher.find()) {
                prefixForMaterial += percentMatcher.group(1);
                materialSearchFragment = materialSearchFragment.substring(percentMatcher.group(1).length());
            } else if (PERCENTAGE_PREFIX_PATTERN.matcher(materialSearchFragment).matches()) {
                materialSearchFragment = null;
            }


            if (materialSearchFragment != null) {
                List<String> matchedMaterials = new ArrayList<>();
                StringUtil.copyPartialMatches(materialSearchFragment, getBlockMaterials(), matchedMaterials);
                for (String matSuggestion : matchedMaterials) {
                    activeEntrySuggestions.add(prefixForMaterial + matSuggestion);
                }
            }

            if (COMPLETE_BLOCK_ENTRY_PATTERN.matcher(activeEntryFragment).matches()) {
                String materialPartForStateCheck = activeEntryFragment.replaceFirst("!", "").replaceFirst("^\\d*\\.?\\d*%", "");
                Material currentMat = Material.matchMaterial(materialPartForStateCheck.split("\\[")[0]);
                if (currentMat != null && currentMat.isBlock()) {
                    if (!activeEntryFragment.contains("[")) {
                        activeEntrySuggestions.add(activeEntryFragment + "[");
                    }
                    activeEntrySuggestions.add(activeEntryFragment + ",");
                }
            }
        }


        String prefix = "";
        if (patternEntries.length > 1) {
            prefix = currentTypingFragment.substring(0, currentTypingFragment.lastIndexOf(patternEntries[patternEntries.length - 1]));
        }

        for (String suggestion : activeEntrySuggestions) {
            finalCompletions.add(prefix + suggestion);
        }

        if (activeEntrySuggestions.isEmpty() && !currentTypingFragment.contains("[") && !currentTypingFragment.contains(",")) {
            StringUtil.copyPartialMatches(currentTypingFragment, getBlockMaterials(), finalCompletions);
            if (PERCENTAGE_PREFIX_PATTERN.matcher(currentTypingFragment).matches() && !currentTypingFragment.endsWith("%")) {
                finalCompletions.add(currentTypingFragment + "%");
            }
            if ("*".startsWith(currentTypingFragment)) finalCompletions.add("*");
            if ("!".startsWith(currentTypingFragment)) finalCompletions.add("!");
        }


        return finalCompletions.stream().distinct().sorted().collect(Collectors.toList());
    }

    private List<String> getPossibleStateKeys(BlockData data, Set<String> existingKeys) {
        List<String> keys = new ArrayList<>();
        if (data instanceof org.bukkit.block.data.AnaloguePowerable && !existingKeys.contains("power"))
            keys.add("power");
        if (data instanceof org.bukkit.block.data.Ageable && !existingKeys.contains("age")) keys.add("age");
        if (data instanceof org.bukkit.block.data.Bisected && !existingKeys.contains("half")) keys.add("half");
        if (data instanceof org.bukkit.block.data.Directional && !existingKeys.contains("facing")) keys.add("facing");
        if (data instanceof org.bukkit.block.data.Levelled && !existingKeys.contains("level")) keys.add("level");
        if (data instanceof org.bukkit.block.data.Openable && !existingKeys.contains("open")) keys.add("open");
        if (data instanceof org.bukkit.block.data.Orientable && !existingKeys.contains("axis")) keys.add("axis");
        if (data instanceof org.bukkit.block.data.Powerable && !existingKeys.contains("powered")) keys.add("powered");
        if (data instanceof org.bukkit.block.data.Rail && !existingKeys.contains("shape")) keys.add("shape");
        if (data instanceof org.bukkit.block.data.Rotatable && !existingKeys.contains("rotation")) keys.add("rotation");
        if (data instanceof org.bukkit.block.data.Waterlogged && !existingKeys.contains("waterlogged"))
            keys.add("waterlogged");
        if (data instanceof org.bukkit.block.data.type.Slab && !existingKeys.contains("type")) keys.add("type");
        if (data instanceof org.bukkit.block.data.type.Stairs && !existingKeys.contains("shape")) keys.add("shape");
        if (data instanceof org.bukkit.block.data.type.Bed && !existingKeys.contains("part")) keys.add("part");
        if (data instanceof org.bukkit.block.data.type.Bed && !existingKeys.contains("occupied")) keys.add("occupied");
        if (data instanceof org.bukkit.block.data.type.Bell && !existingKeys.contains("attachment"))
            keys.add("attachment");
        return keys.stream().filter(k -> !existingKeys.contains(k)).collect(Collectors.toList());
    }

    private List<String> getValidValuesForState(BlockData data, String stateKey) {
        List<String> values = new ArrayList<>();
        switch (stateKey) {
            case "axis":
                if (data instanceof org.bukkit.block.data.Orientable) {
                    Arrays.stream(org.bukkit.Axis.values()).forEach(v -> values.add(v.name().toLowerCase()));
                }
                break;
            case "facing":
                if (data instanceof org.bukkit.block.data.Directional) {
                    Arrays.stream(org.bukkit.block.BlockFace.values())
                            .filter(f -> f.isCartesian() || f == org.bukkit.block.BlockFace.UP || f == org.bukkit.block.BlockFace.DOWN)
                            .forEach(v -> values.add(v.name().toLowerCase()));
                }
                break;
            case "waterlogged":
            case "powered":
            case "open":
            case "occupied":
            case "snowy":
                values.add("true");
                values.add("false");
                break;
            case "type":
                if (data instanceof org.bukkit.block.data.type.Slab) {
                    Arrays.stream(org.bukkit.block.data.type.Slab.Type.values()).forEach(v -> values.add(v.name().toLowerCase()));
                } else if (data instanceof org.bukkit.block.data.type.Chest) {
                    Arrays.stream(org.bukkit.block.data.type.Chest.Type.values()).forEach(v -> values.add(v.name().toLowerCase()));
                } else if (data instanceof org.bukkit.block.data.type.Comparator) {
                    Arrays.stream(org.bukkit.block.data.type.Comparator.Mode.values()).forEach(v -> values.add(v.name().toLowerCase()));
                }
                break;
            case "half":
                if (data instanceof org.bukkit.block.data.Bisected) {
                    Arrays.stream(org.bukkit.block.data.Bisected.Half.values()).forEach(v -> values.add(v.name().toLowerCase()));
                }
                break;
            case "part":
                if (data instanceof org.bukkit.block.data.type.Bed) {
                    Arrays.stream(org.bukkit.block.data.type.Bed.Part.values()).forEach(v -> values.add(v.name().toLowerCase()));
                }
                break;
            case "attachment":
                if (data instanceof org.bukkit.block.data.type.Bell) {
                    Arrays.stream(org.bukkit.block.data.type.Bell.Attachment.values()).forEach(v -> values.add(v.name().toLowerCase()));
                }
                break;
        }
        return values;
    }
}