package com.walnutbeachdigital.epochsmith.menu;

import com.walnutbeachdigital.epochsmith.era.Era;
import com.walnutbeachdigital.epochsmith.era.EraManager;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Opening machine GUIs from blocks. */
public final class MachineMenus {

    /**
     * Opens the machine's GUI, if the player has reached the era that machine belongs to.
     * @param unknownKey the message shown to players who haven't reached that era yet
     */
    public static InteractionResult open(BlockState state, Level level, BlockPos pos, Player player, Era era, String unknownKey) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (era != null && !EraManager.hasReached(player, era)) {
            player.displayClientMessage(Component.translatable(unknownKey), true);
            return InteractionResult.CONSUME;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MachineGui gui) {
            gui.onMenuOpened(player);
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new MachineMenu(id, inventory, be),
                    state.getBlock().getName()), buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    /** Drops everything in a machine's slots (when it's broken). */
    public static void dropAll(Level level, BlockPos pos, IItemHandlerModifiable items) {
        for (int i = 0; i < items.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.getStackInSlot(i).copy());
            items.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    private MachineMenus() {
    }
}
