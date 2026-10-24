package com.walnutbeachdigital.epochsmith.future;

import java.util.List;
import java.util.UUID;

import com.walnutbeachdigital.epochsmith.item.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** The quantum cell item: its tooltip says which network it's linked to. */
public class QuantumCellItem extends BlockItem {

    public QuantumCellItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        UUID link = stack.get(ModDataComponents.QUANTUM_LINK.get());
        lines.add(link == null
                ? Component.translatable("tooltip.epochsmith.quantum_cell.unlinked").withStyle(ChatFormatting.GRAY)
                : Component.translatable("tooltip.epochsmith.quantum_cell.linked", link.toString().substring(0, 8))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("tooltip.epochsmith.quantum_cell.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
