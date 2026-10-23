package com.walnutbeachdigital.epochsmith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.walnutbeachdigital.epochsmith.power.KineticBlockEntity;
import com.walnutbeachdigital.epochsmith.power.Shaft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws axles, wheels, windmills and cranks turning around their axis.
 * Faster force means faster spin.
 */
public class KineticRenderer implements BlockEntityRenderer<KineticBlockEntity> {

    /** Degrees turned per tick for each point of force. */
    private static final float DEGREES_PER_FORCE = 2.0F;

    public KineticRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(KineticBlockEntity kinetic, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        BlockState state = kinetic.getBlockState();
        if (!(state.getBlock() instanceof Shaft shaft) || kinetic.getLevel() == null) {
            return;
        }
        float angle = ((kinetic.getLevel().getGameTime() + partialTick) * kinetic.getSpeed() * DEGREES_PER_FORCE) % 360.0F;

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(switch (shaft.getAxis(state)) {
            case X -> Axis.XP.rotationDegrees(angle);
            case Y -> Axis.YP.rotationDegrees(angle);
            case Z -> Axis.ZP.rotationDegrees(angle);
        });
        pose.translate(-0.5, -0.5, -0.5);

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        dispatcher.getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()), state,
                dispatcher.getBlockModel(state), 1.0F, 1.0F, 1.0F, packedLight, packedOverlay);
        pose.popPose();
    }
}
