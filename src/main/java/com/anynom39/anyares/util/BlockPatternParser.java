package com.anynom39.anyares.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockPatternParser {

    private static final Pattern SINGLE_ENTRY_PATTERN = Pattern.compile("^(?:(!))?(?:(\\d{1,3}(?:\\.\\d+)?)%)?([a-zA-Z0-9_:]+|\\*)(?:\\[([^\\]]+)])?$");


    public static class WeightedBlockData {
        public final BlockData blockData;
        public double probability;

        public WeightedBlockData(BlockData blockData, double probability) {
            this.blockData = blockData;
            this.probability = probability;
        }
    }

    public static class MaskEntry {
        @Nullable
        public final BlockData blockData;
        public final boolean isNegated;
        public final boolean isWildcard;

        public MaskEntry(@Nullable BlockData blockData, boolean isNegated, boolean isWildcard) {
            if (isWildcard && blockData != null) {
                throw new IllegalArgumentException("Wildcard mask entry cannot have specific block data.");
            }
            this.blockData = blockData;
            this.isNegated = isNegated;
            this.isWildcard = isWildcard;
        }

        private boolean directMatch(BlockData dataToTest) {
            if (isWildcard) {
                return true;
            }
            return this.blockData != null && dataToTest.matches(this.blockData);
        }
    }

    private static final Random random = new Random();

    @NotNull
    public static List<MaskEntry> parseMaskPattern(@NotNull String maskPatternString) throws IllegalArgumentException {
        List<MaskEntry> maskEntries = new ArrayList<>();
        if (maskPatternString.trim().isEmpty()) {
            throw new IllegalArgumentException("Mask pattern string cannot be empty.");
        }
        String[] entries = maskPatternString.split(",");

        for (String entryStr : entries) {
            entryStr = entryStr.trim();
            if (entryStr.isEmpty()) continue;

            Matcher matcher = SINGLE_ENTRY_PATTERN.matcher(entryStr.toLowerCase());
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid mask pattern entry format: \"" + entryStr + "\"");
            }

            boolean isNegated = matcher.group(1) != null;
            String materialNameOrWildcard = matcher.group(3);
            String statesString = matcher.group(4);

            if (materialNameOrWildcard.equals("*")) {
                if (statesString != null && !statesString.isEmpty()) {
                    throw new IllegalArgumentException("Wildcard '*' cannot have block states: \"" + entryStr + "\"");
                }
                maskEntries.add(new MaskEntry(null, isNegated, true));
            } else {
                Material material = Material.matchMaterial(materialNameOrWildcard);
                if (material == null || !material.isBlock()) {
                    throw new IllegalArgumentException("Unknown or non-block material: \"" + materialNameOrWildcard + "\" in entry \"" + entryStr + "\"");
                }

                BlockData blockData;
                String fullPatternForBukkit;
                if (materialNameOrWildcard.contains(":")) {
                    fullPatternForBukkit = materialNameOrWildcard;
                } else {
                    fullPatternForBukkit = material.getKey().toString();
                }

                if (statesString != null && !statesString.isEmpty()) {
                    fullPatternForBukkit += "[" + statesString + "]";
                }

                try {
                    blockData = Bukkit.getServer().createBlockData(fullPatternForBukkit);
                    if (blockData.getMaterial() != material) {
                        throw new IllegalArgumentException("Block states \"" + statesString + "\" are not applicable to material \"" + materialNameOrWildcard + "\", resulted in " + blockData.getMaterial().name());
                    }
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid block state format in entry \"" + entryStr + "\": " + e.getMessage(), e);
                }
                maskEntries.add(new MaskEntry(blockData, isNegated, false));
            }
        }
        if (maskEntries.isEmpty() && !maskPatternString.trim().isEmpty()) {
            throw new IllegalArgumentException("Mask pattern resulted in no valid rules: \"" + maskPatternString + "\"");
        }
        return maskEntries;
    }

    public static boolean matchesMask(@NotNull BlockData dataToTest, @NotNull List<MaskEntry> maskRules) {
        if (maskRules.isEmpty()) {
            return false;
        }

        boolean hasPositiveRule = false;
        boolean matchedByPositiveRule = false;

        for (MaskEntry rule : maskRules) {
            if (!rule.isNegated) {
                hasPositiveRule = true;
                if (rule.directMatch(dataToTest)) {
                    matchedByPositiveRule = true;
                    break;
                }
            }
        }

        if (hasPositiveRule && !matchedByPositiveRule) {
            return false;
        }

        for (MaskEntry rule : maskRules) {
            if (rule.isNegated) {
                if (rule.directMatch(dataToTest)) {
                    return false;
                }
            }
        }
        return true;
    }


    @NotNull
    public static List<WeightedBlockData> parseComplexPatternForReplacement(@NotNull String complexPatternString) throws IllegalArgumentException {
        List<WeightedBlockData> weightedList = new ArrayList<>();
        if (complexPatternString.trim().isEmpty()) {
            throw new IllegalArgumentException("Replacement pattern string cannot be empty.");
        }
        String[] entries = complexPatternString.split(",");
        double totalExplicitPercentage = 0;
        int entriesWithoutPercentage = 0;
        List<RawPatternEntry> rawEntries = new ArrayList<>();

        for (String entry : entries) {
            entry = entry.trim();
            if (entry.isEmpty()) continue;

            Matcher matcher = SINGLE_ENTRY_PATTERN.matcher(entry.toLowerCase());
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid replacement pattern entry format: \"" + entry + "\"");
            }

            String percentageStr = matcher.group(2);
            String materialNameOrWildcard = matcher.group(3);
            String statesString = matcher.group(4);

            if (materialNameOrWildcard.equals("*")) {
                throw new IllegalArgumentException("Wildcard '*' is not allowed in 'to' patterns for replacement.");
            }

            Material material = Material.matchMaterial(materialNameOrWildcard);
            if (material == null || !material.isBlock()) {
                throw new IllegalArgumentException("Unknown or non-block material: \"" + materialNameOrWildcard + "\" in entry \"" + entry + "\"");
            }

            BlockData blockData;
            String fullPatternForBukkit;
            if (materialNameOrWildcard.contains(":")) {
                fullPatternForBukkit = materialNameOrWildcard;
            } else {
                fullPatternForBukkit = material.getKey().toString();
            }

            if (statesString != null && !statesString.isEmpty()) {
                fullPatternForBukkit += "[" + statesString + "]";
            }

            try {
                blockData = Bukkit.getServer().createBlockData(fullPatternForBukkit);
                if (blockData.getMaterial() != material) {
                    throw new IllegalArgumentException("Block states \"" + statesString + "\" are not valid for material \"" + materialNameOrWildcard + "\", resulted in " + blockData.getMaterial().name());
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid block states in entry \"" + entry + "\": " + e.getMessage(), e);
            }

            double percentage = -1;
            if (percentageStr != null) {
                try {
                    percentage = Double.parseDouble(percentageStr);
                    if (percentage <= 0 || percentage > 100) {
                        throw new IllegalArgumentException("Percentage must be > 0 and <= 100 in entry: \"" + entry + "\"");
                    }
                    totalExplicitPercentage += percentage;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid percentage format in entry: \"" + entry + "\"");
                }
            } else {
                entriesWithoutPercentage++;
            }
            rawEntries.add(new RawPatternEntry(blockData, percentage));
        }

        if (totalExplicitPercentage > 100.001) {
            throw new IllegalArgumentException("Sum of explicit percentages exceeds 100% (" + String.format("%.2f", totalExplicitPercentage) + "%)");
        }

        double remainingPercentage = 100.0 - totalExplicitPercentage;
        if (remainingPercentage < -0.001) remainingPercentage = 0;

        double implicitWeightPerEntry = 0;
        if (entriesWithoutPercentage > 0) {
            if (remainingPercentage > 0) {
                implicitWeightPerEntry = remainingPercentage / entriesWithoutPercentage;
            } else if (totalExplicitPercentage == 0) {
                implicitWeightPerEntry = 100.0 / entriesWithoutPercentage;
            }
        }

        if (entriesWithoutPercentage == 0 && totalExplicitPercentage > 0 && Math.abs(totalExplicitPercentage - 100.0) > 0.001) {
            double normalizationFactor = 100.0 / totalExplicitPercentage;
            for (RawPatternEntry rawEntry : rawEntries) {
                rawEntry.percentage *= normalizationFactor;
            }
        }


        double cumulativeProbability = 0;
        for (RawPatternEntry rawEntry : rawEntries) {
            double currentEntryEffectivePercentage = (rawEntry.percentage > 0) ? rawEntry.percentage : implicitWeightPerEntry;

            if (currentEntryEffectivePercentage <= 0) {
                if (rawEntries.size() == 1 && rawEntry.percentage < 0) {
                    currentEntryEffectivePercentage = 100.0;
                } else {
                    continue;
                }
            }

            cumulativeProbability += currentEntryEffectivePercentage / 100.0;
            weightedList.add(new WeightedBlockData(rawEntry.blockData, cumulativeProbability));
        }

        if (!weightedList.isEmpty()) {
            if (weightedList.size() == 1 && weightedList.get(0).probability < 0.999) {
                weightedList.get(0).probability = 1.0;
            } else if (Math.abs(weightedList.get(weightedList.size() - 1).probability - 1.0) > 0.001 && weightedList.get(weightedList.size() - 1).probability < 1.0) {
                weightedList.get(weightedList.size() - 1).probability = 1.0;
            } else if (weightedList.get(weightedList.size() - 1).probability > 1.001) {
                throw new IllegalStateException("Cumulative probability exceeded 1.0. Last value: " + weightedList.get(weightedList.size() - 1).probability);
            }
        }

        if (weightedList.isEmpty() && !complexPatternString.trim().isEmpty()) {
            throw new IllegalArgumentException("Pattern resulted in no valid blocks for replacement: \"" + complexPatternString + "\"");
        }
        return weightedList;
    }

    @Nullable
    public static BlockData selectRandomBlockData(@NotNull List<WeightedBlockData> weightedBlockDataList) {
        if (weightedBlockDataList.isEmpty()) {
            return null;
        }
        double r = random.nextDouble();
        for (WeightedBlockData entry : weightedBlockDataList) {
            if (r < entry.probability) {
                return entry.blockData;
            }
        }
        return weightedBlockDataList.get(weightedBlockDataList.size() - 1).blockData;
    }

    private static class RawPatternEntry {
        BlockData blockData;
        double percentage;

        RawPatternEntry(BlockData blockData, double percentage) {
            this.blockData = blockData;
            this.percentage = percentage;
        }
    }
}