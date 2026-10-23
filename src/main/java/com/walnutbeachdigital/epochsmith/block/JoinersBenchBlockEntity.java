package com.walnutbeachdigital.epochsmith.block;

import com.walnutbeachdigital.epochsmith.item.ModItems;
import com.walnutbeachdigital.epochsmith.power.MechanicalPower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cuts machine parts. Work it by hand (each click = 20 work) or connect rotation power
 * (each point of force = 1 work per tick, so force 4 cuts a wooden gear in under a second).
 *  planks -> 2 wooden gears (60 work)
 *  log -> 6 axles (60 work, better than crafting)
 *  wrought iron ingot -> 2 iron gears (120 work)
 */
public class JoinersBenchBlockEntity extends BlockEntity implements WorkpieceHolder {

    public static final int MAX_PIECES = 16;
    public static final int WORK_PER_CLICK = 20;
    public static final int MIN_FORCE = 2;
    private static final int POWER_CHECK_INTERVAL = 10;

    private ItemStack workpiece = ItemStack.EMPTY;
    private int progress = 0;
    private int force = 0;

    public JoinersBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.JOINERS_BENCH.get(), pos, state);
    }

    // ---- Bench recipes ----

    public static ItemStack resultFor(ItemStack input) {
        if (input.is(ItemTags.PLANKS)) {
            return new ItemStack(ModItems.WOODEN_GEAR.get(), 2);
        }
        if (input.is(ItemTags.LOGS)) {
            return new ItemStack(ModBlocks.AXLE_ITEM.get(), 6);
        }
        if (input.is(ModItems.WROUGHT_IRON_INGOT.get())) {
            return new ItemStack(ModItems.IRON_GEAR.get(), 2);
        }
        return ItemStack.EMPTY;
    }

    public static int workFor(ItemStack input) {
        return input.is(ModItems.WROUGHT_IRON_INGOT.get()) ? 120 : 60;
    }

    public static boolean canWork(ItemStack stack) {
        return !resultFor(stack).isEmpty();
    }

    // ---- Workpiece ----

    @Override
    public ItemStack getWorkpiece() {
        return workpiece;
    }

    public int getForce() {
        return force;
    }

    public boolean place(ItemStack stack) {
        if (workpiece.isEmpty()) {
            workpiece = stack.split(Math.min(stack.getCount(), MAX_PIECES));
            progress = 0;
            changed();
            return true;
        }
        if (ItemStack.isSameItemSameComponents(workpiece, stack) && workpiece.getCount() < MAX_PIECES) {
            int moved = Math.min(stack.getCount(), MAX_PIECES - workpiece.getCount());
            workpiece.grow(moved);
            stack.shrink(moved);
            changed();
            return true;
        }
        return false;
    }

    public ItemStack takeAll() {
        ItemStack taken = workpiece;
        workpiece = ItemStack.EMPTY;
        progress = 0;
        changed();
        return taken;
    }

    /** Adds work (from a click or from power). Pops out finished parts. */
    public void work(int amount) {
        if (workpiece.isEmpty() || level == null) {
            return;
        }
        progress += amount;
        int needed = workFor(workpiece);
        while (progress >= needed && !workpiece.isEmpty()) {
            progress -= needed;
            Block.popResource(level, worldPosition.above(), resultFor(workpiece));
            workpiece.shrink(1);
            level.playSound(null, worldPosition, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.7F, 1.0F);
        }
        if (workpiece.isEmpty()) {
            progress = 0;
        }
        changed();
    }

    /** Sawdust and a rasping sound. */
    public void effects() {
        if (level instanceof ServerLevel server && !workpiece.isEmpty()) {
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, workpiece.copyWithCount(1)),
                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5,
                    5, 0.15, 0.02, 0.15, 0.05);
            level.playSound(null, worldPosition, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 0.5F, 1.2F);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, JoinersBenchBlockEntity bench) {
        if (level.getGameTime() % POWER_CHECK_INTERVAL == 0) {
            bench.force = MechanicalPower.forceAt(level, pos);
        }
        if (bench.force >= MIN_FORCE && !bench.workpiece.isEmpty()) {
            if (level.getGameTime() % 10 == 0) {
                bench.effects();
            }
            bench.work(bench.force);
        }
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Saving and syncing ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Workpiece", workpiece.saveOptional(registries));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        workpiece = ItemStack.parseOptional(registries, tag.getCompound("Workpiece"));
        progress = tag.getInt("Progress");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
