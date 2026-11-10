package com.walnutbeachdigital.joulesmith.network;

import java.util.List;

import com.walnutbeachdigital.joulesmith.gear.powered.Module;
import com.walnutbeachdigital.joulesmith.gear.powered.PoweredItem;
import com.walnutbeachdigital.joulesmith.registry.JSDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A Network Terminal in your hand. Sneak and use it on any block of a storage network to link it; then use
 * it anywhere within {@link #RANGE} blocks of that block, in the same dimension, to open the terminal.
 * {@link #COST} FE each time it opens; charge it like any powered gear.
 */
public class WirelessTerminalItem extends Item implements PoweredItem {
    public static final int RANGE = 64, COST = 100;

    public WirelessTerminalItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override public int capacity() { return 200_000; }
    @Override public int transferRate() { return 2_000; }
    @Override public boolean accepts(Module module) { return false; }

    public static GlobalPos link(ItemStack stack) {
        return stack.get(JSDataComponents.LINK.get());
    }

    /** Whether {@code player} holds a Wireless Terminal linked to {@code pos} and is close enough. */
    public static boolean reaches(Player player, BlockPos pos) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            GlobalPos link = held.getItem() instanceof WirelessTerminalItem ? link(held) : null;
            if (link != null && link.pos().equals(pos) && link.dimension() == player.level().dimension()
                    && player.blockPosition().distSqr(pos) <= (double) RANGE * RANGE) return true;
        }
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        Level level = context.getLevel();
        if (!NetworkPart.at(level, context.getClickedPos())) return InteractionResult.PASS;
        if (!level.isClientSide) {
            context.getItemInHand().set(JSDataComponents.LINK.get(), GlobalPos.of(level.dimension(), context.getClickedPos().immutable()));
            player.displayClientMessage(Component.translatable("message.joulesmith.wireless_terminal.linked"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        GlobalPos link = link(stack);
        String problem = link == null ? "unlinked"
                : link.dimension() != level.dimension() || player.blockPosition().distSqr(link.pos()) > (double) RANGE * RANGE ? "range"
                : !level.isLoaded(link.pos()) || !NetworkPart.at(level, link.pos()) ? "gone"
                : !PoweredItem.use(stack, COST, player) ? "power" : null;
        if (problem != null) {
            player.displayClientMessage(Component.translatable("message.joulesmith.wireless_terminal." + problem), true);
            return InteractionResultHolder.fail(stack);
        }
        BlockPos pos = link.pos();
        ((ServerPlayer) player).openMenu(new SimpleMenuProvider((id, inv, p) -> TerminalMenu.wireless(id, inv, pos),
                Component.translatable("item.joulesmith.wireless_terminal")), buf -> buf.writeBlockPos(pos).writeBoolean(true));
        return InteractionResultHolder.consume(stack);
    }

    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return PoweredItem.barWidth(stack); }
    @Override public int getBarColor(ItemStack stack) { return BAR_COLOR; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GlobalPos link = link(stack);
        tooltip.add(link == null ? Component.translatable("tooltip.joulesmith.wireless_terminal.unlinked").withStyle(ChatFormatting.GRAY)
                : Component.translatable("tooltip.joulesmith.wireless_terminal.linked", link.pos().getX(), link.pos().getY(), link.pos().getZ())
                        .withStyle(ChatFormatting.AQUA));
        PoweredItem.appendTooltip(stack, tooltip);
    }
}
