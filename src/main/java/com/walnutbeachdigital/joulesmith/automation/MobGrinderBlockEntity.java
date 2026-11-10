package com.walnutbeachdigital.joulesmith.automation;

import java.util.List;

import com.walnutbeachdigital.joulesmith.item.UpgradeCardItem;
import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.SlotRole;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.registry.JSFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Kills mobs in the 5x5 in front of it (9x9 and 13x13 with Range Upgrades, 3 high) with no weapon needed,
 * as a player kill so they drop their full loot. Drops go into its outputs; the experience they drop is
 * gathered into its tank as Liquid XP, 20 mB per point. Leaves named and tamed mobs, players and bosses alone.
 */
public class MobGrinderBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 40_000, MAX_INPUT = 800, ENERGY_PER_HIT = 400, INTERVAL = 20, DAMAGE = 8,
            TANK = 16_000, MB_PER_XP = 20;

    private final FluidTank tank = new FluidTank(TANK) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private int cooldown;

    public MobGrinderBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.MOB_GRINDER.get(), pos, state, BlockPlacerBlockEntity.grid(98, SlotRole.OUTPUT), CAPACITY, MAX_INPUT, 0, true);
    }

    @Override
    public List<TankDef> tanks() {
        return List.of(new TankDef(tank, 30));
    }

    @Override
    protected FluidTank outputTank() {
        return tank;
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.NONE;
    }

    private AABB area(BlockPos pos) {
        int r = 2 + 2 * Math.min(2, upgrades(UpgradeCardItem.Type.RANGE));
        BlockPos centre = pos.relative(facing(), r + 1);
        return new AABB(centre).inflate(r, 0, r).expandTowards(0, 2, 0);
    }

    private static boolean target(Mob mob) {
        return mob.isAlive() && !mob.isInvulnerable() && !mob.hasCustomName() && !mob.getType().is(Tags.EntityTypes.BOSSES)
                && !(mob instanceof OwnableEntity owned && owned.getOwnerUUID() != null);
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        int interval = Math.max(4, (int) (INTERVAL / (1 + 0.5 * upgrades(UpgradeCardItem.Type.SPEED))));
        if (!allowed || !(level instanceof ServerLevel server) || ++cooldown < interval) return false;
        cooldown = 0;
        AABB area = area(pos);
        collect(level, area);
        boolean hit = false;
        FakePlayer player = MachinePlayer.at(server, pos, facing());
        DamageSource source = level.damageSources().playerAttack(player);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area, MobGrinderBlockEntity::target)) {
            if (energy.getEnergyStored() < ENERGY_PER_HIT) break;
            if (mob.hurt(source, DAMAGE)) {
                energy.consume(ENERGY_PER_HIT);
                hit = true;
            }
        }
        if (hit) setChanged();
        return hit;
    }

    /** Drops into the outputs, experience into the tank. */
    private void collect(Level level, AABB area) {
        AABB wide = area.inflate(1);
        for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, wide, ItemEntity::isAlive)) {
            List<ItemStack> rest = OutputHelper.store(items, slotDefs(), List.of(drop.getItem().copy()));
            if (rest.isEmpty()) drop.discard();
            else drop.setItem(rest.get(0));
        }
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, wide, ExperienceOrb::isAlive)) {
            FluidStack xp = new FluidStack(JSFluids.LIQUID_XP.source.get(), orb.getValue() * MB_PER_XP);
            if (tank.fill(xp, IFluidHandler.FluidAction.SIMULATE) < xp.getAmount()) break;
            tank.fill(xp, IFluidHandler.FluidAction.EXECUTE);
            orb.discard();
        }
    }
}
