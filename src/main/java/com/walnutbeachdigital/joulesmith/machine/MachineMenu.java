package com.walnutbeachdigital.joulesmith.machine;

import java.util.List;

import com.walnutbeachdigital.joulesmith.item.UpgradeCardItem;
import com.walnutbeachdigital.joulesmith.registry.JSMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** The one menu every Joulesmith machine uses: its slots, the player's inventory, and the synced values. */
public class MachineMenu extends AbstractContainerMenu {
    public static final int INVENTORY_Y = 84;

    private final BlockPos pos;
    private final MachineBlockEntity machine;     // only on the server
    private final ContainerData data;
    private final List<SlotDef> slotDefs;
    private final MachineBlockEntity.ProgressStyle progressStyle;
    private final int[] tankXs;
    private final ResourceLocation recipeCategory;

    /** Server side: wraps the real machine. */
    public MachineMenu(int id, Inventory inventory, MachineBlockEntity machine) {
        this(id, inventory, machine.getBlockPos(), machine, machine.items(), machine.containerData());
    }

    /** Client side: rebuilt from the position the server sends; slots and values fill in by syncing. */
    public MachineMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), null, null, null);
    }

    private MachineMenu(int id, Inventory inventory, BlockPos pos, MachineBlockEntity machine, IItemHandler items, ContainerData data) {
        super(JSMenus.MACHINE.get(), id);
        this.pos = pos;
        this.machine = machine;
        if (!(inventory.player.level().getBlockEntity(pos) instanceof MachineBlockEntity be)) {
            throw new IllegalStateException("No Joulesmith machine at " + pos);
        }
        this.slotDefs = be.slotDefs();
        this.progressStyle = be.progressStyle();
        this.tankXs = be.tanks().stream().mapToInt(MachineBlockEntity.TankDef::x).toArray();
        this.recipeCategory = be.recipeCategory();
        IItemHandler handler = items != null ? items : be.items();     // the client copy follows the same slot rules
        this.data = data != null ? data : new SimpleContainerData(MachineBlockEntity.VALUES * 2);
        for (int i = 0; i < slotDefs.size(); i++) {
            SlotDef def = slotDefs.get(i);
            addSlot(new SlotItemHandler(handler, i, def.x(), def.y()) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return def.role() != SlotRole.OUTPUT && super.mayPlace(stack);
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
        addDataSlots(this.data);
    }

    public BlockPos getPos() {
        return pos;
    }

    public List<SlotDef> slotDefs() {
        return slotDefs;
    }

    public MachineBlockEntity.ProgressStyle progressStyle() {
        return progressStyle;
    }

    /** A machine value, rebuilt from the two 16-bit halves it is synced in. */
    public int value(int index) {
        return (data.get(index * 2) & 0xFFFF) | ((data.get(index * 2 + 1) & 0xFFFF) << 16);
    }

    /** The JEI category this machine runs (for the clickable arrow), or null. */
    public ResourceLocation recipeCategory() {
        return recipeCategory;
    }

    public int tankCount() {
        return tankXs.length;
    }

    public int tankX(int tank) {
        return tankXs[tank];
    }

    /** The fluid in one of the machine's tanks (amount included), or empty. */
    public FluidStack fluid(int tank) {
        int amount = value(7 + tank * 3);
        if (amount <= 0) return FluidStack.EMPTY;
        Fluid fluid = BuiltInRegistries.FLUID.byId(value(6 + tank * 3));
        return fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    public int tankCapacity(int tank) {
        return value(8 + tank * 3);
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.values()[Math.floorMod(value(4), RedstoneMode.values().length)];
    }

    public SideMode sideMode(MachineFace face) {
        return SideMode.values()[(value(5) >> (face.ordinal() * 2)) & 3];
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return machine != null && machine.onMenuButton(player, id);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64.0
                && player.level().getBlockEntity(pos) instanceof MachineBlockEntity;
    }

    /** Shift-click: machine slots go to the inventory; inventory items go into the machine's inputs. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int machineSlots = slotDefs.size();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, machineSlots + 36, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            SlotRole target = stack.getItem() instanceof UpgradeCardItem ? SlotRole.UPGRADE : SlotRole.INPUT;
            for (int i = 0; i < machineSlots; i++) {
                if (slotDefs.get(i).role() == target && slots.get(i).mayPlace(stack) && moveItemStackTo(stack, i, i + 1, false)) {
                    moved = true;
                    break;
                }
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
