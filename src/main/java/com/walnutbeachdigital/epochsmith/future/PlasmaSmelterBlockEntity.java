package com.walnutbeachdigital.epochsmith.future;

import java.util.List;
import java.util.Optional;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.electric.ArcFurnaceBlockEntity;
import com.walnutbeachdigital.epochsmith.item.ModItems;

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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The Future era's smelter. Burns plasma cells instead of electricity: one cell smelts 512 items.
 * Ores and raw ores come out tripled; everything else smelts like the arc furnace.
 * Smelts up to 2 items per tick (40 a second).
 *
 * Fully automatable: hoppers and pipes put in items and plasma cells from any side and pull results out.
 */
public class PlasmaSmelterBlockEntity extends BlockEntity {

    public static final int SLOTS = 4;
    public static final int SMELTS_PER_CELL = 512;
    public static final int SMELTS_PER_TICK = 2;
    public static final int MAX_CELLS = 16;

    private final ItemStackHandler inputs = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return level != null && !stack.is(ModItems.PLASMA_CELL.get()) && !resultFor(level, stack).isEmpty();
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
    private final ItemStackHandler fuel = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.PLASMA_CELL.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return MAX_CELLS;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** What hoppers and pipes see: insert into inputs (plasma cells go to the fuel slot), extract from outputs. */
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
            if (stack.is(ModItems.PLASMA_CELL.get())) {
                return fuel.insertItem(0, stack, simulate);
            }
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
            return stack.is(ModItems.PLASMA_CELL.get()) || slot < SLOTS && inputs.isItemValid(slot, stack);
        }
    };

    /** Smelts left from the plasma cell currently burning. */
    private int plasma = 0;
    private int nextSlot = 0;

    public PlasmaSmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLASMA_SMELTER.get(), pos, state);
    }

    public IItemHandler getAutomation() {
        return automation;
    }

    /** What one of these becomes: ores tripled, everything else as the arc furnace makes it. */
    public static ItemStack resultFor(Level level, ItemStack stack) {
        if (stack.is(Tags.Items.RAW_MATERIALS) || stack.is(Tags.Items.ORES)) {
            Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack.copyWithCount(1)), level);
            if (recipe.isPresent()) {
                ItemStack result = recipe.get().value().getResultItem(level.registryAccess()).copy();
                result.setCount(Math.min(result.getCount() * 3, result.getMaxStackSize()));
                return result;
            }
        }
        return ArcFurnaceBlockEntity.resultFor(level, stack);
    }

    /** Puts as much of the held stack in as fits (plasma cells go to the fuel slot). Returns true if anything went in. */
    public boolean insert(ItemStack stack) {
        if (stack.is(ModItems.PLASMA_CELL.get())) {
            ItemStack rest = fuel.insertItem(0, stack.copy(), false);
            boolean moved = rest.getCount() < stack.getCount();
            stack.setCount(rest.getCount());
            return moved;
        }
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
        List<ItemStack> taken = takeAll(inputs);
        taken.addAll(takeAll(fuel));
        return taken;
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
        int cells = fuel.getStackInSlot(0).getCount();
        return Component.translatable("message.epochsmith.plasma_smelter.status", count(inputs), count(outputs),
                cells, plasma);
    }

    /** Finds the next input slot whose result still fits in the outputs, starting where we left off. */
    private int findWork(Level level) {
        for (int i = 0; i < SLOTS; i++) {
            int slot = (nextSlot + i) % SLOTS;
            ItemStack input = inputs.getStackInSlot(slot);
            if (!input.isEmpty() && fits(resultFor(level, input))) {
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, PlasmaSmelterBlockEntity smelter) {
        boolean working = false;
        for (int i = 0; i < SMELTS_PER_TICK; i++) {
            int slot = smelter.findWork(level);
            if (slot < 0) {
                break;
            }
            if (smelter.plasma <= 0) {
                if (smelter.fuel.getStackInSlot(0).isEmpty()) {
                    break;
                }
                smelter.fuel.extractItem(0, 1, false);
                smelter.plasma = SMELTS_PER_CELL;
            }
            ItemStack input = smelter.inputs.getStackInSlot(slot);
            smelter.store(resultFor(level, input));
            smelter.inputs.extractItem(slot, 1, false);
            smelter.nextSlot = (slot + 1) % SLOTS;
            smelter.plasma--;
            working = true;
        }
        if (working) {
            smelter.setChanged();
            if (level.getGameTime() % 20 == 0) {
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.3F, 1.6F);
            }
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
        tag.put("Fuel", fuel.serializeNBT(registries));
        tag.putInt("Plasma", plasma);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inputs.deserializeNBT(registries, tag.getCompound("Inputs"));
        outputs.deserializeNBT(registries, tag.getCompound("Outputs"));
        fuel.deserializeNBT(registries, tag.getCompound("Fuel"));
        plasma = tag.getInt("Plasma");
    }
}
