package com.walnutbeachdigital.epochsmith.future;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.block.WorkpieceHolder;
import com.walnutbeachdigital.epochsmith.electric.ElectricNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Prints items from raw matter. Feed it anything with a matter value (cobblestone, logs, junk)
 * and it breaks it down into matter. Scan an item (sneak + right-click with it) and it prints copies,
 * using that item's matter value plus 64 energy per point of matter.
 */
public class MatterPrinterBlockEntity extends BlockEntity implements WorkpieceHolder {

    public static final int SLOTS = 9;
    public static final int MAX_MATTER = 1000000;
    public static final int ENERGY_PER_MATTER = 64;
    public static final int ENERGY_PER_TICK = 4096;
    private static final int REFRESH_INTERVAL = 20;

    private ItemStack target = ItemStack.EMPTY;
    private int matter = 0;
    private int progress = 0;
    private List<BlockPos> sources = List.of();

    private final ItemStackHandler outputs = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** Hoppers and pipes: anything put in becomes matter; printed items come out. */
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return SLOTS + 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot < SLOTS ? outputs.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != SLOTS) {
                return stack;
            }
            int accepted = recycle(stack, simulate);
            return accepted == 0 ? stack : stack.copyWithCount(stack.getCount() - accepted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot < SLOTS ? outputs.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOTS && level != null && MatterValues.valueOf(level, stack) > 0;
        }
    };

    public MatterPrinterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATTER_PRINTER.get(), pos, state);
    }

    public IItemHandler getAutomation() {
        return automation;
    }

    @Override
    public ItemStack getWorkpiece() {
        return target;
    }

    /** Breaks items down into matter. Returns how many it took (the stack itself isn't changed). */
    public int recycle(ItemStack stack, boolean simulate) {
        if (level == null || stack.isEmpty()) {
            return 0;
        }
        int value = MatterValues.valueOf(level, stack);
        if (value <= 0) {
            return 0;
        }
        int fits = Math.min(stack.getCount(), (MAX_MATTER - matter) / value);
        if (fits > 0 && !simulate) {
            matter += fits * value;
            setChanged();
        }
        return Math.max(fits, 0);
    }

    /** Scans an item so the printer makes copies of it. Returns a message for the player. */
    public Component scan(ItemStack stack) {
        if (level == null) {
            return Component.empty();
        }
        int value = MatterValues.valueOf(level, stack);
        if (value <= 0) {
            return Component.translatable("message.epochsmith.matter_printer.unprintable", stack.getHoverName());
        }
        target = stack.copyWithCount(1);
        progress = 0;
        changed();
        return Component.translatable("message.epochsmith.matter_printer.scanned", target.getHoverName(), value,
                value * ENERGY_PER_MATTER);
    }

    public void clearTarget() {
        target = ItemStack.EMPTY;
        progress = 0;
        changed();
    }

    public List<ItemStack> takeOutputs() {
        List<ItemStack> taken = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = outputs.getStackInSlot(i);
            if (!stack.isEmpty()) {
                taken.add(stack.copy());
                outputs.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        return taken;
    }

    public Component describe() {
        if (target.isEmpty() || level == null) {
            return Component.translatable("message.epochsmith.matter_printer.idle", matter);
        }
        int cost = MatterValues.valueOf(level, target);
        String state = getBlockState().getValue(BlockStateProperties.LIT) ? "message.epochsmith.matter_printer.working"
                : matter < cost ? "message.epochsmith.matter_printer.no_matter"
                : "message.epochsmith.matter_printer.unpowered";
        return Component.translatable("message.epochsmith.matter_printer.status", target.getHoverName(), cost, matter,
                Component.translatable(state));
    }

    private boolean fits(ItemStack result) {
        ItemStack rest = result.copy();
        for (int i = 0; i < SLOTS && !rest.isEmpty(); i++) {
            rest = outputs.insertItem(i, rest, true);
        }
        return rest.isEmpty();
    }

    private void store(ItemStack result) {
        ItemStack rest = result.copy();
        for (int i = 0; i < SLOTS && !rest.isEmpty(); i++) {
            rest = outputs.insertItem(i, rest, false);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MatterPrinterBlockEntity printer) {
        if (level.getGameTime() % REFRESH_INTERVAL == 0) {
            printer.sources = ElectricNetwork.findSources(level, pos);
        }
        boolean working = false;
        int cost = printer.target.isEmpty() ? MatterValues.UNPRINTABLE : MatterValues.valueOf(level, printer.target);
        if (cost > 0 && printer.matter >= cost && printer.fits(printer.target)) {
            int needed = cost * ENERGY_PER_MATTER - printer.progress;
            int drawn = ElectricNetwork.draw(level, printer.sources, Math.min(ENERGY_PER_TICK, needed), false);
            working = drawn > 0;
            printer.progress += drawn;
            if (printer.progress >= cost * ENERGY_PER_MATTER) {
                printer.progress = 0;
                printer.matter -= cost;
                printer.store(printer.target);
                level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.BLOCKS, 0.4F, 1.6F);
            }
            printer.setChanged();
        }
        if (state.getValue(BlockStateProperties.LIT) != working) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, working), Block.UPDATE_ALL);
        }
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Target", target.saveOptional(registries));
        tag.putInt("Matter", matter);
        tag.putInt("Progress", progress);
        tag.put("Outputs", outputs.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        target = ItemStack.parseOptional(registries, tag.getCompound("Target"));
        matter = tag.getInt("Matter");
        progress = tag.getInt("Progress");
        outputs.deserializeNBT(registries, tag.getCompound("Outputs"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Target", target.saveOptional(registries));
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        target = ItemStack.parseOptional(registries, tag.getCompound("Target"));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
