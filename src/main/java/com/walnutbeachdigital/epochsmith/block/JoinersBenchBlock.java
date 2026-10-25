package com.walnutbeachdigital.epochsmith.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.epochsmith.era.Era;
import com.walnutbeachdigital.epochsmith.era.EraManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Mechanical era's crafting station. Two blocks wide: place it and the other half appears to your right.
 *  - Right-click with planks, logs or wrought iron to lay them on the bench (up to 16).
 *  - Right-click with an empty hand to work them, or connect rotation power to any side.
 *  - Sneak + empty hand takes back what hasn't been worked.
 */
public class JoinersBenchBlock extends BaseEntityBlock {

    public static final MapCodec<JoinersBenchBlock> CODEC = simpleCodec(JoinersBenchBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** The bench is two blocks wide: the main half holds the work, the side half is the rest of the bench. */
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public enum Part implements StringRepresentable {
        MAIN, SIDE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public JoinersBenchBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.MAIN));
    }

    /** Which way the other half is: the side half sits to your right as you face the bench. */
    private static Direction towardPartner(BlockState state) {
        Direction facing = state.getValue(FACING);
        return state.getValue(PART) == Part.MAIN ? facing.getCounterClockWise() : facing.getClockWise();
    }

    /** Where the main half (and its workpiece) is. */
    public static BlockPos mainPos(BlockState state, BlockPos pos) {
        return state.getValue(PART) == Part.MAIN ? pos : pos.relative(towardPartner(state));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        BlockPos side = context.getClickedPos().relative(towardPartner(state));
        Level level = context.getLevel();
        // Needs room for both halves
        return level.getBlockState(side).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(side) ? state : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            level.setBlock(pos.relative(towardPartner(state)), state.setValue(PART, Part.SIDE), Block.UPDATE_ALL);
        }
    }

    /** If one half goes, so does the other (only the broken half drops the bench). */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
                                     BlockPos pos, BlockPos neighbourPos) {
        if (direction == towardPartner(state) && !(neighbour.is(this) && neighbour.getValue(PART) != state.getValue(PART)
                && neighbour.getValue(FACING) == state.getValue(FACING))) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == Part.MAIN ? new JoinersBenchBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || state.getValue(PART) != Part.MAIN ? null
                : createTickerHelper(type, ModBlockEntities.JOINERS_BENCH.get(), JoinersBenchBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!JoinersBenchBlockEntity.canWork(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        pos = mainPos(state, pos);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof JoinersBenchBlockEntity bench) {
            if (!EraManager.hasReached(player, Era.MECHANICAL)) {
                player.displayClientMessage(Component.translatable("message.epochsmith.bench.unknown"), true);
            } else if (bench.place(stack)) {
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.7F, 1.0F);
            } else {
                player.displayClientMessage(Component.translatable("message.epochsmith.bench.busy"), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        pos = mainPos(state, pos);
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof JoinersBenchBlockEntity bench)) {
            return InteractionResult.SUCCESS;
        }
        if (bench.getWorkpiece().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.epochsmith.bench.empty"), true);
        } else if (player.isShiftKeyDown()) {
            ItemStack taken = bench.takeAll();
            if (!player.getInventory().add(taken)) {
                player.drop(taken, false);
            }
        } else if (!EraManager.hasReached(player, Era.MECHANICAL)) {
            player.displayClientMessage(Component.translatable("message.epochsmith.bench.unknown"), true);
        } else {
            bench.effects();
            bench.work(JoinersBenchBlockEntity.WORK_PER_CLICK);
            player.causeFoodExhaustion(0.05F);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof JoinersBenchBlockEntity bench) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), bench.takeAll());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
