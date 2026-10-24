package com.walnutbeachdigital.epochsmith.electric;

import com.walnutbeachdigital.epochsmith.menu.MachineLayout;
import com.walnutbeachdigital.epochsmith.menu.MachineItems;
import com.walnutbeachdigital.epochsmith.menu.MachineGui;
import com.walnutbeachdigital.epochsmith.block.StatusDescriber;
import java.util.List;
import java.util.Optional;

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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Smelts with an electric arc: 320 energy per item, drawn at up to 32 per tick
 * (half a second per item on full power). No fuel.
 *  - Raw iron, copper, gold, tin: 2 ingots.
 *  - Iron ingot: crucible steel.
 *  - Anything else a vanilla furnace can smelt: the normal result.
 */
public class ArcFurnaceBlockEntity extends BlockEntity implements StatusDescriber, MachineGui {

    public static final int ENERGY_PER_ITEM = 320;
    public static final int ENERGY_PER_TICK = 32;
    public static final int MAX_ITEMS = 64;
    private static final int REFRESH_INTERVAL = 20;

    public static final MachineLayout LAYOUT = new MachineLayout(3).theme("electrical")
            .input(56, 35).output(116, 35).arrow(79, 34, 0, 1)
            .gaugeOf(152, 17, 2, ENERGY_PER_TICK, MachineLayout.ENERGY, "gui.epochsmith.power");

    private final MachineItems items = new MachineItems(this::setChanged,
            (slot, stack) -> level != null && !resultFor(level, stack).isEmpty(),
            MachineItems.Role.INPUT, MachineItems.Role.OUTPUT);
    private int lastDrawn = 0;
    private int progress = 0;
    private List<BlockPos> sources = List.of();

    public ArcFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARC_FURNACE.get(), pos, state);
    }

    /** What one of these becomes, or EMPTY if it can't be smelted. */
    public static ItemStack resultFor(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (stack.is(Items.RAW_IRON)) return new ItemStack(Items.IRON_INGOT, 2);
        if (stack.is(Items.RAW_COPPER)) return new ItemStack(Items.COPPER_INGOT, 2);
        if (stack.is(Items.RAW_GOLD)) return new ItemStack(Items.GOLD_INGOT, 2);
        if (stack.is(ModItems.RAW_TIN.get())) return new ItemStack(ModItems.TIN_INGOT.get(), 2);
        if (stack.is(Items.IRON_INGOT)) return new ItemStack(ModItems.STEEL_INGOT.get());
        if (stack.is(Items.SAND)) return new ItemStack(ModItems.SILICON.get());
        Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack.copyWithCount(1)), level);
        return recipe.map(holder -> holder.value().getResultItem(level.registryAccess()).copy()).orElse(ItemStack.EMPTY);
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
            case 1 -> ENERGY_PER_ITEM;
            case 2 -> lastDrawn;
            default -> 0;
        };
    }

    public Component describe() {
        return Component.translatable("message.epochsmith.arc_furnace.status", items.getStackInSlot(0).getCount(),
                getBlockState().getValue(BlockStateProperties.LIT)
                        ? Component.translatable("message.epochsmith.arc_furnace.powered")
                        : Component.translatable("message.epochsmith.arc_furnace.unpowered"));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArcFurnaceBlockEntity furnace) {
        if (level.getGameTime() % REFRESH_INTERVAL == 0) {
            furnace.sources = ElectricNetwork.findSources(level, pos);
        }
        boolean working = false;
        ItemStack input = furnace.items.getStackInSlot(0);
        furnace.lastDrawn = 0;
        if (!input.isEmpty() && furnace.items.fitsOutput(resultFor(level, input))) {
            int drawn = ElectricNetwork.draw(level, furnace.sources, ENERGY_PER_TICK, false);
            furnace.lastDrawn = drawn;
            working = drawn > 0;
            furnace.progress += drawn;
            if (furnace.progress >= ENERGY_PER_ITEM) {
                furnace.progress -= ENERGY_PER_ITEM;
                ItemStack result = resultFor(level, input);
                furnace.items.extractItem(0, 1, false);
                furnace.items.addOutput(result);
                if (level.getGameTime() % 4 == 0) {
                    level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.3F, 1.8F);
                }
            }
            furnace.setChanged();
        }
        if (state.getValue(BlockStateProperties.LIT) != working) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, working), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        progress = tag.getInt("Progress");
    }
}
