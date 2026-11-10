package com.walnutbeachdigital.joulesmith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.walnutbeachdigital.joulesmith.storage.bin.BinBlock;
import com.walnutbeachdigital.joulesmith.storage.bin.BinBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Shows a Bulk Bin's item and count on its front face. */
public class BinRenderer implements BlockEntityRenderer<BinBlockEntity> {
    public BinRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BinBlockEntity bin, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack item = bin.item();
        if (item.isEmpty() || bin.getLevel() == null) return;
        Direction facing = bin.getBlockState().getValue(BinBlock.FACING);
        int frontLight = LevelRenderer.getLightColor(bin.getLevel(), bin.getBlockPos().relative(facing));
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));      // local +z now points out of the front
        pose.translate(0, 0.06, 0.505);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180));                    // the FIXED item transform turns it back
        pose.scale(0.45f, 0.45f, 0.01f);
        Minecraft.getInstance().getItemRenderer().renderStatic(item, ItemDisplayContext.FIXED, frontLight, overlay, pose, buffers,
                bin.getLevel(), 0);
        pose.popPose();

        Font font = Minecraft.getInstance().font;
        String text = String.format("%,d", bin.count());
        pose.translate(0, -0.33, 0.002);
        pose.scale(1 / 96f, -1 / 96f, 1 / 96f);
        font.drawInBatch(text, -font.width(text) / 2f, 0, 0xFFFFFFFF, false, pose.last().pose(), buffers,
                Font.DisplayMode.NORMAL, 0, frontLight);
        pose.popPose();
    }
}
