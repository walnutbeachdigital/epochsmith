package com.walnutbeachdigital.epochsmith.future;

import java.util.List;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.electric.ElectricNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/**
 * An anti-gravity lift. While powered (16 energy per tick), anything standing in the column
 * above it floats upward, up to 16 blocks. Sneak to drift gently back down.
 */
public class HoverPadBlockEntity extends BlockEntity {

    public static final int HEIGHT = 16;
    public static final int ENERGY_PER_TICK = 16;
    private static final int REFRESH_INTERVAL = 20;

    private List<BlockPos> sources = List.of();

    public HoverPadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HOVER_PAD.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HoverPadBlockEntity pad) {
        if (level.getGameTime() % REFRESH_INTERVAL == 0) {
            pad.sources = ElectricNetwork.findSources(level, pos);
        }
        boolean powered = ElectricNetwork.draw(level, pad.sources, ENERGY_PER_TICK, false) >= ENERGY_PER_TICK;
        if (state.getValue(BlockStateProperties.LIT) != powered) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, powered), Block.UPDATE_ALL);
        }
        if (!powered || level.getGameTime() % 5 != 0) {
            return;
        }
        // Levitation and slow falling are vanilla effects, so the game (and servers) already understand them
        AABB column = new AABB(pos.getX(), pos.getY() + 0.25, pos.getZ(), pos.getX() + 1, pos.getY() + 1 + HEIGHT, pos.getZ() + 1);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, column)) {
            if (entity.isShiftKeyDown()) {
                entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, false, false, false));
            } else {
                entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 10, 2, false, false, false));
            }
            entity.resetFallDistance();
        }
    }
}
