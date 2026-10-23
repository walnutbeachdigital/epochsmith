package com.walnutbeachdigital.epochsmith.steam;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import com.walnutbeachdigital.epochsmith.Epochsmith;
import com.walnutbeachdigital.epochsmith.item.PneumaticAxeItem;
import com.walnutbeachdigital.epochsmith.item.SteamBootsItem;
import com.walnutbeachdigital.epochsmith.item.SteamDrillItem;
import com.walnutbeachdigital.epochsmith.item.SteamPowered;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The special powers of steam gear:
 *  - Steam Drill: mines a 3x3 square facing the side you hit (1 steam per extra block).
 *  - Pneumatic Axe: fells every connected log, up to 64 (1 steam per extra log).
 *  - Steam Boots: Speed while they hold steam (1 steam per second of sprinting).
 * Sneaking switches the drill and axe back to one block at a time.
 */
@EventBusSubscriber(modid = Epochsmith.MODID)
public final class SteamToolEvents {

    private static final int MAX_TREE = 64;
    /** Stops the extra blocks we break from setting this off again. */
    private static boolean breaking = false;

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (breaking || !(event.getPlayer() instanceof ServerPlayer player) || player.isShiftKeyDown()) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        if (SteamPowered.getSteam(tool) <= 0) {
            return;
        }
        if (tool.getItem() instanceof SteamDrillItem) {
            drill(player, tool, event.getPos());
        } else if (tool.getItem() instanceof PneumaticAxeItem && event.getState().is(BlockTags.LOGS)) {
            fellTree(player, tool, event.getPos());
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
                if (tool.isEmpty() || !SteamPowered.use(tool, 1)) {
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
                if (tool.isEmpty() || !SteamPowered.use(tool, 1)) {
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
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!(boots.getItem() instanceof SteamBootsItem) || SteamPowered.getSteam(boots) <= 0) {
            return;
        }
        if (player.isSprinting()) {
            SteamPowered.use(boots, 1);
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false, true));
    }

    private SteamToolEvents() {
    }
}
