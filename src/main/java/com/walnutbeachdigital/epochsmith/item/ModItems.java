package com.walnutbeachdigital.epochsmith.item;

import com.walnutbeachdigital.epochsmith.Epochsmith;

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

    /** Called from the main mod class so this file loads before items are registered. */
    public static void init() {
    }

    private ModItems() {
    }
}
