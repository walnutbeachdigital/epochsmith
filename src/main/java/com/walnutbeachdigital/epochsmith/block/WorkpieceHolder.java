package com.walnutbeachdigital.epochsmith.block;

import net.minecraft.world.item.ItemStack;

/**
 * A workstation that shows what you're working on lying on top of it (anvil, joiner's bench).
 */
public interface WorkpieceHolder {

    ItemStack getWorkpiece();
}
