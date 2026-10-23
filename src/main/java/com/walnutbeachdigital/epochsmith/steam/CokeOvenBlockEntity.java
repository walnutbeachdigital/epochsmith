package com.walnutbeachdigital.epochsmith.steam;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.item.ModItems;

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
 * Bakes coal into coke. Needs no fuel: the coal smoulders in its own heat.
 * Load coal and it starts by itself; 30 seconds later the whole batch is coke.
 */
public class CokeOvenBlockEntity extends BlockEntity {

    public static final int BAKE_TIME = 600;
    public static final int MAX_ITEMS = 16;

    private ItemStack contents = ItemStack.EMPTY;
    private int progress = 0;

    public CokeOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COKE_OVEN.get(), pos, state);
    }

    public ItemStack getContents() {
        return contents;
    }

    public boolean insertCoal(ItemStack stack) {
        if (contents.isEmpty()) {
            contents = stack.split(Math.min(stack.getCount(), MAX_ITEMS));
        } else if (contents.is(Items.COAL) && contents.getCount() < MAX_ITEMS) {
            int moved = Math.min(stack.getCount(), MAX_ITEMS - contents.getCount());
            contents.grow(moved);
            stack.shrink(moved);
        } else {
            return false;
        }
        progress = 0;
        setChanged();
        return true;
    }

    public ItemStack takeAll() {
        ItemStack taken = contents;
        contents = ItemStack.EMPTY;
        progress = 0;
        setChanged();
        return taken;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CokeOvenBlockEntity oven) {
        boolean baking = oven.contents.is(Items.COAL);
        if (state.getValue(BlockStateProperties.LIT) != baking) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, baking), Block.UPDATE_ALL);
        }
        if (!baking) {
            return;
        }
        oven.progress++;
        if (oven.progress >= BAKE_TIME) {
            oven.contents = new ItemStack(ModItems.COKE.get(), oven.contents.getCount());
            oven.progress = 0;
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 0.8F);
        }
        oven.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Contents", contents.saveOptional(registries));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        contents = ItemStack.parseOptional(registries, tag.getCompound("Contents"));
        progress = tag.getInt("Progress");
    }
}
