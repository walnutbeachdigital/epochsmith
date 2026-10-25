package com.walnutbeachdigital.epochsmith.multiblock;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.epochsmith.item.SmithsHammerItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A building block for multiblock machines (bricks, casings, cylinders).
 * On its own it's an ordinary block. Once the machine is built it turns invisible, and one of them (the core)
 * draws the whole machine as one big model. Clicking any part works the machine; breaking any part takes it apart.
 */
public class MultiblockPartBlock extends Block {

    public static final MapCodec<MultiblockPartBlock> CODEC = simpleCodec(MultiblockPartBlock::new);

    public enum Form implements StringRepresentable {
        NONE, HIDDEN, CORE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public static final EnumProperty<Form> FORM = EnumProperty.create("form", Form.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Set on the core while the machine is working, so its model can glow. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public MultiblockPartBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FORM, Form.NONE).setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORM, FACING, ACTIVE);
    }

    public static boolean isFormed(BlockState state) {
        return state.getValue(FORM) != Form.NONE;
    }

    /** Built parts let light through, so the big model isn't drawn in darkness. */
    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return isFormed(state) ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    /** No dark shading on the blocks around a built machine from these invisible parts. */
    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return isFormed(state) ? 1.0F : super.getShadeBrightness(state, level, pos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof SmithsHammerItem && !isFormed(state)) {
            return Multiblocks.strike(level, pos, player, stack, hand);
        }
        BlockPos controller = isFormed(state) ? Multiblocks.findController(level, pos) : null;
        if (controller == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return level.getBlockState(controller).useItemOn(stack, level, player, hand, hit.withPosition(controller));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos controller = isFormed(state) ? Multiblocks.findController(level, pos) : null;
        if (controller == null) {
            return InteractionResult.PASS;
        }
        return level.getBlockState(controller).useWithoutItem(level, player, hit.withPosition(controller));
    }

    /** Breaking one part takes the whole machine apart (the other blocks stay where they are). */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(newState.getBlock()) && isFormed(state)) {
            Multiblocks.partRemoved(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
