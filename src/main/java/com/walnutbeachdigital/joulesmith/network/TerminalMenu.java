package com.walnutbeachdigital.joulesmith.network;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.registry.JSBlocks;
import com.walnutbeachdigital.joulesmith.registry.JSMenus;
import com.walnutbeachdigital.joulesmith.storage.bin.BinContents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The Network Terminal's screen: everything on the network in a searchable grid above the player's
 * inventory. The server sends the grid's contents twice a second; clicks on the grid come back as
 * {@link TerminalPayloads.Action}s.
 */
public class TerminalMenu extends AbstractContainerMenu {
    public static final int GRID_X = 8, GRID_Y = 18, COLUMNS = 9, ROWS = 6, INVENTORY_Y = 140;
    /** The Crafting Terminal's layout: a shorter grid, the crafting grid, then the inventory. */
    public static final int CRAFTING_ROWS = 4, CRAFTING_INVENTORY_Y = 166;
    public static final int TAKE_STACK = 0, TAKE_HALF = 1, TAKE_TO_INVENTORY = 2, INSERT_ALL = 3, INSERT_ONE = 4,
            CRAFT_ONE = 5, CRAFT_STACK = 6, ENCODE = 7, FILL = 8;

    private final BlockPos pos;
    protected final Player player;
    private final int rows, inventoryY;
    private final ContainerLevelAccess access;
    /** Client side: what the server last sent. */
    private List<BinContents> shown = List.of();
    private List<ItemStack> craftable = List.of();
    private List<FluidStack> fluids = List.of();
    private int lastFluidHash;
    private boolean online;
    private List<BinContents> lastSent;
    private boolean lastOnline;
    private int lastCraftable;
    private int ticks;

    public TerminalMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos());
        wireless = buf.readBoolean();
    }

    /** Opened from a Wireless Terminal: stays open while the player holds it within range of the network. */
    public static TerminalMenu wireless(int id, Inventory inventory, BlockPos pos) {
        TerminalMenu menu = new TerminalMenu(id, inventory, pos);
        menu.wireless = true;
        return menu;
    }

    private boolean wireless;

    public TerminalMenu(int id, Inventory inventory, BlockPos pos) {
        this(JSMenus.TERMINAL.get(), id, inventory, pos, ROWS, INVENTORY_Y);
    }

    protected TerminalMenu(net.minecraft.world.inventory.MenuType<?> type, int id, Inventory inventory, BlockPos pos, int rows, int inventoryY) {
        super(type, id);
        this.pos = pos;
        this.rows = rows;
        this.inventoryY = inventoryY;
        this.player = inventory.player;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, inventoryY + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, inventoryY + 58));
    }

    public int rows() {
        return rows;
    }

    public int inventoryY() {
        return inventoryY;
    }

    public List<BinContents> shown() {
        return shown;
    }

    public boolean online() {
        return online;
    }

    public List<ItemStack> craftable() {
        return craftable;
    }

    public List<FluidStack> fluids() {
        return fluids;
    }

    void receive(List<BinContents> contents, List<ItemStack> craftable, List<FluidStack> fluids, boolean online) {
        this.fluids = fluids;
        this.shown = contents;
        this.craftable = craftable;
        this.online = online;
    }

    protected Network network() {
        return Network.scan(player.level(), pos);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer sp) || ticks++ % 10 != 0 && lastSent != null) return;
        Network net = network();
        List<BinContents> now = net.contents();
        List<ItemStack> canMake = net.craftable();
        List<FluidStack> liquids = net.fluids();
        int fluidHash = 1;
        for (FluidStack f : liquids) fluidHash = fluidHash * 31 + FluidStack.hashFluidAndComponents(f) * 7 + f.getAmount();
        boolean on = net.online();
        if (!now.equals(lastSent) || on != lastOnline || canMake.size() != lastCraftable || fluidHash != lastFluidHash) {
            lastSent = now;
            lastOnline = on;
            lastCraftable = canMake.size();
            lastFluidHash = fluidHash;
            PacketDistributor.sendToPlayer(sp, new TerminalPayloads.Contents(containerId, new ArrayList<>(now), canMake, liquids, on));
        }
    }

    /** A click on the grid (or on the grid with an item held), from the client. */
    void act(ServerPlayer sp, ItemStack like, FluidStack fluid, int action) {
        Network net = network();
        if (!net.online()) return;
        ItemStack carried = getCarried();
        switch (action) {
            case TAKE_STACK, TAKE_HALF -> {
                if (!carried.isEmpty() || like.isEmpty()) return;
                int max = like.getMaxStackSize();
                if (action == TAKE_HALF) {
                    int have = net.extract(like, max, true).getCount();
                    max = Math.max(1, (have + 1) / 2);
                }
                setCarried(net.extract(like, max, false));
            }
            case TAKE_TO_INVENTORY -> {
                if (like.isEmpty()) return;
                ItemStack got = net.extract(like, like.getMaxStackSize(), false);
                if (!sp.getInventory().add(got) && !got.isEmpty()) {
                    ItemStack back = net.insert(got, false);
                    if (!back.isEmpty()) sp.drop(back, false);
                }
            }
            case INSERT_ALL -> {
                if (!pour(sp, net)) setCarried(net.insert(carried, false));
            }
            case FILL -> fill(sp, net, fluid);
            case CRAFT_ONE, CRAFT_STACK -> {
                if (like.isEmpty()) return;
                int per = net.patterns().stream().filter(p -> ItemStack.isSameItemSameComponents(p.output(), like))
                        .mapToInt(p -> p.output().getCount()).findFirst().orElse(1);
                sp.displayClientMessage(net.request(like, action == CRAFT_ONE ? per : Math.max(per, like.getMaxStackSize())), true);
            }
            case ENCODE -> encode(sp);
            case INSERT_ONE -> {
                if (carried.isEmpty()) return;
                if (net.insert(carried.copyWithCount(1), false).isEmpty()) carried.shrink(1);
                setCarried(carried);
            }
            default -> { }
        }
        resend();
    }

    /** Empties one fluid container from the cursor into the network. False if it isn't one, or nothing went in. */
    private boolean pour(ServerPlayer sp, Network net) {
        ItemStack carried = getCarried();
        if (carried.isEmpty() || FluidUtil.getFluidContained(carried).isEmpty()) return false;
        var handler = FluidUtil.getFluidHandler(carried.copyWithCount(1)).orElse(null);
        if (handler == null) return false;
        FluidStack inside = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        int room = net.insertFluid(inside, true);
        if (room <= 0) return false;
        FluidStack out = handler.drain(inside.copyWithAmount(room), IFluidHandler.FluidAction.EXECUTE);
        if (out.isEmpty()) return false;
        net.insertFluid(out, false);
        swapOne(sp, handler.getContainer());
        return true;
    }

    /** Fills one fluid container on the cursor from the network with {@code fluid}. */
    private void fill(ServerPlayer sp, Network net, FluidStack fluid) {
        ItemStack carried = getCarried();
        if (carried.isEmpty() || fluid.isEmpty()) return;
        var handler = FluidUtil.getFluidHandler(carried.copyWithCount(1)).orElse(null);
        if (handler == null) return;
        int can = handler.fill(fluid.copyWithAmount(Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE);
        FluidStack got = net.extractFluid(fluid, can, true);
        if (got.isEmpty() || handler.fill(got, IFluidHandler.FluidAction.SIMULATE) < got.getAmount()) return;   // buckets want a full 1,000
        got = net.extractFluid(fluid, got.getAmount(), false);
        handler.fill(got, IFluidHandler.FluidAction.EXECUTE);
        swapOne(sp, handler.getContainer());
    }

    /** Replaces one item on the cursor with {@code result} (the rest of the stack stays on the cursor). */
    private void swapOne(ServerPlayer sp, ItemStack result) {
        ItemStack carried = getCarried();
        if (carried.getCount() <= 1) {
            setCarried(result);
        } else {
            carried.shrink(1);
            if (!sp.getInventory().add(result)) sp.drop(result, false);
        }
    }

    /** Records the crafting grid on a Blank Pattern (the Crafting Terminal does this). */
    protected void encode(ServerPlayer sp) {
    }

    /** Sends the grid's contents again at the next update. */
    protected void resend() {
        lastSent = null;
        ticks = 0;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || p.level().isClientSide) return ItemStack.EMPTY;
        ItemStack left = network().insert(slot.getItem(), false);
        slot.setByPlayer(left);
        lastSent = null;
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p) {
        if (wireless) return WirelessTerminalItem.reaches(p, pos);
        return stillValid(access, p, block());
    }

    protected net.minecraft.world.level.block.Block block() {
        return JSBlocks.NETWORK_TERMINAL.get();
    }
}
