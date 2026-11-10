package com.walnutbeachdigital.joulesmith.automation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.machine.SlotRole;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.registry.JSItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Sends out up to four Drones over everything within 24 blocks (8 up and down). Once a second each drone
 * fetches the nearest dropped item, or else harvests and replants one ripe crop or nether wart. What they
 * bring back goes in the hub's nine output slots. 2,000 FE a trip.
 */
public class DroneHubBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 400_000, MAX_INPUT = 20_000, RANGE = 24, HEIGHT = 8, ENERGY_PER_TRIP = 2_000,
            SCAN_PER_TICK = 256, DRONES = 4;

    private final ArrayDeque<BlockPos> ripe = new ArrayDeque<>();
    private int cursor;

    static List<SlotDef> layout() {
        List<SlotDef> slots = new ArrayList<>();
        for (int i = 0; i < DRONES; i++) slots.add(SlotDef.input(35 + (i % 2) * 18, 26 + (i / 2) * 18));
        slots.addAll(BlockPlacerBlockEntity.grid(98, SlotRole.OUTPUT));
        return slots;
    }

    public DroneHubBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.DRONE_HUB.get(), pos, state, layout(), CAPACITY, MAX_INPUT, 0, true);
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return stack.is(JSItems.DRONE.get());
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.NONE;
    }

    private int drones() {
        int n = 0;
        for (int i = 0; i < DRONES; i++) if (items.getStackInSlot(i).is(JSItems.DRONE.get())) n += items.getStackInSlot(i).getCount();
        return Math.min(DRONES, n);
    }

    private AABB area(BlockPos pos) {
        return new AABB(pos).inflate(RANGE, HEIGHT, RANGE);
    }

    /** Walks the area a slice at a time, noting ripe crops. */
    private void scan(Level level, BlockPos pos) {
        int side = RANGE * 2 + 1, layers = HEIGHT * 2 + 1, total = side * side * layers;
        for (int i = 0; i < SCAN_PER_TICK && ripe.size() < 64; i++) {
            cursor = (cursor + 1) % total;
            int x = cursor % side - RANGE, z = (cursor / side) % side - RANGE, y = cursor / (side * side) - HEIGHT;
            BlockPos p = pos.offset(x, y, z);
            if (!level.isLoaded(p)) continue;
            if (isRipe(level.getBlockState(p)) && !ripe.contains(p)) ripe.add(p.immutable());
        }
    }

    private static boolean isRipe(BlockState state) {
        return (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state))
                || (state.getBlock() instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= 3);
    }

    private static BlockState young(BlockState ripe) {
        if (ripe.getBlock() instanceof CropBlock crop) return crop.getStateForAge(0);
        return ripe.setValue(NetherWartBlock.AGE, 0);
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        int drones = drones();
        if (!allowed || drones == 0 || !(level instanceof ServerLevel server)) return false;
        scan(level, pos);
        if (level.getGameTime() % 20 != 0) return progress > 0;
        int trips = 0;
        List<ItemEntity> loose = new ArrayList<>(level.getEntitiesOfClass(ItemEntity.class, area(pos), ItemEntity::isAlive));
        loose.sort(Comparator.comparingDouble(e -> e.distanceToSqr(Vec3.atCenterOf(pos))));
        for (int d = 0; d < drones && energy.getEnergyStored() >= ENERGY_PER_TRIP; d++) {
            if (!loose.isEmpty()) {
                ItemEntity drop = loose.remove(0);
                List<ItemStack> rest = OutputHelper.store(items, slotDefs(), List.of(drop.getItem().copy()));
                if (!rest.isEmpty() && rest.get(0).getCount() == drop.getItem().getCount()) break;   // full
                if (rest.isEmpty()) drop.discard(); else drop.setItem(rest.get(0));
                trail(server, pos, drop.position());
            } else {
                BlockPos crop = ripe.poll();
                if (crop == null) break;
                BlockState state = level.getBlockState(crop);
                if (!isRipe(state)) { d--; continue; }
                List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, server, crop, null));
                ItemStack seed = new ItemStack(state.getBlock().asItem());
                for (ItemStack drop : drops) {
                    if (ItemStack.isSameItem(drop, seed)) { drop.shrink(1); break; }
                }
                drops.removeIf(ItemStack::isEmpty);
                List<ItemStack> rest = OutputHelper.store(items, slotDefs(), drops);
                rest.forEach(r -> Block.popResource(level, pos.above(), r));
                level.setBlock(crop, young(state), Block.UPDATE_ALL);
                level.levelEvent(2001, crop, Block.getId(state));
                trail(server, pos, Vec3.atCenterOf(crop));
            }
            energy.consume(ENERGY_PER_TRIP);
            trips++;
        }
        progress = trips;
        if (trips > 0) setChanged();
        return trips > 0;
    }

    private static void trail(ServerLevel level, BlockPos hub, Vec3 to) {
        Vec3 from = Vec3.atCenterOf(hub).add(0, 0.7, 0);
        Vec3 step = to.subtract(from);
        int n = Math.min(40, (int) (step.length() * 1.5));
        for (int i = 1; i <= n; i++) {
            Vec3 at = from.add(step.scale(i / (double) n));
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }
}
