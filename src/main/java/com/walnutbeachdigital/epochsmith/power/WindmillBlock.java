package com.walnutbeachdigital.epochsmith.power;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Wind power. Needs open sky above. The higher it stands, the stronger the wind:
 * force 2 low down, 4 from Y 64, 6 from Y 80, 8 from Y 100. Rain adds 2, storms add 4.
 */
public class WindmillBlock extends KineticBlock implements RotationSource {

    public static final MapCodec<WindmillBlock> CODEC = simpleCodec(WindmillBlock::new);

    public WindmillBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(AXIS, Direction.Axis.Z));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeAlong(state.getValue(AXIS), 4, 12);
    }

    @Override
    public int getForce(Level level, BlockPos pos, BlockState state) {
        if (!level.canSeeSky(pos.above())) {
            return 0;
        }
        int y = pos.getY();
        int force = y >= 100 ? 8 : y >= 80 ? 6 : y >= 64 ? 4 : 2;
        if (level.isThundering()) {
            force += 4;
        } else if (level.isRaining()) {
            force += 2;
        }
        return force;
    }
}
