package com.walnutbeachdigital.joulesmith.automation;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.item.UpgradeCardItem;
import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.MachineFace;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Crafts any 3x3 recipe set in its pattern grid, taking the ingredients from inventories touching its input
 * faces (chests, crates, other machines). Results and leftovers (empty buckets) go to its output slots.
 * Click a pattern slot with an item to set it, with an empty hand to clear it.
 */
public class AutocrafterBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 20_000, MAX_INPUT = 400, ENERGY_PER_CRAFT = 200, TIME = 20;

    static List<SlotDef> layout() {
        List<SlotDef> slots = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) slots.add(SlotDef.ghost(30 + col * 18, 17 + row * 18));
        }
        slots.add(SlotDef.output(116, 35));
        slots.add(SlotDef.byproduct(142, 26));
        slots.add(SlotDef.byproduct(142, 44));
        return slots;
    }

    public AutocrafterBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.AUTOCRAFTER.get(), pos, state, layout(), CAPACITY, MAX_INPUT, 0, true);
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.NONE;
    }

    private List<ItemStack> pattern() {
        List<ItemStack> list = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) list.add(items.getStackInSlot(i));
        return list;
    }

    /** The inventories it may take ingredients from: those touching faces set to take input. */
    private List<IItemHandler> sources(Level level, BlockPos pos) {
        List<IItemHandler> list = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (!sideMode(MachineFace.of(facing(), dir)).accepts()) continue;
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(dir), dir.getOpposite());
            if (handler != null) list.add(handler);
        }
        return list;
    }

    /** How many of {@code wanted} the sources hold in total. */
    private static int available(List<IItemHandler> sources, ItemStack wanted) {
        int n = 0;
        for (IItemHandler source : sources) {
            for (int s = 0; s < source.getSlots(); s++) {
                ItemStack there = source.getStackInSlot(s);
                if (ItemStack.isSameItemSameComponents(there, wanted)) n += there.getCount();
            }
        }
        return n;
    }

    /** Takes one of {@code wanted} from the sources. */
    private static ItemStack take(List<IItemHandler> sources, ItemStack wanted) {
        for (IItemHandler source : sources) {
            for (int s = 0; s < source.getSlots(); s++) {
                if (!ItemStack.isSameItemSameComponents(source.getStackInSlot(s), wanted)) continue;
                ItemStack got = source.extractItem(s, 1, false);
                if (!got.isEmpty()) return got;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        if (!allowed) return false;
        List<ItemStack> pattern = pattern();
        if (pattern.stream().allMatch(ItemStack::isEmpty)) return false;
        CraftingInput preview = CraftingInput.of(3, 3, pattern);
        RecipeHolder<CraftingRecipe> recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, preview, level).orElse(null);
        if (recipe == null) return false;
        ItemStack result = recipe.value().assemble(preview, level.registryAccess());
        List<ItemStack> outputs = new ArrayList<>();
        outputs.add(result);
        outputs.addAll(recipe.value().getRemainingItems(preview));
        outputs.removeIf(ItemStack::isEmpty);
        if (!OutputHelper.fits(items, slotDefs(), outputs)) return false;
        int perTick = (int) Math.ceil(ENERGY_PER_CRAFT * Math.max(0.4, 1 - 0.2 * upgrades(UpgradeCardItem.Type.EFFICIENCY)) / TIME);
        if (energy.getEnergyStored() < perTick) return false;
        // check every ingredient is there before taking any
        List<IItemHandler> sources = sources(level, pos);
        for (int i = 0; i < 9; i++) {
            ItemStack wanted = pattern.get(i);
            if (wanted.isEmpty()) continue;
            int needed = 0;
            for (ItemStack p : pattern) if (ItemStack.isSameItemSameComponents(p, wanted)) needed++;
            if (available(sources, wanted) < needed) {
                progress = 0;
                return false;
            }
        }
        energy.consume(perTick);
        maxProgress = Math.max(1, (int) (TIME / (1 + 0.5 * upgrades(UpgradeCardItem.Type.SPEED))));
        if (++progress < maxProgress) return true;
        progress = 0;
        List<ItemStack> taken = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            ItemStack wanted = pattern.get(i);
            taken.add(wanted.isEmpty() ? ItemStack.EMPTY : take(sources, wanted));
        }
        CraftingInput input = CraftingInput.of(3, 3, taken);
        if (!recipe.value().matches(input, level)) {
            OutputHelper.store(items, slotDefs(), taken.stream().filter(s -> !s.isEmpty()).toList());   // give back
            return false;
        }
        List<ItemStack> made = new ArrayList<>();
        made.add(recipe.value().assemble(input, level.registryAccess()));
        made.addAll(recipe.value().getRemainingItems(input));
        made.removeIf(ItemStack::isEmpty);
        for (ItemStack rest : OutputHelper.store(items, slotDefs(), made)) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, rest);
        }
        return true;
    }
}
