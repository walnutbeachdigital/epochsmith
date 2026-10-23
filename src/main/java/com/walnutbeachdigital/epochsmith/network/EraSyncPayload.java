package com.walnutbeachdigital.epochsmith.network;

import com.walnutbeachdigital.epochsmith.Epochsmith;
import com.walnutbeachdigital.epochsmith.era.Era;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A small message from the server telling a player's game which era they're in.
 */
public record EraSyncPayload(Era era) implements CustomPacketPayload {

    public static final Type<EraSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Epochsmith.MODID, "era_sync"));

    /** Sends the era as a single number (its position in the Era list). */
    public static final StreamCodec<ByteBuf, EraSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, payload -> payload.era().ordinal(),
            index -> new EraSyncPayload(Era.values()[index]));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
