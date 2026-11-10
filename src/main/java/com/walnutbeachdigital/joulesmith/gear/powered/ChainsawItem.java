package com.walnutbeachdigital.joulesmith.gear.powered;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Cuts wood fast and fells a whole tree from one log (sneak to cut one). Also an axe in a fight. */
public class ChainsawItem extends PoweredToolItem {
    private static final int MAX_LOGS = 256;
    private static final ThreadLocal<Boolean> BREAKING = ThreadLocal.withInitial(() -> false);

    public ChainsawItem(Properties properties) {
        super(400_000, properties.component(DataComponents.TOOL, new Tool(List.of(
                Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_AXE, 10.0f),
                Tool.Rule.overrideSpeed(BlockTags.LEAVES, 12.0f)), 1.0f, 0)));
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        super.mineBlock(stack, level, state, pos, miner);
        if (BREAKING.get() || level.isClientSide || !(miner instanceof ServerPlayer player) || player.isShiftKeyDown()
                || !state.is(BlockTags.LOGS)) {
            return true;
        }
        BREAKING.set(true);
        try {
            // every log touching this one, diagonals too, so branches come down with the trunk
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            Set<BlockPos> seen = new HashSet<>();
            queue.add(pos);
            seen.add(pos);
            int felled = 0;
            while (!queue.isEmpty() && felled < MAX_LOGS) {
                BlockPos at = queue.poll();
                for (BlockPos next : BlockPos.betweenClosed(at.offset(-1, 0, -1), at.offset(1, 1, 1))) {
                    if (seen.size() > MAX_LOGS * 2 || !level.getBlockState(next).is(BlockTags.LOGS)) continue;
                    BlockPos immutable = next.immutable();
                    if (!seen.add(immutable)) continue;
                    if (!charged(stack) && !player.getAbilities().instabuild) return true;
                    if (player.gameMode.destroyBlock(immutable)) {
                        felled++;
                        queue.add(immutable);
                    }
                }
            }
        } finally {
            BREAKING.set(false);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.joulesmith.chainsaw").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
