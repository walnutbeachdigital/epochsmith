package com.walnutbeachdigital.epochsmith.era;

import java.util.function.Supplier;

import com.walnutbeachdigital.epochsmith.Epochsmith;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registers the data that Epochsmith saves on each player.
 */
public final class EraAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Epochsmith.MODID);

    /**
     * The era each player has reached. Starts at NONE, is saved with the world,
     * and is kept when the player dies.
     */
    public static final Supplier<AttachmentType<Era>> ERA = ATTACHMENT_TYPES.register("era",
            () -> AttachmentType.builder(() -> Era.NONE)
                    .serialize(Era.CODEC)
                    .copyOnDeath()
                    .build());

    private EraAttachments() {
    }
}
