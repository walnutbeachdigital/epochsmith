package com.walnutbeachdigital.epochsmith.item;

import net.minecraft.world.item.Item;

/**
 * The Smith's Hammer. Its only job is striking metal on a Stone Anvil,
 * which checks for this class. Loses 1 durability per strike.
 */
public class SmithsHammerItem extends Item {

    public SmithsHammerItem(Properties properties) {
        super(properties);
    }
}
