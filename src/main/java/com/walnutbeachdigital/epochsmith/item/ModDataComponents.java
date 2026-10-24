package com.walnutbeachdigital.epochsmith.item;

import com.mojang.serialization.Codec;
import com.walnutbeachdigital.epochsmith.Epochsmith;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Extra values stored on item stacks.
 */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Epochsmith.MODID);

    /** How much steam a steam tool or steam boots hold. Saved with the item and sent to players' games. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STEAM =
            COMPONENTS.registerComponentType("steam",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** How much electric charge an electric tool or coil chestplate holds. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHARGE =
            COMPONENTS.registerComponentType("charge",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Everything stored in a data vault, kept on the vault item when it's broken and picked up. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> VAULT_CONTENTS =
            COMPONENTS.registerComponentType("vault_contents",
                    builder -> builder.persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC));

    /** Which mode a multitool is in (0 = precise, 1 = area). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TOOL_MODE =
            COMPONENTS.registerComponentType("tool_mode",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModDataComponents() {
    }
}
