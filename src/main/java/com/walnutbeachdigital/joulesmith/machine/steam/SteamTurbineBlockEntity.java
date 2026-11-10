package com.walnutbeachdigital.joulesmith.machine.steam;

import java.util.List;

import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.registry.JSFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Turns steam into power: 2 FE for each mB, up to 400 FE/t (200 mB a tick, one fuel-burning Boiler's worth).
 * Pushes its power out of every face set to give out.
 */
public class SteamTurbineBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 100_000, OUTPUT = 800, MAX_STEAM = 200, FE_PER_MB = 2, TANK = 16_000;

    private final FluidTank steam = new FluidTank(TANK, stack -> stack.is(JSFluids.STEAM.source.get())) {
        @Override protected void onContentsChanged() { setChanged(); }
    };

    public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.STEAM_TURBINE.get(), pos, state, List.of(), CAPACITY, 0, OUTPUT);
    }

    @Override
    public List<TankDef> tanks() {
        return List.of(new TankDef(steam, 80));
    }

    @Override
    protected FluidTank inputTank() {
        return steam;
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.NONE;
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        int room = (energy.getMaxEnergyStored() - energy.getEnergyStored()) / FE_PER_MB;
        int used = allowed ? Math.min(MAX_STEAM, Math.min(steam.getFluidAmount(), room)) : 0;
        if (used > 0) {
            steam.drain(used, IFluidHandler.FluidAction.EXECUTE);
            energy.generate(used * FE_PER_MB);
        }
        pushEnergy(level, pos, OUTPUT);
        return used > 0;
    }
}
