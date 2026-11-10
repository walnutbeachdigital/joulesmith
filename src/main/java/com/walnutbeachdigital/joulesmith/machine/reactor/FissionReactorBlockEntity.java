package com.walnutbeachdigital.joulesmith.machine.reactor;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.JSConfig;
import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.registry.JSBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * The Fission Reactor's controller. It sits in the middle of one face of a 3x3x3 shell of Reactor Casing,
 * Reactor Glass and Reactor Ports, with the middle of the shell left empty. Each uranium ingot burns for
 * two minutes at 1,000 FE/t. Fission makes heat, which water carries away (1 mB per point of heat); if
 * the water runs out the reactor heats up, and at 100% it shuts itself down (SCRAM) until it has cooled
 * to 20%. It never explodes and never damages the world.
 */
public class FissionReactorBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 1_000_000, OUTPUT = 8_000, TANK = 16_000, BURN_TIME = 2_400,
            MAX_HEAT = 1_000, HEAT_PER_TICK = 10, RESTART_HEAT = 200;
    public static final TagKey<Item> FUEL = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/uranium"));

    /** Status lines (gui.joulesmith.status.N). */
    public static final int RUNNING = 1, IDLE = 2, INCOMPLETE = 3, SCRAM = 4, NO_WATER = 5;

    private final FluidTank water = new FluidTank(TANK, stack -> stack.getFluid().is(FluidTags.WATER)) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private int heat, status = INCOMPLETE;
    private boolean formed, scrammed;
    private final List<BlockPos> ports = new ArrayList<>();

    public FissionReactorBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.FISSION_REACTOR.get(), pos, state, List.of(SlotDef.input(80, 53)), CAPACITY, 0, OUTPUT);
    }

    @Override
    public List<TankDef> tanks() {
        return List.of(new TankDef(water, 120));
    }

    @Override
    protected FluidTank inputTank() {
        return water;
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return stack.is(FUEL);
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.FLAME;
    }

    @Override
    public int heatPercent() {
        return heat * 100 / MAX_HEAT;
    }

    @Override
    public int status() {
        return status;
    }

    public boolean formed() {
        return formed;
    }

    public FluidTank water() {
        return water;
    }

    // ------------------------------------------------------------------ the structure

    /** The middle of the 3x3x3 shell: one block behind the controller. */
    private BlockPos core() {
        return worldPosition.relative(facing().getOpposite());
    }

    /** Checks the shell and links its ports to this controller. */
    private void checkStructure(Level level) {
        BlockPos core = core();
        List<BlockPos> found = new ArrayList<>();
        boolean ok = level.getBlockState(core).isAir();
        for (int dx = -1; dx <= 1 && ok; dx++) {
            for (int dy = -1; dy <= 1 && ok; dy++) {
                for (int dz = -1; dz <= 1 && ok; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos p = core.offset(dx, dy, dz);
                    if (p.equals(worldPosition)) continue;
                    BlockState state = level.getBlockState(p);
                    if (state.is(JSBlocks.REACTOR_PORT.get())) found.add(p);
                    else if (!state.is(JSBlocks.REACTOR_CASING.get()) && !state.is(JSBlocks.REACTOR_GLASS.get())) ok = false;
                }
            }
        }
        if (!ok) found.clear();
        if (ok != formed || !found.equals(ports)) {
            for (BlockPos p : ports) if (!found.contains(p)) link(level, p, null);
            for (BlockPos p : found) link(level, p, worldPosition);
            ports.clear();
            ports.addAll(found);
            formed = ok;
            setChanged();
        }
    }

    private static void link(Level level, BlockPos port, BlockPos controller) {
        if (level.getBlockEntity(port) instanceof ReactorPortBlockEntity be) be.setController(controller);
    }

    /** Whether a port at {@code p} is part of this reactor right now. */
    public boolean hasPort(BlockPos p) {
        return formed && ports.contains(p);
    }

    // ------------------------------------------------------------------ running

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        if (level.getGameTime() % 20 == 0 || (!formed && level.getGameTime() % 5 == 0)) checkStructure(level);
        boolean running = false;
        if (scrammed && heat <= RESTART_HEAT) scrammed = false;
        if (formed && allowed && !scrammed) {
            if (progress <= 0 && energy.getEnergyStored() < energy.getMaxEnergyStored()) {
                ItemStack fuel = items.getStackInSlot(0);
                if (fuel.is(FUEL)) {
                    fuel.shrink(1);
                    progress = maxProgress = BURN_TIME;
                    setChanged();
                }
            }
            if (progress > 0 && energy.getEnergyStored() < energy.getMaxEnergyStored()) {
                progress--;
                energy.generate(JSConfig.get(JSConfig.FISSION));
                heat += HEAT_PER_TICK;
                running = true;
                setChanged();
            }
        }
        // water carries heat away, 1 mB per point; a little leaks away on its own
        if (heat > 0) {
            int cooled = water.drain(heat, IFluidHandler.FluidAction.EXECUTE).getAmount();
            heat = Math.max(0, heat - cooled - 1);
        }
        if (heat >= MAX_HEAT) {
            heat = MAX_HEAT;
            scrammed = true;
        }
        status = !formed ? INCOMPLETE : scrammed ? SCRAM : running && water.isEmpty() ? NO_WATER : running ? RUNNING : IDLE;
        int sent = pushEnergy(level, pos, OUTPUT);
        for (BlockPos port : ports) {                       // ports hand power to whatever touches them outside
            if (sent >= OUTPUT || energy.getEnergyStored() == 0) break;
            for (Direction dir : Direction.values()) {
                BlockPos out = port.relative(dir);
                if (isPartOfShell(out)) continue;
                IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, out, dir.getOpposite());
                if (target != null && target.canReceive()) {
                    int taken = target.receiveEnergy(Math.min(OUTPUT - sent, energy.getEnergyStored()), false);
                    energy.consume(taken);
                    sent += taken;
                }
            }
        }
        return running;
    }

    private boolean isPartOfShell(BlockPos p) {
        BlockPos c = core();
        return Math.abs(p.getX() - c.getX()) <= 1 && Math.abs(p.getY() - c.getY()) <= 1 && Math.abs(p.getZ() - c.getZ()) <= 1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Heat", heat);
        tag.putBoolean("Scram", scrammed);
        tag.putInt("BurnTotal", maxProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heat = tag.getInt("Heat");
        scrammed = tag.getBoolean("Scram");
        maxProgress = tag.getInt("BurnTotal");
    }
}
