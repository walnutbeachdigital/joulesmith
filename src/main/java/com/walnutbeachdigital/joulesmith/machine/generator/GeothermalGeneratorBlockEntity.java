package com.walnutbeachdigital.joulesmith.machine.generator;

import java.util.List;

import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Makes up to 120 FE/t from heat. Every lava block touching it gives 20 FE/t for free (magma blocks 10,
 * fire and lit campfires 5); whatever that leaves short of 120 it makes by burning lava from its tank,
 * 1 mB a tick. So it runs on its own when built into a lava pool, and on piped lava anywhere else.
 */
public class GeothermalGeneratorBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 40_000, OUTPUT = 240, GENERATION = 120, TANK = 8_000;

    private final FluidTank tank = new FluidTank(TANK, stack -> stack.getFluid().isSame(Fluids.LAVA)) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private int heat = -1;

    public GeothermalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.GEOTHERMAL_GENERATOR.get(), pos, state, List.of(), CAPACITY, 0, OUTPUT);
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

    /** FE/t the blocks around it give. */
    private int surroundingHeat(Level level, BlockPos pos) {
        int total = 0;
        for (Direction dir : Direction.values()) {
            BlockPos p = pos.relative(dir);
            BlockState state = level.getBlockState(p);
            if (level.getFluidState(p).is(FluidTags.LAVA)) total += 20;
            else if (state.is(Blocks.MAGMA_BLOCK)) total += 10;
            else if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                    || (state.is(net.minecraft.tags.BlockTags.CAMPFIRES) && state.getValue(net.minecraft.world.level.block.CampfireBlock.LIT))) {
                total += 5;
            }
        }
        return Math.min(com.walnutbeachdigital.joulesmith.JSConfig.get(com.walnutbeachdigital.joulesmith.JSConfig.GEOTHERMAL), total);
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        if (heat < 0 || level.getGameTime() % 20 == 0) heat = surroundingHeat(level, pos);
        boolean running = false;
        if (allowed && energy.getEnergyStored() + com.walnutbeachdigital.joulesmith.JSConfig.get(com.walnutbeachdigital.joulesmith.JSConfig.GEOTHERMAL) <= energy.getMaxEnergyStored()) {
            int made = heat;
            if (made < com.walnutbeachdigital.joulesmith.JSConfig.get(com.walnutbeachdigital.joulesmith.JSConfig.GEOTHERMAL) && tank.getFluidAmount() >= 1) {
                tank.drain(1, IFluidHandler.FluidAction.EXECUTE);
                made = com.walnutbeachdigital.joulesmith.JSConfig.get(com.walnutbeachdigital.joulesmith.JSConfig.GEOTHERMAL);
            }
            if (made > 0) {
                energy.generate(made);
                running = true;
            }
        }
        pushEnergy(level, pos, OUTPUT);
        return running;
    }
}
