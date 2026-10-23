package com.walnutbeachdigital.epochsmith.information;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.electric.ElectricConnectable;
import com.walnutbeachdigital.epochsmith.era.Era;
import com.walnutbeachdigital.epochsmith.era.EraManager;
import com.walnutbeachdigital.epochsmith.network.VaultNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Information era's storage. Connect copper wire from a generator or battery.
 *  - Right-click with an item to store the stack; sneak + right-click stores every one you carry.
 *  - Empty hand opens the terminal: search everything and click to take items out.
 *  - Keeps everything inside when broken, like a shulker box.
 */
public class DataVaultBlock extends BaseEntityBlock implements ElectricConnectable {

    public static final MapCodec<DataVaultBlock> CODEC = simpleCodec(DataVaultBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public DataVaultBlock(Properties properties) {
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
        return new DataVaultBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.DATA_VAULT.get(), DataVaultBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DataVaultBlockEntity vault) {
            if (!EraManager.hasReached(player, Era.INFORMATION)) {
                player.displayClientMessage(Component.translatable("message.epochsmith.data_vault.unknown"), true);
                return ItemInteractionResult.SUCCESS;
            }
            int stored = 0;
            if (player.isShiftKeyDown()) {
                // Store every matching stack in the player's inventory
                ItemStack kind = stack.copy();
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack slot = player.getInventory().getItem(i);
                    if (ItemStack.isSameItemSameComponents(slot, kind)) {
                        int moved = vault.store(slot, false);
                        slot.shrink(moved);
                        stored += moved;
                    }
                }
            } else {
                stored = vault.store(stack, false);
                stack.shrink(stored);
            }
            if (stored > 0) {
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.5F, 1.6F);
                player.displayClientMessage(vault.describe(), true);
            } else {
                player.displayClientMessage(Component.translatable("message.epochsmith.data_vault.full"), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof DataVaultBlockEntity vault) {
            if (!EraManager.hasReached(player, Era.INFORMATION)) {
                player.displayClientMessage(Component.translatable("message.epochsmith.data_vault.unknown"), true);
            } else if (!vault.isPowered()) {
                player.displayClientMessage(Component.translatable("message.epochsmith.data_vault.unpowered"), true);
            } else {
                VaultNetwork.sendContents(serverPlayer, vault, true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
