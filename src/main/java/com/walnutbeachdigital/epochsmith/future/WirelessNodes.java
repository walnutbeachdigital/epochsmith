package com.walnutbeachdigital.epochsmith.future;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Keeps track of where every loaded wireless power node is, per dimension,
 * so the electric network can jump between nodes without wires.
 */
public final class WirelessNodes {

    public static final int RANGE = 32;

    private static final Map<ResourceKey<Level>, Set<BlockPos>> NODES = new HashMap<>();

    static void add(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            NODES.computeIfAbsent(level.dimension(), key -> new HashSet<>()).add(pos.immutable());
        }
    }

    static void remove(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            Set<BlockPos> nodes = NODES.get(level.dimension());
            if (nodes != null) {
                nodes.remove(pos);
            }
        }
    }

    /** Every other node within range of this one. */
    public static List<BlockPos> near(Level level, BlockPos pos) {
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos other : NODES.getOrDefault(level.dimension(), Set.of())) {
            if (!other.equals(pos) && other.closerThan(pos, RANGE)) {
                found.add(other);
            }
        }
        return found;
    }

    private WirelessNodes() {
    }
}
