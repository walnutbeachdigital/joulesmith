package com.walnutbeachdigital.joulesmith.transport.item;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.Joulesmith;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Items seen travelling through item ducts. Moves happen in one go on the server; each one is sent to the
 * players nearby as a path (from the face it came in at, through the middle of every duct, to the face it
 * leaves by), and their game draws the item sliding along it (client/ItemTransitRenderer).
 */
public final class ItemTransits {
    /** How many ticks an item takes to cross one duct. */
    public static final int TICKS_PER_BLOCK = 4;
    /** Paths longer than this aren't drawn. */
    public static final int MAX_POINTS = 64;

    public record Transit(ItemStack stack, List<Vec3> points, long start) {
        public long end() {
            return start + (long) (points.size() - 1) * TICKS_PER_BLOCK;
        }
    }

    /** What the client is drawing (filled from the messages, read by the renderer). */
    public static final List<Transit> CLIENT = new ArrayList<>();

    public record Message(ItemStack stack, List<Vec3> points) implements CustomPacketPayload {
        public static final Type<Message> TYPE = new Type<>(Joulesmith.id("item_transit"));
        private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, v -> (float) v.x, ByteBufCodecs.FLOAT, v -> (float) v.y, ByteBufCodecs.FLOAT, v -> (float) v.z,
                (x, y, z) -> new Vec3(x, y, z));
        public static final StreamCodec<RegistryFriendlyByteBuf, Message> CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Message::stack,
                VEC.apply(ByteBufCodecs.list()), Message::points,
                Message::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Message.TYPE, Message.CODEC, (msg, context) -> {
            var level = context.player().level();
            synchronized (CLIENT) {
                CLIENT.add(new Transit(msg.stack(), msg.points(), level.getGameTime()));
                if (CLIENT.size() > 512) CLIENT.remove(0);
            }
        });
    }

    /** Tells the players near {@code at} that {@code stack} just went along {@code points}. */
    public static void send(ServerLevel level, BlockPos at, ItemStack stack, List<Vec3> points) {
        if (points.size() < 2 || points.size() > MAX_POINTS || stack.isEmpty()) return;
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(at), new Message(stack.copyWithCount(1), points));
    }

    private ItemTransits() {}
}
