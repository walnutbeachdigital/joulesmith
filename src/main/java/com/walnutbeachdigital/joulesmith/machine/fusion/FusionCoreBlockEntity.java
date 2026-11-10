package com.walnutbeachdigital.joulesmith.machine.fusion;

import java.util.List;

import com.walnutbeachdigital.joulesmith.JSConfig;
import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.SideMode;
import com.walnutbeachdigital.joulesmith.machine.MachineFace;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.registry.JSFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * The Fusion Core burns deuterium and tritium (5 mB of each a tick) for 20,000 FE/t and makes 2 mB of
 * plasma a tick for the Plasma Refinery. Starting it takes 500,000 FE of stored power; once lit it runs for
 * as long as it has fuel, and goes out (to be lit again) when either runs dry. If the plasma tank is
 * full the extra plasma is vented harmlessly.
 */
public class FusionCoreBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 10_000_000, MAX_INPUT = 100_000, OUTPUT = 40_000, TANK = 16_000,
            IGNITION = 500_000, FUEL_PER_TICK = 5, PLASMA_PER_TICK = 2;
    public static final int LIT = 6, NEEDS_IGNITION = 7, NEEDS_FUEL = 8;

    private final FluidTank deuterium = tank(TANK, JSFluids.DEUTERIUM);
    private final FluidTank tritium = tank(TANK, JSFluids.TRITIUM);
    private final FluidTank plasma = tank(TANK, JSFluids.PLASMA);
    private boolean lit;
    private int status = NEEDS_FUEL;

    private FluidTank tank(int capacity, JSFluids.Entry fluid) {
        return new FluidTank(capacity, stack -> stack.getFluid().isSame(fluid.source.get())) {
            @Override protected void onContentsChanged() { setChanged(); }
        };
    }

    public FusionCoreBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.FUSION_CORE.get(), pos, state, List.of(), CAPACITY, MAX_INPUT, OUTPUT);
    }

    @Override
    public List<TankDef> tanks() {
        return List.of(new TankDef(deuterium, 30), new TankDef(tritium, 52), new TankDef(plasma, 140));
    }

    @Override
    protected FluidTank inputTank() {
        return deuterium;
    }

    @Override
    protected FluidTank outputTank() {
        return plasma;
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.NONE;
    }

    @Override
    public int status() {
        return status;
    }

    /** Pipes: deuterium and tritium go into their own tanks; plasma comes out. */
    @Override
    public IFluidHandler fluidHandler(Direction side) {
        SideMode mode = side == null ? SideMode.BOTH : sideMode(MachineFace.of(facing(), side));
        if (mode == SideMode.NONE) return null;
        boolean fill = mode.accepts(), drain = mode.gives();
        return new IFluidHandler() {
            @Override public int getTanks() { return 3; }
            private FluidTank at(int i) { return i == 0 ? deuterium : i == 1 ? tritium : plasma; }
            @Override public FluidStack getFluidInTank(int i) { return at(i).getFluid(); }
            @Override public int getTankCapacity(int i) { return at(i).getCapacity(); }
            @Override public boolean isFluidValid(int i, FluidStack stack) { return fill && i < 2 && at(i).isFluidValid(stack); }
            @Override public int fill(FluidStack resource, FluidAction action) {
                if (!fill) return 0;
                if (deuterium.isFluidValid(resource)) return deuterium.fill(resource, action);
                if (tritium.isFluidValid(resource)) return tritium.fill(resource, action);
                return 0;
            }
            @Override public FluidStack drain(FluidStack resource, FluidAction action) { return drain ? plasma.drain(resource, action) : FluidStack.EMPTY; }
            @Override public FluidStack drain(int max, FluidAction action) { return drain ? plasma.drain(max, action) : FluidStack.EMPTY; }
        };
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        boolean fuelled = deuterium.getFluidAmount() >= FUEL_PER_TICK && tritium.getFluidAmount() >= FUEL_PER_TICK;
        if (!fuelled || !allowed) lit = false;
        if (!lit && allowed && fuelled && energy.getEnergyStored() >= IGNITION) {
            energy.consume(IGNITION);
            lit = true;
        }
        boolean running = false;
        if (lit) {
            deuterium.drain(FUEL_PER_TICK, IFluidHandler.FluidAction.EXECUTE);
            tritium.drain(FUEL_PER_TICK, IFluidHandler.FluidAction.EXECUTE);
            energy.generate(JSConfig.get(JSConfig.FUSION));
            plasma.fill(new FluidStack(JSFluids.PLASMA.source.get(), PLASMA_PER_TICK), IFluidHandler.FluidAction.EXECUTE);
            running = true;
        }
        status = lit ? LIT : !fuelled ? NEEDS_FUEL : allowed ? NEEDS_IGNITION : 2;
        pushEnergy(level, pos, OUTPUT);
        return running;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Lit", lit);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lit = tag.getBoolean("Lit");
    }
}
