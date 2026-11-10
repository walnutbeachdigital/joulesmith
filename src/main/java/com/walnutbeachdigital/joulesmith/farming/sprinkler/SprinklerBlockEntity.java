package com.walnutbeachdigital.joulesmith.farming.sprinkler;

import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Once a second, using {@link #WATER_PER_SECOND} mB of water, it keeps the farmland around it wet and
 * gives {@link #TICKS_PER_SECOND} extra growth ticks to random plants in the 9x9 around it (one block up
 * or down too): crops grow several times faster.
 */
public class SprinklerBlockEntity extends BlockEntity {
    public static final int TANK = 4_000, WATER_PER_SECOND = 20, RADIUS = 4, TICKS_PER_SECOND = 16;

    private final FluidTank tank = new FluidTank(TANK, stack -> stack.getFluid().isSame(Fluids.WATER)) {
        @Override protected void onContentsChanged() { setChanged(); }
    };

    public SprinklerBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.SPRINKLER.get(), pos, state);
    }

    public IFluidHandler fluidHandler() {
        return tank;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SprinklerBlockEntity sprinkler) {
        if (level.getGameTime() % 20 != 0 || !(level instanceof ServerLevel server)) return;
        if (sprinkler.tank.getFluidAmount() < WATER_PER_SECOND) return;
        sprinkler.tank.drain(WATER_PER_SECOND, IFluidHandler.FluidAction.EXECUTE);
        var random = level.random;
        for (int i = 0; i < TICKS_PER_SECOND; i++) {
            BlockPos p = pos.offset(random.nextInt(RADIUS * 2 + 1) - RADIUS, random.nextInt(3) - 1, random.nextInt(RADIUS * 2 + 1) - RADIUS);
            BlockState plant = level.getBlockState(p);
            if (plant.is(BlockTags.CROPS) || plant.is(BlockTags.SAPLINGS) || plant.is(net.minecraft.world.level.block.Blocks.SUGAR_CANE)
                    || plant.is(net.minecraft.world.level.block.Blocks.CACTUS) || plant.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH)
                    || plant.is(net.minecraft.world.level.block.Blocks.BAMBOO) || plant.is(net.minecraft.world.level.block.Blocks.NETHER_WART)) {
                if (plant.isRandomlyTicking()) plant.randomTick(server, p, random);
            }
        }
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dy = -2; dy <= 0; dy++) {
                    BlockPos p = pos.offset(dx, dy, dz);
                    BlockState soil = level.getBlockState(p);
                    if (soil.getBlock() instanceof FarmBlock && soil.getValue(FarmBlock.MOISTURE) < FarmBlock.MAX_MOISTURE) {
                        level.setBlock(p, soil.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE), 2);
                    }
                }
            }
        }
        server.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 12, 1.2, 0.2, 1.2, 0.1);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
    }
}
