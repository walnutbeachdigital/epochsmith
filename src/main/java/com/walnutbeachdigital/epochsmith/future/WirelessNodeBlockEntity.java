package com.walnutbeachdigital.epochsmith.future;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Registers the node's position while it's loaded, so other nodes can find it. */
public class WirelessNodeBlockEntity extends BlockEntity {

    public WirelessNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_NODE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            WirelessNodes.add(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            WirelessNodes.remove(level, worldPosition);
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null) {
            WirelessNodes.remove(level, worldPosition);
        }
        super.onChunkUnloaded();
    }
}
