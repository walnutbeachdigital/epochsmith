package com.walnutbeachdigital.epochsmith.compat.jei;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * One recipe shown in JEI: what goes in (each slot can accept several items), what comes out,
 * and a short note on how (e.g. "Strike 3 times").
 */
public record MachineRecipe(List<List<ItemStack>> inputs, List<ItemStack> outputs, Component note) {

    public static MachineRecipe of(ItemStack input, ItemStack output, Component note) {
        return new MachineRecipe(List.of(List.of(input)), List.of(output), note);
    }
}
