package com.walnutbeachdigital.epochsmith.steam;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.item.ModItems;
import com.walnutbeachdigital.epochsmith.menu.MachineGui;
import com.walnutbeachdigital.epochsmith.menu.MachineItems;
import com.walnutbeachdigital.epochsmith.menu.MachineLayout;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
 * Bakes coal into coke, a whole batch (up to 16) at a time, in 30 seconds. Needs no fuel:
 * the coal's own gases keep it hot.
 */
public class CokeOvenBlockEntity extends BlockEntity implements MachineGui {

    public static final int BAKE_TIME = 600;
    public static final int MAX_ITEMS = 16;

    public static final MachineLayout LAYOUT = new MachineLayout(2)
            .input(56, 35).output(116, 35).arrow(79, 34, 0, 1);

    private final MachineItems items = new MachineItems(this::setChanged, (slot, stack) -> stack.is(Items.COAL),
            MachineItems.Role.INPUT, MachineItems.Role.OUTPUT).limit(0, MAX_ITEMS);
    private int progress = 0;

    public CokeOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COKE_OVEN.get(), pos, state);
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
        return index == 0 ? progress : index == 1 ? BAKE_TIME : 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CokeOvenBlockEntity oven) {
        ItemStack coal = oven.items.getStackInSlot(0);
        ItemStack batch = new ItemStack(ModItems.COKE.get(), coal.getCount());
        boolean baking = !coal.isEmpty() && oven.items.fitsOutput(batch);
        if (state.getValue(BlockStateProperties.LIT) != baking) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, baking), Block.UPDATE_ALL);
        }
        if (!baking) {
            if (oven.progress != 0 && coal.isEmpty()) {
                oven.progress = 0;
                oven.setChanged();
            }
            return;
        }
        oven.progress++;
        if (oven.progress >= BAKE_TIME) {
            oven.items.extractItem(0, coal.getCount(), false);
            oven.items.addOutput(batch);
            oven.progress = 0;
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 0.8F);
        }
        oven.setChanged();
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
