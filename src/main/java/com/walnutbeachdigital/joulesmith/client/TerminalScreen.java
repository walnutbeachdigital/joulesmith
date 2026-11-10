package com.walnutbeachdigital.joulesmith.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.network.TerminalMenu;
import com.walnutbeachdigital.joulesmith.network.TerminalPayloads;
import com.walnutbeachdigital.joulesmith.storage.bin.BinContents;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The Network Terminal: a 9x6 grid of everything on the network, sorted by count, with a search box (type
 * "@mod" to search by mod). Click to take a stack, right-click for half, shift-click to send a stack to your
 * inventory; click with an item held to put it in (right-click: one). Shift-click your own items to store them.
 * Middle-click something a pattern makes to craft a batch (Shift: a stack).
 */
public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
    private static final ResourceLocation TEXTURE = Joulesmith.id("textures/gui/network_terminal.png");
    private EditBox search;
    private int scroll;
    /** One cell of the grid: an item (count) or a fluid (mB). */
    private record Entry(ItemStack item, FluidStack fluid, int count) {
        String name() { return fluid.isEmpty() ? item.getHoverName().getString() : fluid.getHoverName().getString(); }
    }

    private List<Entry> view = List.of();

    private static final ResourceLocation CRAFTING_TEXTURE = Joulesmith.id("textures/gui/crafting_terminal.png");
    private final int rows;

    public TerminalScreen(TerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        rows = menu.rows();
        imageWidth = 195;
        imageHeight = menu.inventoryY() + 82;
        inventoryLabelY = menu.inventoryY() - 11;
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, leftPos + 101, topPos + 5, 84, 10, Component.translatable("gui.joulesmith.terminal.search"));
        search.setBordered(false);
        search.setMaxLength(50);
        search.setTextColor(0xFFFFFF);
        addRenderableWidget(search);
        if (menu instanceof com.walnutbeachdigital.joulesmith.network.CraftingTerminalMenu) {
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("gui.joulesmith.terminal.encode"),
                            b -> PacketDistributor.sendToServer(new TerminalPayloads.Action(menu.containerId, ItemStack.EMPTY, TerminalMenu.ENCODE)))
                    .bounds(leftPos + 84, topPos + 134, 32, 12)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.joulesmith.terminal.encode.tip")))
                    .build());
        }
    }

    private boolean craftable(ItemStack item) {
        for (ItemStack c : menu.craftable()) if (ItemStack.isSameItemSameComponents(c, item)) return true;
        return false;
    }

    private void refresh() {
        String q = search.getValue().toLowerCase(Locale.ROOT).trim();
        List<BinContents> all = new ArrayList<>(menu.shown());
        for (ItemStack c : menu.craftable()) {                     // things patterns can make, shown even when none are stored
            if (all.stream().noneMatch(b -> ItemStack.isSameItemSameComponents(b.item(), c))) all.add(new BinContents(c, 0));
        }
        List<Entry> entries = new ArrayList<>();
        for (BinContents c : all) entries.add(new Entry(c.item(), FluidStack.EMPTY, c.count()));
        for (FluidStack f : menu.fluids()) entries.add(new Entry(ItemStack.EMPTY, f, f.getAmount()));
        List<Entry> list = new ArrayList<>();
        for (Entry e : entries) {
            String mod = e.fluid().isEmpty() ? net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.item().getItem()).getNamespace()
                    : net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(e.fluid().getFluid()).getNamespace();
            if (q.isEmpty() || (q.startsWith("@") ? mod.contains(q.substring(1)) : e.name().toLowerCase(Locale.ROOT).contains(q))) list.add(e);
        }
        // items by count, then fluids by amount
        list.sort(Comparator.<Entry>comparingInt(e -> e.fluid().isEmpty() ? 0 : 1)
                .thenComparing(Comparator.comparingInt(Entry::count).reversed()).thenComparing(Entry::name));
        view = list;
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int maxScroll() {
        return Math.max(0, (view.size() + TerminalMenu.COLUMNS - 1) / TerminalMenu.COLUMNS - rows);
    }

    /** The grid cell under the mouse, or -1. */
    private int cellAt(double mx, double my) {
        int x = (int) mx - leftPos - TerminalMenu.GRID_X, y = (int) my - topPos - TerminalMenu.GRID_Y;
        if (x < 0 || y < 0 || x >= TerminalMenu.COLUMNS * 18 || y >= rows * 18) return -1;
        return (y / 18 + scroll) * TerminalMenu.COLUMNS + x / 18;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        refresh();
        super.render(g, mouseX, mouseY, partialTick);
        for (int i = 0; i < TerminalMenu.COLUMNS * rows; i++) {
            int index = scroll * TerminalMenu.COLUMNS + i;
            if (index >= view.size()) break;
            Entry c = view.get(index);
            int x = leftPos + TerminalMenu.GRID_X + (i % TerminalMenu.COLUMNS) * 18, y = topPos + TerminalMenu.GRID_Y + (i / TerminalMenu.COLUMNS) * 18;
            String n;
            if (c.fluid().isEmpty()) {
                g.renderItem(c.item(), x, y);
                n = c.count() == 0 ? Component.translatable("gui.joulesmith.terminal.craft").getString() : shortCount(c.count());
            } else {
                drawFluid(g, c.fluid(), x, y);
                n = shortCount(c.count() / 1000) + "B";
            }
            g.pose().pushPose();
            g.pose().translate(x + 16, y + 12, 200);
            g.pose().scale(0.5f, 0.5f, 1);
            g.drawString(font, n, -font.width(n), 0, 0xFFFFFF, true);
            g.pose().popPose();
        }
        int cell = cellAt(mouseX, mouseY);
        if (cell >= 0) {
            int i = cell - scroll * TerminalMenu.COLUMNS;
            int x = leftPos + TerminalMenu.GRID_X + (i % TerminalMenu.COLUMNS) * 18, y = topPos + TerminalMenu.GRID_Y + (i / TerminalMenu.COLUMNS) * 18;
            g.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
            if (cell < view.size() && (menu.getCarried().isEmpty() || !view.get(cell).fluid().isEmpty())) {
                Entry c = view.get(cell);
                List<Component> tip = new ArrayList<>();
                if (c.fluid().isEmpty()) {
                    tip.addAll(getTooltipFromContainerItem(c.item()));
                    tip.add(Component.translatable("gui.joulesmith.terminal.stored", String.format("%,d", c.count())).withStyle(ChatFormatting.GRAY));
                    if (craftable(c.item())) tip.add(Component.translatable("gui.joulesmith.terminal.craft_hint").withStyle(ChatFormatting.AQUA));
                } else {
                    tip.add(c.fluid().getHoverName());
                    tip.add(Component.translatable("gui.joulesmith.terminal.stored_fluid", String.format("%,d", c.count())).withStyle(ChatFormatting.GRAY));
                    tip.add(Component.translatable("gui.joulesmith.terminal.fill_hint").withStyle(ChatFormatting.AQUA));
                }
                g.renderComponentTooltip(font, tip, mouseX, mouseY);
            }
        }
        if (!menu.online()) {
            Component off = Component.translatable("gui.joulesmith.terminal.offline");
            g.drawString(font, off, leftPos + 8 + (TerminalMenu.COLUMNS * 18 - font.width(off)) / 2, topPos + TerminalMenu.GRID_Y + rows * 9 - 4, 0xFF5555, true);
        }
        renderTooltip(g, mouseX, mouseY);
    }

    private static void drawFluid(GuiGraphics g, FluidStack fluid, int x, int y) {
        var ext = net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());
        var sprite = net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                .apply(ext.getStillTexture(fluid));
        int color = ext.getTintColor(fluid);
        g.blit(x, y, 0, 16, 16, sprite, (color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 1f);
    }

    private static String shortCount(int n) {
        if (n < 1_000) return Integer.toString(n);
        if (n < 1_000_000) return (n < 10_000 ? String.format("%.1f", n / 1000.0) : Integer.toString(n / 1000)) + "K";
        return (n < 10_000_000 ? String.format("%.1f", n / 1e6) : Integer.toString(n / 1_000_000)) + "M";
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = rows == TerminalMenu.ROWS ? TEXTURE : CRAFTING_TEXTURE;
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int max = maxScroll(), track = rows * 18 - 15;
        int knob = max == 0 ? 0 : track * scroll / max;
        g.blit(texture, leftPos + 175, topPos + TerminalMenu.GRID_Y + knob, max == 0 ? 244 : 232, 0, 12, 15);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int cell = cellAt(mx, my);
        if (cell >= 0 && (button == 0 || button == 1 || button == 2)) {
            ItemStack carried = menu.getCarried();
            int action;
            ItemStack like = ItemStack.EMPTY;
            FluidStack fluid = FluidStack.EMPTY;
            if (!carried.isEmpty() && cell < view.size() && !view.get(cell).fluid().isEmpty()) {
                fluid = view.get(cell).fluid();                     // a container held over a fluid: fill it
                action = TerminalMenu.FILL;
            } else if (!carried.isEmpty()) {
                action = button == 0 ? TerminalMenu.INSERT_ALL : TerminalMenu.INSERT_ONE;
            } else {
                if (cell >= view.size() || !view.get(cell).fluid().isEmpty()) return true;
                Entry c = view.get(cell);
                like = c.item();
                // middle-click crafts (Ctrl-click too, off a Mac); a plain click on something none of is stored crafts it
                if (craftable(like) && (button == 2 || (Screen.hasControlDown() && !net.minecraft.client.Minecraft.ON_OSX) || c.count() == 0)) {
                    action = Screen.hasShiftDown() ? TerminalMenu.CRAFT_STACK : TerminalMenu.CRAFT_ONE;
                } else {
                    action = Screen.hasShiftDown() ? TerminalMenu.TAKE_TO_INVENTORY : button == 0 ? TerminalMenu.TAKE_STACK : TerminalMenu.TAKE_HALF;
                }
            }
            PacketDistributor.sendToServer(new TerminalPayloads.Action(menu.containerId, like, fluid, action));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(dy)));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (search.isFocused()) {
            if (key == 256) return super.keyPressed(key, scan, modifiers);     // Esc still closes
            search.keyPressed(key, scan, modifiers);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
}
