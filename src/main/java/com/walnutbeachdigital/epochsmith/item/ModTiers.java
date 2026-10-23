package com.walnutbeachdigital.epochsmith.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

/**
 * Tool materials. Each era adds one.
 */
public final class ModTiers {

    /**
     * Flint: between wood and stone. Mines stone and coal but not iron.
     * Durability 100 (wood 59, stone 131), speed 3 (wood 2, stone 4).
     * Repaired with flint blades.
     */
    public static final Tier FLINT = new SimpleTier(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL,
            100,
            3.0F,
            0.5F,
            10,
            () -> Ingredient.of(ModItems.FLINT_BLADE.get()));

    /**
     * Bronze: close to iron. Mines everything iron can (including diamond).
     * Durability 220 (iron 250), speed 6 like iron, and easier to enchant (16, iron 14).
     * Repaired with bronze ingots.
     */
    public static final Tier BRONZE = new SimpleTier(
            BlockTags.INCORRECT_FOR_IRON_TOOL,
            220,
            6.0F,
            2.0F,
            16,
            () -> Ingredient.of(ModItems.BRONZE_INGOT.get()));

    private ModTiers() {
    }
}
