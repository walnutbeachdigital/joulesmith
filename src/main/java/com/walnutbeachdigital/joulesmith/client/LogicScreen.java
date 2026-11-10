package com.walnutbeachdigital.joulesmith.client;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.logic.LogicControllerBlockEntity;
import com.walnutbeachdigital.joulesmith.logic.LogicMenu;
import com.walnutbeachdigital.joulesmith.logic.Rule;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Logic Controller screen: six rule rows, each a line of buttons reading
 * "[on] if [reading] on [side] is [< or >] [value] -> power [side]". Left-click a button to step it forward,
 * right-click to step it back. The lamp at the start of a row is green while its rule holds.
 */
public class LogicScreen extends AbstractContainerScreen<LogicMenu> {
    private static final int W = 252, H = 192, ROW_Y = 34, ROW_H = 22, BTN_H = 14;
    /** Field columns: x and width of each button (0 on/off, 1 reading, 2 side, 3 below/above, 4 value, 5 output). */
    private static final int[][] COLS = {{8, 12}, {24, 72}, {100, 46}, {150, 14}, {168, 38}, {218, 30}};

    public LogicScreen(LogicMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = W;
        imageHeight = H;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        // vanilla-style panel
        g.fill(x, y, x + W, y + H, 0xFF000000);
        g.fill(x + 1, y + 1, x + W - 1, y + H - 1, 0xFFFFFFFF);
        g.fill(x + 3, y + 3, x + W - 1, y + H - 1, 0xFF555555);
        g.fill(x + 3, y + 3, x + W - 3, y + H - 3, 0xFFC6C6C6);
        for (int r = 0; r < LogicControllerBlockEntity.RULES; r++) {
            Rule rule = menu.rule(r);
            int ry = y + ROW_Y + r * ROW_H;
            for (int f = 0; f < COLS.length; f++) {
                int bx = x + COLS[f][0];
                boolean hover = mouseX >= bx && mouseX < bx + COLS[f][1] && mouseY >= ry && mouseY < ry + BTN_H;
                if (f == 0) {
                    int lamp = !rule.enabled() ? 0xFF6E6E6E : rule.test(menu.reading(r)) ? 0xFF4FD14A : 0xFFB03A2E;
                    g.fill(bx, ry + 1, bx + 12, ry + 13, 0xFF1E1E1E);
                    g.fill(bx + 1, ry + 2, bx + 11, ry + 12, lamp);
                    continue;
                }
                boolean dim = !rule.enabled() || (f == 2 && !rule.measure().usesSide);
                g.fill(bx, ry, bx + COLS[f][1], ry + BTN_H, 0xFF373737);
                g.fill(bx + 1, ry + 1, bx + COLS[f][1] - 1, ry + BTN_H - 1, hover ? 0xFFA9B7E8 : 0xFF8B8B8B);
                g.fill(bx + 1, ry + 1, bx + COLS[f][1] - 1, ry + 2, 0xFFFFFFFF);
                Component text = label(rule, f);
                int tw = font.width(text);
                g.drawString(font, text, bx + (COLS[f][1] - tw) / 2, ry + 3, dim ? 0xFF505050 : 0xFFFFFFFF, true);
            }
            g.drawString(font, "→", x + 208, ry + 3, 0xFF404040, false);
        }
    }

    private Component label(Rule rule, int field) {
        return switch (field) {
            case 1 -> Component.translatable(rule.measure().key());
            case 2 -> rule.measure().usesSide ? dir(rule.side()) : Component.literal("-");
            case 3 -> Component.literal(rule.above() ? ">" : "<");
            case 4 -> Component.literal(rule.measure().format(rule.value()));
            case 5 -> dir(rule.output());
            default -> Component.empty();
        };
    }

    private static Component dir(Direction d) {
        return Component.translatable("gui.joulesmith.dir." + d.getSerializedName());
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 7, 0xFF404040, false);
        String[] heads = {"reading", "side", "if", "value", "output"};
        int[] at = {COLS[1][0], COLS[2][0], COLS[3][0], COLS[4][0], COLS[5][0]};
        for (int i = 0; i < heads.length; i++) {
            g.drawString(font, Component.translatable("gui.joulesmith.logic.col." + heads[i]), at[i], 22, 0xFF606060, false);
        }
        // which output sides are powered right now
        int lx = 8, ly = H - 16;
        g.drawString(font, Component.translatable("gui.joulesmith.logic.outputs"), lx, ly, 0xFF404040, false);
        lx += font.width(Component.translatable("gui.joulesmith.logic.outputs")) + 6;
        for (Direction d : Direction.values()) {
            Component name = dir(d);
            boolean on = menu.outputOn(d);
            g.drawString(font, name, lx, ly, on ? 0xFFB03A2E : 0xFF808080, false);
            lx += font.width(name) + 6;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int[] hit = fieldAt(mouseX, mouseY);
        if (hit != null) {
            Rule rule = menu.rule(hit[0]);
            List<Component> tip = new ArrayList<>();
            switch (hit[1]) {
                case 0 -> tip.add(Component.translatable(rule.enabled() ? "gui.joulesmith.logic.on" : "gui.joulesmith.logic.off"));
                case 1 -> tip.add(Component.translatable("gui.joulesmith.logic.reading",
                        Component.translatable(rule.measure().key()), rule.measure().format(menu.reading(hit[0]))));
                case 2 -> tip.add(Component.translatable(rule.measure().usesSide ? "gui.joulesmith.logic.side" : "gui.joulesmith.logic.no_side"));
                case 3 -> tip.add(Component.translatable(rule.above() ? "gui.joulesmith.logic.above" : "gui.joulesmith.logic.below"));
                case 4 -> tip.add(Component.translatable("gui.joulesmith.logic.value"));
                case 5 -> tip.add(Component.translatable("gui.joulesmith.logic.output"));
                default -> { }
            }
            tip.add(Component.translatable("gui.joulesmith.sides.hint").withStyle(ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    /** {rule, field} under the mouse, or null. */
    private int[] fieldAt(double mouseX, double mouseY) {
        for (int r = 0; r < LogicControllerBlockEntity.RULES; r++) {
            int ry = topPos + ROW_Y + r * ROW_H;
            if (mouseY < ry || mouseY >= ry + BTN_H) continue;
            for (int f = 0; f < COLS.length; f++) {
                int bx = leftPos + COLS[f][0];
                if (mouseX >= bx && mouseX < bx + COLS[f][1]) return new int[] {r, f};
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int[] hit = fieldAt(mouseX, mouseY);
        if (hit != null && minecraft != null && minecraft.gameMode != null && (button == 0 || button == 1)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, hit[0] * 16 + hit[1] * 2 + (button == 1 ? 1 : 0));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
