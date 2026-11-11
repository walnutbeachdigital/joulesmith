package com.walnutbeachdigital.joulesmith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.walnutbeachdigital.joulesmith.transport.PipeBlock;
import com.walnutbeachdigital.joulesmith.transport.PipeConnection;
import com.walnutbeachdigital.joulesmith.transport.PipeShapes;
import com.walnutbeachdigital.joulesmith.transport.fluid.FluidPipeBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The fluid seen through a fluid pipe's glass: a bar along every joined side, filled from the bottom on
 * level runs (vertical ones show full). The nub is solid, so nothing is drawn there.
 */
public class FluidPipeRenderer implements BlockEntityRenderer<FluidPipeBlockEntity> {
    private static final float LO = 6.3f / 16, HI = 9.7f / 16;

    public FluidPipeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FluidPipeBlockEntity pipe, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        FluidStack fluid = pipe.shown();
        float fill = pipe.shownFill();
        if (fluid.isEmpty() || fill <= 0) return;
        BlockState state = pipe.getBlockState();
        if (!(state.getBlock() instanceof PipeBlock)) return;
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
        int color = ext.getTintColor(fluid);
        int glow = fluid.getFluidType().getLightLevel(fluid);
        int packed = Math.max(light & 0xFFFF, glow << 4) | (light & 0xFFFF0000);
        VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
        PoseStack.Pose p = pose.last();
        boolean node = state.getValue(PipeShapes.NODE);
        float top = LO + (HI - LO) * fill;
        for (Direction dir : Direction.values()) {
            PipeConnection conn = state.getValue(PipeBlock.CONNECTION.get(dir));
            if (conn == PipeConnection.NONE) continue;
            float inner = node ? 5 / 16f : 0.5f;                                 // from the nub (or the middle)
            float outer = conn == PipeConnection.PIPE ? 0 : conn == PipeConnection.PUSH ? 2 / 16f : 3 / 16f;
            float a0, a1;
            if (dir.getAxisDirection() == Direction.AxisDirection.POSITIVE) { a0 = 1 - inner; a1 = 1 - outer; }
            else { a0 = outer; a1 = inner; }
            float yTop = dir.getAxis() == Direction.Axis.Y ? HI : top;
            switch (dir.getAxis()) {
                case X -> box(vc, p, sprite, color, packed, a0, LO, LO, a1, yTop, HI);
                case Z -> box(vc, p, sprite, color, packed, LO, LO, a0, HI, yTop, a1);
                case Y -> box(vc, p, sprite, color, packed, LO, a0, LO, HI, a1, HI);
            }
        }
    }

    private static void box(VertexConsumer vc, PoseStack.Pose p, TextureAtlasSprite s, int color, int light,
                            float x0, float y0, float z0, float x1, float y1, float z1) {
        // six faces, each sampling the part of the fluid texture its size covers
        quad(vc, p, s, color, light, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x1 - x0, z1 - z0);
        quad(vc, p, s, color, light, 0, -1, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, x1 - x0, z1 - z0);
        quad(vc, p, s, color, light, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1 - x0, y1 - y0);
        quad(vc, p, s, color, light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x1 - x0, y1 - y0);
        quad(vc, p, s, color, light, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, z1 - z0, y1 - y0);
        quad(vc, p, s, color, light, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, z1 - z0, y1 - y0);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, TextureAtlasSprite s, int color, int light,
                             float nx, float ny, float nz,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float uSpan, float vSpan) {
        float u0 = s.getU0(), u1 = s.getU0() + (s.getU1() - s.getU0()) * uSpan;
        float v1 = s.getV1(), v0 = s.getV1() - (s.getV1() - s.getV0()) * vSpan;
        vertex(vc, p, x0, y0, z0, u0, v1, color, light, nx, ny, nz);
        vertex(vc, p, x1, y1, z1, u1, v1, color, light, nx, ny, nz);
        vertex(vc, p, x2, y2, z2, u1, v0, color, light, nx, ny, nz);
        vertex(vc, p, x3, y3, z3, u0, v0, color, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v,
                               int color, int light, float nx, float ny, float nz) {
        vc.addVertex(p, x, y, z).setColor(color).setUv(u, v).setLight(light).setNormal(p, nx, ny, nz);
    }
}
