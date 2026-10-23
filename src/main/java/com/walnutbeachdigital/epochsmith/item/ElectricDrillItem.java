package com.walnutbeachdigital.epochsmith.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mines 3x3 while charged (see PoweredToolEvents) and digs 60% faster while it has charge.
 */
public class ElectricDrillItem extends DiggerItem implements ElectricPowered, PoweredTools.AreaDrill {

    public ElectricDrillItem(Tier tier, TagKey<Block> blocks, Properties properties) {
        super(tier, blocks, properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        return ElectricPowered.getCharge(stack) > 0 ? speed * 1.6F : speed;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.epochsmith.electric_drill"));
        ElectricPowered.addTooltip(stack, lines);
    }
}
