package com.walnutbeachdigital.joulesmith.guide;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.walnutbeachdigital.joulesmith.Joulesmith;

import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Ticks off each player's Handbook quests. Once a second it checks every quest in the chapters they've opened:
 * a quest is done once they carry its item and every quest it needs is done. Done quests are saved on the
 * player (kept through death), announced above the hotbar with a chime, and sent to their game for the book.
 */
public final class HandbookProgress {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Joulesmith.MODID);

    public static final Supplier<AttachmentType<List<String>>> DONE = ATTACHMENTS.register("handbook",
            () -> AttachmentType.<List<String>>builder(() -> new ArrayList<>())
                    .serialize(Codec.STRING.listOf())
                    .copyOnDeath()
                    .build());

    /** Whether the player has been handed their first Handbook. */
    public static final Supplier<AttachmentType<Boolean>> GIVEN = ATTACHMENTS.register("handbook_given",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    /** The server telling a player's game which Handbook quests they've done. */
    public record Sync(List<String> done) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(Joulesmith.id("handbook_sync"));
        public static final StreamCodec<ByteBuf, Sync> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Sync::done, Sync::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** The quests this player's game knows are done (client side). */
    private static Set<String> clientDone = new LinkedHashSet<>();

    public static Set<String> clientDone() {
        return java.util.Collections.unmodifiableSet(clientDone);
    }

    public static boolean clientIsDone(String id) {
        return clientDone.contains(id);
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        modBus.addListener(HandbookProgress::payloads);
        NeoForge.EVENT_BUS.addListener(HandbookProgress::tick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) { give(p); sync(p); } });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) sync(p); });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) sync(p); });
    }

    private static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Sync.TYPE, Sync.CODEC, (msg, context) -> clientDone = new LinkedHashSet<>(msg.done()));
    }

    public static Set<String> done(Player player) {
        return new LinkedHashSet<>(player.getData(DONE));
    }

    /** Whether a chapter is open: its opener quest is done (the first chapter always is). */
    public static boolean open(HandbookQuests.Chapter chapter, Set<String> done) {
        return chapter.opener == null || done.contains(chapter.opener);
    }

    private static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) check(player);
    }

    /** Ticks off whatever's newly done (going round again, since one quest can open the next). */
    public static void check(ServerPlayer player) {
        Set<String> done = done(player);
        List<HandbookQuests.Quest> newly = new ArrayList<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (HandbookQuests.Quest q : HandbookQuests.all()) {
                if (done.contains(q.id()) || !open(q.chapter(), done) || !done.containsAll(q.needs()) || !carries(player, q)) continue;
                done.add(q.id());
                newly.add(q);
                changed = true;
            }
        }
        if (newly.isEmpty()) return;
        player.setData(DONE, new ArrayList<>(done));
        sync(player);
        HandbookQuests.Quest last = newly.get(newly.size() - 1);
        player.displayClientMessage(Component.translatable("handbook.joulesmith.quest_done", last.title()).withStyle(ChatFormatting.GREEN), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    private static boolean carries(Player player, HandbookQuests.Quest q) {
        var inv = player.getInventory();
        for (var list : List.of(inv.items, inv.armor, inv.offhand)) {
            for (ItemStack s : list) if (s.is(q.item())) return true;
        }
        return false;
    }

    /** Hands a new player their Handbook (once; the config can turn it off). */
    private static void give(ServerPlayer player) {
        if (player.getData(GIVEN) || !com.walnutbeachdigital.joulesmith.JSConfig.GIVE_HANDBOOK.get()) return;
        player.setData(GIVEN, true);
        ItemStack book = new ItemStack(com.walnutbeachdigital.joulesmith.registry.JSItems.ENGINEERS_HANDBOOK.get());
        if (!player.getInventory().add(book)) player.drop(book, false);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new Sync(new ArrayList<>(done(player))));
    }

    private HandbookProgress() {}
}
