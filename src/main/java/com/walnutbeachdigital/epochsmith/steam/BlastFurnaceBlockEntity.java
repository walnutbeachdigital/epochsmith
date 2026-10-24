package com.walnutbeachdigital.epochsmith.steam;

import com.walnutbeachdigital.epochsmith.menu.MachineLayout;
import com.walnutbeachdigital.epochsmith.menu.MachineItems;
import com.walnutbeachdigital.epochsmith.menu.MachineGui;
import com.walnutbeachdigital.epochsmith.block.StatusDescriber;
import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Smelts with a steam-driven air blast and coke.
 *  - Raw iron, copper, gold or tin: always 2 ingots.
 *  - Iron ingot: 1 crucible steel ingot (steel from ordinary iron, like the Bessemer process).
 * 2 seconds per item while it has steam; each item uses 1 heat, each coke adds 8.
 */
public class BlastFurnaceBlockEntity extends BlockEntity implements StatusDescriber, MachineGui {

    public static final int WORK_PER_ITEM = 40;
    public static final int MAX_ITEMS = 16;
    public static final int HEAT_PER_COKE = 8;
    public static final int MAX_HEAT = 64;
    private static final int CHECK_INTERVAL = 10;

    private static final int INPUT = 0, FUEL = 1;

    public static final MachineLayout LAYOUT = new MachineLayout(5).theme("industrial")
            .input(56, 17).fuel(56, 53).output(116, 35).arrow(79, 34, 0, 1)
            .gaugeOf(30, 17, 2, MAX_HEAT, MachineLayout.HEAT, "gui.epochsmith.heat")
            .status(8, 72 - 12, 3, "gui.epochsmith.steam_status");

    private final MachineItems items = new MachineItems(this::setChanged,
            (slot, stack) -> slot == INPUT ? canSmelt(stack) : stack.is(ModItems.COKE.get()),
            MachineItems.Role.INPUT, MachineItems.Role.FUEL, MachineItems.Role.OUTPUT);
    private int heat = 0;
    private int progress = 0;
    private boolean steam = false;

    public BlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLAST_FURNACE.get(), pos, state);
    }

    public static ItemStack resultFor(ItemStack stack) {
        if (stack.is(Items.RAW_IRON)) {
            return new ItemStack(Items.IRON_INGOT, 2);
        }
        if (stack.is(Items.RAW_COPPER)) {
            return new ItemStack(Items.COPPER_INGOT, 2);
        }
        if (stack.is(Items.RAW_GOLD)) {
            return new ItemStack(Items.GOLD_INGOT, 2);
        }
        if (stack.is(ModItems.RAW_TIN.get())) {
            return new ItemStack(ModItems.TIN_INGOT.get(), 2);
        }
        if (stack.is(Items.IRON_INGOT)) {
            return new ItemStack(ModItems.STEEL_INGOT.get());
        }
        return ItemStack.EMPTY;
    }

    public static boolean canSmelt(ItemStack stack) {
        return !resultFor(stack).isEmpty();
    }

    private ItemStack input() {
        return items.getStackInSlot(INPUT);
    }

    @Override
    public MachineItems getMenuItems() {
        return items;
    }

    @Override
    public MachineLayout getLayout() {
        return LAYOUT;
    }

    @Override
    public int getMenuValue(int index) {
        return switch (index) {
            case 0 -> progress;
            case 1 -> WORK_PER_ITEM;
            case 2 -> heat;
            case 3 -> steam ? 1 : 0;
            default -> 0;
        };
    }

    public Component describe() {
        return Component.translatable("message.epochsmith.blast_furnace.status", input().getCount(), heat,
                Component.translatable(steam ? "message.epochsmith.blast_furnace.steam_on" : "message.epochsmith.blast_furnace.steam_off"));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlastFurnaceBlockEntity furnace) {
        if (level.getGameTime() % CHECK_INTERVAL == 0) {
            furnace.steam = SteamNetwork.explore(level, pos).workingBoilers() > 0;
        }
        // Burn coke from the fuel slot whenever there's room for its heat
        if (furnace.heat + HEAT_PER_COKE <= MAX_HEAT && !furnace.items.getStackInSlot(FUEL).isEmpty()) {
            furnace.items.extractItem(FUEL, 1, false);
            furnace.heat += HEAT_PER_COKE;
            furnace.setChanged();
        }
        ItemStack input = furnace.input();
        boolean working = furnace.steam && furnace.heat > 0 && !input.isEmpty() && furnace.items.fitsOutput(resultFor(input));
        if (state.getValue(BlockStateProperties.LIT) != working) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, working), Block.UPDATE_ALL);
        }
        if (!working) {
            return;
        }
        furnace.progress++;
        if (furnace.progress >= WORK_PER_ITEM) {
            furnace.progress = 0;
            furnace.heat--;
            furnace.items.addOutput(resultFor(input));
            furnace.items.extractItem(INPUT, 1, false);
            level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.2F);
        }
        furnace.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Heat", heat);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        heat = tag.getInt("Heat");
        progress = tag.getInt("Progress");
    }
}
