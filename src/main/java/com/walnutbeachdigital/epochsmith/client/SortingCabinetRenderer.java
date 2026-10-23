package com.walnutbeachdigital.epochsmith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.walnutbeachdigital.epochsmith.electric.SortingCabinetBlock;
import com.walnutbeachdigital.epochsmith.electric.SortingCabinetBlockEntity;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws each drawer's item and count on the front of the Sorting Cabinet.
 */
public class SortingCabinetRenderer implements BlockEntityRenderer<SortingCabinetBlockEntity> {

    private final ItemRenderer itemRenderer;
    private final Font font;

    public SortingCabinetRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
        this.font = context.getFont();
    }

    @Override
    public void render(SortingCabinetBlockEntity cabinet, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (cabinet.getLevel() == null) {
            return;
        }
        Direction front = cabinet.getBlockState().getValue(SortingCabinetBlock.FACING);
        // Light from the block in front of the cabinet, so the icons aren't drawn in darkness
        int light = LevelRenderer.getLightColor(cabinet.getLevel(), cabinet.getBlockPos().relative(front));

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        // Turn so that local +Z points out of the front face, with +X to the viewer's right
        pose.mulPose(Axis.YP.rotationDegrees(-front.toYRot()));

        for (int drawer = 0; drawer < SortingCabinetBlockEntity.DRAWERS; drawer++) {
            ItemStack type = cabinet.getType(drawer);
            if (type.isEmpty()) {
                continue;
            }
            float x = ((drawer % 3) - 1) / 3.0F;
            float y = (1 - (drawer / 3)) / 3.0F;

            // The item icon, flattened onto the drawer front
            pose.pushPose();
            pose.translate(x, y + 0.03F, 0.502F);
            pose.scale(0.18F, 0.18F, 0.001F);
            itemRenderer.renderStatic(type, ItemDisplayContext.GUI, light, packedOverlay, pose, buffers,
                    cabinet.getLevel(), drawer);
            pose.popPose();

            // The count underneath
            String text = String.valueOf(cabinet.getCount(drawer));
            pose.pushPose();
            pose.translate(x, y - 0.11F, 0.503F);
            pose.scale(0.0055F, -0.0055F, 0.0055F);
            font.drawInBatch(text, -font.width(text) / 2.0F, 0, 0xFFFFFFFF, false, pose.last().pose(), buffers,
                    Font.DisplayMode.NORMAL, 0, light);
            pose.popPose();
        }
        pose.popPose();
    }
}
