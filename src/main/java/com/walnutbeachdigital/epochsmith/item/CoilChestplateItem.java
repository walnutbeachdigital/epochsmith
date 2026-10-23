package com.walnutbeachdigital.epochsmith.item;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The coil chestplate: while charged, it shocks anything that hits you in melee (see PoweredToolEvents).
 */
public class CoilChestplateItem extends ArmorItem implements ElectricPowered {

    public CoilChestplateItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.epochsmith.coil_chestplate"));
        ElectricPowered.addTooltip(stack, lines);
    }
}
