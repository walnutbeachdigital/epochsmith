package com.walnutbeachdigital.epochsmith.future;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.walnutbeachdigital.epochsmith.information.VaultStorage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Every quantum storage network in the world, saved with the world (not inside any one block).
 * Cells that share a link id all open the same storage, wherever and in whatever dimension they are.
 */
public class QuantumNetworks extends SavedData {

    public static final int MAX_TYPES = 1024;
    public static final int CAPACITY = 1048576;
    private static final String FILE = "epochsmith_quantum";

    private final Map<UUID, VaultStorage> networks = new HashMap<>();

    /** The world's quantum networks. Always saved alongside the overworld. */
    public static QuantumNetworks get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(QuantumNetworks::new, QuantumNetworks::load, null), FILE);
    }

    public VaultStorage storage(UUID link) {
        return networks.computeIfAbsent(link, id -> new VaultStorage(MAX_TYPES, CAPACITY, this::setDirty));
    }

    private static QuantumNetworks load(CompoundTag tag, HolderLookup.Provider registries) {
        QuantumNetworks data = new QuantumNetworks();
        CompoundTag all = tag.getCompound("Networks");
        for (String key : all.getAllKeys()) {
            try {
                UUID id = UUID.fromString(key);
                data.storage(id).load(registries, all.getCompound(key));
            } catch (IllegalArgumentException ignored) {
                // Skip anything that isn't a valid link id
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag all = new CompoundTag();
        networks.forEach((id, storage) -> {
            if (!storage.isEmpty()) {
                all.put(id.toString(), storage.save(registries));
            }
        });
        tag.put("Networks", all);
        return tag;
    }
}
