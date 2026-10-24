package com.walnutbeachdigital.epochsmith.information;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Items stored as data: a list of kinds and how many of each. Used by the data vault
 * and by quantum storage cells.
 */
public class VaultStorage {

    /** One kind of item and how many of it are stored. */
    public static final class Entry {
        public final ItemStack kind;
        public int count;

        Entry(ItemStack kind, int count) {
            this.kind = kind.copyWithCount(1);
            this.count = count;
        }
    }

    private final int maxTypes;
    private final int capacity;
    private final Runnable onChange;
    private final List<Entry> entries = new ArrayList<>();
    private int total = 0;

    /** Hoppers see one slot per stored kind (up to a stack each), plus an empty slot to insert into. */
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return entries.size() + 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot >= entries.size()) {
                return ItemStack.EMPTY;
            }
            Entry entry = entries.get(slot);
            return entry.kind.copyWithCount(Math.min(entry.count, entry.kind.getMaxStackSize()));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            int accepted = store(stack, simulate);
            return accepted == 0 ? stack : stack.copyWithCount(stack.getCount() - accepted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot >= entries.size()) {
                return ItemStack.EMPTY;
            }
            Entry entry = entries.get(slot);
            int taken = Math.min(amount, Math.min(entry.count, entry.kind.getMaxStackSize()));
            ItemStack result = entry.kind.copyWithCount(taken);
            if (!simulate) {
                remove(entry.kind, taken);
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    };

    public VaultStorage(int maxTypes, int capacity, Runnable onChange) {
        this.maxTypes = maxTypes;
        this.capacity = capacity;
        this.onChange = onChange;
    }

    public IItemHandler getAutomation() {
        return automation;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public int getTotal() {
        return total;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getMaxTypes() {
        return maxTypes;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    private Entry find(ItemStack kind) {
        for (Entry entry : entries) {
            if (ItemStack.isSameItemSameComponents(entry.kind, kind)) {
                return entry;
            }
        }
        return null;
    }

    /** Stores as much of the stack as fits. Returns how many it took (the stack itself isn't changed). */
    public int store(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return 0;
        }
        Entry entry = find(stack);
        if (entry == null && entries.size() >= maxTypes) {
            return 0;
        }
        int accepted = Math.min(stack.getCount(), capacity - total);
        if (accepted <= 0 || simulate) {
            return Math.max(accepted, 0);
        }
        if (entry == null) {
            entries.add(new Entry(stack, accepted));
        } else {
            entry.count += accepted;
        }
        total += accepted;
        onChange.run();
        return accepted;
    }

    /** Takes up to this many of a kind out. Returns what was taken. */
    public ItemStack remove(ItemStack kind, int amount) {
        Entry entry = find(kind);
        if (entry == null || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int taken = Math.min(amount, entry.count);
        entry.count -= taken;
        total -= taken;
        if (entry.count <= 0) {
            entries.remove(entry);
        }
        onChange.run();
        return entry.kind.copyWithCount(taken);
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry entry : entries) {
            CompoundTag item = new CompoundTag();
            item.put("Kind", entry.kind.save(registries));
            item.putInt("Count", entry.count);
            list.add(item);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("Entries", list);
        return tag;
    }

    public void load(HolderLookup.Provider registries, CompoundTag tag) {
        entries.clear();
        total = 0;
        ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            int count = item.getInt("Count");
            ItemStack.parse(registries, item.getCompound("Kind")).ifPresent(kind -> {
                if (count > 0) {
                    entries.add(new Entry(kind, count));
                    total += count;
                }
            });
        }
    }
}
