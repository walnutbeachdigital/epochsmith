package com.walnutbeachdigital.epochsmith.future;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import com.walnutbeachdigital.epochsmith.menu.MachineLayout;
import com.walnutbeachdigital.epochsmith.menu.MachineItems;
import com.walnutbeachdigital.epochsmith.menu.MachineGui;
import com.walnutbeachdigital.epochsmith.block.StatusDescriber;
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
public class MatterPrinterBlockEntity extends BlockEntity implements WorkpieceHolder, StatusDescriber, MachineGui {

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

    /** Anything put here is broken down into matter. */
    private final ItemStackHandler feed = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return level != null && MatterValues.valueOf(level, stack) > 0;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** The item to copy: put one here to scan it (it isn't used up). */
    private final ItemStackHandler pattern = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private ItemStack lastPattern = ItemStack.EMPTY;
    private int lastDrawn = 0;

    public static final MachineLayout LAYOUT = new MachineLayout(5)
            .pattern(26, 17).input(26, 53)
            .output(98, 17).output(116, 17).output(134, 17)
            .output(98, 35).output(116, 35).output(134, 35)
            .output(98, 53).output(116, 53).output(134, 53)
            .arrow(56, 35, 2, 3)
            .gauge(8, 17, 0, 1, MachineLayout.MATTER, "gui.epochsmith.matter")
            .gaugeOf(160, 17, 4, ENERGY_PER_TICK, MachineLayout.ENERGY, "gui.epochsmith.power");

    /** What the GUI shows: the pattern slot, the matter feed slot, then nine output slots. */
    private final IItemHandlerModifiable menuItems = new CombinedInvWrapper(pattern, feed, outputs);

    @Override
    public IItemHandlerModifiable getMenuItems() {
        return menuItems;
    }

    @Override
    public MachineLayout getLayout() {
        return LAYOUT;
    }

    @Override
    public int getMenuValue(int index) {
        int cost = target.isEmpty() || level == null ? 0 : Math.max(0, MatterValues.valueOf(level, target));
        return switch (index) {
            case 0 -> matter;
            case 1 -> MAX_MATTER;
            case 2 -> progress;
            case 3 -> Math.max(1, cost * ENERGY_PER_MATTER);
            case 4 -> lastDrawn;
            default -> 0;
        };
    }

    /** Breaks down whatever is in the feed slot, and follows the pattern slot. */
    private void handleSlots() {
        ItemStack food = feed.getStackInSlot(0);
        if (!food.isEmpty()) {
            int taken = recycle(food, false);
            if (taken > 0) {
                feed.extractItem(0, taken, false);
            }
        }
        ItemStack wanted = pattern.getStackInSlot(0);
        if (!ItemStack.isSameItemSameComponents(wanted, lastPattern)) {
            lastPattern = wanted.copy();
            if (wanted.isEmpty()) {
                clearTarget();
            } else {
                scan(wanted);
            }
        }
    }

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
        printer.handleSlots();
        printer.lastDrawn = 0;
        boolean working = false;
        int cost = printer.target.isEmpty() ? MatterValues.UNPRINTABLE : MatterValues.valueOf(level, printer.target);
        if (cost > 0 && printer.matter >= cost && printer.fits(printer.target)) {
            int needed = cost * ENERGY_PER_MATTER - printer.progress;
            int drawn = ElectricNetwork.draw(level, printer.sources, Math.min(ENERGY_PER_TICK, needed), false);
            printer.lastDrawn = drawn;
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
        tag.put("Feed", feed.serializeNBT(registries));
        tag.put("Pattern", pattern.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        target = ItemStack.parseOptional(registries, tag.getCompound("Target"));
        matter = tag.getInt("Matter");
        progress = tag.getInt("Progress");
        outputs.deserializeNBT(registries, tag.getCompound("Outputs"));
        if (tag.contains("Pattern")) {
            feed.deserializeNBT(registries, tag.getCompound("Feed"));
            pattern.deserializeNBT(registries, tag.getCompound("Pattern"));
            lastPattern = pattern.getStackInSlot(0).copy();
        }
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
