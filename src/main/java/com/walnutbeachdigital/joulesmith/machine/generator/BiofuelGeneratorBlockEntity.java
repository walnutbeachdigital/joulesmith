package com.walnutbeachdigital.joulesmith.machine.generator;

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
 * Burns Biofuel from its tank for 160 FE/t, using 1 mB a tick (a bucket runs it for 50 seconds). Fill it
 * with a bucket or a fluid pipe; it pushes its power out of every face set to give out.
 */
public class BiofuelGeneratorBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 40_000, OUTPUT = 320, GENERATION = 160, MB_PER_TICK = 1, TANK = 8_000;

    private final FluidTank tank = new FluidTank(TANK, stack -> stack.is(JSFluids.BIOFUEL.source.get())) {
        @Override protected void onContentsChanged() { setChanged(); }
    };

    public BiofuelGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.BIOFUEL_GENERATOR.get(), pos, state, List.of(), CAPACITY, 0, OUTPUT);
    }

    @Override
    public List<TankDef> tanks() {
        return List.of(new TankDef(tank, 80));
    }

    @Override
    protected FluidTank inputTank() {
        return tank;
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.NONE;
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        boolean burning = false;
        if (allowed && tank.getFluidAmount() >= MB_PER_TICK && energy.getEnergyStored() + GENERATION <= energy.getMaxEnergyStored()) {
            tank.drain(MB_PER_TICK, IFluidHandler.FluidAction.EXECUTE);
            energy.generate(GENERATION);
            burning = true;
        }
        pushEnergy(level, pos, OUTPUT);
        return burning;
    }
}
