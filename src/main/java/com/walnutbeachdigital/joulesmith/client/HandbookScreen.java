package com.walnutbeachdigital.joulesmith.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.walnutbeachdigital.joulesmith.guide.HandbookProgress;
import com.walnutbeachdigital.joulesmith.guide.HandbookQuests;
import com.walnutbeachdigital.joulesmith.guide.HandbookQuests.Chapter;
import com.walnutbeachdigital.joulesmith.guide.HandbookQuests.Kind;
import com.walnutbeachdigital.joulesmith.guide.HandbookQuests.Quest;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * The Engineer's Handbook: a chapter per tier down the left, and the open chapter's quests as a tree of
 * machines joined by circuit traces. Drag to pan; hover a quest for what it needs and how it's made.
 * Square quests are the main path, round ones are optional, and the gold gear-framed one opens the next tier.
 */
public class HandbookScreen extends Screen {
    private static final int CELL = 32, SIDEBAR = 156, MARGIN = 16, HEADER = 30, TILE = 128, NODE = 24, ROW = 24;

    private Chapter chapter = Chapter.BASIC;
    private double panX, panY;
    private boolean dragging;
    private double dragX, dragY;

    public HandbookScreen() {
        super(Component.translatable("handbook.joulesmith.title"));
    }

    @Override
    protected void init() {
        if (!open(chapter)) chapter = Chapter.BASIC;
        centre();
    }

    private static Set<String> done() {
        return HandbookProgress.clientDone();
    }

    private static boolean open(Chapter c) {
        return HandbookProgress.open(c, done());
    }

    // ---- layout ----

    private int left() { return MARGIN; }
    private int top() { return MARGIN; }
    private int right() { return width - MARGIN; }
    private int bottom() { return height - MARGIN; }
    private int canvasLeft() { return left() + SIDEBAR; }
    private int canvasTop() { return top() + HEADER; }

    private void centre() {
        List<Quest> quests = HandbookQuests.chapter(chapter);
        float minX = 0, maxX = 0, minY = 0, maxY = 0;
        for (Quest q : quests) {
            minX = Math.min(minX, q.x()); maxX = Math.max(maxX, q.x());
            minY = Math.min(minY, q.y()); maxY = Math.max(maxY, q.y());
        }
        int w = right() - canvasLeft(), h = bottom() - canvasTop();
        double treeW = (maxX - minX) * CELL;
        panX = treeW + 2 * CELL < w ? (w - treeW) / 2 - minX * CELL : CELL - minX * CELL;
        panY = h / 2.0 - (minY + maxY) / 2 * CELL;
    }

    private int nodeX(Quest q) {
        return (int) Math.round(canvasLeft() + panX + q.x() * CELL);
    }

    private int nodeY(Quest q) {
        return (int) Math.round(canvasTop() + panY + q.y() * CELL);
    }

    // ---- drawing ----

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        HandbookTheme theme = HandbookTheme.of(chapter);
        HandbookTheme frame = HandbookTheme.FRAME;

        // the canvas
        int cl = canvasLeft(), ct = canvasTop(), cr = right(), cb = bottom();
        tile(g, theme, cl, ct, cr, cb, (int) panX, (int) panY);
        g.enableScissor(cl, ct, cr, cb);
        List<Quest> quests = HandbookQuests.chapter(chapter);
        for (Quest q : quests) {
            for (String need : q.needs()) {
                Quest from = HandbookQuests.get(need);
                if (from == null || from.chapter() != chapter) continue;
                trace(g, nodeX(from), nodeY(from), nodeX(q), nodeY(q), done().contains(need) ? theme.done() : theme.lineOff());
            }
        }
        Quest hovered = null;
        for (Quest q : quests) {
            int x = nodeX(q), y = nodeY(q);
            drawNode(g, theme, q, x, y);
            if (inCanvas(mouseX, mouseY) && Math.abs(mouseX - x) <= NODE / 2 && Math.abs(mouseY - y) <= NODE / 2) hovered = q;
        }
        g.disableScissor();
        border(g, cl, ct, cr, cb, theme.edge());

        // sidebar and header
        drawSidebar(g, frame, mouseX, mouseY);
        drawHeader(g, frame, theme);
        drawClose(g, mouseX, mouseY);

        if (hovered != null) drawTooltip(g, theme, hovered, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g);
    }

    private void tile(GuiGraphics g, HandbookTheme theme, int x0, int y0, int x1, int y1, int offX, int offY) {
        int ox = Math.floorMod(offX, TILE), oy = Math.floorMod(offY, TILE);
        g.enableScissor(x0, y0, x1, y1);
        for (int y = y0 - (oy == 0 ? 0 : TILE - oy); y < y1; y += TILE) {
            for (int x = x0 - (ox == 0 ? 0 : TILE - ox); x < x1; x += TILE) {
                g.blit(theme.texture(), x, y, 0, 0, TILE, TILE, TILE, TILE);
            }
        }
        g.disableScissor();
    }

    private static void border(GuiGraphics g, int x0, int y0, int x1, int y1, int colour) {
        g.fill(x0, y0, x1, y0 + 1, colour);
        g.fill(x0, y1 - 1, x1, y1, colour);
        g.fill(x0, y0, x0 + 1, y1, colour);
        g.fill(x1 - 1, y0, x1, y1, colour);
    }

    /** A circuit trace: out of the left quest, across to halfway, then up or down, then into the right one. */
    private static void trace(GuiGraphics g, int x0, int y0, int x1, int y1, int colour) {
        int mid = (x0 + x1) / 2;
        hline(g, x0, mid, y0, colour);
        vline(g, mid, y0, y1, colour);
        hline(g, mid, x1, y1, colour);
        g.fill(mid - 1, y0 - 1, mid + 2, y0 + 2, colour);
        g.fill(mid - 1, y1 - 1, mid + 2, y1 + 2, colour);
    }

    private static void hline(GuiGraphics g, int a, int b, int y, int colour) {
        g.fill(Math.min(a, b), y - 1, Math.max(a, b) + 1, y + 1, colour);
    }

    private static void vline(GuiGraphics g, int x, int a, int b, int colour) {
        g.fill(x - 1, Math.min(a, b), x + 1, Math.max(a, b) + 1, colour);
    }

    private enum State { DONE, READY, WAITING }

    private static State state(Quest q) {
        Set<String> done = done();
        if (done.contains(q.id())) return State.DONE;
        return done.containsAll(q.needs()) ? State.READY : State.WAITING;
    }

    private void drawNode(GuiGraphics g, HandbookTheme theme, Quest q, int cx, int cy) {
        State state = state(q);
        int h = NODE / 2;
        int x0 = cx - h, y0 = cy - h, x1 = cx + h, y1 = cy + h;
        int fill = state == State.DONE ? theme.doneFill() : theme.nodeFill();
        int rim = switch (state) {
            case DONE -> theme.done();
            case READY -> theme.ready();
            case WAITING -> theme.waiting();
        };
        if (q.kind() == Kind.KEY) {
            // a gear: teeth round a gold frame
            int gold = state == State.WAITING ? theme.waiting() : theme.key();
            for (int t = -h + 4; t <= h - 6; t += 6) {
                g.fill(cx + t, y0 - 3, cx + t + 3, y0, gold);
                g.fill(cx + t, y1, cx + t + 3, y1 + 3, gold);
                g.fill(x0 - 3, cy + t, x0, cy + t + 3, gold);
                g.fill(x1, cy + t, x1 + 3, cy + t + 3, gold);
            }
            g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, gold);
            g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill);
        } else if (q.kind() == Kind.OPTIONAL) {
            // rounded corners
            g.fill(x0 + 2, y0, x1 - 2, y1, rim);
            g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, rim);
            g.fill(x0, y0 + 2, x1, y1 - 2, rim);
            g.fill(x0 + 2, y0 + 1, x1 - 2, y1 - 1, fill);
            g.fill(x0 + 1, y0 + 2, x1 - 1, y1 - 2, fill);
        } else {
            g.fill(x0, y0, x1, y1, rim);
            g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill);
        }
        g.renderItem(new ItemStack(q.item()), cx - 8, cy - 8);
        if (state == State.WAITING) g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0x80101418);
        if (state == State.DONE) {
            // a tick in the corner
            int tx = x1 - 6, ty = y1 - 5;
            g.fill(tx, ty + 2, tx + 2, ty + 4, theme.done());
            g.fill(tx + 2, ty + 3, tx + 4, ty + 5, theme.done());
            g.fill(tx + 4, ty, tx + 6, ty + 4, theme.done());
        }
    }

    private void drawSidebar(GuiGraphics g, HandbookTheme frame, int mouseX, int mouseY) {
        int x0 = left(), y0 = top(), x1 = left() + SIDEBAR, y1 = bottom();
        tile(g, frame, x0, y0, x1, y1, 0, 0);
        border(g, x0, y0, x1, y1, frame.edge());
        g.drawString(font, title, x0 + 8, y0 + 10, frame.title());
        g.fill(x0 + 6, y0 + 24, x1 - 6, y0 + 25, frame.edge());
        int y = y0 + 32;
        for (Chapter c : Chapter.values()) {
            boolean isOpen = open(c);
            boolean hover = mouseX >= x0 + 4 && mouseX < x1 - 4 && mouseY >= y && mouseY < y + ROW;
            if (c == chapter) g.fill(x0 + 4, y, x1 - 4, y + ROW, HandbookTheme.of(c).edge());
            else if (hover && isOpen) g.fill(x0 + 4, y, x1 - 4, y + ROW, frame.select());
            Quest key = HandbookQuests.key(c);
            if (isOpen) {
                if (key != null) g.renderItem(new ItemStack(key.item()), x0 + 8, y + 4);
                g.drawString(font, c.displayName(), x0 + 28, y + 4, frame.text());
                List<Quest> qs = HandbookQuests.chapter(c);
                int n = 0;
                for (Quest q : qs) if (done().contains(q.id())) n++;
                g.drawString(font, n + "/" + qs.size(), x0 + 28, y + 14, n == qs.size() ? frame.done() : frame.faint());
            } else {
                drawLock(g, x0 + 12, y + 6, frame.faint());
                g.drawString(font, "???", x0 + 28, y + 8, frame.faint());
            }
            y += ROW + 2;
        }
    }

    private static void drawLock(GuiGraphics g, int x, int y, int colour) {
        g.fill(x + 1, y, x + 7, y + 1, colour);
        g.fill(x, y + 1, x + 1, y + 5, colour);
        g.fill(x + 7, y + 1, x + 8, y + 5, colour);
        g.fill(x - 1, y + 5, x + 9, y + 12, colour);
    }

    private void drawHeader(GuiGraphics g, HandbookTheme frame, HandbookTheme theme) {
        int x0 = canvasLeft(), y0 = top(), x1 = right(), y1 = canvasTop();
        tile(g, frame, x0, y0, x1, y1, 0, 0);
        border(g, x0, y0, x1, y1, theme.edge());
        g.drawString(font, chapter.displayName().copy().withStyle(ChatFormatting.BOLD), x0 + 8, y0 + 5, theme.title());
        Component intro = Component.translatable("handbook.joulesmith." + chapter.id() + ".intro");
        List<FormattedCharSequence> lines = font.split(intro, x1 - x0 - 34);
        if (!lines.isEmpty()) g.drawString(font, lines.get(0), x0 + 8, y0 + 17, frame.faint());
    }

    private int closeX() { return right() - 14; }
    private int closeY() { return top() + 4; }

    private void drawClose(GuiGraphics g, int mouseX, int mouseY) {
        int x = closeX(), y = closeY();
        boolean hover = mouseX >= x && mouseX < x + 10 && mouseY >= y && mouseY < y + 10;
        int c = hover ? 0xFFFF6060 : 0xFFC0C0C0;
        for (int i = 0; i < 8; i++) {
            g.fill(x + 1 + i, y + 1 + i, x + 3 + i, y + 2 + i, c);
            g.fill(x + 8 - i, y + 1 + i, x + 10 - i, y + 2 + i, c);
        }
    }

    private void drawTooltip(GuiGraphics g, HandbookTheme theme, Quest q, int mouseX, int mouseY) {
        State state = state(q);
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(q.title().copy().withStyle(q.kind() == Kind.KEY ? ChatFormatting.GOLD : ChatFormatting.WHITE).getVisualOrderText());
        lines.add(Component.translatable("handbook.joulesmith.kind." + q.kind().name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
        Component status = switch (state) {
            case DONE -> Component.translatable("handbook.joulesmith.status.done").withStyle(ChatFormatting.GREEN);
            case READY -> Component.translatable("handbook.joulesmith.status.ready", q.title()).withStyle(ChatFormatting.YELLOW);
            case WAITING -> Component.translatable("handbook.joulesmith.status.waiting").withStyle(ChatFormatting.GRAY);
        };
        lines.addAll(font.split(status, 200));
        if (state == State.WAITING) {
            for (String need : q.needs()) {
                if (done().contains(need)) continue;
                Quest n = HandbookQuests.get(need);
                if (n != null) lines.add(Component.literal(" - ").append(n.title()).withStyle(ChatFormatting.RED).getVisualOrderText());
            }
        }
        String descKey = "desc." + q.itemId().getNamespace() + "." + q.itemId().getPath();
        Component desc = Component.translatable(descKey);
        if (!desc.getString().equals(descKey)) {
            lines.addAll(font.split(FormattedText.EMPTY, 200));
            lines.addAll(font.split(desc.copy().withStyle(ChatFormatting.GRAY), 200));
        }

        List<HandbookRecipe> recipes = HandbookRecipe.of(q.item());
        HandbookRecipe recipe = recipes.isEmpty() ? null : recipes.get((int) (Util.getMillis() / 3000 % recipes.size()));

        int textW = 0;
        for (FormattedCharSequence l : lines) textW = Math.max(textW, font.width(l));
        int w = Math.max(textW, recipe == null ? 0 : recipe.width()) + 8;
        int h = lines.size() * 10 + 6 + (recipe == null ? 0 : recipe.height() + 6);
        int x = mouseX + 12, y = mouseY - 12;
        if (x + w > width - 4) x = mouseX - 12 - w;
        y = Mth.clamp(y, 4, Math.max(4, height - h - 4));

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, theme.edge());
        g.fill(x, y, x + w, y + h, 0xF0101418);
        int ty = y + 4;
        for (FormattedCharSequence l : lines) {
            g.drawString(font, l, x + 4, ty, 0xFFFFFFFF);
            ty += 10;
        }
        if (recipe != null) {
            g.fill(x + 3, ty + 1, x + w - 3, ty + 2, theme.lineOff());
            recipe.render(g, font, x + 4, ty + 5);
        }
        g.pose().popPose();
    }

    // ---- input ----

    private boolean inCanvas(double x, double y) {
        return x >= canvasLeft() && x < right() && y >= canvasTop() && y < bottom();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (mouseX >= closeX() && mouseX < closeX() + 10 && mouseY >= closeY() && mouseY < closeY() + 10) {
                onClose();
                return true;
            }
            int x0 = left(), x1 = left() + SIDEBAR, y = top() + 32;
            for (Chapter c : Chapter.values()) {
                if (mouseX >= x0 + 4 && mouseX < x1 - 4 && mouseY >= y && mouseY < y + ROW) {
                    if (open(c) && c != chapter) {
                        chapter = c;
                        centre();
                    }
                    return true;
                }
                y += ROW + 2;
            }
            if (inCanvas(mouseX, mouseY)) {
                dragging = true;
                dragX = mouseX;
                dragY = mouseY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging) {
            panX += mouseX - dragX;
            panY += mouseY - dragY;
            dragX = mouseX;
            dragY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
