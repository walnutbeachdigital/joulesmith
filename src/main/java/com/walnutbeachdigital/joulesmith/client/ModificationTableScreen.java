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

/** The Modification Table: the gear slot on the left, its two module slots on the right. */
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
        if (hoveredSlot != null && hoveredSlot.index < 3 && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            String key = hoveredSlot.index == 0 ? "gui.joulesmith.modification_table.gear" : "gui.joulesmith.modification_table.module";
            g.renderComponentTooltip(font, List.of(Component.translatable(key).withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
