package com.walnutbeachdigital.epochsmith.future;

import javax.annotation.Nullable;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.electric.ElectricConnectable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import com.walnutbeachdigital.epochsmith.era.Era;
import com.walnutbeachdigital.epochsmith.era.EraManager;
import com.walnutbeachdigital.epochsmith.item.ModItems;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The fusion reactor. Connect copper wire to run power in (for ignition) and out (to machines).
 *  - Right-click with plasma cells to fuel it.
 *  - Empty hand shows status; sneak + empty hand takes unburned fuel back out.
 */
public class FusionReactorBlock extends BaseEntityBlock implements ElectricConnectable {

    public static final MapCodec<FusionReactorBlock> CODEC = simpleCodec(FusionReactorBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public FusionReactorBlock(Properties properties) {
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
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionReactorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.FUSION_REACTOR.get(), FusionReactorBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof FusionReactorBlockEntity core) {
            core.setOwner(player);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.PLASMA_CELL.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FusionReactorBlockEntity reactor) {
            if (!EraManager.hasReached(player, Era.FUTURE)) {
                player.displayClientMessage(Component.translatable("message.epochsmith.fusion.unknown"), true);
            } else if (reactor.addFuel(stack) > 0) {
                level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.7F, 1.2F);
                player.displayClientMessage(reactor.describe(), true);
            } else {
                player.displayClientMessage(Component.translatable("message.epochsmith.fusion.full"), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FusionReactorBlockEntity reactor) {
            if (player.isShiftKeyDown() && !reactor.isLit()) {
                ItemStack fuel = reactor.takeFuel();
                if (!fuel.isEmpty() && !player.getInventory().add(fuel)) {
                    player.drop(fuel, false);
                }
            }
            player.displayClientMessage(reactor.describe(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FusionReactorBlockEntity reactor) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), reactor.takeFuel());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Glowing plasma particles while it burns. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + 1.05,
                    pos.getZ() + random.nextDouble(), 0.0, 0.03, 0.0);
        }
    }
}
