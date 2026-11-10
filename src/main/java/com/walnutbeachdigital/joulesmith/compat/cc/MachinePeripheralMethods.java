package com.walnutbeachdigital.joulesmith.compat.cc;

import java.util.Locale;

import com.walnutbeachdigital.joulesmith.machine.MachineBlock;
import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.RedstoneMode;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;

/**
 * Lua methods every Joulesmith machine gets when a ComputerCraft computer or wired modem touches it, on top
 * of CC's own inventory, tank and energy methods:
 * <pre>
 *   local m = peripheral.wrap("left")
 *   print(m.getProgress(), m.isRunning())
 *   m.setRedstoneMode("never")   -- "ignored", "high", "low", "never" or "pulse"
 * </pre>
 */
public final class MachinePeripheralMethods implements GenericPeripheral {
    @Override
    public String id() {
        return "joulesmith:machine";
    }

    /** How far the current job is, from 0 to 1 (0 when idle). */
    @LuaFunction(mainThread = true)
    public double getProgress(MachineBlockEntity machine) {
        int max = machine.value(3);
        return max <= 0 ? 0 : (double) machine.value(2) / max;
    }

    /** Whether the machine is working right now. */
    @LuaFunction(mainThread = true)
    public boolean isRunning(MachineBlockEntity machine) {
        var state = machine.getBlockState();
        return state.hasProperty(MachineBlock.LIT) && state.getValue(MachineBlock.LIT);
    }

    /** FE per tick made and used, averaged over the last second. */
    @LuaFunction(mainThread = true)
    public int getEnergyMade(MachineBlockEntity machine) {
        return machine.value(MachineBlockEntity.MADE);
    }

    @LuaFunction(mainThread = true)
    public int getEnergyUsed(MachineBlockEntity machine) {
        return machine.value(MachineBlockEntity.USED);
    }

    @LuaFunction(mainThread = true)
    public String getRedstoneMode(MachineBlockEntity machine) {
        return RedstoneMode.values()[machine.value(4)].name().toLowerCase(Locale.ROOT);
    }

    @LuaFunction(mainThread = true)
    public void setRedstoneMode(MachineBlockEntity machine, String mode) throws LuaException {
        try {
            machine.setRedstoneMode(RedstoneMode.valueOf(mode.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            throw new LuaException("Unknown redstone mode '" + mode + "': use ignored, high, low, never or pulse");
        }
    }
}
