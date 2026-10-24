package com.walnutbeachdigital.epochsmith.menu;

import java.util.ArrayList;
import java.util.List;

/**
 * How a machine's GUI looks: where its slots, progress arrow, flame, gauges and buttons go.
 * Positions are in GUI pixels, the same as vanilla's 176-wide furnace screen.
 * "Values" are numbers the machine reports (progress, heat, energy...), numbered from 0.
 */
public final class MachineLayout {

    public record SlotDef(int x, int y, MachineItems.Role role) {
    }

    /** A progress indicator filling from value/max. */
    public record Meter(int x, int y, int value, int max) {
    }

    /**
     * A vertical gauge, filling from the bottom. max is a value number, or -1 to use fixedMax.
     * label is a translation key; its tooltip reads "label: value / max" (plus unit).
     */
    public record Gauge(int x, int y, int width, int height, int value, int max, int fixedMax, int color, String label) {
    }

    public record ButtonDef(int x, int y, int width, int height, int id, String label) {
    }

    /** A line of status text chosen by a value: key + "." + the value. */
    public record Status(int x, int y, int value, String keyPrefix) {
    }

    public final List<SlotDef> slots = new ArrayList<>();
    public final List<Meter> arrows = new ArrayList<>();
    public final List<Meter> flames = new ArrayList<>();
    public final List<Gauge> gauges = new ArrayList<>();
    public final List<ButtonDef> buttons = new ArrayList<>();
    public final List<Status> statuses = new ArrayList<>();
    public final int values;
    public int inventoryY = 84;

    public MachineLayout(int values) {
        this.values = values;
    }

    public MachineLayout slot(int x, int y, MachineItems.Role role) {
        slots.add(new SlotDef(x, y, role));
        return this;
    }

    public MachineLayout input(int x, int y) {
        return slot(x, y, MachineItems.Role.INPUT);
    }

    public MachineLayout fuel(int x, int y) {
        return slot(x, y, MachineItems.Role.FUEL);
    }

    public MachineLayout output(int x, int y) {
        return slot(x, y, MachineItems.Role.OUTPUT);
    }

    public MachineLayout pattern(int x, int y) {
        return slot(x, y, MachineItems.Role.PATTERN);
    }

    /** Vanilla's furnace arrow (24x16), pointing right. */
    public MachineLayout arrow(int x, int y, int value, int max) {
        arrows.add(new Meter(x, y, value, max));
        return this;
    }

    /** Vanilla's furnace flame (14x14), burning down. */
    public MachineLayout flame(int x, int y, int value, int max) {
        flames.add(new Meter(x, y, value, max));
        return this;
    }

    public MachineLayout gauge(int x, int y, int value, int max, int color, String label) {
        gauges.add(new Gauge(x, y, 8, 52, value, max, 0, color, label));
        return this;
    }

    /** A gauge whose maximum never changes. */
    public MachineLayout gaugeOf(int x, int y, int value, int fixedMax, int color, String label) {
        gauges.add(new Gauge(x, y, 8, 52, value, -1, fixedMax, color, label));
        return this;
    }

    public MachineLayout button(int x, int y, int width, int height, int id, String label) {
        buttons.add(new ButtonDef(x, y, width, height, id, label));
        return this;
    }

    public MachineLayout status(int x, int y, int value, String keyPrefix) {
        statuses.add(new Status(x, y, value, keyPrefix));
        return this;
    }

    /** Gauge colors */
    public static final int HEAT = 0xFFE0602A;
    public static final int ENERGY = 0xFFE03A3A;
    public static final int STEAM = 0xFFD8E4EC;
    public static final int FORCE = 0xFF8AB4D8;
    public static final int MATTER = 0xFF56D4F0;
    public static final int PLASMA = 0xFFC060F0;
}
