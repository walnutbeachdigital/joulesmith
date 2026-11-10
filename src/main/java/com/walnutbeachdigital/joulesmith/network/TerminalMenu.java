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

/**
 * The Network Terminal's screen: everything on the network in a searchable grid above the player's
 * inventory. The server sends the grid's contents twice a second; clicks on the grid come back as
 * {@link TerminalPayloads.Action}s.
 */
public class TerminalMenu extends AbstractContainerMenu {
    public static final int GRID_X = 8, GRID_Y = 18, COLUMNS = 9, ROWS = 6, INVENTORY_Y = 140;
    /** The Crafting Terminal's layout: a shorter grid, the crafting grid, then the inventory. */
    public static final int CRAFTING_ROWS = 4, CRAFTING_INVENTORY_Y = 166;
    public static final int TAKE_STACK = 0, TAKE_HALF = 1, TAKE_TO_INVENTORY = 2, INSERT_ALL = 3, INSERT_ONE = 4;

    private final BlockPos pos;
    protected final Player player;
    private final int rows, inventoryY;
    private final ContainerLevelAccess access;
    /** Client side: what the server last sent. */
    private List<BinContents> shown = List.of();
    private boolean online;
    private List<BinContents> lastSent;
    private boolean lastOnline;
    private int ticks;

    public TerminalMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos());
    }

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

    void receive(List<BinContents> contents, boolean online) {
        this.shown = contents;
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
        boolean on = net.online();
        if (!now.equals(lastSent) || on != lastOnline) {
            lastSent = now;
            lastOnline = on;
            PacketDistributor.sendToPlayer(sp, new TerminalPayloads.Contents(containerId, new ArrayList<>(now), on));
        }
    }

    /** A click on the grid (or on the grid with an item held), from the client. */
    void act(ServerPlayer sp, ItemStack like, int action) {
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
            case INSERT_ALL -> setCarried(net.insert(carried, false));
            case INSERT_ONE -> {
                if (carried.isEmpty()) return;
                if (net.insert(carried.copyWithCount(1), false).isEmpty()) carried.shrink(1);
                setCarried(carried);
            }
            default -> { }
        }
        resend();
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
        return stillValid(access, p, block());
    }

    protected net.minecraft.world.level.block.Block block() {
        return JSBlocks.NETWORK_TERMINAL.get();
    }
}
