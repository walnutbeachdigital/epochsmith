package com.walnutbeachdigital.epochsmith;

import org.slf4j.Logger;

import com.walnutbeachdigital.epochsmith.era.EraAttachments;
import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.block.ModBlocks;
import com.walnutbeachdigital.epochsmith.item.ModArmorMaterials;
import com.walnutbeachdigital.epochsmith.item.ModItems;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Epochsmith.MODID)
public class Epochsmith {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "epochsmith";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "epochsmith" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "epochsmith" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "epochsmith" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "epochsmith:example_block", combining the namespace and path
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "epochsmith:example_block", combining the namespace and path
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    // Creates a new food item with the id "epochsmith:example_id", nutrition 1 and saturation 2
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    // Creates a creative tab with the id "epochsmith:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.epochsmith")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.CHRONICLE.get());
                output.accept(ModBlocks.KNAPPING_STONE_ITEM.get());
                output.accept(ModBlocks.PIT_KILN_ITEM.get());
                output.accept(ModBlocks.WOVEN_BASKET_ITEM.get());
                output.accept(ModItems.FLINT_BLADE.get());
                output.accept(ModItems.PLANT_FIBER.get());
                output.accept(ModItems.CORDAGE.get());
                output.accept(ModItems.FLINT_KNIFE.get());
                output.accept(ModItems.FLINT_PICKAXE.get());
                output.accept(ModItems.FLINT_AXE.get());
                output.accept(ModItems.FLINT_SHOVEL.get());
                output.accept(ModItems.HIDE_HELMET.get());
                output.accept(ModItems.HIDE_CHESTPLATE.get());
                output.accept(ModItems.HIDE_LEGGINGS.get());
                output.accept(ModItems.HIDE_BOOTS.get());
                output.accept(ModBlocks.TIN_ORE_ITEM.get());
                output.accept(ModBlocks.DEEPSLATE_TIN_ORE_ITEM.get());
                output.accept(ModItems.RAW_TIN.get());
                output.accept(ModItems.TIN_INGOT.get());
                output.accept(ModBlocks.BLOOMERY_ITEM.get());
                output.accept(ModItems.BRONZE_INGOT.get());
                output.accept(ModItems.IRON_BLOOM.get());
                output.accept(ModBlocks.STONE_ANVIL_ITEM.get());
                output.accept(ModItems.SMITHS_HAMMER.get());
                output.accept(ModItems.BRONZE_PLATE.get());
                output.accept(ModItems.WROUGHT_IRON_INGOT.get());
                output.accept(ModItems.BRONZE_SWORD.get());
                output.accept(ModItems.BRONZE_PICKAXE.get());
                output.accept(ModItems.BRONZE_AXE.get());
                output.accept(ModItems.BRONZE_SHOVEL.get());
                output.accept(ModItems.BRONZE_HOE.get());
                output.accept(ModItems.BRONZE_HELMET.get());
                output.accept(ModItems.BRONZE_CHESTPLATE.get());
                output.accept(ModItems.BRONZE_LEGGINGS.get());
                output.accept(ModItems.BRONZE_BOOTS.get());
                output.accept(ModItems.UNFIRED_AMPHORA.get());
                output.accept(ModBlocks.AMPHORA_ITEM.get());
                output.accept(ModBlocks.AXLE_ITEM.get());
                output.accept(ModBlocks.HAND_CRANK_ITEM.get());
                output.accept(ModBlocks.WATER_WHEEL_ITEM.get());
                output.accept(ModBlocks.WINDMILL_ITEM.get());
                output.accept(ModBlocks.MILLSTONE_ITEM.get());
                output.accept(ModBlocks.GEARBOX_ITEM.get());
                output.accept(ModBlocks.JOINERS_BENCH_ITEM.get());
                output.accept(ModItems.WOODEN_GEAR.get());
                output.accept(ModItems.IRON_GEAR.get());
                output.accept(EXAMPLE_ITEM.get());// Add the example item to the tab. For your own tabs, this method is preferred over the event
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Epochsmith(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        // Load Epochsmith's block list (and their block items) before anything registers
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModBlocks.init();
        BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        // Load Epochsmith's item list, then register all items
        ModItems.init();
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the data Epochsmith saves on each player (their current era)
        EraAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (Epochsmith) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
