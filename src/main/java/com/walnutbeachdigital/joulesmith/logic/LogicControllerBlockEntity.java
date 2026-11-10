package com.walnutbeachdigital.joulesmith.logic;

import java.util.Arrays;

import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Six rules, each "if a reading on one side is below / above a value, power an output side". Checked twice
 * a second; an output side is powered (strength 15) while any rule pointing at it holds.
 */
public class LogicControllerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int RULES = 6;

    private final Rule[] rules = new Rule[RULES];
    private final int[] readings = new int[RULES];
    private int outputs;                              // a bit per world direction

    public LogicControllerBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.LOGIC_CONTROLLER.get(), pos, state);
        Arrays.fill(rules, Rule.DEFAULT);
    }

    public int power(Direction toward) {
        return (outputs >> toward.get3DDataValue() & 1) != 0 ? 15 : 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LogicControllerBlockEntity controller) {
        if (level.getGameTime() % 10 != 0) return;
        int out = 0;
        for (int i = 0; i < RULES; i++) {
            Rule rule = controller.rules[i];
            if (!rule.enabled()) continue;
            controller.readings[i] = rule.measure().read(level, pos, rule.side());
            if (rule.test(controller.readings[i])) out |= 1 << rule.output().get3DDataValue();
        }
        if (out != controller.outputs) {
            controller.outputs = out;
            controller.setChanged();
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }

    public void step(int rule, int field, boolean back) {
        if (rule < 0 || rule >= RULES) return;
        rules[rule] = rules[rule].step(field, back);
        setChanged();
    }

    /** Synced to the screen: each rule packed, each rule's last reading, then the outputs. */
    public ContainerData data() {
        return new ContainerData() {
            private int value(int index) {
                if (index < RULES) return rules[index].pack();
                if (index < 2 * RULES) return readings[index - RULES];
                return outputs;
            }

            @Override public int get(int i) { int v = value(i / 2); return i % 2 == 0 ? v & 0xFFFF : v >>> 16; }
            @Override public void set(int i, int value) { }
            @Override public int getCount() { return (2 * RULES + 1) * 2; }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new LogicMenu(id, inventory, worldPosition, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        int[] packed = new int[RULES];
        for (int i = 0; i < RULES; i++) packed[i] = rules[i].pack();
        tag.putIntArray("Rules", packed);
        tag.putInt("Outputs", outputs);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] packed = tag.getIntArray("Rules");
        for (int i = 0; i < Math.min(RULES, packed.length); i++) rules[i] = Rule.unpack(packed[i]);
        outputs = tag.getInt("Outputs");
    }
}
