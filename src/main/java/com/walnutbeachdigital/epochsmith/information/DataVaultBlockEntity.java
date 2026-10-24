package com.walnutbeachdigital.epochsmith.information;

import java.util.List;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.electric.ElectricNetwork;
import com.walnutbeachdigital.epochsmith.item.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Stores items as data: up to 256 different kinds and 65,536 items in total, in one block.
 * The terminal (right-click with an empty hand) needs 16 energy per tick to run.
 * Hoppers and pipes can put items in and take them out even without power.
 */
public class DataVaultBlockEntity extends BlockEntity implements ItemVault {

    public static final int MAX_TYPES = 256;
    public static final int CAPACITY = 65536;
    public static final int ENERGY_PER_TICK = 16;
    private static final int REFRESH_INTERVAL = 20;

    private final VaultStorage storage = new VaultStorage(MAX_TYPES, CAPACITY, this::setChanged);
    private List<BlockPos> sources = List.of();

    public DataVaultBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DATA_VAULT.get(), pos, state);
    }

    @Override
    public VaultStorage getStorage() {
        return storage;
    }

    public IItemHandler getAutomation() {
        return storage.getAutomation();
    }

    @Override
    public boolean isPowered() {
        return getBlockState().getValue(BlockStateProperties.LIT);
    }

    /** Stores as much of the stack as fits. Returns how many it took (the stack itself isn't changed). */
    public int store(ItemStack stack, boolean simulate) {
        return storage.store(stack, simulate);
    }

    public Component describe() {
        return Component.translatable("message.epochsmith.data_vault.status", storage.getTotal(), CAPACITY,
                storage.getEntries().size(), MAX_TYPES);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DataVaultBlockEntity vault) {
        if (level.getGameTime() % REFRESH_INTERVAL == 0) {
            vault.sources = ElectricNetwork.findSources(level, pos);
        }
        boolean powered = ElectricNetwork.draw(level, vault.sources, ENERGY_PER_TICK, false) >= ENERGY_PER_TICK;
        if (state.getValue(BlockStateProperties.LIT) != powered) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, powered), Block.UPDATE_ALL);
        }
    }

    // ---- Saving: in the world, and on the item when the vault is broken

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Vault", storage.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        storage.load(registries, tag.getCompound("Vault"));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!storage.isEmpty() && level != null) {
            components.set(ModDataComponents.VAULT_CONTENTS.get(), CustomData.of(storage.save(level.registryAccess())));
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        CustomData data = input.get(ModDataComponents.VAULT_CONTENTS.get());
        if (data != null && level != null) {
            storage.load(level.registryAccess(), data.copyTag());
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Vault");
    }
}
