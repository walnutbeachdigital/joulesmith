package com.walnutbeachdigital.joulesmith.compat.jade;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.energy.solar.SolarPanelBlock;
import com.walnutbeachdigital.joulesmith.energy.solar.SolarPanelBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.MachineBlock;
import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
 * Jade support. Jade already shows any block's energy, fluids and items through NeoForge's capabilities;
 * this adds what it can't see: how far along a machine's job is, its redstone setting, and a Solar Panel's
 * output right now. Only loaded when Jade is installed.
 */
@WailaPlugin
public class JoulesmithJadePlugin implements IWailaPlugin {
    static final ResourceLocation MACHINE = Joulesmith.id("machine");
    static final ResourceLocation SOLAR = Joulesmith.id("solar_panel");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(MachineProvider.INSTANCE, MachineBlockEntity.class);
        registration.registerBlockDataProvider(SolarProvider.INSTANCE, SolarPanelBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(MachineProvider.INSTANCE, MachineBlock.class);
        registration.registerBlockComponent(SolarProvider.INSTANCE, SolarPanelBlock.class);
    }

    enum MachineProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof MachineBlockEntity machine) {
                data.putInt("Progress", machine.value(2));
                data.putInt("MaxProgress", machine.value(3));
                data.putInt("Redstone", machine.value(4));
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            int max = data.getInt("MaxProgress"), progress = data.getInt("Progress");
            if (max > 0 && progress > 0) {
                tooltip.add(Component.translatable("gui.joulesmith.progress", progress * 100 / max));
            }
            if (data.contains("Redstone") && data.getInt("Redstone") != 0) {
                var modes = com.walnutbeachdigital.joulesmith.machine.RedstoneMode.values();
                tooltip.add(Component.translatable(modes[Math.floorMod(data.getInt("Redstone"), modes.length)].key()));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return MACHINE;
        }
    }

    enum SolarProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlock() instanceof SolarPanelBlock panel) {
                data.putInt("Making", SolarPanelBlockEntity.output(accessor.getLevel(), accessor.getPosition(), panel.rate()));
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            tooltip.add(Component.translatable("jade.joulesmith.making", accessor.getServerData().getInt("Making")));
        }

        @Override
        public ResourceLocation getUid() {
            return SOLAR;
        }
    }
}
