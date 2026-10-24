package com.walnutbeachdigital.epochsmith.item;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Mines from a distance. Right-click (or hold right-click) to fire a beam that breaks the block
 * you're looking at, up to 32 blocks away. Anything it drops is pulled back to you.
 * Mines anything a pickaxe or shovel can, at netherite level. 2 charge per block.
 */
public class MiningLaserItem extends Item implements ElectricPowered {

    public static final double RANGE = 32.0;
    public static final int COST = 2;
    private static final int COOLDOWN = 4;

    public MiningLaserItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0 || !state.is(ModItems.HAND_DRILL_MINEABLE) || state.is(ModTiers.NANO.getIncorrectBlocksForDrops())) {
            return InteractionResultHolder.fail(stack);
        }
        if (ElectricPowered.getCharge(stack) < COST) {
            player.displayClientMessage(Component.translatable("message.epochsmith.mining_laser.empty"), true);
            return InteractionResultHolder.fail(stack);
        }
        ElectricPowered.use(stack, COST);
        player.getCooldowns().addCooldown(this, COOLDOWN);

        // Draw the beam
        Vec3 target = Vec3.atCenterOf(pos);
        Vec3 step = target.subtract(eye).normalize().scale(0.5);
        Vec3 point = eye.add(player.getViewVector(1.0F).scale(0.8)).subtract(0, 0.2, 0);
        for (int i = 0; i < 64 && point.distanceToSqr(target) > 0.5; i++) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            point = point.add(step);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.4F, 2.0F);

        PoweredToolEvents.breakAsPlayer(server, pos);
        // Pull the drops back to the player
        for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.0))) {
            drop.setPos(player.getX(), player.getY() + 0.5, player.getZ());
            drop.setNoPickUpDelay();
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.epochsmith.mining_laser"));
        ElectricPowered.addTooltip(stack, lines);
    }
}
