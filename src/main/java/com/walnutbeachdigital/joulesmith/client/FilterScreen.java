package com.walnutbeachdigital.joulesmith.client;

import java.util.List;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.item.filter.FilterMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The Filter Card screen: a 3x3 grid of ghost slots and a whitelist / blacklist button. */
public class FilterScreen extends AbstractContainerScreen<FilterMenu> {
    private static final ResourceLocation TEXTURE = Joulesmith.id("textures/gui/filter.png");
    private static final int BUTTON_X = 116, BUTTON_Y = 34, BUTTON = 20;
    private static final int BUTTON_U = 176, BUTTON_HOVER_U = 196, ICON_V = 20, WHITE_U = 176, BLACK_U = 192;

    public FilterScreen(FilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = FilterMenu.INVENTORY_Y - 11;
    }

    private boolean overButton(double mouseX, double mouseY) {
        double rx = mouseX - leftPos, ry = mouseY - topPos;
        return rx >= BUTTON_X && rx < BUTTON_X + BUTTON && ry >= BUTTON_Y && ry < BUTTON_Y + BUTTON;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (overButton(mouseX, mouseY)) {
            g.renderComponentTooltip(font, List.of(
                    Component.translatable(menu.blacklist() ? "gui.joulesmith.filter.blacklist" : "gui.joulesmith.filter.whitelist"),
                    Component.translatable("gui.joulesmith.click_to_change").withStyle(ChatFormatting.DARK_GRAY)), mouseX, mouseY);
        } else if (hoveredSlot != null && hoveredSlot.index < 9 && !hoveredSlot.hasItem()) {
            g.renderComponentTooltip(font, List.of(Component.translatable("gui.joulesmith.filter.hint").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        g.blit(TEXTURE, leftPos + BUTTON_X, topPos + BUTTON_Y, overButton(mouseX, mouseY) ? BUTTON_HOVER_U : BUTTON_U, 0, BUTTON, BUTTON);
        g.blit(TEXTURE, leftPos + BUTTON_X + 2, topPos + BUTTON_Y + 2, menu.blacklist() ? BLACK_U : WHITE_U, ICON_V, 16, 16);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (overButton(mouseX, mouseY) && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, FilterMenu.BUTTON_TOGGLE);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
