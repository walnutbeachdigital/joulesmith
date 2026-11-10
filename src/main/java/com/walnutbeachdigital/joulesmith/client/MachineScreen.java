package com.walnutbeachdigital.joulesmith.client;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.MachineFace;
import com.walnutbeachdigital.joulesmith.machine.MachineMenu;
import com.walnutbeachdigital.joulesmith.machine.RedstoneMode;
import com.walnutbeachdigital.joulesmith.machine.SideMode;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.machine.SlotRole;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The one screen every Joulesmith machine uses: Thermal-style grey panel, red energy bar on the left,
 * progress arrow between input and output, and slide-out tabs on the right for redstone control and
 * side configuration.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final ResourceLocation TEXTURE = Joulesmith.id("textures/gui/machine.png");

    // sprites in machine.png
    private static final int SLOT_U = 176, SLOT_V = 0;          // 18x18
    private static final int BIG_U = 176, BIG_V = 18;           // 26x26
    private static final int ARROW_U = 176, ARROW_V = 44;       // 24x17 empty, full below it
    private static final int BAR_U = 202, BAR_V = 0;            // 16x50 frame, 14x48 fill to its right
    private static final int FLAME_U = 176, FLAME_V = 78;       // 14x14 unlit, lit to its right
    private static final int FLAME_X = 81, FLAME_Y = 35;
    private static final int MARKS_U = 176, MARKS_V = 96;       // 14x48 gauge marks drawn over the fluid
    private static final int TANK_Y = 17;

    private static final int BAR_X = 8, BAR_Y = 17, ARROW_X = 79, ARROW_Y = 35;
    private static final int TAB = 24, SIDE_PANEL = 62;
    private static final int RED_TAB_Y = 4, SIDE_TAB_Y = 32;

    private boolean sidesOpen;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = MachineMenu.INVENTORY_Y - 11;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        List<Component> tip = tooltipAt(mouseX - leftPos, mouseY - topPos);
        if (!tip.isEmpty()) g.renderComponentTooltip(font, tip, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        if (hasUpgrades()) drawUpgradePanel(g, x, y);
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        for (SlotDef def : menu.slotDefs()) {
            if (def.role() == SlotRole.OUTPUT && def.big()) {
                g.blit(TEXTURE, x + def.x() - 5, y + def.y() - 5, BIG_U, BIG_V, 26, 26);
            } else {
                g.blit(TEXTURE, x + def.x() - 1, y + def.y() - 1, SLOT_U, SLOT_V, 18, 18);
            }
        }
        // energy bar, filling from the bottom (machines that hold no power, like the Boiler, have none)
        if (menu.value(1) > 0) g.blit(TEXTURE, x + BAR_X, y + BAR_Y, BAR_U, BAR_V, 16, 50);
        int energy = menu.value(0), capacity = Math.max(1, menu.value(1));
        int filled = (int) Math.round(48.0 * Math.min(energy, capacity) / capacity);
        if (filled > 0) {
            g.blit(TEXTURE, x + BAR_X + 1, y + BAR_Y + 1 + 48 - filled, BAR_U + 16, BAR_V + 1 + 48 - filled, 14, filled);
        }
        int progress = menu.value(2), max = menu.value(3);
        switch (menu.progressStyle()) {
            case ARROW -> {                                       // fills left to right
                g.blit(TEXTURE, x + ARROW_X, y + ARROW_Y, ARROW_U, ARROW_V, 24, 17);
                if (max > 0 && progress > 0) {
                    int w = Math.min(24, (int) Math.ceil(24.0 * progress / max));
                    g.blit(TEXTURE, x + ARROW_X, y + ARROW_Y, ARROW_U, ARROW_V + 17, w, 17);
                }
            }
            case FLAME -> {                                       // burns down as the fuel runs out
                g.blit(TEXTURE, x + FLAME_X, y + FLAME_Y, FLAME_U, FLAME_V, 14, 14);
                if (max > 0 && progress > 0) {
                    int h = Math.max(1, (int) Math.ceil(14.0 * progress / max));
                    g.blit(TEXTURE, x + FLAME_X, y + FLAME_Y + 14 - h, FLAME_U + 14, FLAME_V + 14 - h, 14, h);
                }
            }
            case NONE -> { }
        }
        for (int t = 0; t < menu.tankCount(); t++) drawTank(g, t, x + menu.tankX(t), y + TANK_Y);
        drawTabs(g, x + imageWidth, y);
    }

    private int tankAt(double rx, double ry) {
        for (int t = 0; t < menu.tankCount(); t++) {
            int tx = menu.tankX(t);
            if (rx >= tx && rx < tx + 16 && ry >= TANK_Y && ry < TANK_Y + 50) return t;
        }
        return -1;
    }

    private boolean hasUpgrades() {
        return menu.slotDefs().stream().anyMatch(d -> d.role() == SlotRole.UPGRADE);
    }

    // the upgrade panel hangs off the left edge of the screen
    private static final int UPG_X = -24, UPG_Y = 3, UPG_W = 26, UPG_H = 62;

    private void drawUpgradePanel(GuiGraphics g, int x, int y) {
        int px = x + UPG_X, py = y + UPG_Y;
        g.fill(px, py, px + UPG_W, py + UPG_H, 0xFF000000);
        g.fill(px + 1, py + 1, px + UPG_W, py + UPG_H - 1, 0xFFFFFFFF);
        g.fill(px + 2, py + 2, px + UPG_W, py + UPG_H - 1, 0xFF555555);
        g.fill(px + 2, py + 2, px + UPG_W, py + UPG_H - 2, 0xFFC6C6C6);
    }

    private boolean inUpgradePanel(double rx, double ry) {
        return hasUpgrades() && rx >= UPG_X && rx < 0 && ry >= UPG_Y && ry < UPG_Y + UPG_H;
    }

    /** Screen areas outside the main panel (the upgrade panel and the tabs), for JEI to keep clear of. */
    public List<Rect2i> extraAreas() {
        List<Rect2i> areas = new ArrayList<>();
        if (hasUpgrades()) areas.add(new Rect2i(leftPos + UPG_X, topPos + UPG_Y, -UPG_X, UPG_H));
        areas.add(new Rect2i(leftPos + imageWidth, topPos + RED_TAB_Y, TAB, TAB));
        int sideH = sidesOpen ? SIDE_PANEL + 20 : TAB, sideW = sidesOpen ? SIDE_PANEL : TAB;
        areas.add(new Rect2i(leftPos + imageWidth, topPos + SIDE_TAB_Y, sideW, sideH));
        return areas;
    }

    public static int arrowX() { return ARROW_X; }
    public static int arrowY() { return ARROW_Y; }

    /** A fluid gauge: the same frame as the energy bar, filled from the bottom with the fluid's own texture. */
    private void drawTank(GuiGraphics g, int t, int x, int y) {
        g.blit(TEXTURE, x, y, BAR_U, BAR_V, 16, 50);
        FluidStack fluid = menu.fluid(t);
        int capacity = Math.max(1, menu.tankCapacity(t));
        if (!fluid.isEmpty()) {
            int h = Math.max(1, (int) Math.round(48.0 * Math.min(fluid.getAmount(), capacity) / capacity));
            IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
            int color = ext.getTintColor(fluid);
            float r = (color >> 16 & 255) / 255f, gr = (color >> 8 & 255) / 255f, b = (color & 255) / 255f;
            int bottom = y + 1 + 48;
            for (int drawn = 0; drawn < h; drawn += 16) {
                int piece = Math.min(16, h - drawn);
                g.blit(x + 1, bottom - drawn - piece, 0, 14, piece, sprite, r, gr, b, 1f);
            }
        }
        g.blit(TEXTURE, x + 1, y + 1, MARKS_U, MARKS_V, 14, 48);
    }

    // ------------------------------------------------------------------ tabs

    private void panel(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, 0xFF000000);
        g.fill(x, y + 1, x + w - 1, y + h - 1, 0xFFFFFFFF);
        g.fill(x + 1, y + 2, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x, y + 2, x + w - 2, y + h - 2, color);
    }

    private void drawTabs(GuiGraphics g, int x, int y) {
        panel(g, x - 2, y + RED_TAB_Y, TAB + 2, TAB, 0xFFB03A2E);
        g.renderItem(new ItemStack(Items.REDSTONE), x + 3, y + RED_TAB_Y + 4);
        int sideH = sidesOpen ? SIDE_PANEL + 20 : TAB;
        int sideW = sidesOpen ? SIDE_PANEL + 2 : TAB + 2;
        panel(g, x - 2, y + SIDE_TAB_Y, sideW, sideH, 0xFF3F76E4);
        g.renderItem(new ItemStack(Items.HOPPER), x + 3, y + SIDE_TAB_Y + 4);
        if (sidesOpen) {
            for (MachineFace face : MachineFace.values()) {
                int[] p = facePos(face);
                int fx = x + p[0], fy = y + p[1];
                SideMode mode = menu.sideMode(face);
                g.fill(fx, fy, fx + 14, fy + 14, 0xFF1E1E1E);
                g.fill(fx + 1, fy + 1, fx + 13, fy + 13, 0xFF000000 | mode.color);
                if (face == MachineFace.FRONT) {
                    g.fill(fx + 4, fy + 4, fx + 10, fy + 10, 0x66000000);
                }
            }
        }
    }

    /** Where each face's button sits inside the open side panel (a cross, front in the middle, back bottom-right). */
    private int[] facePos(MachineFace face) {
        int ox = 6, oy = SIDE_TAB_Y + 24;
        return switch (face) {
            case TOP -> new int[] {ox + 18, oy};
            case LEFT -> new int[] {ox + 2, oy + 16};
            case FRONT -> new int[] {ox + 18, oy + 16};
            case RIGHT -> new int[] {ox + 34, oy + 16};
            case BOTTOM -> new int[] {ox + 18, oy + 32};
            case BACK -> new int[] {ox + 34, oy + 32};
        };
    }

    private MachineFace faceAt(double rx, double ry) {
        if (!sidesOpen) return null;
        for (MachineFace face : MachineFace.values()) {
            int[] p = facePos(face);
            if (rx >= imageWidth + p[0] && rx < imageWidth + p[0] + 14 && ry >= p[1] && ry < p[1] + 14) return face;
        }
        return null;
    }

    private boolean inRedTab(double rx, double ry) {
        return rx >= imageWidth && rx < imageWidth + TAB && ry >= RED_TAB_Y && ry < RED_TAB_Y + TAB;
    }

    private boolean inSideTab(double rx, double ry) {
        return rx >= imageWidth && rx < imageWidth + TAB && ry >= SIDE_TAB_Y && ry < SIDE_TAB_Y + TAB;
    }

    private boolean inSidePanel(double rx, double ry) {
        return sidesOpen && rx >= imageWidth && rx < imageWidth + SIDE_PANEL && ry >= SIDE_TAB_Y && ry < SIDE_TAB_Y + SIDE_PANEL + 20;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double rx = mouseX - leftPos, ry = mouseY - topPos;
        if (inRedTab(rx, ry)) {
            press(MachineBlockEntity.BUTTON_REDSTONE);
            return true;
        }
        MachineFace face = faceAt(rx, ry);
        if (face != null) {
            press((button == 1 ? MachineBlockEntity.BUTTON_FACE_PREVIOUS : MachineBlockEntity.BUTTON_FACE_NEXT) + face.ordinal());
            return true;
        }
        if (inSideTab(rx, ry)) {
            sidesOpen = !sidesOpen;
            return true;
        }
        if (inSidePanel(rx, ry)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        double rx = mouseX - left, ry = mouseY - top;
        return super.hasClickedOutside(mouseX, mouseY, left, top, button)
                && !inUpgradePanel(rx, ry) && !inRedTab(rx, ry) && !inSideTab(rx, ry) && !inSidePanel(rx, ry);
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    // ------------------------------------------------------------------ tooltips

    private List<Component> tooltipAt(double rx, double ry) {
        List<Component> tip = new ArrayList<>();
        if (menu.value(1) > 0 && rx >= BAR_X && rx < BAR_X + 16 && ry >= BAR_Y && ry < BAR_Y + 50) {
            tip.add(Component.translatable("gui.joulesmith.energy", String.format("%,d", menu.value(0)), String.format("%,d", menu.value(1))));
        } else if (tankAt(rx, ry) >= 0) {
            int t = tankAt(rx, ry);
            FluidStack fluid = menu.fluid(t);
            String cap = String.format("%,d", menu.tankCapacity(t));
            tip.add(fluid.isEmpty() ? Component.translatable("message.joulesmith.fluid.empty", cap)
                    : Component.translatable("message.joulesmith.fluid", fluid.getHoverName(), String.format("%,d", fluid.getAmount()), cap));
        } else if (menu.progressStyle() == MachineBlockEntity.ProgressStyle.ARROW
                && rx >= ARROW_X && rx < ARROW_X + 24 && ry >= ARROW_Y && ry < ARROW_Y + 17 && menu.value(3) > 0) {
            tip.add(Component.translatable("gui.joulesmith.progress", menu.value(2) * 100 / menu.value(3)));
        } else if (menu.progressStyle() == MachineBlockEntity.ProgressStyle.FLAME
                && rx >= FLAME_X && rx < FLAME_X + 14 && ry >= FLAME_Y && ry < FLAME_Y + 14) {
            tip.add(Component.translatable("gui.joulesmith.burn_time", (menu.value(2) + 19) / 20));
        } else if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < menu.slotDefs().size()
                && menu.slotDefs().get(hoveredSlot.index).role() == SlotRole.UPGRADE) {
            tip.add(Component.translatable("gui.joulesmith.upgrade_slot"));
            tip.add(Component.translatable("gui.joulesmith.upgrade_slot.hint").withStyle(ChatFormatting.GRAY));
        } else if (inRedTab(rx, ry)) {
            RedstoneMode mode = menu.redstoneMode();
            tip.add(Component.translatable("gui.joulesmith.redstone.title"));
            tip.add(Component.translatable(mode.key()).withStyle(ChatFormatting.GRAY));
            tip.add(Component.translatable("gui.joulesmith.click_to_change").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            MachineFace face = faceAt(rx, ry);
            if (face != null) {
                tip.add(Component.translatable(face.key()));
                tip.add(Component.translatable(menu.sideMode(face).key()).withStyle(ChatFormatting.GRAY));
                tip.add(Component.translatable("gui.joulesmith.sides.hint").withStyle(ChatFormatting.DARK_GRAY));
            } else if (inSideTab(rx, ry)) {
                tip.add(Component.translatable("gui.joulesmith.sides.title"));
            }
        }
        return tip;
    }
}
