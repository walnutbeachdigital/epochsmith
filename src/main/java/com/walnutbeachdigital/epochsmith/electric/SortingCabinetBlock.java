package com.walnutbeachdigital.epochsmith.electric;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Electrical era's storage: nine labeled drawers that file items for you.
 *  - Right-click with an item: it's filed into the right drawer. Double-click: files every
 *    matching item from your inventory.
 *  - Right-click a drawer's front with an empty hand: take a stack. Sneak: take one.
 *  - Hoppers and pipes can feed it and pull from it.
 */
public class SortingCabinetBlock extends BaseEntityBlock {

    public static final MapCodec<SortingCabinetBlock> CODEC = simpleCodec(SortingCabinetBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final int DOUBLE_CLICK_TICKS = 10;

    /** When each player last filed something, to spot double-clicks. */
    private static final Map<UUID, Long> LAST_FILE = new HashMap<>();

    public SortingCabinetBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SortingCabinetBlockEntity(pos, state);
    }

    /**
     * Which drawer (0-8, left to right, top to bottom) a click on the front face landed on,
     * or -1 if the click wasn't on the front.
     */
    public static int drawerAt(BlockState state, BlockPos pos, BlockHitResult hit) {
        Direction front = state.getValue(FACING);
        if (hit.getDirection() != front) {
            return -1;
        }
        Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        double across = switch (front) {
            case SOUTH -> local.x;
            case NORTH -> 1.0 - local.x;
            case WEST -> local.z;
            default -> 1.0 - local.z;
        };
        int col = Math.min(2, Math.max(0, (int) (across * 3)));
        int row = Math.min(2, Math.max(0, (int) ((1.0 - local.y) * 3)));
        return row * 3 + col;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof SortingCabinetBlockEntity cabinet)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!EraManager.hasReached(player, Era.ELECTRICAL)) {
            player.displayClientMessage(Component.translatable("message.epochsmith.cabinet.unknown"), true);
            return ItemInteractionResult.CONSUME;
        }
        ItemStack template = stack.copyWithCount(1);
        ItemStack left = cabinet.file(stack, false);
        player.setItemInHand(hand, left);

        // A second click soon after files every matching item from the whole inventory
        long now = level.getGameTime();
        Long last = LAST_FILE.put(player.getUUID(), now);
        if (last != null && now - last <= DOUBLE_CLICK_TICKS) {
            Inventory inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack other = inventory.getItem(i);
                if (!other.isEmpty() && ItemStack.isSameItemSameComponents(other, template)) {
                    inventory.setItem(i, cabinet.file(other, false));
                }
            }
        }
        level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_INSERT, SoundSource.BLOCKS, 0.8F, 1.0F);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof SortingCabinetBlockEntity cabinet)) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        int drawer = drawerAt(state, pos, hit);
        if (drawer < 0) {
            return InteractionResult.PASS;
        }
        ItemStack taken = cabinet.take(drawer, player.isShiftKeyDown() ? 1 : 64, false);
        if (!taken.isEmpty()) {
            if (!player.getInventory().add(taken)) {
                player.drop(taken, false);
            }
            level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return InteractionResult.CONSUME;
    }

    /** Breaking it spills every drawer's contents. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SortingCabinetBlockEntity cabinet) {
            for (int drawer = 0; drawer < SortingCabinetBlockEntity.DRAWERS; drawer++) {
                ItemStack stack;
                while (!(stack = cabinet.take(drawer, 64, false)).isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
