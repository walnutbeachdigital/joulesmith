package com.walnutbeachdigital.joulesmith.network;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.energy.wireless.WirelessEnergyBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What Jade and The One Probe show for the storage network's blocks and the wireless power blocks, worked
 * out on the server: whether the network is online, what a Drive Bay holds, what a CPU is crafting, a
 * bus's filter, a wireless block's channel.
 */
public final class NetworkInfo {
    public static List<Component> lines(Level level, BlockPos pos) {
        List<Component> lines = new ArrayList<>();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof WirelessEnergyBlock wireless) {
            lines.add(Component.translatable(wireless.transmitter() ? "info.joulesmith.wireless.transmitter" : "info.joulesmith.wireless.receiver",
                    Component.translatable("color.minecraft." + state.getValue(WirelessEnergyBlock.CHANNEL).getName())));
            return lines;
        }
        if (!(state.getBlock() instanceof NetworkPart) || state.getBlock() instanceof NetworkCableBlock) return lines;
        Network net = Network.scan(level, pos);
        lines.add(net.core == null ? Component.translatable("info.joulesmith.network.no_core")
                : net.online() ? Component.translatable("info.joulesmith.network.online", net.bays.size(), net.cpus.size())
                : Component.translatable("info.joulesmith.network.unpowered"));
        var be = level.getBlockEntity(pos);
        if (be instanceof DriveBayBlockEntity bay) {
            long items = 0, itemCap = 0, fluid = 0, fluidCap = 0;
            for (ItemStack d : bay.drives()) { items += DriveItem.stored(d); itemCap += ((DriveItem) d.getItem()).capacity(); }
            for (ItemStack d : bay.fluidDrives()) { fluid += FluidDriveItem.stored(d); fluidCap += ((FluidDriveItem) d.getItem()).capacity(); }
            if (itemCap > 0) lines.add(Component.translatable("info.joulesmith.bay.items", String.format("%,d", items), String.format("%,d", itemCap)));
            if (fluidCap > 0) lines.add(Component.translatable("info.joulesmith.bay.fluids", String.format("%,d", fluid), String.format("%,d", fluidCap)));
            if (itemCap == 0 && fluidCap == 0) lines.add(Component.translatable("info.joulesmith.bay.empty"));
        } else if (be instanceof CraftingCpuBlockEntity cpu) {
            lines.add(cpu.busy() ? Component.translatable("message.joulesmith.cpu.busy", cpu.target().getHoverName(), cpu.stepsLeft())
                    : Component.translatable("message.joulesmith.cpu.idle", cpu.patterns().size()));
        } else if (be instanceof BusBlockEntity bus) {
            lines.add(bus.filter().isEmpty() ? Component.translatable("info.joulesmith.bus.no_filter") : Component.translatable("info.joulesmith.bus.filter"));
        }
        return lines;
    }

    private NetworkInfo() {}
}
