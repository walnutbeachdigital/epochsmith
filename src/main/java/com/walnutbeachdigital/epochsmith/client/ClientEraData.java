package com.walnutbeachdigital.epochsmith.client;

import com.walnutbeachdigital.epochsmith.era.Era;

/**
 * The player's era as their own game knows it. The server keeps the real value
 * and sends updates here; the Chronicle reads it to decide which chapters to show.
 */
public final class ClientEraData {
    private static Era era = Era.NONE;

    public static Era getEra() {
        return era;
    }

    public static void setEra(Era newEra) {
        era = newEra;
    }

    private ClientEraData() {
    }
}
