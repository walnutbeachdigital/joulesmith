package com.walnutbeachdigital.joulesmith.item;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.walnutbeachdigital.joulesmith.registry.JSBlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Upgrades a cable, duct, pipe, battery, crate, bulk bin, tank or pump to the next tier where it stands,
 * keeping everything in it and its settings. The Reinforced kit takes tier 1 to 2, the Advanced kit 2 to 3
 * (and a Pump to an Advanced Pump), the Elite kit 3 to 4 and the Ultimate kit 4 to 5.
 */
public class UpgradeKitItem extends Item {
    private final int tier;
    private Map<Block, Block> upgrades;

    public UpgradeKitItem(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    private Map<Block, Block> upgrades() {
        if (upgrades == null) {
            List<List<Supplier<? extends Block>>> chains = List.of(
                    List.of(JSBlocks.BASIC_BATTERY, JSBlocks.REINFORCED_BATTERY, JSBlocks.ADVANCED_BATTERY,
                            JSBlocks.ELITE_BATTERY, JSBlocks.ULTIMATE_BATTERY),
                    List.of(JSBlocks.BASIC_CRATE, JSBlocks.REINFORCED_CRATE, JSBlocks.ADVANCED_CRATE,
                            JSBlocks.ELITE_CRATE, JSBlocks.ULTIMATE_CRATE),
                    List.of(JSBlocks.BASIC_BULK_BIN, JSBlocks.REINFORCED_BULK_BIN, JSBlocks.ADVANCED_BULK_BIN,
                            JSBlocks.ELITE_BULK_BIN, JSBlocks.ULTIMATE_BULK_BIN),
                    List.of(JSBlocks.FLUID_TANK, JSBlocks.REINFORCED_FLUID_TANK, JSBlocks.ADVANCED_FLUID_TANK,
                            JSBlocks.ELITE_FLUID_TANK, JSBlocks.ULTIMATE_FLUID_TANK),
                    List.of(JSBlocks.SOLAR_PANEL, JSBlocks.REINFORCED_SOLAR_PANEL, JSBlocks.ADVANCED_SOLAR_PANEL,
                            JSBlocks.ELITE_SOLAR_PANEL, JSBlocks.ULTIMATE_SOLAR_PANEL));
            Map<Block, Block> map = new java.util.HashMap<>();
            for (var chain : chains) map.put(chain.get(tier - 2).get(), chain.get(tier - 1).get());
            if (tier == 3) map.put(JSBlocks.PUMP.get(), JSBlocks.ADVANCED_PUMP.get());
            upgrades = map;
        }
        return upgrades;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (level.getBlockEntity(pos) instanceof com.walnutbeachdigital.joulesmith.transport.conduit.BundleBlockEntity bundle) {
            return upgradeConduit(context, bundle);
        }
        Block next = upgrades().get(state.getBlock());
        if (next == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity old = level.getBlockEntity(pos);
        CompoundTag saved = old != null ? old.saveWithoutMetadata(level.registryAccess()) : null;
        // take the old block entity out first, so nothing in it is dropped when the block is swapped
        if (old != null) level.removeBlockEntity(pos);
        level.setBlock(pos, copyProperties(state, next.defaultBlockState()), Block.UPDATE_ALL);
        BlockEntity fresh = level.getBlockEntity(pos);
        if (fresh != null && saved != null) {
            fresh.loadWithComponents(saved, level.registryAccess());
            fresh.setChanged();
        }
        level.invalidateCapabilities(pos);
        for (Direction dir : Direction.values()) level.invalidateCapabilities(pos.relative(dir));
        BlockState now = level.getBlockState(pos);
        level.sendBlockUpdated(pos, now, now, Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, next);
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
        Player player = context.getPlayer();
        if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    /** A duct, pipe or cable in a bundle: the one clicked, if it's a tier below the kit (else any that is). */
    private InteractionResult upgradeConduit(UseOnContext context, com.walnutbeachdigital.joulesmith.transport.conduit.BundleBlockEntity bundle) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        var clicked = com.walnutbeachdigital.joulesmith.transport.conduit.BundleBlock.target(level, pos, context.getClickLocation());
        com.walnutbeachdigital.joulesmith.transport.conduit.Conduit target = clicked == null ? null : bundle.get(clicked);
        if (target == null || target.tier() != tier - 1 || target.type.maxTier < tier) {
            target = null;
            for (var c : bundle.all()) if (c.tier() == tier - 1 && c.type.maxTier >= tier) { target = c; break; }
        }
        if (target == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        target.setTier(tier);
        level.invalidateCapabilities(pos);
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
        Player player = context.getPlayer();
        if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState copyProperties(BlockState from, BlockState to) {
        for (Property property : from.getProperties()) {
            if (to.hasProperty(property)) to = to.setValue(property, from.getValue(property));
        }
        return to;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.joulesmith.upgrade_kit." + tier).withStyle(ChatFormatting.GRAY));
    }
}
