package com.walnutbeachdigital.epochsmith.steam;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.epochsmith.multiblock.MultiblockPartBlock;

import net.minecraft.world.level.block.Block;

/** Part of the steam engine. Steam pipes join onto it; once the engine is built it carries steam to the engine. */
public class SteamCylinderBlock extends MultiblockPartBlock implements SteamConnectable {

    public static final MapCodec<SteamCylinderBlock> CODEC = simpleCodec(SteamCylinderBlock::new);

    public SteamCylinderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
