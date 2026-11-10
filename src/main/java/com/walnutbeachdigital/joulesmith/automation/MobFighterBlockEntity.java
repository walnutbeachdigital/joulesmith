package com.walnutbeachdigital.joulesmith.automation;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.item.UpgradeCardItem;
import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.machine.SlotRole;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * Attacks hostile mobs in front of it with the weapon in its slot, as a player would: the weapon's damage
 * and enchantments count (Looting too), mobs drop their player-kill loot and XP, and the weapon wears down.
 * It sweeps up the drops in its area into its outputs. Area: 5x5, 3 high (9x9 and 13x13 with Range Upgrades).
 */
public class MobFighterBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 20_000, MAX_INPUT = 400, ENERGY_PER_HIT = 100, INTERVAL = 12;

    private int cooldown;

    static List<SlotDef> layout() {
        List<SlotDef> slots = new ArrayList<>();
        slots.add(SlotDef.input(35, 35));                           // weapon
        slots.addAll(BlockPlacerBlockEntity.grid(98, SlotRole.OUTPUT));
        return slots;
    }

    public MobFighterBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.MOB_FIGHTER.get(), pos, state, layout(), CAPACITY, MAX_INPUT, 0, true);
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return stack.getMaxStackSize() == 1;
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.NONE;
    }

    private AABB area(BlockPos pos) {
        int r = 2 + 2 * Math.min(2, upgrades(UpgradeCardItem.Type.RANGE));
        Direction facing = facing();
        BlockPos centre = pos.relative(facing, r + 1);
        return new AABB(centre).inflate(r, 0, r).expandTowards(0, 2, 0);
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        int interval = Math.max(2, (int) (INTERVAL / (1 + 0.5 * upgrades(UpgradeCardItem.Type.SPEED))));
        if (!allowed || !(level instanceof ServerLevel server) || ++cooldown < interval) return false;
        cooldown = 0;
        AABB area = area(pos);
        collect(level, area);
        if (energy.getEnergyStored() < ENERGY_PER_HIT) return false;
        List<Mob> targets = level.getEntitiesOfClass(Mob.class, area, m -> m instanceof Enemy && m.isAlive() && !m.isInvulnerable());
        if (targets.isEmpty()) return false;
        Mob target = targets.get(0);
        ItemStack weapon = items.getStackInSlot(0);
        FakePlayer player = MachinePlayer.at(server, pos, facing());
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);        // so Looting and the kill count as the weapon's
        float damage = (float) player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        float[] bonus = {0};
        weapon.getAttributeModifiers().forEach(EquipmentSlotGroup.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(Attributes.ATTACK_DAMAGE) && modifier.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) {
                bonus[0] += (float) modifier.amount();
            }
        });
        DamageSource source = level.damageSources().playerAttack(player);
        float total = EnchantmentHelper.modifyDamage(server, weapon, target, source, damage + bonus[0]);
        boolean hit = target.hurt(source, total);
        if (hit && !weapon.isEmpty() && weapon.isDamageableItem()) {
            weapon.hurtAndBreak(1, server, (ServerPlayer) null, item -> {});
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        energy.consume(ENERGY_PER_HIT);
        setChanged();
        return hit;
    }

    /** Sweeps item drops lying in the area into the output slots. */
    private void collect(Level level, AABB area) {
        for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, area.inflate(1), ItemEntity::isAlive)) {
            ItemStack stack = drop.getItem().copy();
            List<ItemStack> rest = OutputHelper.store(items, slotDefs(), List.of(stack));
            if (rest.isEmpty()) drop.discard();
            else drop.setItem(rest.get(0));
        }
    }
}
