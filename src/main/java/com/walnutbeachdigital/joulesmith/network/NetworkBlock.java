package com.walnutbeachdigital.joulesmith.network;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/** The storage network's blocks that face you: the Network Core, the Drive Bay and the Network Terminal. */
public class NetworkBlock extends BaseEntityBlock implements NetworkPart {
    public enum Kind { CORE, BAY, TERMINAL, CRAFTING_TERMINAL }

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final Kind kind;
    private final MapCodec<NetworkBlock> codec;

    public NetworkBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.codec = simpleCodec(p -> new NetworkBlock(kind, p));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case CORE -> new NetworkCoreBlockEntity(pos, state);
            case BAY -> new DriveBayBlockEntity(pos, state);
            case TERMINAL, CRAFTING_TERMINAL -> null;
        };
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || kind != Kind.CORE ? null
                : createTickerHelper(type, JSBlockEntities.NETWORK_CORE.get(), NetworkCoreBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        switch (kind) {
            case BAY -> {
                if (level.getBlockEntity(pos) instanceof DriveBayBlockEntity bay) {
                    player.openMenu(new SimpleMenuProvider((id, inv, p) -> new DriveBayMenu(id, inv, bay.container()),
                            Component.translatable("block.joulesmith.drive_bay")));
                }
            }
            case TERMINAL -> player.openMenu(new SimpleMenuProvider((id, inv, p) -> new TerminalMenu(id, inv, pos),
                    Component.translatable("block.joulesmith.network_terminal")), buf -> buf.writeBlockPos(pos));
            case CRAFTING_TERMINAL -> player.openMenu(new SimpleMenuProvider((id, inv, p) -> new CraftingTerminalMenu(id, inv, pos),
                    Component.translatable("block.joulesmith.crafting_terminal")), buf -> buf.writeBlockPos(pos));
            case CORE -> {
                if (level.getBlockEntity(pos) instanceof NetworkCoreBlockEntity core) {
                    Network net = Network.scan(level, pos);
                    player.displayClientMessage(Component.translatable(net.core == null ? "message.joulesmith.network.cores"
                                    : core.powered() ? "message.joulesmith.network.online" : "message.joulesmith.network.unpowered",
                            net.parts.size(), net.bays.size()), true);
                }
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DriveBayBlockEntity bay) {
            Containers.dropContents(level, pos, bay.container());
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
