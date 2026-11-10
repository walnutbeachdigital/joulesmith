package com.walnutbeachdigital.joulesmith.automation;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.item.UpgradeCardItem;
import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * Right-clicks with the item in its slot, once a second: on a creature in front of it if there is one
 * (shears on sheep, buckets on cows), otherwise on the block in front (bone meal, hoes, buckets on
 * fluid). What the item turns into goes back in the slot; anything else it gets goes to the outputs.
 */
public class MechanicalUserBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 20_000, MAX_INPUT = 400, ENERGY = 100, INTERVAL = 20;

    private int cooldown;

    static List<SlotDef> layout() {
        List<SlotDef> slots = new ArrayList<>();
        slots.add(SlotDef.input(44, 35));
        slots.add(SlotDef.byproduct(116, 26));
        slots.add(SlotDef.byproduct(134, 26));
        slots.add(SlotDef.byproduct(116, 44));
        slots.add(SlotDef.byproduct(134, 44));
        return slots;
    }

    public MechanicalUserBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.MECHANICAL_USER.get(), pos, state, layout(), CAPACITY, MAX_INPUT, 0, true);
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.NONE;
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        int interval = Math.max(2, (int) (INTERVAL / (1 + 0.5 * upgrades(UpgradeCardItem.Type.SPEED))));
        if (!allowed || !(level instanceof ServerLevel server) || ++cooldown < interval) return false;
        cooldown = 0;
        ItemStack held = items.getStackInSlot(0);
        if (held.isEmpty() || energy.getEnergyStored() < ENERGY) return false;
        Direction facing = facing();
        BlockPos target = pos.relative(facing);
        FakePlayer player = MachinePlayer.at(server, pos, facing);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, held.copy());
        boolean used = false;
        for (Entity entity : level.getEntities((Entity) null, new AABB(target), e -> e instanceof LivingEntity && e.isAlive())) {
            if (player.interactOn(entity, InteractionHand.MAIN_HAND).consumesAction()) {
                used = true;
                break;
            }
        }
        if (!used) {
            Vec3 hit = Vec3.atCenterOf(target).relative(facing.getOpposite(), 0.5);
            InteractionResult result = player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(hit, facing.getOpposite(), target, false));
            used = result.consumesAction();
            if (!used) used = player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND).consumesAction();
        }
        // what is in the hand now goes back in the slot; anything else the player picked up goes to the outputs
        items.setStackInSlot(0, player.getMainHandItem().copy());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        List<ItemStack> extra = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty()) extra.add(s.copy());
        }
        player.getInventory().clearContent();
        for (ItemStack rest : OutputHelper.store(items, slotDefs(), extra)) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, rest);
        }
        if (used) energy.consume(ENERGY);
        return used;
    }
}
