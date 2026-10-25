package com.walnutbeachdigital.epochsmith.multiblock;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * The block at the heart of a multiblock machine: it holds the machine's contents and works once the rest of
 * the structure has been built around it and struck with a Smith's Hammer.
 * Its block state needs the {@link #FORMED} and {@link #FACING} properties.
 */
public interface MultiblockController {

    BooleanProperty FORMED = BooleanProperty.create("formed");
    DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    MultiblockShape shape();

    /** The controller's state once the machine is put together (or taken apart). */
    default BlockState withFormed(BlockState state, Direction front, boolean formed) {
        return state.setValue(FORMED, formed).setValue(FACING, front);
    }

    static boolean isFormed(BlockState state) {
        return state.getBlock() instanceof MultiblockController && state.getValue(FORMED);
    }
}
