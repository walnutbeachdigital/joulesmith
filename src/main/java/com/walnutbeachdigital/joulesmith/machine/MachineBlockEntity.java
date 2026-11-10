package com.walnutbeachdigital.joulesmith.machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.walnutbeachdigital.joulesmith.energy.JSEnergyStorage;
import com.walnutbeachdigital.joulesmith.item.UpgradeCardItem;
import com.walnutbeachdigital.joulesmith.recipe.ChanceResult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The shared base for every Joulesmith machine: an FE store, an inventory with input and output slots,
 * per-face settings, a redstone mode and a progress bar. A processing machine only says what it can make
 * from its inputs ({@link #findProcess()}); the base runs it, spends the energy and moves the items.
 * Generators and batteries override {@link #work} instead.
 */
public abstract class MachineBlockEntity extends BlockEntity implements MenuProvider {

    /**
     * One run of a machine: what it makes (some outputs only by chance), how long it takes, the energy it
     * uses each tick before upgrades, and how many items it takes from each of the machine's slots.
     */
    public record Process(List<ChanceResult> outputs, int ticks, int energyPerTick, int[] consume,
                          FluidStack fluidIn, FluidStack fluidOut) {
        public Process(List<ChanceResult> outputs, int ticks, int energyPerTick, int[] consume) {
            this(outputs, ticks, energyPerTick, consume, FluidStack.EMPTY, FluidStack.EMPTY);
        }

        /** A run with one sure output that uses {@code count} items from slot {@code slot}. */
        public static Process simple(ItemStack result, int ticks, int energyPerTick, int slotCount, int slot, int count) {
            int[] consume = new int[slotCount];
            consume[slot] = count;
            return new Process(List.of(ChanceResult.always(result)), ticks, energyPerTick, consume);
        }
    }

    /** The three upgrade slots, in a panel on the left of the screen. */
    public static final List<SlotDef> UPGRADE_SLOTS = List.of(
            SlotDef.upgrade(-19, 8), SlotDef.upgrade(-19, 26), SlotDef.upgrade(-19, 44));
    private static final int EJECT_INTERVAL = 10, EJECT_AMOUNT = 16;

    /** How the screen shows progress: an arrow (processing), a flame (burning fuel), or nothing. */
    public enum ProgressStyle { ARROW, FLAME, NONE }

    protected final JSEnergyStorage energy;
    protected final ItemStackHandler items;
    private final List<SlotDef> slots;
    private final SideMode[] sides = new SideMode[MachineFace.values().length];
    private RedstoneMode redstone = RedstoneMode.IGNORED;
    /** For PULSE: whether the signal was on last tick, and ticks left of the window a new pulse opened. */
    private boolean wasPowered;
    private int pulseWindow;
    private static final int PULSE_WINDOW = 20;
    protected int progress;
    protected int maxProgress;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                 List<SlotDef> slots, int capacity, int maxInput) {
        this(type, pos, state, slots, capacity, maxInput, 0);
    }

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                 List<SlotDef> slots, int capacity, int maxInput, int maxOutput) {
        this(type, pos, state, slots, capacity, maxInput, maxOutput, false);
    }

    /** {@code upgrades}: whether the machine takes upgrade cards (three slots, added after its own). */
    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                 List<SlotDef> slots, int capacity, int maxInput, int maxOutput, boolean upgrades) {
        super(type, pos, state);
        if (upgrades) {
            List<SlotDef> all = new ArrayList<>(slots);
            all.addAll(UPGRADE_SLOTS);
            slots = List.copyOf(all);
        }
        this.slots = slots;
        this.energy = new JSEnergyStorage(capacity, maxInput, maxOutput, this::setChanged);
        this.items = new ItemStackHandler(slots.size()) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return switch (MachineBlockEntity.this.slots.get(slot).role()) {
                    case OUTPUT -> true;
                    case INPUT -> isValidInput(stack);
                    case UPGRADE -> stack.getItem() instanceof UpgradeCardItem;
                    case GHOST -> true;
                };
            }

            @Override
            public int getSlotLimit(int slot) {
                SlotRole role = MachineBlockEntity.this.slots.get(slot).role();
                return role == SlotRole.UPGRADE || role == SlotRole.GHOST ? 1 : super.getSlotLimit(slot);
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
        boolean powered = level.hasNeighborSignal(pos);
        boolean allowed;
        if (redstone == RedstoneMode.PULSE) {
            if (powered && !wasPowered) pulseWindow = PULSE_WINDOW;
            // a job started inside the window carries on until it is done
            allowed = pulseWindow > 0 || progress > 0;
            if (pulseWindow > 0) pulseWindow--;
        } else {
            allowed = redstone.allows(powered);
        }
        wasPowered = powered;
        boolean running = work(level, pos, allowed);
        if (level.getGameTime() % 20 == 0) {
            long[] sample = energy.sample();
            madePerTick = (int) (sample[0] / 20);
            usedPerTick = (int) (sample[1] / 20);
        }
        if (allowed && level.getGameTime() % EJECT_INTERVAL == 0 && upgrades(UpgradeCardItem.Type.EJECT) > 0) {
            eject(level, pos);
        }
        if (running && level.getGameTime() % SOUND_INTERVAL == Math.floorMod(pos.hashCode(), SOUND_INTERVAL)
                && com.walnutbeachdigital.joulesmith.JSConfig.SOUNDS.getAsBoolean()) {
            if (sound == null) sound = java.util.Optional.ofNullable(com.walnutbeachdigital.joulesmith.registry.JSSounds.forBlock(
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath()));
            sound.ifPresent(s -> level.playSound(null, pos, s, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f,
                    0.95f + level.random.nextFloat() * 0.1f));
        }
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
        if (process != null && outputFits(process)) {
            int speed = upgrades(UpgradeCardItem.Type.SPEED), saving = upgrades(UpgradeCardItem.Type.EFFICIENCY);
            double configSpeed = com.walnutbeachdigital.joulesmith.JSConfig.get(com.walnutbeachdigital.joulesmith.JSConfig.MACHINE_SPEED);
            int ticks = Math.max(1, (int) Math.ceil(process.ticks() / (1 + 0.5 * speed) / configSpeed));
            double total = (double) process.energyPerTick() * process.ticks() * (1 + 0.25 * speed) * Math.max(0.4, 1 - 0.2 * saving)
                    * com.walnutbeachdigital.joulesmith.JSConfig.get(com.walnutbeachdigital.joulesmith.JSConfig.MACHINE_ENERGY);
            int perTick = Math.max(1, (int) Math.ceil(total / ticks));
            maxProgress = ticks;
            if (energy.getEnergyStored() >= perTick) {
                energy.consume(perTick);
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

    /** Uses the inputs, rolls the chance outputs and puts what came out into the output slots. */
    protected void finish(Process process) {
        pulseWindow = 0;                                  // a pulse runs one job
        int[] consume = process.consume();
        for (int i = 0; i < Math.min(consume.length, slots.size()); i++) {
            if (consume[i] > 0) items.extractItem(i, consume[i], false);
        }
        if (!process.fluidIn().isEmpty() && inputTank() != null) inputTank().drain(process.fluidIn(), IFluidHandler.FluidAction.EXECUTE);
        if (!process.fluidOut().isEmpty() && outputTank() != null) outputTank().fill(process.fluidOut(), IFluidHandler.FluidAction.EXECUTE);
        int bonus = upgrades(UpgradeCardItem.Type.BONUS);
        RandomSource random = level != null ? level.random : RandomSource.create();
        boolean first = true;
        for (ChanceResult out : process.outputs()) {
            ItemStack made = out.stack().copy();
            if (out.certain()) {
                if (first && bonus > 0 && random.nextFloat() < 0.1f * bonus) made.grow(1);
            } else if (random.nextFloat() >= Math.min(1f, out.chance() + 0.1f * bonus)) {
                made = ItemStack.EMPTY;
            }
            first = false;
            for (int i = 0; i < slots.size() && !made.isEmpty(); i++) {
                if (slots.get(i).role() == SlotRole.OUTPUT) made = items.insertItem(i, made, false);
            }
        }
        setChanged();
    }

    /** Whether every output, at its most, would fit in the output slots together. */
    private boolean outputFits(Process process) {
        List<ItemStack> space = new ArrayList<>();
        List<Integer> limits = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).role() == SlotRole.OUTPUT) {
                space.add(items.getStackInSlot(i).copy());
                limits.add(items.getSlotLimit(i));
            }
        }
        boolean first = true;
        for (ChanceResult out : process.outputs()) {
            ItemStack left = out.stack().copy();
            if (first && upgrades(UpgradeCardItem.Type.BONUS) > 0) left.grow(1);
            first = false;
            for (int i = 0; i < space.size() && !left.isEmpty(); i++) {
                ItemStack there = space.get(i);
                int max = Math.min(limits.get(i), left.getMaxStackSize());
                if (there.isEmpty()) {
                    int moved = Math.min(max, left.getCount());
                    space.set(i, left.copyWithCount(moved));
                    left.shrink(moved);
                } else if (ItemStack.isSameItemSameComponents(there, left)) {
                    int moved = Math.min(max - there.getCount(), left.getCount());
                    if (moved > 0) {
                        there.grow(moved);
                        left.shrink(moved);
                    }
                }
            }
            if (!left.isEmpty()) return false;
        }
        if (!process.fluidOut().isEmpty()) {
            FluidTank out = outputTank();
            if (out == null || out.fill(process.fluidOut(), IFluidHandler.FluidAction.SIMULATE) < process.fluidOut().getAmount()) return false;
        }
        return true;
    }

    /** How many upgrade cards of a type are fitted. */
    public int upgrades(UpgradeCardItem.Type type) {
        int n = 0;
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).role() == SlotRole.UPGRADE && items.getStackInSlot(i).getItem() instanceof UpgradeCardItem card && card.type() == type) {
                n++;
            }
        }
        return n;
    }

    /** Auto-Eject card: pushes finished items into blocks touching faces set to give out. */
    private void eject(Level level, BlockPos pos) {
        int budget = EJECT_AMOUNT;
        for (Direction dir : Direction.values()) {
            if (!sideMode(MachineFace.of(facing(), dir)).gives()) continue;
            IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(dir), dir.getOpposite());
            if (target == null) continue;
            for (int i = 0; i < slots.size() && budget > 0; i++) {
                if (slots.get(i).role() != SlotRole.OUTPUT) continue;
                ItemStack offered = items.extractItem(i, budget, true);
                if (offered.isEmpty()) continue;
                ItemStack left = ItemHandlerHelper.insertItem(target, offered, false);
                int moved = offered.getCount() - left.getCount();
                if (moved > 0) {
                    items.extractItem(i, moved, false);
                    budget -= moved;
                }
            }
        }
    }

    /** The JEI recipe category this machine runs, for the clickable progress arrow. Null if none. */
    public ResourceLocation recipeCategory() {
        return null;
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

    /** Sets the redstone mode (from a ComputerCraft computer, say). */
    public void setRedstoneMode(RedstoneMode mode) {
        redstone = mode;
        setChanged();
    }

    public Direction facing() {
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

    /** One fluid tank and where its gauge sits on the screen. */
    public record TankDef(FluidTank tank, int x) {}

    /** The machine's tanks, shown as gauges on its screen (at most two). Saved and synced by the base. */
    public List<TankDef> tanks() {
        return List.of();
    }

    /** The tank recipes drain from and pipes fill, or null. */
    protected FluidTank inputTank() {
        return null;
    }

    /** The tank recipes fill and pipes drain, or null. */
    protected FluidTank outputTank() {
        return null;
    }

    /**
     * What pipes and buckets see for fluids: on faces that take input, filling goes into the input tank;
     * on faces that give output, draining comes from the output tank. {@code side} null: both.
     */
    public IFluidHandler fluidHandler(Direction side) {
        FluidTank in = inputTank(), out = outputTank();
        if (in == null && out == null) return null;
        SideMode mode = side == null ? SideMode.BOTH : sideMode(MachineFace.of(facing(), side));
        if (mode == SideMode.NONE) return null;
        boolean fill = mode.accepts() && in != null, drain = mode.gives() && out != null;
        return new IFluidHandler() {
            @Override public int getTanks() { return 2; }
            @Override public FluidStack getFluidInTank(int i) { FluidTank t = i == 0 ? in : out; return t == null ? FluidStack.EMPTY : t.getFluid(); }
            @Override public int getTankCapacity(int i) { FluidTank t = i == 0 ? in : out; return t == null ? 0 : t.getCapacity(); }
            @Override public boolean isFluidValid(int i, FluidStack stack) { return i == 0 && fill && in.isFluidValid(stack); }
            @Override public int fill(FluidStack resource, FluidAction action) { return fill ? in.fill(resource, action) : 0; }
            @Override public FluidStack drain(FluidStack resource, FluidAction action) { return drain ? out.drain(resource, action) : FluidStack.EMPTY; }
            @Override public FluidStack drain(int maxDrain, FluidAction action) { return drain ? out.drain(maxDrain, action) : FluidStack.EMPTY; }
        };
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
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);   // for the wrench overlay
        }
        return true;
    }

    /**
     * Values the screen shows: 0 energy, 1 capacity, 2 progress, 3 max progress, 4 redstone mode,
     * 5 face settings, then for each of up to two tanks: fluid (registry id), amount, capacity.
     */
    /** 0-5 energy, progress, redstone, sides; then 3 per tank; then FE/t made and used (averaged over a second). */
    public static final int MAX_TANKS = 2, VALUES = 6 + 3 * MAX_TANKS + 2, MADE = 6 + 3 * MAX_TANKS, USED = MADE + 1;
    private int madePerTick, usedPerTick;
    /** The running sound, looked up once; empty for machines that run quietly. */
    private java.util.Optional<net.minecraft.sounds.SoundEvent> sound;
    private static final int SOUND_INTERVAL = 40;

    public int value(int index) {
        return switch (index) {
            case 0 -> energy.getEnergyStored();
            case 1 -> energy.getMaxEnergyStored();
            case 2 -> progress;
            case 3 -> maxProgress;
            case 4 -> redstone.ordinal();
            case MADE -> madePerTick;
            case USED -> usedPerTick;
            case 5 -> {
                int packed = 0;
                for (int f = 0; f < sides.length; f++) packed |= sides[f].ordinal() << (f * 2);
                yield packed;
            }
            default -> {
                int t = (index - 6) / 3;
                List<TankDef> tanks = tanks();
                if (index < 6 || t >= tanks.size()) yield 0;
                FluidTank tank = tanks.get(t).tank();
                yield switch ((index - 6) % 3) {
                    case 0 -> tank.isEmpty() ? 0 : BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
                    case 1 -> tank.getFluidAmount();
                    default -> tank.getCapacity();
                };
            }
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
            if (slots.get(i).role() == SlotRole.GHOST) continue;            // pattern copies, not real items
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
        List<TankDef> tanks = tanks();
        for (int t = 0; t < tanks.size(); t++) {
            tag.put(t == 0 ? "Tank" : "Tank" + t, tanks.get(t).tank().writeToNBT(registries, new CompoundTag()));
        }
    }

    // Clients get the face settings only, for the wrench's coloured overlay.
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        int[] faces = new int[sides.length];
        for (int f = 0; f < sides.length; f++) faces[f] = sides[f].ordinal();
        tag.putIntArray("Sides", faces);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readSides(tag);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,
                             HolderLookup.Provider registries) {
        readSides(packet.getTag());
    }

    private void readSides(CompoundTag tag) {
        int[] faces = tag.getIntArray("Sides");
        for (int f = 0; f < Math.min(faces.length, sides.length); f++) {
            sides[f] = SideMode.values()[Math.floorMod(faces[f], SideMode.values().length)];
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) energy.deserializeNBT(registries, tag.get("Energy"));
        if (tag.contains("Items")) {
            // read into a spare handler first: a save from before a machine gained slots must not shrink it
            ItemStackHandler saved = new ItemStackHandler();
            saved.deserializeNBT(registries, tag.getCompound("Items"));
            for (int i = 0; i < items.getSlots(); i++) {
                items.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : ItemStack.EMPTY);
            }
        }
        redstone = RedstoneMode.values()[Math.floorMod(tag.getInt("Redstone"), RedstoneMode.values().length)];
        int[] faces = tag.getIntArray("Sides");
        for (int f = 0; f < Math.min(faces.length, sides.length); f++) {
            sides[f] = SideMode.values()[Math.floorMod(faces[f], SideMode.values().length)];
        }
        progress = tag.getInt("Progress");
        List<TankDef> tanks = tanks();
        for (int t = 0; t < tanks.size(); t++) {
            tanks.get(t).tank().readFromNBT(registries, tag.getCompound(t == 0 ? "Tank" : "Tank" + t));
        }
    }
}
