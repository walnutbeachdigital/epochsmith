package com.walnutbeachdigital.epochsmith.item;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import com.walnutbeachdigital.epochsmith.Epochsmith;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The special powers of steam and electric gear:
 *  - Steam Drill / Electric Drill: mine a 3x3 square facing the side you hit (1 power per extra block).
 *  - Pneumatic Axe / Chainsaw: fell every connected log, up to 64 (1 power per extra log).
 *  - Steam Boots: Speed while they hold steam (1 steam per second of sprinting).
 *  - Coil Chestplate: shocks melee attackers for 4 damage (5 charge per shock).
 * Sneaking switches the drills and axes back to one block at a time.
 */
@EventBusSubscriber(modid = Epochsmith.MODID)
public final class PoweredToolEvents {

    private static final int MAX_TREE = 64;
    private static final int SHOCK_COST = 5;
    private static final float SHOCK_DAMAGE = 4.0F;
    /** Stops the extra blocks we break from setting this off again. */
    private static boolean breaking = false;

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (breaking || !(event.getPlayer() instanceof ServerPlayer player) || player.isShiftKeyDown()) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        if (!PoweredTools.hasPower(tool)) {
            return;
        }
        // Tree felling comes first, so a multitool in area mode fells trees instead of cutting 3x3 through them
        if (tool.getItem() instanceof PoweredTools.TreeFeller feller && feller.fellsTrees(tool)
                && event.getState().is(BlockTags.LOGS)) {
            fellTree(player, tool, event.getPos());
        } else if (tool.getItem() instanceof PoweredTools.AreaDrill drill && drill.drillsArea(tool)) {
            drill(player, tool, event.getPos());
        } else if (tool.getItem() instanceof PoweredTools.TunnelBorer) {
            tunnel(player, tool, event.getPos());
        }
    }

    /** Laser cutter: also cuts the two blocks behind the one you mined, straight ahead. */
    private static void tunnel(ServerPlayer player, ItemStack tool, BlockPos center) {
        Direction face = faceHit(player, center);
        if (face == null) {
            return;
        }
        Level level = player.level();
        for (int depth = 1; depth <= 2; depth++) {
            BlockPos pos = center.relative(face.getOpposite(), depth);
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (state.isAir() || hardness < 0 || !tool.isCorrectToolForDrops(state)) {
                return;
            }
            if (tool.isEmpty() || !PoweredTools.usePower(tool)) {
                return;
            }
            breakAsPlayer(player, pos);
        }
    }

    private static void drill(ServerPlayer player, ItemStack tool, BlockPos center) {
        Direction face = faceHit(player, center);
        if (face == null) {
            return;
        }
        Level level = player.level();
        float centerHardness = level.getBlockState(center).getDestroySpeed(level, center);
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                BlockPos pos = switch (face.getAxis()) {
                    case X -> center.offset(0, a, b);
                    case Y -> center.offset(a, 0, b);
                    case Z -> center.offset(a, b, 0);
                };
                BlockState state = level.getBlockState(pos);
                float hardness = state.getDestroySpeed(level, pos);
                // Skip air, unbreakable blocks, wrong-tool blocks, and anything much harder than what you aimed at
                if (state.isAir() || hardness < 0 || hardness > centerHardness + 2.0F || !tool.isCorrectToolForDrops(state)) {
                    continue;
                }
                if (tool.isEmpty() || !PoweredTools.usePower(tool)) {
                    return;
                }
                breakAsPlayer(player, pos);
            }
        }
    }

    private static void fellTree(ServerPlayer player, ItemStack tool, BlockPos start) {
        Level level = player.level();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        int felled = 0;
        while (!queue.isEmpty() && felled < MAX_TREE) {
            BlockPos pos = queue.poll();
            for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (seen.contains(next) || !level.getBlockState(next).is(BlockTags.LOGS)) {
                    continue;
                }
                BlockPos log = next.immutable();
                seen.add(log);
                if (tool.isEmpty() || !PoweredTools.usePower(tool)) {
                    return;
                }
                breakAsPlayer(player, log);
                queue.add(log);
                felled++;
            }
        }
    }

    /** Breaks a block exactly as if the player had mined it (drops, tool wear, protections). */
    private static void breakAsPlayer(ServerPlayer player, BlockPos pos) {
        breaking = true;
        try {
            player.gameMode.destroyBlock(pos);
        } finally {
            breaking = false;
        }
    }

    /** Which side of the block the player is looking at, or null if they aren't looking at it. */
    private static Direction faceHit(ServerPlayer player, BlockPos pos) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange() + 1.0));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));
        return hit.getBlockPos().equals(pos) ? hit.getDirection() : null;
    }

    /** Steam boots: Speed while they hold steam; steam is used only while sprinting. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().getGameTime() % 20 != 0) {
            return;
        }
        // Composite helmet: the HUD uses one charge every ten seconds
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.getItem() instanceof CompositeHelmetItem && player.level().getGameTime() % 200 == 0) {
            ElectricPowered.use(helmet, 1);
        }
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!(boots.getItem() instanceof SteamBootsItem) || SteamPowered.getSteam(boots) <= 0) {
            return;
        }
        if (player.isSprinting()) {
            SteamPowered.use(boots, 1);
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false, true));
    }

    /** Coil chestplate: shock whoever hits the wearer in melee. */
    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        ItemStack chest = victim.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof CoilChestplateItem)) {
            return;
        }
        Entity direct = event.getSource().getDirectEntity();
        if (!(direct instanceof LivingEntity attacker) || direct != event.getSource().getEntity() || attacker == victim) {
            return;
        }
        if (!ElectricPowered.use(chest, SHOCK_COST)) {
            return;
        }
        attacker.hurt(victim.damageSources().thorns(victim), SHOCK_DAMAGE);
        if (victim.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, attacker.getX(), attacker.getY() + attacker.getBbHeight() / 2,
                    attacker.getZ(), 12, 0.3, 0.4, 0.3, 0.2);
        }
        victim.level().playSound(null, victim.blockPosition(), SoundEvents.COPPER_BULB_TURN_ON, SoundSource.PLAYERS, 1.0F, 1.6F);
    }

    private PoweredToolEvents() {
    }
}
