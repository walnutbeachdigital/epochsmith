package com.walnutbeachdigital.epochsmith.item;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The composite helmet's visor shows your coordinates, heading and the nearest ore
 * while it has charge. It uses one charge every ten seconds.
 */
public class CompositeHelmetItem extends ArmorItem implements ElectricPowered {

    public CompositeHelmetItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.epochsmith.composite_helmet"));
        ElectricPowered.addTooltip(stack, lines);
    }
}
