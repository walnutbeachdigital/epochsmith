package com.walnutbeachdigital.epochsmith.information;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.electric.ElectricNetwork;
import com.walnutbeachdigital.epochsmith.item.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Stores items as data: up to 256 different kinds and 65,536 items in total, in one block.
 * The terminal (right-click with an empty hand) needs 16 energy per tick to run.
 * Hoppers and pipes can put items in and take them out even without power.
 */
public class DataVaultBlockEntity extends BlockEntity {

    public static final int MAX_TYPES = 256;
    public static final int CAPACITY = 65536;
    public static final int ENERGY_PER_TICK = 16;
    private static final int REFRESH_INTERVAL = 20;

    /** One kind of item and how many of it are stored. */
    public static final class Entry {
        public final ItemStack kind;
        public int count;

        Entry(ItemStack kind, int count) {
            this.kind = kind.copyWithCount(1);
            this.count = count;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private int total = 0;
    private List<BlockPos> sources = List.of();

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

    public DataVaultBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DATA_VAULT.get(), pos, state);
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

    public boolean isPowered() {
        return getBlockState().getValue(BlockStateProperties.LIT);
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
        if (entry == null && entries.size() >= MAX_TYPES) {
            return 0;
        }
        int accepted = Math.min(stack.getCount(), CAPACITY - total);
        if (accepted <= 0 || simulate) {
            return Math.max(accepted, 0);
        }
        if (entry == null) {
            entries.add(new Entry(stack, accepted));
        } else {
            entry.count += accepted;
        }
        total += accepted;
        setChanged();
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
        setChanged();
        return entry.kind.copyWithCount(taken);
    }

    public Component describe() {
        return Component.translatable("message.epochsmith.data_vault.status", total, CAPACITY, entries.size(), MAX_TYPES);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DataVaultBlockEntity vault) {
        if (level.getGameTime() % REFRESH_INTERVAL == 0) {
            vault.sources = ElectricNetwork.findSources(level, pos);
        }
        boolean powered = ElectricNetwork.draw(level, vault.sources, ENERGY_PER_TICK, false) >= ENERGY_PER_TICK;
        if (state.getValue(BlockStateProperties.LIT) != powered) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, powered), Block.UPDATE_ALL);
        }
    }

    // ---- Saving: in the world, and on the item when the vault is broken

    private CompoundTag saveEntries(HolderLookup.Provider registries) {
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

    private void loadEntries(HolderLookup.Provider registries, CompoundTag tag) {
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Vault", saveEntries(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadEntries(registries, tag.getCompound("Vault"));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!entries.isEmpty() && level != null) {
            components.set(ModDataComponents.VAULT_CONTENTS.get(), CustomData.of(saveEntries(level.registryAccess())));
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        CustomData data = input.get(ModDataComponents.VAULT_CONTENTS.get());
        if (data != null && level != null) {
            loadEntries(level.registryAccess(), data.copyTag());
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Vault");
    }
}
