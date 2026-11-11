package com.walnutbeachdigital.joulesmith.compat.top;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Function;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.energy.solar.SolarPanelBlock;
import com.walnutbeachdigital.joulesmith.energy.solar.SolarPanelBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.RedstoneMode;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

/**
 * The One Probe support. The probe already shows any block's energy and fluids through NeoForge's
 * capabilities; this adds a machine's job progress and redstone setting, and a Solar Panel's output.
 * It talks to the probe by reflection, so Joulesmith needs nothing from The One Probe to build or run.
 */
public final class TopCompat {
    private static final ResourceLocation ID = Joulesmith.id("machines");

    public static void enqueue(InterModEnqueueEvent event) {
        if (!ModList.get().isLoaded("theoneprobe")) return;
        InterModComms.sendTo("theoneprobe", "getTheOneProbe", () -> (Function<Object, Void>) TopCompat::register);
    }

    private static Void register(Object probe) {
        try {
            Class<?> providerType = Class.forName("mcjty.theoneprobe.api.IProbeInfoProvider");
            Object provider = Proxy.newProxyInstance(TopCompat.class.getClassLoader(), new Class<?>[] {providerType}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getID": return ID;
                    case "addProbeInfo": addInfo(args[1], (Level) args[3], args[5]); return null;
                    case "hashCode": return System.identityHashCode(proxy);
                    case "equals": return proxy == args[0];
                    case "toString": return "Joulesmith probe info";
                    default: return null;
                }
            });
            probe.getClass().getMethod("registerProvider", providerType).invoke(probe, provider);
        } catch (ReflectiveOperationException | LinkageError e) {
            Joulesmith.LOGGER.warn("Could not register with The One Probe", e);
        }
        return null;
    }

    /** {@code info} is an IProbeInfo, {@code data} an IProbeHitData. */
    private static void addInfo(Object info, Level level, Object data) throws ReflectiveOperationException {
        // look methods up on the API interfaces: the implementing classes may not be public
        Class<?> infoType = Class.forName("mcjty.theoneprobe.api.IProbeInfo");
        BlockPos pos = (BlockPos) Class.forName("mcjty.theoneprobe.api.IProbeHitData").getMethod("getPos").invoke(data);
        Method text = infoType.getMethod("text", Component.class);
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            int progress = machine.value(2), max = machine.value(3);
            if (max > 0 && progress > 0) {
                infoType.getMethod("progress", int.class, int.class).invoke(info, progress * 100 / max, 100);
            }
            int made = machine.value(MachineBlockEntity.MADE), used = machine.value(MachineBlockEntity.USED);
            if (made > 0) text.invoke(info, Component.translatable("gui.joulesmith.energy.making", String.format("%,d", made)));
            if (used > 0) text.invoke(info, Component.translatable("gui.joulesmith.energy.using", String.format("%,d", used)));
            int mode = machine.value(4);
            if (mode != 0) text.invoke(info, Component.translatable(RedstoneMode.values()[Math.floorMod(mode, RedstoneMode.values().length)].key()));
            if (machine.heatPercent() >= 0) text.invoke(info, Component.translatable("gui.joulesmith.heat", machine.heatPercent()));
            if (machine.status() > 0) text.invoke(info, Component.translatable("gui.joulesmith.status." + machine.status()));
        } else if (level.getBlockState(pos).getBlock() instanceof com.walnutbeachdigital.joulesmith.network.NetworkPart
                || level.getBlockState(pos).getBlock() instanceof com.walnutbeachdigital.joulesmith.energy.wireless.WirelessEnergyBlock) {
            for (Component line : com.walnutbeachdigital.joulesmith.network.NetworkInfo.lines(level, pos)) text.invoke(info, line);
        } else if (level.getBlockState(pos).getBlock() instanceof SolarPanelBlock panel) {
            text.invoke(info, Component.translatable("jade.joulesmith.making", SolarPanelBlockEntity.output(level, pos, panel.rate())));
        }
    }

    private TopCompat() {}
}
