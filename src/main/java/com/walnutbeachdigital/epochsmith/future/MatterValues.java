package com.walnutbeachdigital.epochsmith.future;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.walnutbeachdigital.epochsmith.item.ModItems;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

/**
 * How much matter each item is worth, for the matter printer.
 *
 * Raw resources have fixed values (cobblestone 1, iron ore 16, diamond 128...). Everything else
 * is worked out from its crafting and smelting recipes: the cheapest recipe's ingredients,
 * divided by how many it makes. That way it works for other mods' items too.
 * Items that can't be traced back to raw resources (like nether stars) can't be printed.
 */
public final class MatterValues {

    public static final int UNPRINTABLE = -1;
    private static final int MAX_DEPTH = 10;

    private static final Map<Item, Integer> BASE = new HashMap<>();
    private static final Map<Item, Integer> cache = new HashMap<>();
    private static final Map<Item, List<RecipeHolder<?>>> recipesByResult = new HashMap<>();
    private static RecipeManager indexedFor = null;

    private static void base(int value, Item... items) {
        for (Item item : items) {
            BASE.put(item, value);
        }
    }

    static {
        base(1, Items.COBBLESTONE, Items.COBBLED_DEEPSLATE, Items.DIRT, Items.SAND, Items.RED_SAND, Items.GRAVEL,
                Items.NETHERRACK, Items.BLACKSTONE, Items.BASALT, Items.TUFF, Items.CALCITE, Items.ICE, Items.SNOWBALL,
                Items.KELP, Items.BAMBOO, Items.VINE, Items.MELON_SLICE, Items.ROTTEN_FLESH, Items.SHORT_GRASS);
        base(2, Items.CLAY_BALL, Items.FLINT, Items.SUGAR_CANE, Items.WHEAT, Items.CACTUS, Items.EGG, Items.CARROT,
                Items.POTATO, Items.BEETROOT, Items.COCOA_BEANS, Items.END_STONE, Items.SOUL_SAND, Items.SOUL_SOIL,
                Items.MOSS_BLOCK, Items.BROWN_MUSHROOM, Items.RED_MUSHROOM, ModItems.PLANT_FIBER.get());
        base(4, Items.STRING, Items.FEATHER, Items.BONE, Items.SPIDER_EYE, Items.PUMPKIN, Items.APPLE, Items.INK_SAC,
                Items.SWEET_BERRIES, Items.GLOW_BERRIES);
        base(8, Items.LEATHER, Items.GUNPOWDER, Items.SLIME_BALL, Items.COAL, Items.REDSTONE, Items.GLOWSTONE_DUST,
                Items.PRISMARINE_SHARD, Items.HONEYCOMB, Items.RAW_COPPER, ModItems.RAW_TIN.get());
        base(16, Items.RAW_IRON, Items.LAPIS_LAZULI, Items.QUARTZ, Items.AMETHYST_SHARD, Items.OBSIDIAN,
                Items.PRISMARINE_CRYSTALS);
        base(32, Items.RAW_GOLD, Items.ENDER_PEARL, Items.BLAZE_ROD);
        base(64, Items.GHAST_TEAR);
        base(128, Items.DIAMOND, Items.EMERALD);
        base(512, Items.NETHERITE_SCRAP);
    }

    /** Values for whole groups of natural items, checked after the fixed list. */
    private static int tagValue(ItemStack stack) {
        if (stack.is(ItemTags.LOGS)) return 8;
        if (stack.is(ItemTags.SAPLINGS)) return 4;
        if (stack.is(ItemTags.FLOWERS)) return 2;
        if (stack.is(ItemTags.LEAVES)) return 1;
        if (stack.is(Tags.Items.SEEDS)) return 2;
        if (stack.is(Tags.Items.ORES)) return 16;
        return UNPRINTABLE;
    }

    /** The matter value of one of this item, or UNPRINTABLE. */
    public static int valueOf(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return UNPRINTABLE;
        }
        index(level);
        return compute(level, stack.getItem(), 0, new HashSet<>());
    }

    /** Builds a list of recipes by what they make (redone if recipes are reloaded with /reload). */
    private static void index(Level level) {
        RecipeManager manager = level.getRecipeManager();
        if (manager == indexedFor) {
            return;
        }
        indexedFor = manager;
        cache.clear();
        recipesByResult.clear();
        List<RecipeHolder<?>> all = new ArrayList<>();
        all.addAll(manager.getAllRecipesFor(RecipeType.CRAFTING));
        all.addAll(manager.getAllRecipesFor(RecipeType.SMELTING));
        for (RecipeHolder<?> holder : all) {
            Recipe<?> recipe = holder.value();
            if (recipe.isSpecial() || recipe.getIngredients().isEmpty()) {
                continue;
            }
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (!result.isEmpty()) {
                recipesByResult.computeIfAbsent(result.getItem(), item -> new ArrayList<>()).add(holder);
            }
        }
    }

    private static int compute(Level level, Item item, int depth, Set<Item> visiting) {
        Integer known = BASE.get(item);
        if (known != null) {
            return known;
        }
        Integer cached = cache.get(item);
        if (cached != null) {
            return cached;
        }
        int tagged = tagValue(item.getDefaultInstance());
        if (tagged != UNPRINTABLE) {
            cache.put(item, tagged);
            return tagged;
        }
        if (depth > MAX_DEPTH || !visiting.add(item)) {
            return UNPRINTABLE; // a loop (iron ingot <-> iron block) or too deep
        }
        int best = UNPRINTABLE;
        for (RecipeHolder<?> holder : recipesByResult.getOrDefault(item, List.of())) {
            Recipe<?> recipe = holder.value();
            long sum = recipe.getType() == RecipeType.SMELTING ? 1 : 0; // a little fuel for smelting
            boolean ok = true;
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) {
                    continue;
                }
                int cheapest = UNPRINTABLE;
                for (ItemStack option : ingredient.getItems()) {
                    int value = compute(level, option.getItem(), depth + 1, visiting);
                    if (value != UNPRINTABLE && (cheapest == UNPRINTABLE || value < cheapest)) {
                        cheapest = value;
                    }
                }
                if (cheapest == UNPRINTABLE) {
                    ok = false;
                    break;
                }
                sum += cheapest;
            }
            if (!ok) {
                continue;
            }
            int made = Math.max(1, recipe.getResultItem(level.registryAccess()).getCount());
            int each = (int) Math.max(1, Math.min(Integer.MAX_VALUE / 2, (sum + made - 1) / made));
            if (best == UNPRINTABLE || each < best) {
                best = each;
            }
        }
        visiting.remove(item);
        // Only remember answers found from the top level of a search, where loops can't skew them
        if (depth == 0 || best != UNPRINTABLE) {
            cache.put(item, best);
        }
        return best;
    }

    private MatterValues() {
    }
}
