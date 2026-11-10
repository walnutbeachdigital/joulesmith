package com.walnutbeachdigital.joulesmith.machine;

import java.util.Arrays;
import java.util.List;

import com.walnutbeachdigital.joulesmith.energy.JSEnergyStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The shared base for every Joulesmith machine: an FE store, an inventory with input and output slots,
 * per-face settings, a redstone mode and a progress bar. A processing machine only says what it can make
 * from its inputs ({@link #findProcess()}); the base runs it, spends the energy and moves the items.
 * Generators and batteries override {@link #work} instead.
 */
public abstract class MachineBlockEntity extends BlockEntity implements MenuProvider {

    /** One run of a machine: what it makes, how long it takes and the energy it uses each tick. */
    public record Process(ItemStack result, int ticks, int energyPerTick, int inputCount) {}

    /** How the screen shows progress: an arrow (processing), a flame (burning fuel), or nothing. */
    public enum ProgressStyle { ARROW, FLAME, NONE }

    protected final JSEnergyStorage energy;
    protected final ItemStackHandler items;
    private final List<SlotDef> slots;
    private final SideMode[] sides = new SideMode[MachineFace.values().length];
    private RedstoneMode redstone = RedstoneMode.IGNORED;
    protected int progress;
    protected int maxProgress;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                 List<SlotDef> slots, int capacity, int maxInput) {
        this(type, pos, state, slots, capacity, maxInput, 0);
    }

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                 List<SlotDef> slots, int capacity, int maxInput, int maxOutput) {
        super(type, pos, state);
        this.slots = slots;
        this.energy = new JSEnergyStorage(capacity, maxInput, maxOutput, this::setChanged);
        this.items = new ItemStackHandler(slots.size()) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slots.get(slot).role() == SlotRole.OUTPUT || isValidInput(stack);
            }
        };
        Arrays.fill(sides, SideMode.BOTH);
        sides[MachineFace.FRONT.ordinal()] = SideMode.NONE;
    }

    // ------------------------------------------------------------------ what each machine decides

    /** What the machine can make right now from its inputs, or null if nothing. */
    protected Process findProcess() {
        return null;
    }

    /** Whether an item may go into an input slot at all (used by hoppers, pipes and shift-click). */
    protected boolean isValidInput(ItemStack stack) {
        return true;
    }

    public ProgressStyle progressStyle() {
        return ProgressStyle.ARROW;
    }

    /** Sets a face's starting setting; call from a constructor to change the defaults. */
    protected void setDefaultSide(MachineFace face, SideMode mode) {
        sides[face.ordinal()] = mode;
    }

    // ------------------------------------------------------------------ running

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        machine.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        boolean running = work(level, pos, redstone.allows(level.hasNeighborSignal(pos)));
        if (state.getValue(MachineBlock.LIT) != running) {
            level.setBlock(pos, state.setValue(MachineBlock.LIT, running), 3);
        }
    }

    /**
     * One tick of work. Returns whether the machine is running (lights its front). {@code allowed} is
     * whether its redstone setting lets it run right now. By default: run {@link #findProcess()}.
     */
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        boolean running = false;
        Process process = allowed ? findProcess() : null;
        if (process != null && outputFits(process.result())) {
            maxProgress = process.ticks();
            if (energy.getEnergyStored() >= process.energyPerTick()) {
                energy.consume(process.energyPerTick());
                progress++;
                running = true;
                if (progress >= maxProgress) {
                    finish(process);
                    progress = 0;
                }
            }
        } else if (progress != 0) {
            progress = 0;
            setChanged();
        }
        return running;
    }

    /** Sends up to {@code max} FE in total to neighbours on faces set to give out. Returns how much went. */
    protected int pushEnergy(Level level, BlockPos pos, int max) {
        int sent = 0;
        for (Direction dir : Direction.values()) {
            if (sent >= max || energy.getEnergyStored() == 0) break;
            if (!sideMode(MachineFace.of(facing(), dir)).gives()) continue;
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
            if (target != null && target.canReceive()) {
                int offered = Math.min(max - sent, energy.getEnergyStored());
                int taken = target.receiveEnergy(offered, false);
                energy.consume(taken);
                sent += taken;
            }
        }
        return sent;
    }

    /** Uses the inputs and puts the result into the output slots. */
    protected void finish(Process process) {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).role() == SlotRole.INPUT && !items.getStackInSlot(i).isEmpty()) {
                items.extractItem(i, process.inputCount(), false);
                break;
            }
        }
        ItemStack left = process.result().copy();
        for (int i = 0; i < slots.size() && !left.isEmpty(); i++) {
            if (slots.get(i).role() == SlotRole.OUTPUT) {
                left = items.insertItem(i, left, false);
            }
        }
        setChanged();
    }

    private boolean outputFits(ItemStack result) {
        ItemStack left = result.copy();
        for (int i = 0; i < slots.size() && !left.isEmpty(); i++) {
            if (slots.get(i).role() == SlotRole.OUTPUT) {
                left = items.insertItem(i, left, true);
            }
        }
        return left.isEmpty();
    }

    /** The first input slot's contents (most machines have one input). */
    protected ItemStack firstInput() {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).role() == SlotRole.INPUT) return items.getStackInSlot(i);
        }
        return ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------ faces and automation

    public SideMode sideMode(MachineFace face) {
        return sides[face.ordinal()];
    }

    protected Direction facing() {
        return getBlockState().getValue(MachineBlock.FACING);
    }

    /** What pipes and hoppers see on a face: inputs can be filled through input faces, outputs emptied through output faces. */
    public IItemHandler itemHandler(Direction side) {
        if (side == null) return items;
        SideMode mode = sideMode(MachineFace.of(facing(), side));
        return mode == SideMode.NONE ? null : new FaceItems(mode);
    }

    public JSEnergyStorage energy() {
        return energy;
    }

    /** What cables see on a face. By default the whole store, on every face. */
    public IEnergyStorage energyHandler(Direction side) {
        return energy;
    }

    public ItemStackHandler items() {
        return items;
    }

    public List<SlotDef> slotDefs() {
        return slots;
    }

    private final class FaceItems implements IItemHandler {
        private final SideMode mode;

        FaceItems(SideMode mode) {
            this.mode = mode;
        }

        @Override public int getSlots() { return items.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return mode.accepts() && slots.get(slot).role() == SlotRole.INPUT ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return mode.gives() && slots.get(slot).role() == SlotRole.OUTPUT ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slots.get(slot).role() == SlotRole.INPUT && items.isItemValid(slot, stack);
        }
    }

    // ------------------------------------------------------------------ screen

    /** Button ids the screen sends: cycle the redstone mode, or a face's setting forward / back. */
    public static final int BUTTON_REDSTONE = 0, BUTTON_FACE_NEXT = 10, BUTTON_FACE_PREVIOUS = 20;

    public boolean onMenuButton(Player player, int id) {
        if (id == BUTTON_REDSTONE) {
            redstone = redstone.next();
        } else if (id >= BUTTON_FACE_NEXT && id < BUTTON_FACE_NEXT + 6) {
            int f = id - BUTTON_FACE_NEXT;
            sides[f] = sides[f].next();
        } else if (id >= BUTTON_FACE_PREVIOUS && id < BUTTON_FACE_PREVIOUS + 6) {
            int f = id - BUTTON_FACE_PREVIOUS;
            sides[f] = sides[f].previous();
        } else {
            return false;
        }
        setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);     // face settings changed what pipes may do
        }
        return true;
    }

    /** Values the screen shows: 0 energy, 1 capacity, 2 progress, 3 max progress, 4 redstone mode, 5 face settings. */
    public static final int VALUES = 6;

    public int value(int index) {
        return switch (index) {
            case 0 -> energy.getEnergyStored();
            case 1 -> energy.getMaxEnergyStored();
            case 2 -> progress;
            case 3 -> maxProgress;
            case 4 -> redstone.ordinal();
            case 5 -> {
                int packed = 0;
                for (int f = 0; f < sides.length; f++) packed |= sides[f].ordinal() << (f * 2);
                yield packed;
            }
            default -> 0;
        };
    }

    /** Syncs each value in two 16-bit halves (menus only sync shorts). */
    public ContainerData containerData() {
        return new ContainerData() {
            @Override
            public int get(int i) {
                int v = value(i / 2);
                return i % 2 == 0 ? v & 0xFFFF : v >>> 16;
            }

            @Override
            public void set(int i, int value) {
            }

            @Override
            public int getCount() {
                return VALUES * 2;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineMenu(id, inventory, this);
    }

    public void dropContents() {
        if (level == null) return;
        for (int i = 0; i < items.getSlots(); i++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(i));
        }
    }

    // ------------------------------------------------------------------ saving

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Energy", energy.serializeNBT(registries));
        tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Redstone", redstone.ordinal());
        int[] faces = new int[sides.length];
        for (int f = 0; f < sides.length; f++) faces[f] = sides[f].ordinal();
        tag.putIntArray("Sides", faces);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) energy.deserializeNBT(registries, tag.get("Energy"));
        if (tag.contains("Items")) items.deserializeNBT(registries, tag.getCompound("Items"));
        redstone = RedstoneMode.values()[Math.floorMod(tag.getInt("Redstone"), RedstoneMode.values().length)];
        int[] faces = tag.getIntArray("Sides");
        for (int f = 0; f < Math.min(faces.length, sides.length); f++) {
            sides[f] = SideMode.values()[Math.floorMod(faces[f], SideMode.values().length)];
        }
        progress = tag.getInt("Progress");
    }
}
