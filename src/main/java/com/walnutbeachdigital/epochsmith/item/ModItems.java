package com.walnutbeachdigital.epochsmith.item;

import com.walnutbeachdigital.epochsmith.Epochsmith;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
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

    /** Called from the main mod class so this file loads before items are registered. */
    public static void init() {
    }

    private ModItems() {
    }
}
