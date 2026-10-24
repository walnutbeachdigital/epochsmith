package com.walnutbeachdigital.epochsmith.information;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import com.walnutbeachdigital.epochsmith.menu.MachineLayout;
import com.walnutbeachdigital.epochsmith.menu.MachineItems;
import com.walnutbeachdigital.epochsmith.menu.MachineGui;
import com.walnutbeachdigital.epochsmith.block.StatusDescriber;
import java.util.List;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.electric.ArcFurnaceBlockEntity;
import com.walnutbeachdigital.epochsmith.electric.ElectricNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
 * The Information era's smelter. Four input slots and four output slots, so it smelts
 * several different things at once. It works through the inputs in turn, one item per
 * 160 energy, at up to 256 energy per tick (about 30 items a second on full power).
 * Smelts everything the arc furnace does, ore doubling included.
 *
 * Fully automatable: hoppers and pipes put items in from any side and pull results out.
 */
public class InductionFurnaceBlockEntity extends BlockEntity implements StatusDescriber, MachineGui {

    public static final int SLOTS = 4;
    public static final int ENERGY_PER_ITEM = 160;
    public static final int ENERGY_PER_TICK = 256;
    private static final int REFRESH_INTERVAL = 20;

    private final ItemStackHandler inputs = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return level != null && !ArcFurnaceBlockEntity.resultFor(level, stack).isEmpty();
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler outputs = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** What hoppers and pipes see: insert into inputs, extract from outputs. */
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return SLOTS * 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot < SLOTS ? inputs.getStackInSlot(slot) : outputs.getStackInSlot(slot - SLOTS);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot < SLOTS ? inputs.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot < SLOTS ? ItemStack.EMPTY : outputs.extractItem(slot - SLOTS, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < SLOTS && inputs.isItemValid(slot, stack);
        }
    };

    public static final MachineLayout LAYOUT = new MachineLayout(3).theme("information")
            .input(38, 26).input(56, 26).input(38, 44).input(56, 44)
            .output(110, 26).output(128, 26).output(110, 44).output(128, 44)
            .arrow(79, 35, 0, 1)
            .gaugeOf(156, 17, 2, ENERGY_PER_TICK, MachineLayout.ENERGY, "gui.epochsmith.power");

    /** What the GUI shows: the four inputs, then the four outputs. */
    private final IItemHandlerModifiable menuItems = new CombinedInvWrapper(inputs, outputs);
    private int lastDrawn = 0;
    private int progress = 0;
    private int nextSlot = 0;
    private List<BlockPos> sources = List.of();

    public InductionFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUCTION_FURNACE.get(), pos, state);
    }

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
        return switch (index) {
            case 0 -> progress;
            case 1 -> ENERGY_PER_ITEM;
            case 2 -> lastDrawn;
            default -> 0;
        };
    }

    public IItemHandler getAutomation() {
        return automation;
    }

    /** Puts as much of the held stack in as fits. Returns true if anything went in. */
    public boolean insert(ItemStack stack) {
        int before = stack.getCount();
        ItemStack rest = stack.copy();
        for (int i = 0; i < SLOTS && !rest.isEmpty(); i++) {
            rest = inputs.insertItem(i, rest, false);
        }
        stack.setCount(rest.getCount());
        return stack.getCount() < before;
    }

    /** Everything that's been smelted, for a player collecting by hand. */
    public List<ItemStack> takeOutputs() {
        return takeAll(outputs);
    }

    public List<ItemStack> takeInputs() {
        progress = 0;
        return takeAll(inputs);
    }

    private static List<ItemStack> takeAll(ItemStackHandler handler) {
        List<ItemStack> taken = new java.util.ArrayList<>();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                taken.add(stack.copy());
                handler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        return taken;
    }

    private static int count(ItemStackHandler handler) {
        int total = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            total += handler.getStackInSlot(i).getCount();
        }
        return total;
    }

    public Component describe() {
        boolean powered = getBlockState().getValue(BlockStateProperties.LIT) || count(inputs) == 0;
        return Component.translatable("message.epochsmith.induction_furnace.status", count(inputs), count(outputs),
                Component.translatable(powered ? "message.epochsmith.induction_furnace.ok"
                        : "message.epochsmith.induction_furnace.unpowered"));
    }

    /** Finds the next input slot whose result still fits in the outputs, starting where we left off. */
    private int findWork(Level level) {
        for (int i = 0; i < SLOTS; i++) {
            int slot = (nextSlot + i) % SLOTS;
            ItemStack input = inputs.getStackInSlot(slot);
            if (!input.isEmpty() && fits(ArcFurnaceBlockEntity.resultFor(level, input))) {
                return slot;
            }
        }
        return -1;
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, InductionFurnaceBlockEntity furnace) {
        if (level.getGameTime() % REFRESH_INTERVAL == 0) {
            furnace.sources = ElectricNetwork.findSources(level, pos);
        }
        boolean working = false;
        furnace.lastDrawn = 0;
        if (furnace.findWork(level) >= 0) {
            int drawn = ElectricNetwork.draw(level, furnace.sources, ENERGY_PER_TICK, false);
            furnace.lastDrawn = drawn;
            working = drawn > 0;
            furnace.progress += drawn;
            int slot;
            while (furnace.progress >= ENERGY_PER_ITEM && (slot = furnace.findWork(level)) >= 0) {
                furnace.progress -= ENERGY_PER_ITEM;
                ItemStack input = furnace.inputs.getStackInSlot(slot);
                furnace.store(ArcFurnaceBlockEntity.resultFor(level, input));
                furnace.inputs.extractItem(slot, 1, false);
                furnace.nextSlot = (slot + 1) % SLOTS;
            }
            if (working && level.getGameTime() % 20 == 0) {
                level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.4F, 2.0F);
            }
        } else {
            furnace.progress = 0;
        }
        if (state.getValue(BlockStateProperties.LIT) != working) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, working), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inputs", inputs.serializeNBT(registries));
        tag.put("Outputs", outputs.serializeNBT(registries));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inputs.deserializeNBT(registries, tag.getCompound("Inputs"));
        outputs.deserializeNBT(registries, tag.getCompound("Outputs"));
        progress = tag.getInt("Progress");
    }
}
