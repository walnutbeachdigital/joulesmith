package com.walnutbeachdigital.joulesmith.client;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.walnutbeachdigital.joulesmith.item.WrenchItem;
import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.MachineFace;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * While the player holds a Wrench and looks at a machine, tints each face by its side setting: blue input,
 * orange output, green both, grey off (the same colours as the side configuration tab).
 */
public final class WrenchOverlay {
    private static final float E = 0.003f;
    // the four corners of each face, in Direction order: down, up, north, south, west, east
    private static final float[][][] FACES = {
            {{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}},
            {{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}},
            {{0, 0, 0}, {0, 1, 0}, {1, 1, 0}, {1, 0, 0}},
            {{0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}},
            {{0, 0, 0}, {0, 0, 1}, {0, 1, 1}, {0, 1, 0}},
            {{1, 0, 0}, {1, 1, 0}, {1, 1, 1}, {1, 0, 1}}};

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!(mc.player.getMainHandItem().getItem() instanceof WrenchItem) && !(mc.player.getOffhandItem().getItem() instanceof WrenchItem)) return;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        if (!(mc.level.getBlockEntity(pos) instanceof MachineBlockEntity machine)) return;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
        Matrix4f matrix = pose.last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer out = buffers.getBuffer(RenderType.debugQuads());
        for (Direction dir : Direction.values()) {
            int color = machine.sideMode(MachineFace.of(machine.facing(), dir)).color;
            float r = (color >> 16 & 0xFF) / 255f, g = (color >> 8 & 0xFF) / 255f, b = (color & 0xFF) / 255f;
            float[][] face = FACES[dir.ordinal()];
            for (int k = 0; k < 2; k++) {                       // both windings, so it shows from either side
                for (int i = 0; i < 4; i++) {
                    float[] v = face[k == 0 ? i : 3 - i];
                    out.addVertex(matrix, v[0] * (1 + 2 * E) - E, v[1] * (1 + 2 * E) - E, v[2] * (1 + 2 * E) - E)
                            .setColor(r, g, b, 0.4f);
                }
            }
        }
        pose.popPose();
        buffers.endBatch(RenderType.debugQuads());
    }

    private WrenchOverlay() {}
}
