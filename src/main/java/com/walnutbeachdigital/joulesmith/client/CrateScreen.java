package com.walnutbeachdigital.joulesmith.client;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.storage.crate.CrateMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Crate screen. The texture holds the top of the panel with nine rows of slots and, below it, the
 * player inventory part; a crate draws as many rows as it has, then the inventory part under them.
 */
public class CrateScreen extends AbstractContainerScreen<CrateMenu> {
    private static final ResourceLocation TEXTURE = Joulesmith.id("textures/gui/crate.png");

    private static final int TEX_W = 256, TEX_H = 512, INVENTORY_V = 180, INVENTORY_H = 96;

    public CrateScreen(CrateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 16 + CrateMenu.columns(menu.rows()) * 18;
        imageHeight = 17 + CrateMenu.shownRows(menu.rows()) * 18 + INVENTORY_H + 1;
        inventoryLabelY = CrateMenu.inventoryY(menu.rows()) - 11;
        inventoryLabelX = CrateMenu.inventoryX(menu.rows());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int top = 17 + CrateMenu.shownRows(menu.rows()) * 18;
        int cols = CrateMenu.columns(menu.rows());
        // wide crates have their own sheet, 512 wide: crate_12, crate_15, crate_18
        ResourceLocation texture = cols > 9 ? Joulesmith.id("textures/gui/crate_" + cols + ".png") : TEXTURE;
        int texW = cols > 9 ? 512 : TEX_W;
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, top, texW, TEX_H);
        g.blit(texture, leftPos, topPos + top, 0, INVENTORY_V, imageWidth, INVENTORY_H + 1, texW, TEX_H);
    }
}
