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
import net.minecraft.world.item.Items;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Primitive era's smelter.
 *  1. Right-click with clay balls or logs to load it (up to 16).
 *  2. Right-click with fuel to light it: 1 coal or charcoal, or 4 sticks.
 *  3. After 20 seconds the whole batch is fired: clay becomes bricks, logs become charcoal.
 *  4. Right-click with an empty hand to take the results out.
 */
public class PitKilnBlock extends BaseEntityBlock {

    public static final MapCodec<PitKilnBlock> CODEC = simpleCodec(PitKilnBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    private static final int STICKS_PER_FIRING = 4;

    public PitKilnBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Block entities are invisible by default; this makes the kiln use its normal model. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PitKilnBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.PIT_KILN.get(), PitKilnBlockEntity::serverTick);
    }

    // ---- Using the kiln ----

    private static boolean isFuel(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL)
                || (stack.is(Items.STICK) && stack.getCount() >= STICKS_PER_FIRING);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        boolean relevant = isFuel(stack) || stack.is(Items.STICK) || PitKilnBlockEntity.canFire(stack);
        if (!relevant) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof PitKilnBlockEntity kiln)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!EraManager.hasReached(player, Era.PRIMITIVE)) {
            player.displayClientMessage(Component.translatable("message.epochsmith.kiln.unknown"), true);
        } else if (state.getValue(LIT)) {
            player.displayClientMessage(Component.translatable("message.epochsmith.kiln.burning"), true);
        } else if (stack.is(Items.STICK) && !isFuel(stack)) {
            player.displayClientMessage(Component.translatable("message.epochsmith.kiln.more_sticks", STICKS_PER_FIRING), true);
        } else if (isFuel(stack)) {
            if (!kiln.hasFirableContents()) {
                player.displayClientMessage(Component.translatable("message.epochsmith.kiln.empty"), true);
            } else {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(stack.is(Items.STICK) ? STICKS_PER_FIRING : 1);
                }
                kiln.ignite(player);
                level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.7F, 1.0F);
                player.displayClientMessage(Component.translatable("message.epochsmith.kiln.lit"), true);
            }
        } else {
            // A firable item: clay or logs
            if (kiln.insert(stack)) {
                level.playSound(null, pos, SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 0.6F, 1.2F);
            } else {
                player.displayClientMessage(Component.translatable("message.epochsmith.kiln.full"), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Empty hand: take everything out (only while it isn't burning). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(LIT)) {
            player.displayClientMessage(Component.translatable("message.epochsmith.kiln.burning"), true);
            return InteractionResult.CONSUME;
        }
        if (level.getBlockEntity(pos) instanceof PitKilnBlockEntity kiln && !kiln.getContents().isEmpty()) {
            ItemStack taken = kiln.takeAll();
            if (!player.getInventory().add(taken)) {
                player.drop(taken, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 1.0F);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /** Breaking the kiln drops whatever is inside. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PitKilnBlockEntity kiln) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), kiln.takeAll());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Smoke and flames while lit. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
        level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 0.6, z, 0.0, 0.05, 0.0);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FLAME, x, pos.getY() + 0.55, z, 0.0, 0.01, 0.0);
        }
        if (random.nextInt(20) == 0) {
            level.playLocalSound(pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 1.0F, false);
        }
    }
}
