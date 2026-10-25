package com.walnutbeachdigital.epochsmith.steam;

import com.walnutbeachdigital.epochsmith.menu.MachineMenus;
import com.walnutbeachdigital.epochsmith.menu.MachineGui;
import com.walnutbeachdigital.epochsmith.block.ModBlocks;
import com.walnutbeachdigital.epochsmith.item.SmithsHammerItem;
import com.walnutbeachdigital.epochsmith.multiblock.MultiblockController;
import com.walnutbeachdigital.epochsmith.multiblock.MultiblockShape;
import com.walnutbeachdigital.epochsmith.multiblock.Multiblocks;
import java.util.Map;
import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.era.Era;
import com.walnutbeachdigital.epochsmith.era.EraManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Turns coal into coke, a fuel that burns twice as long.
 * A multiblock: build a 3x3x3 cube of Coke Oven Bricks with this door in the middle of one side,
 * then strike it with a Smith's Hammer. Right-click any part of the built oven to open it.
 */
public class CokeOvenBlock extends BaseEntityBlock implements MultiblockController {

    public static final MapCodec<CokeOvenBlock> CODEC = simpleCodec(CokeOvenBlock::new);
    public static final DirectionProperty FACING = MultiblockController.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /** Layers bottom to top, rows front to back. O is this door, b a Coke Oven Brick. */
    public static final MultiblockShape SHAPE = new MultiblockShape(new String[][] {
            {"bbb", "bbb", "bbb"},
            {"bOb", "bbb", "bbb"},
            {"bbb", "bbb", "bbb"}},
            Map.of('b', ModBlocks.COKE_OVEN_BRICKS), 'O', new MultiblockShape.Cell(1, 1, 1));

    public CokeOvenBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false)
                .setValue(FORMED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public MultiblockShape shape() {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, FORMED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Once built it lets light through, like the bricks around it. */
    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(FORMED) ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CokeOvenBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.COKE_OVEN.get(), CokeOvenBlockEntity::serverTick);
    }

    /** Right-click opens the oven, once it's built. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(FORMED)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.epochsmith.coke_oven.unbuilt"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return MachineMenus.open(state, level, pos, player, Era.INDUSTRIAL, "message.epochsmith.coke_oven.unknown");
    }

    /** Strike with a Smith's Hammer to build the oven; anything else opens it. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof SmithsHammerItem) {
            return Multiblocks.strike(level, pos, player, stack, hand);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Breaking the door drops everything inside and takes the oven apart. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof MachineGui gui) {
                MachineMenus.dropAll(level, pos, gui.getMenuItems());
            }
            if (!level.isClientSide()) {
                Multiblocks.unform(level, pos, state);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Smoke rises from the chimney while it bakes. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && state.getValue(FORMED)) {
            BlockPos core = Multiblocks.corePos(pos, state);
            level.addParticle(ParticleTypes.LARGE_SMOKE, core.getX() + 0.5, core.getY() + 2.05, core.getZ() + 0.5, 0.0, 0.07, 0.0);
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, core.getX() + 0.5, core.getY() + 2.1, core.getZ() + 0.5, 0.0, 0.05, 0.0);
            }
        }
    }
}
