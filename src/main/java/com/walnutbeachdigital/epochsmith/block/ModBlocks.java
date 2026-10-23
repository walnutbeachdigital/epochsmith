package com.walnutbeachdigital.epochsmith.block;

import com.walnutbeachdigital.epochsmith.Epochsmith;
import com.walnutbeachdigital.epochsmith.power.AxleBlock;
import com.walnutbeachdigital.epochsmith.power.GearboxBlock;
import com.walnutbeachdigital.epochsmith.power.HandCrankBlock;
import com.walnutbeachdigital.epochsmith.power.WaterWheelBlock;
import com.walnutbeachdigital.epochsmith.power.WindmillBlock;

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

    /** The Bronze and Iron era's crafting station. Work metal on it with a Smith's Hammer. */
    public static final DeferredBlock<StoneAnvilBlock> STONE_ANVIL = Epochsmith.BLOCKS.register("stone_anvil",
            () -> new StoneAnvilBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()));
    public static final DeferredItem<BlockItem> STONE_ANVIL_ITEM =
            Epochsmith.ITEMS.registerSimpleBlockItem("stone_anvil", STONE_ANVIL);

    /** The Bronze and Iron era's storage: 18 slots, keeps its contents when broken. */
    public static final DeferredBlock<AmphoraBlock> AMPHORA = Epochsmith.BLOCKS.register("amphora",
            () -> new AmphoraBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_ORANGE)
                    .strength(0.6F)
                    .sound(SoundType.DECORATED_POT)
                    .noOcclusion()));
    /** Stacks to 16 so a full pit kiln batch of fired amphorae fits in one stack. */
    public static final DeferredItem<BlockItem> AMPHORA_ITEM =
            Epochsmith.ITEMS.registerSimpleBlockItem("amphora", AMPHORA, new Item.Properties().stacksTo(16));

    // ---- Mechanical era: rotation power ----

    private static BlockBehaviour.Properties wooden() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion();
    }

    /** Carries rotation in a straight line. */
    public static final DeferredBlock<AxleBlock> AXLE = Epochsmith.BLOCKS.register("axle", () -> new AxleBlock(wooden()));
    public static final DeferredItem<BlockItem> AXLE_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("axle", AXLE);

    /** Muscle power: right-click to turn. */
    public static final DeferredBlock<HandCrankBlock> HAND_CRANK = Epochsmith.BLOCKS.register("hand_crank", () -> new HandCrankBlock(wooden()));
    public static final DeferredItem<BlockItem> HAND_CRANK_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("hand_crank", HAND_CRANK);

    /** Water power: place it touching water. */
    public static final DeferredBlock<WaterWheelBlock> WATER_WHEEL = Epochsmith.BLOCKS.register("water_wheel", () -> new WaterWheelBlock(wooden()));
    public static final DeferredItem<BlockItem> WATER_WHEEL_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("water_wheel", WATER_WHEEL);

    /** Wind power: place it high up under open sky. */
    public static final DeferredBlock<WindmillBlock> WINDMILL = Epochsmith.BLOCKS.register("windmill", () -> new WindmillBlock(wooden()));
    public static final DeferredItem<BlockItem> WINDMILL_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("windmill", WINDMILL);

    /** Grinds cobblestone, gravel, bones and sugar cane using rotation power. */
    public static final DeferredBlock<MillstoneBlock> MILLSTONE = Epochsmith.BLOCKS.register("millstone",
            () -> new MillstoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(2.5F)
                    .sound(SoundType.STONE)));
    public static final DeferredItem<BlockItem> MILLSTONE_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("millstone", MILLSTONE);

    /** Passes rotation out of all six sides: turn corners and split power. */
    public static final DeferredBlock<GearboxBlock> GEARBOX = Epochsmith.BLOCKS.register("gearbox",
            () -> new GearboxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> GEARBOX_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("gearbox", GEARBOX);

    /** The Mechanical era's crafting station: cuts gears and axles, by hand or with power. */
    public static final DeferredBlock<JoinersBenchBlock> JOINERS_BENCH = Epochsmith.BLOCKS.register("joiners_bench",
            () -> new JoinersBenchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F)
                    .sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredItem<BlockItem> JOINERS_BENCH_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("joiners_bench", JOINERS_BENCH);

    /** Called from the main mod class so this file loads before blocks are registered. */
    public static void init() {
    }

    private ModBlocks() {
    }
}
