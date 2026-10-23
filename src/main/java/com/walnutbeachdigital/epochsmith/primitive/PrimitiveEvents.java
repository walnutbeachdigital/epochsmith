package com.walnutbeachdigital.epochsmith.primitive;

import com.walnutbeachdigital.epochsmith.Epochsmith;
import com.walnutbeachdigital.epochsmith.era.Era;
import com.walnutbeachdigital.epochsmith.era.EraManager;
import com.walnutbeachdigital.epochsmith.item.ModItems;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Primitive era world interactions.
 */
@EventBusSubscriber(modid = Epochsmith.MODID)
public final class PrimitiveEvents {

    /** Chance that breaking grass or a fern drops plant fiber (30%). */
    private static final float FIBER_CHANCE = 0.3F;

    /**
     * Breaking grass or ferns sometimes drops plant fiber, but only for players
     * who have started Epochsmith, so normal vanilla play is untouched.
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        BlockState state = event.getState();
        if (player.isCreative() || !(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        boolean isGrass = state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN);
        if (isGrass && EraManager.hasReached(player, Era.PRIMITIVE) && level.random.nextFloat() < FIBER_CHANCE) {
            Block.popResource(level, event.getPos(), new ItemStack(ModItems.PLANT_FIBER.get()));
        }
    }

    private PrimitiveEvents() {
    }
}
