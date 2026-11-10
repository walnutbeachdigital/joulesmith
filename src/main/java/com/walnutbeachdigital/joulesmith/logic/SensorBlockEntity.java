package com.walnutbeachdigital.joulesmith.logic;

import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A sensor's last reading and signal, checked twice a second. */
public class SensorBlockEntity extends BlockEntity {
    private static final Measure[] PROXIMITY = {Measure.PLAYERS, Measure.HOSTILES, Measure.ANIMALS};

    private int reading;
    private int power;
    private int proximity;

    public SensorBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.SENSOR.get(), pos, state);
    }

    private SensorBlock.Kind kind() {
        return getBlockState().getBlock() instanceof SensorBlock block ? block.kind() : SensorBlock.Kind.INVENTORY;
    }

    public Measure measure() {
        return kind() == SensorBlock.Kind.PROXIMITY ? PROXIMITY[proximity] : kind().measure;
    }

    public void nextTarget() {
        proximity = (proximity + 1) % PROXIMITY.length;
        setChanged();
    }

    public int reading() {
        return reading;
    }

    public int power() {
        return power;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SensorBlockEntity sensor) {
        if (level.getGameTime() % 10 != 0) return;
        Measure measure = sensor.measure();
        sensor.reading = measure.read(level, pos, state.getValue(SensorBlock.FACING));
        int power = switch (sensor.kind()) {
            case TIME -> sensor.reading >= 6 && sensor.reading < 18 ? 15 : 0;           // daytime
            case PROXIMITY -> Math.min(15, sensor.reading);
            default -> measure.signal(sensor.reading);
        };
        if (power != sensor.power) {
            sensor.power = power;
            sensor.setChanged();
            if (state.getValue(SensorBlock.POWERED) != power > 0) {
                level.setBlock(pos, state.setValue(SensorBlock.POWERED, power > 0), Block.UPDATE_ALL);
            }
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Power", power);
        tag.putInt("Proximity", proximity);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getInt("Power");
        proximity = Math.floorMod(tag.getInt("Proximity"), PROXIMITY.length);
    }
}
