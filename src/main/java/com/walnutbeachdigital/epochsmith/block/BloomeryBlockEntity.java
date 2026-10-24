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
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The Bronze and Iron era's furnace. Slots: copper, tin and raw iron on top, charcoal below.
 *  - 3 copper + 1 tin makes 4 bronze ingots.
 *  - Raw iron becomes iron blooms (hammer them on the stone anvil). Iron is fired before bronze.
 * Each firing burns 2 charcoal and takes 30 seconds.
 */
public class BloomeryBlockEntity extends BlockEntity implements StatusDescriber, MachineGui {

    public static final int FIRE_TIME = 600;
    public static final int MAX_COPPER = 12;
    public static final int MAX_TIN = 4;
    public static final int MAX_RAW_IRON = 8;
    public static final int CHARCOAL_PER_FIRING = 2;
    private static final int COPPER = 0, TIN = 1, IRON = 2, FUEL = 3;

    public static final TagKey<Item> TIN_INGOTS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/tin"));

    public static final MachineLayout LAYOUT = new MachineLayout(3)
            .input(30, 17).input(48, 17).input(66, 17).fuel(48, 53).output(124, 35)
            .flame(48, 36, 2, 1).arrow(88, 34, 0, 1);

    private final MachineItems items = new MachineItems(this::setChanged, BloomeryBlockEntity::accepts,
            MachineItems.Role.INPUT, MachineItems.Role.INPUT, MachineItems.Role.INPUT, MachineItems.Role.FUEL, MachineItems.Role.OUTPUT)
            .limit(COPPER, MAX_COPPER).limit(TIN, MAX_TIN).limit(IRON, MAX_RAW_IRON);
    private int progress = 0;
    private UUID user = null;

    public BloomeryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLOOMERY.get(), pos, state);
    }

    private static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case COPPER -> stack.is(Items.COPPER_INGOT);
            case TIN -> stack.is(TIN_INGOTS) || stack.is(ModItems.TIN_INGOT.get());
            case IRON -> stack.is(Items.RAW_IRON);
            case FUEL -> stack.is(Items.CHARCOAL);
            default -> false;
        };
    }

    private int count(int slot) {
        return items.getStackInSlot(slot).getCount();
    }

    /** What the next firing would make, or EMPTY if there isn't a full charge. */
    private ItemStack nextResult() {
        if (count(IRON) > 0) {
            return new ItemStack(ModItems.IRON_BLOOM.get(), count(IRON));
        }
        int batches = Math.min(count(COPPER) / 3, count(TIN));
        return batches > 0 ? new ItemStack(ModItems.BRONZE_INGOT.get(), batches * 4) : ItemStack.EMPTY;
    }

    public Component describe() {
        if (count(IRON) > 0) {
            return Component.translatable("message.epochsmith.bloomery.status_iron", count(IRON));
        }
        return Component.translatable("message.epochsmith.bloomery.status_bronze", count(COPPER), count(TIN));
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, BloomeryBlockEntity bloomery) {
        boolean lit = state.getValue(BlockStateProperties.LIT);
        ItemStack result = bloomery.nextResult();
        if (!lit) {
            if (!result.isEmpty() && bloomery.items.fitsOutput(result) && bloomery.count(FUEL) >= CHARCOAL_PER_FIRING) {
                bloomery.items.extractItem(FUEL, CHARCOAL_PER_FIRING, false);
                bloomery.progress = 0;
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.9F, 0.8F);
                bloomery.setChanged();
            }
            return;
        }
        if (bloomery.progress < FIRE_TIME) {
            bloomery.progress++;
            bloomery.setChanged();
            return;
        }
        if (!result.isEmpty() && !bloomery.items.fitsOutput(result)) {
            return; // wait for room in the output
        }
        boolean madeIron = result.is(ModItems.IRON_BLOOM.get());
        if (madeIron) {
            bloomery.items.extractItem(IRON, result.getCount(), false);
        } else if (!result.isEmpty()) {
            int batches = result.getCount() / 4;
            bloomery.items.extractItem(COPPER, batches * 3, false);
            bloomery.items.extractItem(TIN, batches, false);
        }
        bloomery.items.addOutput(result);
        bloomery.progress = 0;
        level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.8F);
        // Smelting iron for the first time is the Bronze and Iron era's signature invention
        if (madeIron && bloomery.user != null && level.getPlayerByUUID(bloomery.user) instanceof ServerPlayer player) {
            EraManager.advanceTo(player, Era.MECHANICAL);
        }
        bloomery.setChanged();
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
