package com.walnutbeachdigital.epochsmith.item;

import net.minecraft.world.item.ItemStack;

/**
 * Shared helpers for tools that run on stored power, whether steam or electric charge.
 */
public final class PoweredTools {

    /** A tool that mines 3x3 while powered (steam drill, electric drill). */
    public interface AreaDrill {
    }

    /** A tool that fells whole trees while powered (pneumatic axe, chainsaw). */
    public interface TreeFeller {
    }

    public static boolean hasPower(ItemStack stack) {
        if (stack.getItem() instanceof SteamPowered) {
            return SteamPowered.getSteam(stack) > 0;
        }
        if (stack.getItem() instanceof ElectricPowered) {
            return ElectricPowered.getCharge(stack) > 0;
        }
        return false;
    }

    /** Uses one point of steam or charge. Returns false if there was none. */
    public static boolean usePower(ItemStack stack) {
        if (stack.getItem() instanceof SteamPowered) {
            return SteamPowered.use(stack, 1);
        }
        if (stack.getItem() instanceof ElectricPowered) {
            return ElectricPowered.use(stack, 1);
        }
        return false;
    }

    private PoweredTools() {
    }
}
