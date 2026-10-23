package com.walnutbeachdigital.epochsmith.item;

import com.walnutbeachdigital.epochsmith.Epochsmith;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * All of Epochsmith's items live here.
 */
public final class ModItems {

    /** The Chronicle of Invention: the in-game journal. */
    public static final DeferredItem<ChronicleItem> CHRONICLE = Epochsmith.ITEMS.registerItem(
            "chronicle", ChronicleItem::new, new Item.Properties().stacksTo(1));

    /** Called from the main mod class so this file loads before items are registered. */
    public static void init() {
    }

    private ModItems() {
    }
}
