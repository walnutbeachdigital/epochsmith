package com.walnutbeachdigital.epochsmith.information;

import net.minecraft.core.BlockPos;

/**
 * A block with a searchable terminal: the data vault and quantum storage cells.
 */
public interface ItemVault {

    VaultStorage getStorage();

    /** The terminal only works with power. */
    boolean isPowered();

    BlockPos getBlockPos();
}
