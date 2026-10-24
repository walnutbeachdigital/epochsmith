package com.walnutbeachdigital.epochsmith.block;

import java.util.UUID;

import com.walnutbeachdigital.epochsmith.era.Era;
import com.walnutbeachdigital.epochsmith.era.EraManager;
import com.walnutbeachdigital.epochsmith.item.ModItems;
import com.walnutbeachdigital.epochsmith.menu.MachineGui;
import com.walnutbeachdigital.epochsmith.menu.MachineItems;
import com.walnutbeachdigital.epochsmith.menu.MachineLayout;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The Primitive era's kiln. Put clay, logs or an unfired amphora in the top slot and fuel
 * (1 coal or charcoal, or 4 sticks) in the bottom slot: it lights and fires the whole batch
 * (up to 16) in 20 seconds.
 */
public class PitKilnBlockEntity extends BlockEntity implements MachineGui {

    public static final int FIRE_TIME = 400;
    public static final int MAX_ITEMS = 16;
    public static final int STICKS_PER_FIRING = 4;
    private static final int INPUT = 0, FUEL = 1, OUTPUT = 2;

    public static final MachineLayout LAYOUT = new MachineLayout(3).theme("primitive")
            .input(56, 17).fuel(56, 53).output(116, 35)
            .flame(56, 36, 2, 1).arrow(79, 34, 0, 1);

    private final MachineItems items = new MachineItems(this::setChanged, (slot, stack) -> slot == INPUT ? canFire(stack) : isFuel(stack),
            MachineItems.Role.INPUT, MachineItems.Role.FUEL, MachineItems.Role.OUTPUT).limit(INPUT, MAX_ITEMS);
    private int progress = 0;
    private UUID user = null;

    public PitKilnBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIT_KILN.get(), pos, state);
    }

    /** The fired result for an item, or EMPTY if the kiln can't fire it. */
    public static ItemStack resultFor(ItemStack input) {
        if (input.is(Items.CLAY_BALL)) {
            return new ItemStack(Items.BRICK, input.getCount());
        }
        if (input.is(ModItems.UNFIRED_AMPHORA.get())) {
            return new ItemStack(ModBlocks.AMPHORA_ITEM.get(), input.getCount());
        }
        if (input.is(ItemTags.LOGS_THAT_BURN)) {
            return new ItemStack(Items.CHARCOAL, input.getCount());
        }
        return ItemStack.EMPTY;
    }

    public static boolean canFire(ItemStack stack) {
        return !resultFor(stack).isEmpty();
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || stack.is(Items.STICK);
    }

    public MachineItems getItems() {
        return items;
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
        boolean lit = getBlockState().getValue(BlockStateProperties.LIT);
        return switch (index) {
            case 0 -> progress;
            case 1 -> FIRE_TIME;
            case 2 -> lit ? FIRE_TIME - progress : 0;
            default -> 0;
        };
    }

    @Override
    public void onMenuOpened(Player player) {
        user = player.getUUID();
        setChanged();
    }

    // ---- Ticking ----

    /** Takes one firing's worth of fuel. Returns false if there isn't enough. */
    private boolean useFuel() {
        ItemStack fuel = items.getStackInSlot(FUEL);
        int needed = fuel.is(Items.STICK) ? STICKS_PER_FIRING : 1;
        if (fuel.isEmpty() || fuel.getCount() < needed) {
            return false;
        }
        items.extractItem(FUEL, needed, false);
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PitKilnBlockEntity kiln) {
        ItemStack input = kiln.items.getStackInSlot(INPUT);
        boolean lit = state.getValue(BlockStateProperties.LIT);
        if (!lit) {
            if (canFire(input) && kiln.items.fitsOutput(resultFor(input)) && kiln.useFuel()) {
                kiln.progress = 0;
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.7F, 1.0F);
                kiln.setChanged();
            }
            return;
        }
        if (kiln.progress < FIRE_TIME) {
            kiln.progress++;
            kiln.setChanged();
            return;
        }
        // Fired: the whole batch comes out together, once there's room
        ItemStack result = resultFor(input);
        if (!result.isEmpty() && !kiln.items.fitsOutput(result)) {
            return;
        }
        if (!result.isEmpty()) {
            kiln.items.extractItem(INPUT, input.getCount(), false);
            kiln.items.addOutput(result);
        }
        kiln.progress = 0;
        level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.0F);
        // The first successful firing is the Primitive era's signature invention
        if (!result.isEmpty() && kiln.user != null && level.getPlayerByUUID(kiln.user) instanceof ServerPlayer player) {
            EraManager.advanceTo(player, Era.BRONZE_IRON);
        }
        kiln.setChanged();
    }

    // ---- Saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Progress", progress);
        if (user != null) {
            tag.putUUID("User", user);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        progress = tag.getInt("Progress");
        user = tag.hasUUID("User") ? tag.getUUID("User") : null;
    }
}
