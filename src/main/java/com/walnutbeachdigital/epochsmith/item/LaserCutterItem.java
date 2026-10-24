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
 * The Information era's mining tool. Very fast, and while charged it cuts a tunnel
 * three blocks deep with every block you mine. Sneak to mine just one.
 */
public class LaserCutterItem extends DiggerItem implements ElectricPowered, PoweredTools.TunnelBorer {

    public LaserCutterItem(Tier tier, TagKey<Block> blocks, Properties properties) {
        super(tier, blocks, properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        return ElectricPowered.getCharge(stack) > 0 ? speed * 2.5F : speed;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.epochsmith.laser_cutter"));
        ElectricPowered.addTooltip(stack, lines);
    }
}
