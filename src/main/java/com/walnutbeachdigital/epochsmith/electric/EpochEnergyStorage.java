package com.walnutbeachdigital.epochsmith.electric;

import net.minecraft.util.Mth;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * Standard Forge Energy storage (so other mods' cables and machines understand it),
 * plus a couple of helpers Epochsmith uses internally.
 */
public class EpochEnergyStorage extends EnergyStorage {

    private final Runnable onChange;

    public EpochEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChange) {
        super(capacity, maxReceive, maxExtract);
        this.onChange = onChange;
    }

    /** Adds generated energy, ignoring the receive limit. Returns how much fitted. */
    public int generate(int amount) {
        int added = Math.min(amount, capacity - energy);
        if (added > 0) {
            energy += added;
            onChange.run();
        }
        return added;
    }

    /** Removes energy for Epochsmith's own machines, ignoring the extract limit. */
    public int take(int amount) {
        int removed = Math.min(amount, energy);
        if (removed > 0) {
            energy -= removed;
            onChange.run();
        }
        return removed;
    }

    public void setEnergy(int amount) {
        energy = Mth.clamp(amount, 0, capacity);
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int received = super.receiveEnergy(toReceive, simulate);
        if (received > 0 && !simulate) {
            onChange.run();
        }
        return received;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int extracted = super.extractEnergy(toExtract, simulate);
        if (extracted > 0 && !simulate) {
            onChange.run();
        }
        return extracted;
    }
}
