package com.walnutbeachdigital.joulesmith.farming;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.automation.BlockPlacerBlockEntity;
import com.walnutbeachdigital.joulesmith.automation.OutputHelper;
import com.walnutbeachdigital.joulesmith.item.UpgradeCardItem;
import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.machine.SlotRole;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Looks after animals in the 5x5 in front of it (9x9 and 13x13 with Range Upgrades, 3 high): shears sheep,
 * milks cows into empty buckets from its input slots, picks up eggs, wool and other drops, and breeds
 * adults with food from its inputs while there are fewer than {@link #CAP} animals, so the pen never
 * overcrowds. Each job uses {@link #ENERGY_PER_JOB} FE.
 */
public class AnimalRancherBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 20_000, MAX_INPUT = 200, ENERGY_PER_JOB = 200, INTERVAL = 40, CAP = 16;

    private int cooldown;

    private static List<SlotDef> layout() {
        List<SlotDef> slots = new ArrayList<>();
        for (int i = 0; i < 3; i++) slots.add(SlotDef.input(26 + i * 18, 35));
        slots.addAll(BlockPlacerBlockEntity.grid(98, SlotRole.OUTPUT));
        return slots;
    }

    public AnimalRancherBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.ANIMAL_RANCHER.get(), pos, state, layout(), CAPACITY, MAX_INPUT, 0, true);
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

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        int interval = Math.max(4, (int) (INTERVAL / (1 + 0.5 * upgrades(UpgradeCardItem.Type.SPEED))));
        if (!allowed || level.isClientSide || ++cooldown < interval) return false;
        cooldown = 0;
        AABB area = area(pos);
        boolean did = false;
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, area, Animal::isAlive);
        for (Animal animal : animals) {
            if (energy.getEnergyStored() < ENERGY_PER_JOB) break;
            if (animal instanceof Sheep sheep && sheep.readyForShearing()) {
                sheep.shear(SoundSource.BLOCKS);
                energy.consume(ENERGY_PER_JOB);
                did = true;
            } else if (animal instanceof Cow cow && !cow.isBaby() && level.getGameTime() % (INTERVAL * 3) < INTERVAL) {
                int bucket = find(new ItemStack(Items.BUCKET));
                ItemStack milk = new ItemStack(Items.MILK_BUCKET);
                if (bucket >= 0 && OutputHelper.fits(items, slotDefs(), List.of(milk))) {
                    items.extractItem(bucket, 1, false);
                    OutputHelper.store(items, slotDefs(), List.of(milk));
                    level.playSound(null, cow.blockPosition(), SoundEvents.COW_MILK, SoundSource.BLOCKS, 1f, 1f);
                    energy.consume(ENERGY_PER_JOB);
                    did = true;
                }
            }
        }
        if (animals.size() < CAP) did |= breed(animals);
        collect(level, area);
        if (did) setChanged();
        return did;
    }

    /** Feeds two adults of one kind that can breed, if their food is in the inputs. */
    private boolean breed(List<Animal> animals) {
        for (Animal a : animals) {
            if (a.isBaby() || a.getAge() != 0 || a.isInLove() || !a.canFallInLove()) continue;
            for (Animal b : animals) {
                if (b == a || b.getType() != a.getType() || b.isBaby() || b.getAge() != 0 || b.isInLove() || !b.canFallInLove()) continue;
                int food = -1;
                for (int i = 0; i < 3; i++) {
                    ItemStack stack = items.getStackInSlot(i);
                    if (stack.getCount() >= 2 && a.isFood(stack)) food = i;
                }
                if (food < 0 || energy.getEnergyStored() < ENERGY_PER_JOB) return false;
                items.extractItem(food, 2, false);
                a.setInLove(null);
                b.setInLove(null);
                energy.consume(ENERGY_PER_JOB);
                return true;
            }
        }
        return false;
    }

    private int find(ItemStack wanted) {
        for (int i = 0; i < 3; i++) {
            if (ItemStack.isSameItem(items.getStackInSlot(i), wanted)) return i;
        }
        return -1;
    }

    /** Picks up eggs, wool and other items lying in the pen. */
    private void collect(Level level, AABB area) {
        for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, area.inflate(1), ItemEntity::isAlive)) {
            ItemStack stack = drop.getItem().copy();
            List<ItemStack> rest = OutputHelper.store(items, slotDefs(), List.of(stack));
            if (rest.isEmpty()) drop.discard();
            else drop.setItem(rest.get(0));
        }
    }
}
