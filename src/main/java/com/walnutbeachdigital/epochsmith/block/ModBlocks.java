package com.walnutbeachdigital.epochsmith.block;

import com.walnutbeachdigital.epochsmith.Epochsmith;
import com.walnutbeachdigital.epochsmith.future.FusionReactorBlock;
import com.walnutbeachdigital.epochsmith.future.NanofabricatorBlock;
import com.walnutbeachdigital.epochsmith.information.AssemblerBlock;
import com.walnutbeachdigital.epochsmith.information.ComputerCoreBlock;
import com.walnutbeachdigital.epochsmith.information.DataVaultBlock;
import com.walnutbeachdigital.epochsmith.information.FabricatorBlock;
import com.walnutbeachdigital.epochsmith.information.InductionFurnaceBlock;
import com.walnutbeachdigital.epochsmith.information.SolarPanelBlock;
import com.walnutbeachdigital.epochsmith.electric.ArcFurnaceBlock;
import com.walnutbeachdigital.epochsmith.electric.BatteryBlock;
import com.walnutbeachdigital.epochsmith.electric.CopperWireBlock;
import com.walnutbeachdigital.epochsmith.electric.DynamoBlock;
import com.walnutbeachdigital.epochsmith.electric.ElectricLampBlock;
import com.walnutbeachdigital.epochsmith.electric.SortingCabinetBlock;
import com.walnutbeachdigital.epochsmith.power.AxleBlock;
import com.walnutbeachdigital.epochsmith.power.GearboxBlock;
import com.walnutbeachdigital.epochsmith.power.HandCrankBlock;
import com.walnutbeachdigital.epochsmith.power.WaterWheelBlock;
import com.walnutbeachdigital.epochsmith.power.WindmillBlock;
import com.walnutbeachdigital.epochsmith.steam.BlastFurnaceBlock;
import com.walnutbeachdigital.epochsmith.steam.BoilerBlock;
import com.walnutbeachdigital.epochsmith.steam.CokeOvenBlock;
import com.walnutbeachdigital.epochsmith.steam.LatheBlock;
import com.walnutbeachdigital.epochsmith.steam.SteamEngineBlock;
import com.walnutbeachdigital.epochsmith.steam.SteamPipeBlock;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
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

    /** The Mechanical era's smelter: powered bellows, extra ore, and crucible steel. */
    public static final DeferredBlock<ForgeBlock> FORGE = Epochsmith.BLOCKS.register("forge",
            () -> new ForgeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .requiresCorrectToolForDrops()
                    .strength(3.5F)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 14 : 0)));
    public static final DeferredItem<BlockItem> FORGE_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("forge", FORGE);

    /** The Mechanical era's signature invention: reads the sky, unlocks the Industrial era. */
    public static final DeferredBlock<GearTrainBlock> GEAR_TRAIN = Epochsmith.BLOCKS.register("gear_train",
            () -> new GearTrainBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_ORANGE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.COPPER)));
    public static final DeferredItem<BlockItem> GEAR_TRAIN_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("gear_train", GEAR_TRAIN);

    /** The Mechanical era's storage: 27 slots, two side by side open as one. */
    public static final DeferredBlock<BandedCrateBlock> BANDED_CRATE = Epochsmith.BLOCKS.register("banded_crate",
            () -> new BandedCrateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> BANDED_CRATE_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("banded_crate", BANDED_CRATE);

    // ---- Industrial era: steam ----

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                .strength(4.0F, 6.0F).sound(SoundType.METAL);
    }

    /** Bakes coal into coke. */
    public static final DeferredBlock<CokeOvenBlock> COKE_OVEN = Epochsmith.BLOCKS.register("coke_oven",
            () -> new CokeOvenBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).requiresCorrectToolForDrops()
                    .strength(3.0F).sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 10 : 0)));
    public static final DeferredItem<BlockItem> COKE_OVEN_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("coke_oven", COKE_OVEN);

    /** Boils water into steam. */
    public static final DeferredBlock<BoilerBlock> BOILER = Epochsmith.BLOCKS.register("boiler",
            () -> new BoilerBlock(metal().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 9 : 0)));
    public static final DeferredItem<BlockItem> BOILER_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("boiler", BOILER);

    /** Carries steam; joins up to neighbours on its own. */
    public static final DeferredBlock<SteamPipeBlock> STEAM_PIPE = Epochsmith.BLOCKS.register("steam_pipe",
            () -> new SteamPipeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.COPPER).noOcclusion()));
    public static final DeferredItem<BlockItem> STEAM_PIPE_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("steam_pipe", STEAM_PIPE);

    /** Turns steam into rotation power. The Industrial era's signature invention. */
    public static final DeferredBlock<SteamEngineBlock> STEAM_ENGINE = Epochsmith.BLOCKS.register("steam_engine",
            () -> new SteamEngineBlock(metal().noOcclusion()));
    public static final DeferredItem<BlockItem> STEAM_ENGINE_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("steam_engine", STEAM_ENGINE);

    /** The Industrial era's crafting station: steam-powered, turns precision parts. */
    public static final DeferredBlock<LatheBlock> LATHE = Epochsmith.BLOCKS.register("lathe",
            () -> new LatheBlock(metal().noOcclusion()));
    public static final DeferredItem<BlockItem> LATHE_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("lathe", LATHE);

    /** The Industrial era's smelter: steam blast and coke, 2 ingots per ore, steel from iron. */
    public static final DeferredBlock<BlastFurnaceBlock> STEAM_BLAST_FURNACE = Epochsmith.BLOCKS.register("steam_blast_furnace",
            () -> new BlastFurnaceBlock(metal().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 14 : 0)));
    public static final DeferredItem<BlockItem> STEAM_BLAST_FURNACE_ITEM =
            Epochsmith.ITEMS.registerSimpleBlockItem("steam_blast_furnace", STEAM_BLAST_FURNACE);

    /** The Industrial era's storage: 54 slots, locked to its owner, blast-proof, hopper-proof. */
    public static final DeferredBlock<StrongboxBlock> STRONGBOX = Epochsmith.BLOCKS.register("strongbox",
            () -> new StrongboxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops().strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK)));
    public static final DeferredItem<BlockItem> STRONGBOX_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("strongbox", STRONGBOX);

    // ---- Electrical era ----

    /** Rotation in, electricity out. */
    public static final DeferredBlock<DynamoBlock> DYNAMO = Epochsmith.BLOCKS.register("dynamo",
            () -> new DynamoBlock(metal()));
    public static final DeferredItem<BlockItem> DYNAMO_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("dynamo", DYNAMO);

    /** Carries electricity; joins up on its own. */
    public static final DeferredBlock<CopperWireBlock> COPPER_WIRE = Epochsmith.BLOCKS.register("copper_wire",
            () -> new CopperWireBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.5F).sound(SoundType.WOOL).noOcclusion()));
    public static final DeferredItem<BlockItem> COPPER_WIRE_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("copper_wire", COPPER_WIRE);

    /** Stores 200,000 energy. */
    public static final DeferredBlock<BatteryBlock> BATTERY = Epochsmith.BLOCKS.register("battery",
            () -> new BatteryBlock(metal()));
    public static final DeferredItem<BlockItem> BATTERY_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("battery", BATTERY);

    /** Full-brightness electric light. The Electrical era's signature invention. */
    public static final DeferredBlock<ElectricLampBlock> ELECTRIC_LAMP = Epochsmith.BLOCKS.register("electric_lamp",
            () -> new ElectricLampBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW)
                    .strength(1.0F).sound(SoundType.GLASS)
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0)));
    public static final DeferredItem<BlockItem> ELECTRIC_LAMP_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("electric_lamp", ELECTRIC_LAMP);

    /** The Electrical era's smelter: electric, very fast, smelts anything. */
    public static final DeferredBlock<ArcFurnaceBlock> ARC_FURNACE = Epochsmith.BLOCKS.register("arc_furnace",
            () -> new ArcFurnaceBlock(metal().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0)));
    public static final DeferredItem<BlockItem> ARC_FURNACE_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("arc_furnace", ARC_FURNACE);

    /** The Electrical era's storage: nine labeled drawers of 1,024 that file items for you. */
    public static final DeferredBlock<SortingCabinetBlock> SORTING_CABINET = Epochsmith.BLOCKS.register("sorting_cabinet",
            () -> new SortingCabinetBlock(metal()));
    public static final DeferredItem<BlockItem> SORTING_CABINET_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("sorting_cabinet", SORTING_CABINET);

    // ---- Information era
    /** Free power from sunlight. */
    public static final DeferredBlock<SolarPanelBlock> SOLAR_PANEL = Epochsmith.BLOCKS.register("solar_panel",
            () -> new SolarPanelBlock(metal().noOcclusion()));
    public static final DeferredItem<BlockItem> SOLAR_PANEL_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("solar_panel", SOLAR_PANEL);

    /** Etches silicon into transistors. */
    public static final DeferredBlock<FabricatorBlock> FABRICATOR = Epochsmith.BLOCKS.register("circuit_fabricator",
            () -> new FabricatorBlock(metal().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 10 : 0)));
    public static final DeferredItem<BlockItem> FABRICATOR_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("circuit_fabricator", FABRICATOR);

    /** The Information era's signature invention. Booting one up unlocks the Future era. */
    public static final DeferredBlock<ComputerCoreBlock> COMPUTER_CORE = Epochsmith.BLOCKS.register("computer_core",
            () -> new ComputerCoreBlock(metal().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 7 : 0)));
    public static final DeferredItem<BlockItem> COMPUTER_CORE_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("computer_core", COMPUTER_CORE);

    /** The Information era's smelter: batch smelting, fully automatable. */
    public static final DeferredBlock<InductionFurnaceBlock> INDUCTION_FURNACE = Epochsmith.BLOCKS.register("induction_furnace",
            () -> new InductionFurnaceBlock(metal().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 13 : 0)));
    public static final DeferredItem<BlockItem> INDUCTION_FURNACE_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("induction_furnace", INDUCTION_FURNACE);

    /** The Information era's storage: 65,536 items as data, with a searchable terminal. */
    public static final DeferredBlock<DataVaultBlock> DATA_VAULT = Epochsmith.BLOCKS.register("data_vault",
            () -> new DataVaultBlock(metal().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 5 : 0)));
    public static final DeferredItem<BlockItem> DATA_VAULT_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("data_vault", DATA_VAULT,
            new Item.Properties().stacksTo(1));

    /** The Information era's crafting: teach it a recipe and it crafts on its own. */
    public static final DeferredBlock<AssemblerBlock> ASSEMBLER = Epochsmith.BLOCKS.register("automated_assembler",
            () -> new AssemblerBlock(metal().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 6 : 0)));
    public static final DeferredItem<BlockItem> ASSEMBLER_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("automated_assembler", ASSEMBLER);

    // ---- Future era
    /** Builds graphene and nanobots atom by atom. */
    public static final DeferredBlock<NanofabricatorBlock> NANOFABRICATOR = Epochsmith.BLOCKS.register("nanofabricator",
            () -> new NanofabricatorBlock(metal().lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 12 : 0)));
    public static final DeferredItem<BlockItem> NANOFABRICATOR_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("nanofabricator", NANOFABRICATOR);

    /** The Future era's signature invention and Epochsmith's end goal: fusion power from plasma cells. */
    public static final DeferredBlock<FusionReactorBlock> FUSION_REACTOR = Epochsmith.BLOCKS.register("fusion_reactor",
            () -> new FusionReactorBlock(metal().strength(10.0F, 1200.0F)
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0)));
    public static final DeferredItem<BlockItem> FUSION_REACTOR_ITEM = Epochsmith.ITEMS.registerSimpleBlockItem("fusion_reactor", FUSION_REACTOR,
            new Item.Properties().rarity(Rarity.EPIC));

    /** Called from the main mod class so this file loads before blocks are registered. */
    public static void init() {
    }

    private ModBlocks() {
    }
}
