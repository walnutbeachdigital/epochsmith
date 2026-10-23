package com.walnutbeachdigital.epochsmith.item;

import com.walnutbeachdigital.epochsmith.Epochsmith;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * All of Epochsmith's items live here.
 */
public final class ModItems {

    /** The Chronicle of Invention: the in-game journal. */
    public static final DeferredItem<ChronicleItem> CHRONICLE = Epochsmith.ITEMS.registerItem(
            "chronicle", ChronicleItem::new, new Item.Properties().stacksTo(1));

    // ---- Primitive era: materials ----

    /** A sharp flint edge, made by knapping flint on a knapping stone. */
    public static final DeferredItem<Item> FLINT_BLADE = Epochsmith.ITEMS.registerSimpleItem("flint_blade");

    /** Dropped sometimes when breaking grass and ferns. */
    public static final DeferredItem<Item> PLANT_FIBER = Epochsmith.ITEMS.registerSimpleItem("plant_fiber");

    /** Twisted plant fiber, used to lash tool heads to handles. */
    public static final DeferredItem<Item> CORDAGE = Epochsmith.ITEMS.registerSimpleItem("cordage");

    // ---- Primitive era: flint tools ----

    public static final DeferredItem<SwordItem> FLINT_KNIFE = Epochsmith.ITEMS.register("flint_knife",
            () -> new SwordItem(ModTiers.FLINT, new Item.Properties()
                    .attributes(SwordItem.createAttributes(ModTiers.FLINT, 2, -2.0F))));

    public static final DeferredItem<PickaxeItem> FLINT_PICKAXE = Epochsmith.ITEMS.register("flint_pickaxe",
            () -> new PickaxeItem(ModTiers.FLINT, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(ModTiers.FLINT, 1.0F, -2.8F))));

    public static final DeferredItem<AxeItem> FLINT_AXE = Epochsmith.ITEMS.register("flint_axe",
            () -> new AxeItem(ModTiers.FLINT, new Item.Properties()
                    .attributes(AxeItem.createAttributes(ModTiers.FLINT, 6.0F, -3.2F))));

    public static final DeferredItem<ShovelItem> FLINT_SHOVEL = Epochsmith.ITEMS.register("flint_shovel",
            () -> new ShovelItem(ModTiers.FLINT, new Item.Properties()
                    .attributes(ShovelItem.createAttributes(ModTiers.FLINT, 1.5F, -3.0F))));

    // ---- Bronze and Iron era: tin ----

    /** Dropped by tin ore. Smelts into a tin ingot. */
    public static final DeferredItem<Item> RAW_TIN = Epochsmith.ITEMS.registerSimpleItem("raw_tin");
    /** Alloyed with copper in the bloomery to make bronze. */
    public static final DeferredItem<Item> TIN_INGOT = Epochsmith.ITEMS.registerSimpleItem("tin_ingot");

    /** Copper and tin alloyed in the bloomery. The Bronze and Iron era's main metal. */
    public static final DeferredItem<Item> BRONZE_INGOT = Epochsmith.ITEMS.registerSimpleItem("bronze_ingot");
    /** A spongy lump of iron from the bloomery. Hammer it on an anvil to get wrought iron. */
    public static final DeferredItem<Item> IRON_BLOOM = Epochsmith.ITEMS.registerSimpleItem("iron_bloom");

    /** Bronze hammered flat on the anvil. Used for bronze tool heads and armor. */
    public static final DeferredItem<Item> BRONZE_PLATE = Epochsmith.ITEMS.registerSimpleItem("bronze_plate");
    /** An iron bloom with the slag beaten out. The era's strongest metal. */
    public static final DeferredItem<Item> WROUGHT_IRON_INGOT = Epochsmith.ITEMS.registerSimpleItem("wrought_iron_ingot");
    /** Strikes metal on the Stone Anvil. 192 strikes before it breaks. */
    public static final DeferredItem<SmithsHammerItem> SMITHS_HAMMER = Epochsmith.ITEMS.registerItem(
            "smiths_hammer", SmithsHammerItem::new, new Item.Properties().durability(192));

    // ---- Primitive era: hide armor (durability a bit above leather) ----

    private static final int HIDE_DURABILITY = 6;

    public static final DeferredItem<ArmorItem> HIDE_HELMET = Epochsmith.ITEMS.register("hide_helmet",
            () -> new ArmorItem(ModArmorMaterials.HIDE, ArmorItem.Type.HELMET, new Item.Properties()
                    .durability(ArmorItem.Type.HELMET.getDurability(HIDE_DURABILITY))));
    public static final DeferredItem<ArmorItem> HIDE_CHESTPLATE = Epochsmith.ITEMS.register("hide_chestplate",
            () -> new ArmorItem(ModArmorMaterials.HIDE, ArmorItem.Type.CHESTPLATE, new Item.Properties()
                    .durability(ArmorItem.Type.CHESTPLATE.getDurability(HIDE_DURABILITY))));
    public static final DeferredItem<ArmorItem> HIDE_LEGGINGS = Epochsmith.ITEMS.register("hide_leggings",
            () -> new ArmorItem(ModArmorMaterials.HIDE, ArmorItem.Type.LEGGINGS, new Item.Properties()
                    .durability(ArmorItem.Type.LEGGINGS.getDurability(HIDE_DURABILITY))));
    public static final DeferredItem<ArmorItem> HIDE_BOOTS = Epochsmith.ITEMS.register("hide_boots",
            () -> new ArmorItem(ModArmorMaterials.HIDE, ArmorItem.Type.BOOTS, new Item.Properties()
                    .durability(ArmorItem.Type.BOOTS.getDurability(HIDE_DURABILITY))));

    /** Shaped from clay; fire it in a pit kiln to get a Clay Amphora. */
    public static final DeferredItem<Item> UNFIRED_AMPHORA = Epochsmith.ITEMS.registerItem(
            "unfired_amphora", Item::new, new Item.Properties().stacksTo(16));

    // ---- Mechanical era: machine parts (cut on the Joiner's Bench) ----

    public static final DeferredItem<Item> WOODEN_GEAR = Epochsmith.ITEMS.registerSimpleItem("wooden_gear");
    public static final DeferredItem<Item> IRON_GEAR = Epochsmith.ITEMS.registerSimpleItem("iron_gear");

    /** Wrought iron refined in the forge. The Mechanical era's metal. */
    public static final DeferredItem<Item> STEEL_INGOT = Epochsmith.ITEMS.registerSimpleItem("steel_ingot");

    // ---- Bronze and Iron era: bronze tools (attack stats match iron's) ----

    public static final DeferredItem<SwordItem> BRONZE_SWORD = Epochsmith.ITEMS.register("bronze_sword",
            () -> new SwordItem(ModTiers.BRONZE, new Item.Properties()
                    .attributes(SwordItem.createAttributes(ModTiers.BRONZE, 3, -2.4F))));
    public static final DeferredItem<PickaxeItem> BRONZE_PICKAXE = Epochsmith.ITEMS.register("bronze_pickaxe",
            () -> new PickaxeItem(ModTiers.BRONZE, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(ModTiers.BRONZE, 1.0F, -2.8F))));
    public static final DeferredItem<AxeItem> BRONZE_AXE = Epochsmith.ITEMS.register("bronze_axe",
            () -> new AxeItem(ModTiers.BRONZE, new Item.Properties()
                    .attributes(AxeItem.createAttributes(ModTiers.BRONZE, 6.0F, -3.1F))));
    public static final DeferredItem<ShovelItem> BRONZE_SHOVEL = Epochsmith.ITEMS.register("bronze_shovel",
            () -> new ShovelItem(ModTiers.BRONZE, new Item.Properties()
                    .attributes(ShovelItem.createAttributes(ModTiers.BRONZE, 1.5F, -3.0F))));
    public static final DeferredItem<HoeItem> BRONZE_HOE = Epochsmith.ITEMS.register("bronze_hoe",
            () -> new HoeItem(ModTiers.BRONZE, new Item.Properties()
                    .attributes(HoeItem.createAttributes(ModTiers.BRONZE, -2.0F, -1.0F))));

    // ---- Bronze and Iron era: bronze lamellar armor (durability a bit under iron) ----

    private static final int BRONZE_DURABILITY = 14;

    public static final DeferredItem<ArmorItem> BRONZE_HELMET = Epochsmith.ITEMS.register("bronze_helmet",
            () -> new ArmorItem(ModArmorMaterials.BRONZE, ArmorItem.Type.HELMET, new Item.Properties()
                    .durability(ArmorItem.Type.HELMET.getDurability(BRONZE_DURABILITY))));
    public static final DeferredItem<ArmorItem> BRONZE_CHESTPLATE = Epochsmith.ITEMS.register("bronze_chestplate",
            () -> new ArmorItem(ModArmorMaterials.BRONZE, ArmorItem.Type.CHESTPLATE, new Item.Properties()
                    .durability(ArmorItem.Type.CHESTPLATE.getDurability(BRONZE_DURABILITY))));
    public static final DeferredItem<ArmorItem> BRONZE_LEGGINGS = Epochsmith.ITEMS.register("bronze_leggings",
            () -> new ArmorItem(ModArmorMaterials.BRONZE, ArmorItem.Type.LEGGINGS, new Item.Properties()
                    .durability(ArmorItem.Type.LEGGINGS.getDurability(BRONZE_DURABILITY))));
    public static final DeferredItem<ArmorItem> BRONZE_BOOTS = Epochsmith.ITEMS.register("bronze_boots",
            () -> new ArmorItem(ModArmorMaterials.BRONZE, ArmorItem.Type.BOOTS, new Item.Properties()
                    .durability(ArmorItem.Type.BOOTS.getDurability(BRONZE_DURABILITY))));

    // ---- Mechanical era: steel tools (attack stats a step above iron) ----

    public static final DeferredItem<SwordItem> STEEL_SWORD = Epochsmith.ITEMS.register("steel_sword",
            () -> new SwordItem(ModTiers.STEEL, new Item.Properties()
                    .attributes(SwordItem.createAttributes(ModTiers.STEEL, 3, -2.4F))));
    public static final DeferredItem<PickaxeItem> STEEL_PICKAXE = Epochsmith.ITEMS.register("steel_pickaxe",
            () -> new PickaxeItem(ModTiers.STEEL, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(ModTiers.STEEL, 1.0F, -2.8F))));
    public static final DeferredItem<AxeItem> STEEL_AXE = Epochsmith.ITEMS.register("steel_axe",
            () -> new AxeItem(ModTiers.STEEL, new Item.Properties()
                    .attributes(AxeItem.createAttributes(ModTiers.STEEL, 5.5F, -3.0F))));
    public static final DeferredItem<ShovelItem> STEEL_SHOVEL = Epochsmith.ITEMS.register("steel_shovel",
            () -> new ShovelItem(ModTiers.STEEL, new Item.Properties()
                    .attributes(ShovelItem.createAttributes(ModTiers.STEEL, 1.5F, -3.0F))));
    public static final DeferredItem<HoeItem> STEEL_HOE = Epochsmith.ITEMS.register("steel_hoe",
            () -> new HoeItem(ModTiers.STEEL, new Item.Properties()
                    .attributes(HoeItem.createAttributes(ModTiers.STEEL, -2.5F, -0.5F))));

    /** Blocks the Hand Drill can dig: everything a pickaxe or a shovel can. */
    public static final TagKey<Block> HAND_DRILL_MINEABLE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Epochsmith.MODID, "mineable/hand_drill"));

    /** A crank-wound brace drill: one tool for both stone and earth. */
    public static final DeferredItem<DiggerItem> HAND_DRILL = Epochsmith.ITEMS.register("hand_drill",
            () -> new DiggerItem(ModTiers.STEEL, HAND_DRILL_MINEABLE, new Item.Properties()
                    .attributes(DiggerItem.createAttributes(ModTiers.STEEL, 1.0F, -2.8F))));

    // ---- Mechanical era: knight's plate armor ----

    private static final int KNIGHT_DURABILITY = 24;

    public static final DeferredItem<ArmorItem> KNIGHT_HELMET = Epochsmith.ITEMS.register("knight_helmet",
            () -> new ArmorItem(ModArmorMaterials.KNIGHT, ArmorItem.Type.HELMET, new Item.Properties()
                    .durability(ArmorItem.Type.HELMET.getDurability(KNIGHT_DURABILITY))));
    public static final DeferredItem<ArmorItem> KNIGHT_CHESTPLATE = Epochsmith.ITEMS.register("knight_chestplate",
            () -> new ArmorItem(ModArmorMaterials.KNIGHT, ArmorItem.Type.CHESTPLATE, new Item.Properties()
                    .durability(ArmorItem.Type.CHESTPLATE.getDurability(KNIGHT_DURABILITY))));
    public static final DeferredItem<ArmorItem> KNIGHT_LEGGINGS = Epochsmith.ITEMS.register("knight_leggings",
            () -> new ArmorItem(ModArmorMaterials.KNIGHT, ArmorItem.Type.LEGGINGS, new Item.Properties()
                    .durability(ArmorItem.Type.LEGGINGS.getDurability(KNIGHT_DURABILITY))));
    public static final DeferredItem<ArmorItem> KNIGHT_BOOTS = Epochsmith.ITEMS.register("knight_boots",
            () -> new ArmorItem(ModArmorMaterials.KNIGHT, ArmorItem.Type.BOOTS, new Item.Properties()
                    .durability(ArmorItem.Type.BOOTS.getDurability(KNIGHT_DURABILITY))));

    /** Called from the main mod class so this file loads before items are registered. */
    public static void init() {
    }

    private ModItems() {
    }
}
