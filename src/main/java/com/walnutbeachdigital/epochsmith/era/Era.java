package com.walnutbeachdigital.epochsmith.era;

import com.mojang.serialization.Codec;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * The eras a player moves through, in order.
 * NONE means the player hasn't started Epochsmith yet (no flint picked up).
 */
public enum Era implements StringRepresentable {
    NONE("none"),
    PRIMITIVE("primitive"),
    BRONZE_IRON("bronze_iron"),
    MECHANICAL("mechanical"),
    INDUSTRIAL("industrial"),
    ELECTRICAL("electrical"),
    INFORMATION("information"),
    FUTURE("future");

    /** Used to save the era to the world file. */
    public static final Codec<Era> CODEC = StringRepresentable.fromEnum(Era::values);

    private final String id;

    Era(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** The era's name, translated from the lang file (e.g. "Primitive Era"). */
    public Component displayName() {
        return Component.translatable("era.epochsmith." + id);
    }

    /** True if this era is the same as, or later than, the other era. */
    public boolean isAtLeast(Era other) {
        return this.ordinal() >= other.ordinal();
    }

    /** The era after this one. FUTURE is the last era, so it returns itself. */
    public Era next() {
        return this == FUTURE ? FUTURE : values()[ordinal() + 1];
    }

    /** Finds an era by its id (e.g. "bronze_iron"), or returns null if there's no match. */
    public static Era byId(String id) {
        for (Era era : values()) {
            if (era.id.equals(id)) {
                return era;
            }
        }
        return null;
    }
}
