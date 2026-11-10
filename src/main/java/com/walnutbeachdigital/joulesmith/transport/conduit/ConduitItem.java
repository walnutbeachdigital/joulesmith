package com.walnutbeachdigital.joulesmith.transport.conduit;

import com.walnutbeachdigital.joulesmith.registry.JSBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * A duct, pipe or cable. Used on a conduit bundle that doesn't have one of its kind yet, it goes in alongside
 * what's there; otherwise it's placed as a new bundle, like a block.
 */
public class ConduitItem extends Item {
    private final ConduitType type;
    private final int tier;

    public ConduitItem(ConduitType type, int tier, Properties properties) {
        super(properties);
        this.type = type;
        this.tier = tier;
    }

    public ConduitType type() {
        return type;
    }

    public int tier() {
        return tier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        if (addTo(level, clicked, context)) return InteractionResult.sidedSuccess(level.isClientSide);
        BlockPlaceContext place = new BlockPlaceContext(context);
        BlockPos pos = place.getClickedPos();
        if (!pos.equals(clicked) && addTo(level, pos, context)) return InteractionResult.sidedSuccess(level.isClientSide);
        if (!place.canPlace()) return InteractionResult.FAIL;
        Player player = context.getPlayer();
        if (player != null && !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        BlockState state = JSBlocks.CONDUIT_BUNDLE.get().defaultBlockState();
        if (!level.isClientSide) {
            if (!level.setBlock(pos, state, Block.UPDATE_ALL)) return InteractionResult.FAIL;
            if (level.getBlockEntity(pos) instanceof BundleBlockEntity bundle) bundle.add(type.create(tier));
            placed(level, pos, state, context);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Adds this conduit to a bundle at {@code pos} if there is one without this kind. */
    private boolean addTo(Level level, BlockPos pos, UseOnContext context) {
        if (!(level.getBlockEntity(pos) instanceof BundleBlockEntity bundle) || bundle.has(type)) return false;
        if (!level.isClientSide) {
            bundle.add(type.create(tier));
            placed(level, pos, level.getBlockState(pos), context);
        }
        return true;
    }

    private void placed(Level level, BlockPos pos, BlockState state, UseOnContext context) {
        SoundType sound = type == ConduitType.ENERGY ? SoundType.WOOL : SoundType.METAL;
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8f);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(context.getPlayer(), state));
        Player player = context.getPlayer();
        if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
    }
}
