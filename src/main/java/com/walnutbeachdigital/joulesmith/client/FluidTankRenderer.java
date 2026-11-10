package com.walnutbeachdigital.joulesmith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.walnutbeachdigital.joulesmith.storage.tank.FluidTankBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** Draws the fluid inside a Fluid Tank, filling from the bottom, seen through the glass. */
public class FluidTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity> {
    private static final float MIN = 1.02f / 16f, MAX = 14.98f / 16f, BOTTOM = 1.02f / 16f, TOP = 14.98f / 16f;

    public FluidTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FluidTankBlockEntity tank, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        FluidStack fluid = tank.tank().getFluid();
        if (fluid.isEmpty()) return;
        float fill = Math.min(1f, fluid.getAmount() / (float) tank.tank().getCapacity());
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
        int color = ext.getTintColor(fluid);
        int glow = fluid.getFluidType().getLightLevel(fluid);
        int packedLight = Math.max(light & 0xFFFF, glow << 4) | (light & 0xFFFF0000);
        float top = BOTTOM + (TOP - BOTTOM) * fill;
        VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
        PoseStack.Pose p = pose.last();
        float h = top - BOTTOM;
        // top
        quad(vc, p, sprite, color, packedLight, 0, 1, 0,
                MIN, top, MIN, MIN, top, MAX, MAX, top, MAX, MAX, top, MIN, 1f, 1f);
        // four sides, texture cut to the fluid's height
        quad(vc, p, sprite, color, packedLight, 0, 0, -1,
                MAX, BOTTOM, MIN, MIN, BOTTOM, MIN, MIN, top, MIN, MAX, top, MIN, 1f, h);
        quad(vc, p, sprite, color, packedLight, 0, 0, 1,
                MIN, BOTTOM, MAX, MAX, BOTTOM, MAX, MAX, top, MAX, MIN, top, MAX, 1f, h);
        quad(vc, p, sprite, color, packedLight, -1, 0, 0,
                MIN, BOTTOM, MIN, MIN, BOTTOM, MAX, MIN, top, MAX, MIN, top, MIN, 1f, h);
        quad(vc, p, sprite, color, packedLight, 1, 0, 0,
                MAX, BOTTOM, MAX, MAX, BOTTOM, MIN, MAX, top, MIN, MAX, top, MAX, 1f, h);
    }

    /** One face; corners go bottom-left, bottom-right, top-right, top-left as seen from outside. */
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
