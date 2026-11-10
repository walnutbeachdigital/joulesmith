package com.walnutbeachdigital.joulesmith.logic;

import com.walnutbeachdigital.joulesmith.registry.JSMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** The Logic Controller's screen: its rules, their live readings and which outputs are on. No slots. */
public class LogicMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final LogicControllerBlockEntity controller;     // server only
    private final ContainerData data;

    public LogicMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), null);
    }

    public LogicMenu(int id, Inventory inventory, BlockPos pos, LogicControllerBlockEntity controller) {
        super(JSMenus.LOGIC.get(), id);
        this.pos = pos;
        this.controller = controller;
        this.data = controller != null ? controller.data() : new SimpleContainerData((2 * LogicControllerBlockEntity.RULES + 1) * 2);
        addDataSlots(data);
    }

    private int value(int index) {
        return (data.get(index * 2) & 0xFFFF) | ((data.get(index * 2 + 1) & 0xFFFF) << 16);
    }

    public Rule rule(int i) {
        return Rule.unpack(value(i));
    }

    public int reading(int i) {
        return value(LogicControllerBlockEntity.RULES + i);
    }

    public boolean outputOn(net.minecraft.core.Direction dir) {
        return (value(2 * LogicControllerBlockEntity.RULES) >> dir.get3DDataValue() & 1) != 0;
    }

    /** Button id: rule * 16 + field * 2 + (1 to step back). */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (controller == null) return false;
        controller.step(id / 16, (id % 16) / 2, id % 2 == 1);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64.0 && player.level().getBlockEntity(pos) instanceof LogicControllerBlockEntity;
    }
}
