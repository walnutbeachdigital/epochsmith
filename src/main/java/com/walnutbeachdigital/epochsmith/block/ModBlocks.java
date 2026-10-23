package com.walnutbeachdigital.epochsmith.block;

import com.walnutbeachdigital.epochsmith.Epochsmith;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * All of Epochsmith's blocks live here, each with the item you hold to place it.
 */
public final class ModBlocks {

    // ---- Primitive era ----

    /** The Primitive era's crafting station: knap flint into blades here. */
    public static final DeferredBlock<KnappingStoneBlock> KNAPPING_STONE = Epochsmith.BLOCKS.register("knapping_stone",
            () -> new KnappingStoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.5F)
                    .sound(SoundType.STONE)
                    .noOcclusion()));
    public static final DeferredItem<BlockItem> KNAPPING_STONE_ITEM =
            Epochsmith.ITEMS.registerSimpleBlockItem("knapping_stone", KNAPPING_STONE);

    /** The Primitive era's smelter and signature invention. Glows while lit. */
    public static final DeferredBlock<PitKilnBlock> PIT_KILN = Epochsmith.BLOCKS.register("pit_kiln",
            () -> new PitKilnBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_ORANGE)
                    .strength(1.0F)
                    .sound(SoundType.MUD_BRICKS)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 13 : 0)));
    public static final DeferredItem<BlockItem> PIT_KILN_ITEM =
            Epochsmith.ITEMS.registerSimpleBlockItem("pit_kiln", PIT_KILN);

    /** The Primitive era's storage: 9 slots, keeps its contents when broken. */
    public static final DeferredBlock<WovenBasketBlock> WOVEN_BASKET = Epochsmith.BLOCKS.register("woven_basket",
            () -> new WovenBasketBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.5F)
                    .sound(SoundType.GRASS)
                    .noOcclusion()));
    public static final DeferredItem<BlockItem> WOVEN_BASKET_ITEM =
            Epochsmith.ITEMS.registerSimpleBlockItem("woven_basket", WOVEN_BASKET, new Item.Properties().stacksTo(1));

    // ---- Bronze and Iron era: tin ore ----

    /** Tin ore. Any pickaxe can mine it, including flint (tin was historically easy to gather). */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> TIN_ORE = Epochsmith.BLOCKS.registerSimpleBlock("tin_ore",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 3.0F));
    public static final DeferredItem<BlockItem> TIN_ORE_ITEM =
            Epochsmith.ITEMS.registerSimpleBlockItem("tin_ore", TIN_ORE);

    public static final DeferredBlock<net.minecraft.world.level.block.Block> DEEPSLATE_TIN_ORE = Epochsmith.BLOCKS.registerSimpleBlock("deepslate_tin_ore",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .requiresCorrectToolForDrops()
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE));
    public static final DeferredItem<BlockItem> DEEPSLATE_TIN_ORE_ITEM =
            Epochsmith.ITEMS.registerSimpleBlockItem("deepslate_tin_ore", DEEPSLATE_TIN_ORE);

    /** The Bronze and Iron era's smelter and signature invention. Glows while lit. */
    public static final DeferredBlock<BloomeryBlock> BLOOMERY = Epochsmith.BLOCKS.register("bloomery",
            () -> new BloomeryBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_RED)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.MUD_BRICKS)
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 13 : 0)));
    public static final DeferredItem<BlockItem> BLOOMERY_ITEM =
            Epochsmith.ITEMS.registerSimpleBlockItem("bloomery", BLOOMERY);

    /** Called from the main mod class so this file loads before blocks are registered. */
    public static void init() {
    }

    private ModBlocks() {
    }
}
