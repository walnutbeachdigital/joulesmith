package com.walnutbeachdigital.joulesmith.client;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.guide.HandbookQuests.Chapter;

import net.minecraft.resources.ResourceLocation;

/**
 * How the Handbook looks while a chapter is open: every chapter shares one dark blueprint page, framed and
 * titled in its tier's trim colour (copper, steel, electrum gold, signal blue, plasma violet; the network's
 * is green). The surfaces are tileable textures from tools/make_machine_textures.py.
 */
public record HandbookTheme(ResourceLocation texture, int edge, int title, int text, int faint, int lineOff, int done,
                            int nodeFill, int doneFill, int ready, int waiting, int key, int veil, int select) {

    private static ResourceLocation surface(String name) {
        return Joulesmith.id("textures/gui/handbook/" + name + ".png");
    }

    private static HandbookTheme page(int trim, int trimBright) {
        return new HandbookTheme(surface("page"), trim, trimBright, 0xFFE8ECF0, 0xFF9AA4B0, 0xFF3A4452, 0xFF6FE07A,
                0xFF161C24, 0xFF173A22, 0xFFE8ECF0, 0xFF56606C, 0xFFF2C94C, 0xA0101820, 0x30FFFFFF);
    }

    private static final HandbookTheme BASIC = page(0xFFB8622A, 0xFFF0A030);
    private static final HandbookTheme REINFORCED = page(0xFF6F7884, 0xFFC8CED6);
    private static final HandbookTheme ADVANCED = page(0xFFB8932A, 0xFFF4D860);
    private static final HandbookTheme ELITE = page(0xFF1F5FBF, 0xFF6FB0F6);
    private static final HandbookTheme ULTIMATE = page(0xFF7322AD, 0xFFC27CF4);
    private static final HandbookTheme NETWORK = page(0xFF1A7F3C, 0xFF5FD47C);

    /** The sidebar and header: the same steel cover in every chapter. */
    public static final HandbookTheme FRAME = new HandbookTheme(surface("cover"), 0xFF4A505A, 0xFFFFFFFF, 0xFFE8E8E8,
            0xFFA0A6B0, 0, 0xFF7FE07F, 0xFF22262E, 0, 0, 0, 0, 0, 0x30FFFFFF);

    public static HandbookTheme of(Chapter chapter) {
        return switch (chapter) {
            case BASIC -> BASIC;
            case REINFORCED -> REINFORCED;
            case ADVANCED -> ADVANCED;
            case ELITE -> ELITE;
            case ULTIMATE -> ULTIMATE;
            case NETWORK -> NETWORK;
        };
    }
}
