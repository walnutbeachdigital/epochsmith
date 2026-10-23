package com.walnutbeachdigital.epochsmith.network;

import com.walnutbeachdigital.epochsmith.Epochsmith;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Player to server: "give me some of this from the vault". fullStack is false for just one.
 */
public record VaultWithdrawPayload(BlockPos pos, ItemStack kind, boolean fullStack) implements CustomPacketPayload {

    public static final Type<VaultWithdrawPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Epochsmith.MODID, "vault_withdraw"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VaultWithdrawPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, VaultWithdrawPayload::pos,
            ItemStack.STREAM_CODEC, VaultWithdrawPayload::kind,
            ByteBufCodecs.BOOL, VaultWithdrawPayload::fullStack,
            VaultWithdrawPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
