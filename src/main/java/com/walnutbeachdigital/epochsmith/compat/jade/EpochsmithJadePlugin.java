package com.walnutbeachdigital.epochsmith.compat.jade;

import com.walnutbeachdigital.epochsmith.Epochsmith;
import com.walnutbeachdigital.epochsmith.block.StatusDescriber;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade support: looking at an Epochsmith machine shows its live status (the same line you get
 * by right-clicking it), worked out on the server so it's always up to date.
 * Energy and item storage are shown by Jade on its own, through the standard capabilities.
 * Jade only loads this class when Jade is installed.
 */
@WailaPlugin
public class EpochsmithJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(MachineStatus.INSTANCE, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(MachineStatus.INSTANCE, Block.class);
    }

    /** Sends a machine's status line from the server and shows it in the Jade tooltip. */
    public enum MachineStatus implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Epochsmith.MODID, "machine_status");
        private static final String KEY = "EpochsmithStatus";

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof StatusDescriber machine) {
                data.putString(KEY, Component.Serializer.toJson(machine.describe(), accessor.getLevel().registryAccess()));
            }
        }

        @Override
        public boolean shouldRequestData(BlockAccessor accessor) {
            return accessor.getBlockEntity() instanceof StatusDescriber;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.contains(KEY)) {
                Component status = Component.Serializer.fromJson(data.getString(KEY), accessor.getLevel().registryAccess());
                if (status != null) {
                    tooltip.add(status);
                }
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
