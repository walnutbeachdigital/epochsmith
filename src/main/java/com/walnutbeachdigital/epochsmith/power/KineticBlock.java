package com.walnutbeachdigital.epochsmith.power;

import javax.annotation.Nullable;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Base class for blocks that spin around an axis. The block's own model is not drawn
 * the normal way; KineticRenderer draws it turning instead.
 */
public abstract class KineticBlock extends BaseEntityBlock implements Shaft {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    protected KineticBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public Direction.Axis getAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    /** A box of the given thickness (in pixels, out of 16) running along the axis. */
    protected static VoxelShape shapeAlong(Direction.Axis axis, double min, double max) {
        return switch (axis) {
            case X -> Block.box(0, min, min, 16, max, max);
            case Y -> Block.box(min, 0, min, max, 16, max);
            case Z -> Block.box(min, min, 0, max, max, 16);
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeAlong(state.getValue(AXIS), 6, 10);
    }

    /** Drawn by KineticRenderer, so the normal block drawing is switched off. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KineticBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.KINETIC.get(), KineticBlockEntity::serverTick);
    }
}
