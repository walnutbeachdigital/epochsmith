package com.walnutbeachdigital.epochsmith.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fells whole trees while charged (see PoweredToolEvents) and cuts 60% faster while it has charge.
 */
public class ChainsawItem extends AxeItem implements ElectricPowered, PoweredTools.TreeFeller {

    public ChainsawItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        return ElectricPowered.getCharge(stack) > 0 ? speed * 1.6F : speed;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.epochsmith.chainsaw"));
        ElectricPowered.addTooltip(stack, lines);
    }
}
