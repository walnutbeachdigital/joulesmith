package com.walnutbeachdigital.joulesmith.automation;

import java.util.List;

import com.walnutbeachdigital.joulesmith.item.UpgradeCardItem;
import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.registry.JSFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Grows copies of the mob on the Mob Imprint in its slot, two blocks in front of it, from Nutrient Paste and
 * power: 10 seconds and 40,000 FE a mob, and 20 mB of paste per point of the mob's health (at least 200 mB).
 * It waits while 12 or more mobs of that kind are within 4 blocks of the spot. Speed Upgrades work.
 */
public class MobDuplicatorBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 200_000, MAX_INPUT = 4_000, TANK = 16_000, TICKS = 200, ENERGY = 40_000,
            MB_PER_HEALTH = 20, MIN_PASTE = 200, CROWD = 12;

    private final FluidTank paste = new FluidTank(TANK, stack -> stack.getFluid().isSame(JSFluids.NUTRIENT_PASTE.source.get())) {
        @Override protected void onContentsChanged() { setChanged(); }
    };

    public MobDuplicatorBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.MOB_DUPLICATOR.get(), pos, state, List.of(SlotDef.input(53, 35)), CAPACITY, MAX_INPUT, 0, true);
    }

    @Override
    public List<TankDef> tanks() {
        return List.of(new TankDef(paste, 120));
    }

    @Override
    protected FluidTank inputTank() {
        return paste;
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return stack.getItem() instanceof MobImprintItem && MobImprintItem.mob(stack).isPresent();
    }

    private int pasteFor(Level level, EntityType<?> type) {
        var probe = type.create(level);
        float health = probe instanceof LivingEntity living ? living.getMaxHealth() : 20;
        if (probe != null) probe.discard();
        return Math.max(MIN_PASTE, (int) (health * MB_PER_HEALTH));
    }

    private int cost = -1;
    private EntityType<?> costFor;

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        var type = MobImprintItem.mob(items.getStackInSlot(0)).orElse(null);
        if (!allowed || type == null || !(level instanceof ServerLevel server)) {
            if (progress != 0) { progress = 0; setChanged(); }
            return false;
        }
        if (type != costFor) { cost = pasteFor(level, type); costFor = type; }
        BlockPos spot = pos.relative(facing(), 2);
        int speed = upgrades(UpgradeCardItem.Type.SPEED);
        maxProgress = Math.max(20, (int) (TICKS / (1 + 0.5 * speed)));
        int perTick = (int) Math.ceil(ENERGY * (1 + 0.25 * speed) / maxProgress);
        if (paste.getFluidAmount() < cost || energy.getEnergyStored() < perTick) return false;
        if (level.getGameTime() % 20 == 0
                && level.getEntities(type, new AABB(spot).inflate(4), e -> e.isAlive()).size() >= CROWD) return false;
        energy.consume(perTick);
        if (++progress >= maxProgress) {
            progress = 0;
            if (type.spawn(server, spot, MobSpawnType.SPAWNER) != null) {
                paste.drain(cost, IFluidHandler.FluidAction.EXECUTE);
            }
        }
        setChanged();
        return true;
    }
}
