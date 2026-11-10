package com.walnutbeachdigital.joulesmith.client;

import java.util.List;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.gear.table.ModificationTableMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The Modification Table: the gear slot on the left, its module slots (two to six) on the right. */
public class ModificationTableScreen extends AbstractContainerScreen<ModificationTableMenu> {
    private static final ResourceLocation TEXTURE = Joulesmith.id("textures/gui/modification_table.png");

    public ModificationTableScreen(ModificationTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = ModificationTableMenu.INVENTORY_Y - 11;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (hoveredSlot != null && hoveredSlot.index <= ModificationTableMenu.MAX && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            String key = hoveredSlot.index == 0 ? "gui.joulesmith.modification_table.gear" : "gui.joulesmith.modification_table.module";
            g.renderComponentTooltip(font, List.of(Component.translatable(key).withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (int i = 0; i < menu.slotCount(); i++) {               // a frame for each module slot the gear has
            int x = leftPos + ModificationTableMenu.MODULE_X + (i % 3) * 18 - 1, y = topPos + ModificationTableMenu.MODULE_Y + (i / 3) * 18 - 1;
            g.fill(x, y, x + 18, y + 18, 0xFFFFFFFF);
            g.fill(x, y, x + 17, y + 17, 0xFF373737);
            g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        }
    }
}
