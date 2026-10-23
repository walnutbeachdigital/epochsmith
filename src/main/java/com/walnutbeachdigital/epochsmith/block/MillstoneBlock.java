package com.walnutbeachdigital.epochsmith.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.epochsmith.era.Era;
import com.walnutbeachdigital.epochsmith.era.EraManager;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The first machine that runs on rotation. Connect an axle (or a source) to any side.
 *  - Right-click with cobblestone, gravel, bones or sugar cane to load it (up to 16).
 *  - Ground items pop out on top: cobblestone -> gravel, gravel -> sand,
 *    bone -> 4 bone meal, sugar cane -> 2 sugar.
 *  - Empty hand: take back what hasn't been ground (or check its status).
 */
public class MillstoneBlock extends BaseEntityBlock {

    public static final MapCodec<MillstoneBlock> CODEC = simpleCodec(MillstoneBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public MillstoneBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MillstoneBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.MILLSTONE.get(), MillstoneBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!MillstoneBlockEntity.canGrind(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MillstoneBlockEntity mill) {
            if (!EraManager.hasReached(player, Era.MECHANICAL)) {
                player.displayClientMessage(Component.translatable("message.epochsmith.millstone.unknown"), true);
            } else if (mill.insert(stack)) {
                level.playSound(null, pos, SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 0.6F, 1.0F);
                player.displayClientMessage(mill.getForce() >= MillstoneBlockEntity.MIN_FORCE
                        ? mill.describe()
                        : Component.translatable("message.epochsmith.millstone.no_power"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.epochsmith.millstone.busy"), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MillstoneBlockEntity mill) {
            if (player.isShiftKeyDown() || mill.getInput().isEmpty()) {
                player.displayClientMessage(mill.getForce() >= MillstoneBlockEntity.MIN_FORCE
                        ? mill.describe()
                        : Component.translatable("message.epochsmith.millstone.no_power"), true);
            } else {
                ItemStack taken = mill.takeAll();
                if (!player.getInventory().add(taken)) {
                    player.drop(taken, false);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MillstoneBlockEntity mill) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), mill.takeAll());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Stone dust while grinding. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE) && random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.WHITE_ASH,
                    pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.9,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.02, 0.0);
        }
    }
}
