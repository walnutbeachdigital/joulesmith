package com.walnutbeachdigital.joulesmith.network;

import java.util.List;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.storage.bin.BinContents;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The Network Terminal's two messages: the grid's contents (to the client) and grid clicks (to the server). */
public final class TerminalPayloads {
    public record Contents(int containerId, List<BinContents> contents, List<ItemStack> craftable, boolean online) implements CustomPacketPayload {
        public static final Type<Contents> TYPE = new Type<>(Joulesmith.id("terminal_contents"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Contents> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Contents::containerId,
                BinContents.STREAM_CODEC.apply(ByteBufCodecs.list()), Contents::contents,
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), Contents::craftable,
                ByteBufCodecs.BOOL, Contents::online,
                Contents::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Action(int containerId, ItemStack like, int action) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Joulesmith.id("terminal_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::containerId,
                ItemStack.OPTIONAL_STREAM_CODEC, Action::like,
                ByteBufCodecs.VAR_INT, Action::action,
                Action::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(Contents.TYPE, Contents.CODEC, TerminalPayloads::onContents);
        registrar.playToServer(Action.TYPE, Action.CODEC, TerminalPayloads::onAction);
    }

    private static void onContents(Contents msg, IPayloadContext context) {
        if (context.player().containerMenu instanceof TerminalMenu menu && menu.containerId == msg.containerId()) {
            menu.receive(msg.contents(), msg.craftable(), msg.online());
        }
    }

    private static void onAction(Action msg, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer sp && sp.containerMenu instanceof TerminalMenu menu
                && menu.containerId == msg.containerId()) {
            menu.act(sp, msg.like(), msg.action());
        }
    }

    private TerminalPayloads() {}
}
