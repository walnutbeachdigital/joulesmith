package com.walnutbeachdigital.joulesmith.gear;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Hammer (stone and ores) and Excavator (dirt, sand, gravel): breaks the 3x3 square around the block
 * you mine, facing you, but digs slower than a pickaxe or shovel. Sneak to break just one block.
 * Each extra block costs durability, and drops and enchantments work as if mined by hand.
 */
public class AreaToolItem extends DiggerItem {
    private static final float SPEED_FACTOR = 0.4f;
    private static final ThreadLocal<Boolean> BREAKING = ThreadLocal.withInitial(() -> false);

    public AreaToolItem(Tier tier, TagKey<Block> mineable, Properties properties) {
        super(tier, mineable, properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return super.getDestroySpeed(stack, state) * SPEED_FACTOR;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.mineBlock(stack, level, state, pos, miner);
        if (BREAKING.get() || level.isClientSide || !(miner instanceof ServerPlayer player) || player.isShiftKeyDown()) {
            return result;
        }
        if (!isCorrectToolForDrops(stack, state)) return result;
        Direction face = facing(player);
        float hardness = state.getDestroySpeed(level, pos);
        BREAKING.set(true);
        try {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) continue;
                    BlockPos other = switch (face.getAxis()) {
                        case X -> pos.offset(0, a, b);
                        case Y -> pos.offset(a, 0, b);
                        case Z -> pos.offset(a, b, 0);
                    };
                    BlockState otherState = level.getBlockState(other);
                    float otherHardness = otherState.getDestroySpeed(level, other);
                    // only blocks this tool is for, and nothing much harder than the one mined
                    if (otherState.isAir() || otherHardness < 0 || otherHardness > hardness + 1.5f) continue;
                    if (!isCorrectToolForDrops(stack, otherState) || stack.isEmpty()) continue;
                    player.gameMode.destroyBlock(other);
                }
            }
        } finally {
            BREAKING.set(false);
        }
        return result;
    }

    /** Which face of the block the player is looking at. */
    private static Direction facing(ServerPlayer player) {
        double reach = player.blockInteractionRange();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(reach));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getDirection() : Direction.UP;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.joulesmith.area_tool").withStyle(ChatFormatting.GRAY));
    }
}
