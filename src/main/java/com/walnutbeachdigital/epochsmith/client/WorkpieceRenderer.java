package com.walnutbeachdigital.epochsmith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.walnutbeachdigital.epochsmith.block.WorkpieceHolder;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Draws the item being worked lying flat on top of a workstation (anvil, joiner's bench).
 */
public class WorkpieceRenderer<T extends BlockEntity & WorkpieceHolder> implements BlockEntityRenderer<T> {

    private final ItemRenderer itemRenderer;

    public WorkpieceRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(T station, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        ItemStack stack = station.getWorkpiece();
        if (stack.isEmpty()) {
            return;
        }
        BlockState state = station.getBlockState();
        pose.pushPose();
        pose.translate(0.5, 1.01, 0.5);
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            pose.mulPose(Axis.YP.rotationDegrees(-state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot()));
        }
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(0.45F, 0.45F, 0.45F);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, pose, buffers,
                station.getLevel(), 0);
        pose.popPose();
    }
}
