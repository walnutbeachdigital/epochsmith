package com.walnutbeachdigital.epochsmith.network;

import java.util.List;

import com.walnutbeachdigital.epochsmith.Epochsmith;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Server to player: everything in a data vault, for the terminal screen.
 * Kinds and counts are sent as two lists, because one item stack can't hold a count above 99.
 * "open" is true when the player just clicked the vault, false when it's a refresh.
 */
public record VaultContentsPayload(BlockPos pos, List<ItemStack> kinds, List<Integer> counts, boolean open)
        implements CustomPacketPayload {

    public static final Type<VaultContentsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Epochsmith.MODID, "vault_contents"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VaultContentsPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, VaultContentsPayload::pos,
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), VaultContentsPayload::kinds,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), VaultContentsPayload::counts,
            ByteBufCodecs.BOOL, VaultContentsPayload::open,
            VaultContentsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
