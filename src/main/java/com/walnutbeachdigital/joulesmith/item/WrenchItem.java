package com.walnutbeachdigital.joulesmith.item;

import java.util.List;

import com.walnutbeachdigital.joulesmith.machine.MachineBlock;
import com.walnutbeachdigital.joulesmith.transport.conduit.BundleBlock;
import com.walnutbeachdigital.joulesmith.transport.conduit.BundleBlockEntity;
import com.walnutbeachdigital.joulesmith.transport.conduit.Conduit;
import com.walnutbeachdigital.joulesmith.transport.conduit.ConduitType;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The wrench. On a pipe side: push, pull, both or off (or join/split two pipes). Sneaking on an item duct
 * side: take off its filter card, or change its routing. On a machine: turn it to face the next way.
 */
public class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        if (level.getBlockEntity(pos) instanceof BundleBlockEntity bundle) {
            ConduitType type = BundleBlock.target(level, pos, context.getClickLocation());
            Conduit conduit = type == null ? null : bundle.get(type);
            if (conduit == null) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            Direction side = BundleBlock.side(level, pos, type, context.getClickLocation(), context.getClickedFace());
            Component message = player != null && player.isSecondaryUseActive() ? conduit.sneakWrench(player, side) : conduit.wrench(player, side);
            if (player != null && message != null) player.displayClientMessage(message, true);
            return InteractionResult.SUCCESS;
        }
        if (state.getBlock() instanceof MachineBlock && state.hasProperty(MachineBlock.FACING)) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(MachineBlock.FACING, state.getValue(MachineBlock.FACING).getClockWise()), Block.UPDATE_ALL);
                level.invalidateCapabilities(pos);       // its faces now point other ways
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.joulesmith.wrench.pipe").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.joulesmith.wrench.sneak").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.joulesmith.wrench.machine").withStyle(ChatFormatting.GRAY));
    }
}
