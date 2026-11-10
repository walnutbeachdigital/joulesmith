package com.walnutbeachdigital.joulesmith.network;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.energy.JSEnergyStorage;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.storage.bin.BinContents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The Auto-Crafting CPU: holds nine Patterns, which every terminal on its network can craft from, and
 * runs one crafting job at a time. A job takes all the stored ingredients it needs up front, then makes one
 * batch every {@link #TICKS_PER_BATCH} ticks ({@link #FE_PER_BATCH} FE each) and puts everything it made back
 * into the network at the end. If it can't finish, what it holds goes back too.
 */
public class CraftingCpuBlockEntity extends BlockEntity {
    public static final int SLOTS = 9, CAPACITY = 100_000, MAX_INPUT = 2_000, FE_PER_BATCH = 200, TICKS_PER_BATCH = 5;

    private final SimpleContainer patterns = new SimpleContainer(SLOTS) {
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { return stack.getItem() instanceof PatternItem; }
        @Override public int getMaxStackSize() { return 1; }
        @Override public void setChanged() { super.setChanged(); CraftingCpuBlockEntity.this.setChanged(); }
    };
    private final JSEnergyStorage energy = new JSEnergyStorage(CAPACITY, MAX_INPUT, 0, this::setChanged);
    /** The running job: what it holds, and the steps left (pattern, batches). */
    private final List<BinContents> buffer = new ArrayList<>();
    private final List<CraftingPlan.Step> steps = new ArrayList<>();
    private ItemStack target = ItemStack.EMPTY;
    private int wait;

    public CraftingCpuBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.CRAFTING_CPU.get(), pos, state);
    }

    public Container container() {
        return patterns;
    }

    public IEnergyStorage energyHandler() {
        return energy;
    }

    public List<PatternContents> patterns() {
        List<PatternContents> list = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            PatternContents p = PatternItem.pattern(patterns.getItem(i));
            if (p != null) list.add(p);
        }
        return list;
    }

    public boolean busy() {
        return !steps.isEmpty() || !buffer.isEmpty();
    }

    public ItemStack target() {
        return target;
    }

    public int stepsLeft() {
        int n = 0;
        for (CraftingPlan.Step s : steps) n += s.batches();
        return n;
    }

    /** Starts a job: takes the plan's ingredients from the network. False if they're no longer all there. */
    boolean start(Network net, CraftingPlan plan, ItemStack what) {
        if (busy() || !plan.ok()) return false;
        List<ItemStack> taken = new ArrayList<>();
        for (BinContents t : plan.take) {
            ItemStack got = net.extract(t.item(), t.count(), false);
            taken.add(got);
            if (got.getCount() < t.count()) {
                for (ItemStack back : taken) net.insert(back, false);
                return false;
            }
        }
        for (ItemStack s : taken) CraftingPlan.add(buffer, s, s.getCount());
        steps.addAll(plan.steps);
        target = what.copyWithCount(1);
        setChanged();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CraftingCpuBlockEntity cpu) {
        if (!cpu.busy() || ++cpu.wait < TICKS_PER_BATCH) return;
        cpu.wait = 0;
        Network net = Network.scan(level, pos);
        if (!net.online()) return;
        if (cpu.steps.isEmpty()) {                                 // finished: everything back into the network
            cpu.flush(net, level, pos);
            return;
        }
        if (cpu.energy.getEnergyStored() < FE_PER_BATCH) return;
        CraftingPlan.Step step = cpu.steps.get(0);
        if (!cpu.batch(step.pattern(), (ServerLevel) level)) {     // ingredients gone missing: give up
            cpu.steps.clear();
            cpu.flush(net, level, pos);
            return;
        }
        cpu.energy.consume(FE_PER_BATCH);
        if (step.batches() <= 1) cpu.steps.remove(0);
        else cpu.steps.set(0, new CraftingPlan.Step(step.pattern(), step.batches() - 1));
        cpu.setChanged();
    }

    /** Crafts one batch of a pattern from the buffer. */
    private boolean batch(PatternContents p, ServerLevel level) {
        CraftingInput input = CraftingInput.of(3, 3, p.grid());
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (recipe.isEmpty()) return false;
        List<BinContents> trial = new ArrayList<>(buffer);
        for (ItemStack s : p.grid()) {
            if (!s.isEmpty() && !CraftingPlan.remove(trial, s, 1)) return false;
        }
        ItemStack out = recipe.get().value().assemble(input, level.registryAccess());
        NonNullList<ItemStack> leftovers = recipe.get().value().getRemainingItems(input);
        buffer.clear();
        buffer.addAll(trial);
        if (!out.isEmpty()) CraftingPlan.add(buffer, out, out.getCount());
        for (ItemStack r : leftovers) if (!r.isEmpty()) CraftingPlan.add(buffer, r, r.getCount());
        return true;
    }

    private void flush(Network net, Level level, BlockPos pos) {
        for (BinContents c : buffer) {
            int left = c.count();
            while (left > 0) {
                int n = Math.min(left, c.item().getMaxStackSize());
                ItemStack rest = net.insert(c.item().copyWithCount(n), false);
                if (!rest.isEmpty()) Block.popResource(level, pos.above(), rest);
                left -= n;
            }
        }
        buffer.clear();
        target = ItemStack.EMPTY;
        setChanged();
    }

    /** When broken: patterns and whatever a job holds drop. */
    public void dropAll(Level level, BlockPos pos) {
        for (BinContents c : buffer) {
            int left = c.count();
            while (left > 0) {
                int n = Math.min(left, c.item().getMaxStackSize());
                Block.popResource(level, pos, c.item().copyWithCount(n));
                left -= n;
            }
        }
        buffer.clear();
        steps.clear();
        net.minecraft.world.Containers.dropContents(level, pos, patterns);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < SLOTS; i++) list.set(i, patterns.getItem(i));
        ContainerHelper.saveAllItems(tag, list, registries);
        tag.putInt("Energy", energy.getEnergyStored());
        var ops = registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        tag.put("Buffer", BinContents.CODEC.listOf().encodeStart(ops, buffer).getOrThrow());
        ListTag stepTags = new ListTag();
        for (CraftingPlan.Step s : steps) {
            CompoundTag t = new CompoundTag();
            t.put("Pattern", PatternContents.CODEC.encodeStart(ops, s.pattern()).getOrThrow());
            t.putInt("Batches", s.batches());
            stepTags.add(t);
        }
        tag.put("Steps", stepTags);
        if (!target.isEmpty()) tag.put("Target", target.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, list, registries);
        for (int i = 0; i < SLOTS; i++) patterns.setItem(i, list.get(i));
        energy.setEnergy(tag.getInt("Energy"));
        var ops = registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        buffer.clear();
        if (tag.contains("Buffer")) BinContents.CODEC.listOf().parse(ops, tag.get("Buffer")).result().ifPresent(buffer::addAll);
        steps.clear();
        for (Tag t : tag.getList("Steps", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            PatternContents.CODEC.parse(ops, c.get("Pattern")).result().ifPresent(p -> steps.add(new CraftingPlan.Step(p, c.getInt("Batches"))));
        }
        target = tag.contains("Target") ? ItemStack.parse(registries, tag.getCompound("Target")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
    }
}
