package com.walnutbeachdigital.epochsmith.block;

import com.walnutbeachdigital.epochsmith.menu.MachineLayout;
import com.walnutbeachdigital.epochsmith.menu.MachineItems;
import com.walnutbeachdigital.epochsmith.menu.MachineGui;
import com.walnutbeachdigital.epochsmith.item.ModItems;
import com.walnutbeachdigital.epochsmith.power.MechanicalPower;

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
 * Smelts one item at a time while its bellows are turned (force 4 or more) and it has heat.
 *  - Raw iron, copper, gold or tin: 1 ingot, plus a 50% chance of a second. Costs 1 heat.
 *  - Wrought iron ingot: 1 crucible steel ingot. Costs 2 heat.
 * Each coal or charcoal adds 8 heat (64 max).
 */
public class ForgeBlockEntity extends BlockEntity implements StatusDescriber, MachineGui {

    public static final int MIN_FORCE = 4;
    public static final int MAX_ITEMS = 16;
    public static final int HEAT_PER_FUEL = 8;
    public static final int MAX_HEAT = 64;
    private static final int POWER_CHECK_INTERVAL = 10;

    private static final int INPUT = 0, FUEL = 1;

    public static final MachineLayout LAYOUT = new MachineLayout(5)
            .input(56, 17).fuel(56, 53).output(116, 35).arrow(79, 34, 0, 1)
            .gaugeOf(30, 17, 2, MAX_HEAT, MachineLayout.HEAT, "gui.epochsmith.heat")
            .gaugeOf(152, 17, 3, 16, MachineLayout.FORCE, "gui.epochsmith.force");

    private final MachineItems items = new MachineItems(this::setChanged,
            (slot, stack) -> slot == INPUT ? canSmelt(stack) : isFuel(stack),
            MachineItems.Role.INPUT, MachineItems.Role.FUEL, MachineItems.Role.OUTPUT);
    private int heat = 0;
    private int progress = 0;
    private int force = 0;

    public ForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FORGE.get(), pos, state);
    }

    // ---- Forge recipes ----

    public static ItemStack resultFor(ItemStack stack) {
        if (stack.is(Items.RAW_IRON)) {
            return new ItemStack(Items.IRON_INGOT);
        }
        if (stack.is(Items.RAW_COPPER)) {
            return new ItemStack(Items.COPPER_INGOT);
        }
        if (stack.is(Items.RAW_GOLD)) {
            return new ItemStack(Items.GOLD_INGOT);
        }
        if (stack.is(ModItems.RAW_TIN.get())) {
            return new ItemStack(ModItems.TIN_INGOT.get());
        }
        if (stack.is(ModItems.WROUGHT_IRON_INGOT.get())) {
            return new ItemStack(ModItems.STEEL_INGOT.get());
        }
        return ItemStack.EMPTY;
    }

    private static boolean isSteel(ItemStack stack) {
        return stack.is(ModItems.WROUGHT_IRON_INGOT.get());
    }

    /** Steel takes twice as long and twice the heat. */
    private static int workFor(ItemStack stack) {
        return isSteel(stack) ? 640 : 320;
    }

    private static int heatFor(ItemStack stack) {
        return isSteel(stack) ? 2 : 1;
    }

    public static boolean canSmelt(ItemStack stack) {
        return !resultFor(stack).isEmpty();
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
    }

    // ---- Loading ----

    private ItemStack input() {
        return items.getStackInSlot(INPUT);
    }

    public Component describe() {
        return Component.translatable("message.epochsmith.forge.status", input().getCount(), heat, force);
    }

    // ---- GUI ----

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
            case 1 -> input().isEmpty() ? 1 : workFor(input());
            case 2 -> heat;
            case 3 -> force;
            default -> 0;
        };
    }

    // ---- Ticking ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, ForgeBlockEntity forge) {
        if (level.getGameTime() % POWER_CHECK_INTERVAL == 0) {
            forge.force = MechanicalPower.forceAt(level, pos);
        }
        // Burn fuel from the fuel slot whenever there's room for its heat
        if (forge.heat + HEAT_PER_FUEL <= MAX_HEAT && !forge.items.getStackInSlot(FUEL).isEmpty()) {
            forge.items.extractItem(FUEL, 1, false);
            forge.heat += HEAT_PER_FUEL;
            forge.setChanged();
        }
        ItemStack input = forge.input();
        boolean working = !input.isEmpty() && forge.force >= MIN_FORCE && forge.heat >= heatFor(input)
                && forge.items.fitsOutput(resultFor(input).copyWithCount(2));
        if (state.getValue(BlockStateProperties.LIT) != working) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, working), Block.UPDATE_ALL);
        }
        if (!working) {
            return;
        }
        forge.progress += forge.force;
        if (forge.progress >= workFor(input)) {
            forge.progress = 0;
            forge.heat -= heatFor(input);
            ItemStack result = resultFor(input);
            if (!isSteel(input) && level.random.nextBoolean()) {
                result.grow(1);
            }
            forge.items.extractItem(INPUT, 1, false);
            forge.items.addOutput(result);
            level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.4F);
        }
        forge.setChanged();
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
