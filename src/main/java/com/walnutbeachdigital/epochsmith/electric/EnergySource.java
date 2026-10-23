package com.walnutbeachdigital.epochsmith.electric;

/**
 * Something on a wire network that electric machines can draw energy from (dynamos, batteries).
 */
public interface EnergySource {

    /** Takes up to this much energy out. Returns how much was actually taken. */
    int drawEnergy(int max);
}
