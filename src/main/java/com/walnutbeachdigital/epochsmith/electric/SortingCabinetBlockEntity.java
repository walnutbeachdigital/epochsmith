package com.walnutbeachdigital.epochsmith.electric;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Nine drawers, each holding up to 1,024 of a single item type.
 * Incoming items are filed into the drawer that already holds them, or the first empty one.
 */
public class SortingCabinetBlockEntity extends BlockEntity {

    public static final int DRAWERS = 9;
    public static final int CAPACITY = 1024;

    /** Each drawer's item type (count 1), or EMPTY if the drawer is unused. */
    private final ItemStack[] types = new ItemStack[DRAWERS];
    private final int[] counts = new int[DRAWERS];
    private final IItemHandler handler = new Handler();

    public SortingCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SORTING_CABINET.get(), pos, state);
        for (int i = 0; i < DRAWERS; i++) {
            types[i] = ItemStack.EMPTY;
        }
    }

    public IItemHandler getHandler() {
        return handler;
    }

    /** The item a drawer holds (count 1), or EMPTY. Used to draw the drawer fronts. */
    public ItemStack getType(int drawer) {
        return types[drawer];
    }

    public int getCount(int drawer) {
        return counts[drawer];
    }

    // ---- Filing ----

    /** Which drawer this item belongs in: one already holding it, else the first empty one, else -1. */
    private int drawerFor(ItemStack stack) {
        int firstEmpty = -1;
        for (int i = 0; i < DRAWERS; i++) {
            if (types[i].isEmpty()) {
                if (firstEmpty < 0) {
                    firstEmpty = i;
                }
            } else if (ItemStack.isSameItemSameComponents(types[i], stack)) {
                return i;
            }
        }
        return firstEmpty;
    }

    /** Files as much of the stack as fits. Returns what's left over. */
    public ItemStack file(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int drawer = drawerFor(stack);
        if (drawer < 0) {
            return stack;
        }
        int moved = Math.min(stack.getCount(), CAPACITY - counts[drawer]);
        if (moved <= 0) {
            return stack;
        }
        if (!simulate) {
            if (types[drawer].isEmpty()) {
                types[drawer] = stack.copyWithCount(1);
            }
            counts[drawer] += moved;
            changed();
        }
        return stack.copyWithCount(stack.getCount() - moved);
    }

    /** Takes up to 'amount' (never more than one stack) out of a drawer. */
    public ItemStack take(int drawer, int amount, boolean simulate) {
        if (drawer < 0 || drawer >= DRAWERS || types[drawer].isEmpty()) {
            return ItemStack.EMPTY;
        }
        int taken = Math.min(Math.min(amount, counts[drawer]), types[drawer].getMaxStackSize());
        ItemStack result = types[drawer].copyWithCount(taken);
        if (!simulate && taken > 0) {
            counts[drawer] -= taken;
            if (counts[drawer] == 0) {
                types[drawer] = ItemStack.EMPTY;
            }
            changed();
        }
        return result;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Hoppers and other mods' pipes see the cabinet through this ----

    private class Handler implements IItemHandler {
        @Override
        public int getSlots() {
            return DRAWERS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return types[slot].isEmpty() ? ItemStack.EMPTY
                    : types[slot].copyWithCount(Math.min(counts[slot], types[slot].getMaxStackSize()));
        }

        /** Ignores which slot was asked for and files the item where it belongs. */
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return file(stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return take(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return CAPACITY;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    }

    // ---- Saving and syncing ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (int i = 0; i < DRAWERS; i++) {
            CompoundTag drawer = new CompoundTag();
            drawer.put("Item", types[i].saveOptional(registries));
            drawer.putInt("Count", counts[i]);
            list.add(drawer);
        }
        tag.put("Drawers", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ListTag list = tag.getList("Drawers", Tag.TAG_COMPOUND);
        for (int i = 0; i < DRAWERS; i++) {
            CompoundTag drawer = i < list.size() ? list.getCompound(i) : new CompoundTag();
            types[i] = ItemStack.parseOptional(registries, drawer.getCompound("Item"));
            counts[i] = types[i].isEmpty() ? 0 : drawer.getInt("Count");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
