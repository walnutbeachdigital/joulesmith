package com.walnutbeachdigital.joulesmith.farming;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.recipe.ChanceResult;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Grows a crop indoors with no soil: put seeds (or a melon or pumpkin seed, sugar cane, cactus, bamboo,
 * berries, cocoa beans or nether wart) in its top slot. Each harvest takes 30 seconds, 100 mB of water and
 * power under its grow lights, and gives what the ripe plant drops; the seeds stay in. Fertilizer in its
 * lower slot halves the time, one per harvest. Beds can be stacked on top of each other.
 */
public class HydroponicBedBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 20_000, MAX_INPUT = 200, TIME = 600, ENERGY_PER_TICK = 20, WATER = 100, TANK = 8_000;

    private final FluidTank tank = new FluidTank(TANK, stack -> stack.getFluid().isSame(Fluids.WATER)) {
        @Override protected void onContentsChanged() { setChanged(); }
    };

    private static List<SlotDef> layout() {
        return List.of(SlotDef.input(56, 26), SlotDef.input(56, 48),
                SlotDef.byproduct(112, 26), SlotDef.byproduct(130, 26), SlotDef.byproduct(112, 44), SlotDef.byproduct(130, 44));
    }

    public HydroponicBedBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.HYDROPONIC_BED.get(), pos, state, layout(), CAPACITY, MAX_INPUT, 0, true);
    }

    @Override
    public List<TankDef> tanks() {
        return List.of(new TankDef(tank, 30));
    }

    @Override
    protected FluidTank inputTank() {
        return tank;
    }

    /** What one harvest of {@code seed} gives, or nothing if it can't grow here. */
    public static List<ItemStack> harvest(ServerLevel level, BlockPos pos, ItemStack seed) {
        if (seed.isEmpty()) return List.of();
        if (seed.is(Items.MELON_SEEDS)) return Block.getDrops(Blocks.MELON.defaultBlockState(), level, pos, null);
        if (seed.is(Items.PUMPKIN_SEEDS)) return List.of(new ItemStack(Items.PUMPKIN));
        if (!(seed.getItem() instanceof BlockItem item)) return List.of();
        Block block = item.getBlock();
        BlockState ripe;
        if (block instanceof CropBlock crop) ripe = crop.getStateForAge(crop.getMaxAge());
        else if (block instanceof NetherWartBlock) ripe = block.defaultBlockState().setValue(NetherWartBlock.AGE, 3);
        else if (block instanceof SweetBerryBushBlock) return List.of(new ItemStack(Items.SWEET_BERRIES, 2));
        else if (block instanceof CocoaBlock) return List.of(new ItemStack(Items.COCOA_BEANS, 2));
        else if (block instanceof SugarCaneBlock || block instanceof CactusBlock || block instanceof BambooSaplingBlock) {
            return List.of(new ItemStack(seed.getItem(), 2));
        } else if (block instanceof StemBlock) return List.of();
        else return List.of();
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack drop : Block.getDrops(ripe, level, pos, null)) drops.add(drop.copy());
        for (ItemStack drop : drops) {                        // the planted seed stays in the bed
            if (ItemStack.isSameItem(drop, seed)) {
                drop.shrink(1);
                break;
            }
        }
        drops.removeIf(ItemStack::isEmpty);
        return drops;
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        if (stack.getItem() instanceof FertilizerItem) return true;
        return level instanceof ServerLevel server && !harvest(server, worldPosition, stack).isEmpty();
    }

    @Override
    protected Process findProcess() {
        if (!(level instanceof ServerLevel server) || tank.getFluidAmount() < WATER) return null;
        // either input slot may hold the seed or the fertilizer (pipes fill whichever is free)
        int seedSlot = items.getStackInSlot(0).getItem() instanceof FertilizerItem ? 1 : 0;
        int fertSlot = 1 - seedSlot;
        List<ItemStack> drops = harvest(server, worldPosition, items.getStackInSlot(seedSlot));
        if (drops.isEmpty()) return null;
        boolean fertilized = items.getStackInSlot(fertSlot).getItem() instanceof FertilizerItem;
        int[] consume = new int[slotDefs().size()];
        if (fertilized) consume[fertSlot] = 1;
        List<ChanceResult> out = drops.stream().map(ChanceResult::always).toList();
        return new Process(out, fertilized ? TIME / 2 : TIME, ENERGY_PER_TICK, consume, new FluidStack(Fluids.WATER, WATER), FluidStack.EMPTY);
    }
}
