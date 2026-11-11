package com.walnutbeachdigital.joulesmith.client;

import java.util.Iterator;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.walnutbeachdigital.joulesmith.transport.item.ItemTransits;
import com.walnutbeachdigital.joulesmith.transport.item.ItemTransits.Transit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Draws the items sliding through item ducts ({@link ItemTransits}), small enough to fit inside. */
public final class ItemTransitRenderer {
    private static final float SCALE = 0.22f;

    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double now = mc.level.getGameTime() + partial;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean any = false;
        synchronized (ItemTransits.CLIENT) {
            for (Transit t : ItemTransits.CLIENT) {
                Vec3 at = position(t, now);
                if (at == null || at.distanceToSqr(cam) > 64 * 64) continue;
                any = true;
                pose.pushPose();
                pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
                pose.mulPose(Axis.YP.rotationDegrees((float) ((now * 4) % 360)));
                pose.scale(SCALE * 2, SCALE * 2, SCALE * 2);
                int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(at));
                mc.getItemRenderer().renderStatic(t.stack(), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                        pose, buffers, mc.level, 0);
                pose.popPose();
            }
        }
        if (any) buffers.endBatch();
    }

    /** Where along its path a transit is at time {@code now}, or null if it hasn't started or has arrived. */
    private static Vec3 position(Transit t, double now) {
        List<Vec3> pts = t.points();
        double progress = (now - t.start()) / ItemTransits.TICKS_PER_BLOCK;
        if (progress < 0 || progress >= pts.size() - 1) return null;
        int i = (int) progress;
        double f = progress - i;
        Vec3 a = pts.get(i), b = pts.get(i + 1);
        return a.add(b.subtract(a).scale(f));
    }

    /** Forgets transits that have arrived. */
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            synchronized (ItemTransits.CLIENT) { ItemTransits.CLIENT.clear(); }
            return;
        }
        long now = mc.level.getGameTime();
        synchronized (ItemTransits.CLIENT) {
            for (Iterator<Transit> it = ItemTransits.CLIENT.iterator(); it.hasNext(); ) {
                if (it.next().end() < now) it.remove();
            }
        }
    }

    private ItemTransitRenderer() {}
}
