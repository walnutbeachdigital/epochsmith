package com.walnutbeachdigital.epochsmith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.walnutbeachdigital.epochsmith.block.StoneAnvilBlock;
import com.walnutbeachdigital.epochsmith.block.StoneAnvilBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the metal lying flat on top of the Stone Anvil.
 */
public class StoneAnvilRenderer implements BlockEntityRenderer<StoneAnvilBlockEntity> {

    private final ItemRenderer itemRenderer;

    public StoneAnvilRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(StoneAnvilBlockEntity anvil, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        ItemStack stack = anvil.getWorkpiece();
        if (stack.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, 1.01, 0.5);
        // Line the piece up with the anvil, then lay it flat
        pose.mulPose(Axis.YP.rotationDegrees(-anvil.getBlockState().getValue(StoneAnvilBlock.FACING).toYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(0.45F, 0.45F, 0.45F);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, pose, buffers,
                anvil.getLevel(), 0);
        pose.popPose();
    }
}
