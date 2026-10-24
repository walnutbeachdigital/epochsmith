package com.walnutbeachdigital.epochsmith.future;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.block.ModBlocks;
import com.walnutbeachdigital.epochsmith.item.ModDataComponents;
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
 * The Future era's storage. Connect copper wire from a generator or battery for the terminal.
 *  - Right-click with an item to store the stack; sneak + right-click stores every one you carry.
 *  - Sneak + right-click while holding other quantum cells to link them to this one.
 *    Linked cells share one inventory, anywhere in the world and in any dimension.
 *  - Empty hand opens the terminal.
 *  - Breaking a cell loses nothing: the items stay in the network and the cell keeps its link.
 */
public class QuantumCellBlock extends BaseEntityBlock implements ElectricConnectable {

    public static final MapCodec<QuantumCellBlock> CODEC = simpleCodec(QuantumCellBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public QuantumCellBlock(Properties properties) {
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
        return new QuantumCellBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.QUANTUM_CELL.get(), QuantumCellBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // Holding a quantum cell without sneaking just places it, like any block
        if (stack.is(ModBlocks.QUANTUM_CELL_ITEM.get()) && !player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof QuantumCellBlockEntity vault) {
            if (!EraManager.hasReached(player, Era.FUTURE)) {
                player.displayClientMessage(Component.translatable("message.epochsmith.quantum_cell.unknown"), true);
                return ItemInteractionResult.SUCCESS;
            }
            if (stack.is(ModBlocks.QUANTUM_CELL_ITEM.get())) {
                if (player.isShiftKeyDown()) {
                    stack.set(ModDataComponents.QUANTUM_LINK.get(), vault.getLink());
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.2F);
                    player.displayClientMessage(Component.translatable("message.epochsmith.quantum_cell.linked",
                            stack.getCount(), vault.shortLink()), true);
                    return ItemInteractionResult.SUCCESS;
                }
            }
            int stored = 0;
            if (player.isShiftKeyDown()) {
                // Store every matching stack in the player's inventory
                ItemStack kind = stack.copy();
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack slot = player.getInventory().getItem(i);
                    if (ItemStack.isSameItemSameComponents(slot, kind)) {
                        int moved = vault.getStorage().store(slot, false);
                        slot.shrink(moved);
                        stored += moved;
                    }
                }
            } else {
                stored = vault.getStorage().store(stack, false);
                stack.shrink(stored);
            }
            if (stored > 0) {
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.5F, 1.6F);
                player.displayClientMessage(vault.describe(), true);
            } else {
                player.displayClientMessage(Component.translatable("message.epochsmith.quantum_cell.full"), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof QuantumCellBlockEntity vault) {
            if (!EraManager.hasReached(player, Era.FUTURE)) {
                player.displayClientMessage(Component.translatable("message.epochsmith.quantum_cell.unknown"), true);
            } else if (!vault.isPowered()) {
                player.displayClientMessage(Component.translatable("message.epochsmith.quantum_cell.unpowered"), true);
            } else {
                VaultNetwork.sendContents(serverPlayer, vault, true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
