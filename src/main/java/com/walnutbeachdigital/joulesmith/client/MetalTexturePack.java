package com.walnutbeachdigital.joulesmith.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


import com.mojang.blaze3d.platform.NativeImage;
import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.material.Metal;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.IoSupplier;

/**
 * A built-in resource pack that makes Joulesmith's vanilla-shaped textures while the game loads.
 * It reads Minecraft's own textures (copper ingot, iron nugget, raw iron, iron tools and armor, and
 * birch and oak wood for rubber wood) from the player's copy of the game and swaps each of their shades for one step of a
 * metal's eight-step colour ramp, so ingots, nuggets, raw ore and raw blocks keep vanilla's shapes with
 * clean, hand-picked palettes. Ingots are made from the copper ingot, whose top face is shiny.
 * No Mojang texture is shipped inside the mod.
 */
public final class MetalTexturePack implements PackResources {
    private static final PackLocationInfo INFO = new PackLocationInfo(
            Joulesmith.MODID + "_metal_textures", Component.literal("Joulesmith metal textures"),
            PackSource.BUILT_IN, java.util.Optional.empty());

    /**
     * A texture to make: the vanilla texture it starts from and the colour ramp (darkest first) its shades
     * become. {@code metalOnly}: recolour only the grey (metal) pixels and keep coloured ones, like a
     * tool's wooden handle.
     */
    private record Source(int[] ramp, ResourceLocation vanilla, boolean metalOnly, boolean byBrightness, int[] otherRamp,
                          boolean colouredOnly) {
        Source(int[] ramp, ResourceLocation vanilla, boolean metalOnly) {
            this(ramp, vanilla, metalOnly, false, null, false);
        }

        Source(int[] ramp, ResourceLocation vanilla, boolean metalOnly, boolean byBrightness) {
            this(ramp, vanilla, metalOnly, byBrightness, null, false);
        }

        Source(int[] ramp, ResourceLocation vanilla, boolean metalOnly, boolean byBrightness, int[] otherRamp) {
            this(ramp, vanilla, metalOnly, byBrightness, otherRamp, false);
        }
    }

    /** Rubber wood, in vanilla shapes: acacia's smooth streaked bark, oak's log ends, planks and leaves. */
    private static final int[] EXO = {0x11161D, 0x1F2A35, 0x2F3E4C, 0x41566A, 0x557089, 0x6C8BA3, 0x8AAABF, 0xB4D4E4};
    private static final int[] RUBBER_BARK = {0x3D3127, 0x4F4235, 0x615244, 0x736353, 0x857463, 0x978673, 0xA89885, 0xB8A994};
    private static final int[] RUBBER_END = {0x3A3027, 0x5B4F41, 0x7A6447, 0x97794E, 0xAD8E5C, 0xC2A26A, 0xD3B47A, 0xE2C78B};
    private static final int[] RUBBER_PLANKS = {0x5A4128, 0x7A5A38, 0x96734A, 0xAD8A5A, 0xC09E6A, 0xD0B07A, 0xDDC08C, 0xE8D0A0};
    private static final int[] RUBBER_LEAVES = {0x0F2814, 0x16351A, 0x1F4A24, 0x2A5F2E, 0x367539, 0x438843, 0x529A4C, 0x63AC58};

    private static int[] ramp(Metal metal) {
        int[] ramp = new int[metal.rampSize()];
        for (int i = 0; i < ramp.length; i++) ramp[i] = metal.rampStep(i);
        return ramp;
    }

    /** Joulesmith texture path -> the vanilla texture and metal it is made from. */
    private static final Map<ResourceLocation, Source> TEXTURES = new LinkedHashMap<>();
    private final Map<ResourceLocation, byte[]> cache = new ConcurrentHashMap<>();

    static {
        for (Metal m : Metal.values()) {
            String id = m.id();
            if (m.hasIngot()) {
                add(m, "item/" + id + "_ingot", "item/copper_ingot");   // copper's ingot has the shiny, streaked top face
                add(m, "item/" + id + "_nugget", "item/iron_nugget");
            }
            if (m.hasOre() || !m.hasIngot()) {
                add(m, "item/" + id + "_crystal", "item/amethyst_shard");    // Crystallizer crystals: amethyst shards in the metal's colours
            }
            if (m.hasOre()) {
                add(m, "item/raw_" + id, "item/raw_iron");
                add(m, "block/raw_" + id + "_block", "block/raw_iron_block");
            }
        }
        // copper, bronze and steel gear: vanilla's iron tools and armor in each metal's colours
        for (Metal m : new Metal[] {Metal.COPPER, Metal.BRONZE, Metal.STEEL}) {
            String id = m.id();
            for (String piece : new String[] {"sword", "pickaxe", "axe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots"}) {
                addMetal(m, "item/" + id + "_" + piece, "item/iron_" + piece);
            }
            addMetal(m, "models/armor/" + id + "_layer_1", "models/armor/iron_layer_1");
            addMetal(m, "models/armor/" + id + "_layer_2", "models/armor/iron_layer_2");
        }
        // fluid buckets: vanilla's water bucket with the water in each fluid's colours, the bucket kept
        addFluidBucket(0xD9D23A, "sulfuric_acid");
        addFluidBucket(0x3ED6E0, "coolant");
        addFluidBucket(0x7FA331, "biofuel");
        addFluidBucket(0x4A4238, "oil");
        addFluidBucket(0xEDE6CF, "liquid_latex");
        // the Exo-Suit: vanilla's diamond armor in dark blue-grey plating
        for (String piece : new String[] {"helmet", "chestplate", "leggings", "boots"}) {
            addFull(EXO, "item/exo_" + piece, "item/diamond_" + piece);
        }
        addFull(EXO, "models/armor/exo_layer_1", "models/armor/diamond_layer_1");
        addFull(EXO, "models/armor/exo_layer_2", "models/armor/diamond_layer_2");
        addRamp(RUBBER_BARK, "block/rubber_log", "block/acacia_log");
        addRamp(RUBBER_END, "block/rubber_log_top", "block/oak_log_top");
        addRamp(RUBBER_PLANKS, "block/rubber_planks", "block/oak_planks");
        addRamp(RUBBER_LEAVES, "block/rubber_leaves", "block/oak_leaves");
        // the sapling in two parts: birch's leaves in rubber greens, its stem in rubber bark
        TEXTURES.put(Joulesmith.id("textures/block/rubber_sapling.png"), new Source(RUBBER_LEAVES,
                ResourceLocation.withDefaultNamespace("textures/block/birch_sapling.png"), false, true, RUBBER_BARK));
    }

    private static void addFluidBucket(int colour, String fluid) {
        TEXTURES.put(Joulesmith.id("textures/item/" + fluid + "_bucket.png"), new Source(fluidRamp(colour),
                ResourceLocation.withDefaultNamespace("textures/item/water_bucket.png"), false, false, null, true));
    }

    /** Eight shades of a fluid's colour, darkest first, the way vanilla's water runs from deep to bright. */
    private static int[] fluidRamp(int colour) {
        int[] ramp = new int[8];
        int r = colour >> 16 & 0xFF, g = colour >> 8 & 0xFF, b = colour & 0xFF;
        for (int i = 0; i < 8; i++) {
            float f = 0.45f + i * 0.11f;                         // 0.45 .. 1.22
            int rr, gg, bb;
            if (f <= 1) {
                rr = Math.round(r * f); gg = Math.round(g * f); bb = Math.round(b * f);
            } else {                                              // past full colour: lighten toward white
                float t = f - 1;
                rr = Math.round(r + (255 - r) * t); gg = Math.round(g + (255 - g) * t); bb = Math.round(b + (255 - b) * t);
            }
            ramp[i] = rr << 16 | gg << 8 | bb;
        }
        return ramp;
    }

    private static void addFull(int[] ramp, String path, String vanillaPath) {
        TEXTURES.put(Joulesmith.id("textures/" + path + ".png"),
                new Source(ramp, ResourceLocation.withDefaultNamespace("textures/" + vanillaPath + ".png"), false));
    }

    private static void addRamp(int[] ramp, String path, String vanillaPath) {
        TEXTURES.put(Joulesmith.id("textures/" + path + ".png"),
                new Source(ramp, ResourceLocation.withDefaultNamespace("textures/" + vanillaPath + ".png"), false, true));
    }

    private static void addMetal(Metal metal, String path, String vanillaPath) {
        TEXTURES.put(Joulesmith.id("textures/" + path + ".png"),
                new Source(ramp(metal), ResourceLocation.withDefaultNamespace("textures/" + vanillaPath + ".png"), true));
    }

    private static void add(Metal metal, String path, String vanillaPath) {
        TEXTURES.put(Joulesmith.id("textures/" + path + ".png"),
                new Source(ramp(metal), ResourceLocation.withDefaultNamespace("textures/" + vanillaPath + ".png"), false));
    }

    /** Adds this pack, always on and hidden from the resource pack screen. */
    public static RepositorySource source() {
        return consumer -> {
            Pack pack = Pack.readMetaAndCreate(INFO, new Pack.ResourcesSupplier() {
                @Override
                public PackResources openPrimary(PackLocationInfo info) {
                    return new MetalTexturePack();
                }

                @Override
                public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                    return new MetalTexturePack();
                }
            }, PackType.CLIENT_RESOURCES, new PackSelectionConfig(true, Pack.Position.TOP, false));
            if (pack != null) {
                consumer.accept(pack.hidden());
            }
        };
    }

    private byte[] make(ResourceLocation target) throws IOException {
        Source src = TEXTURES.get(target);
        if (src.otherRamp() != null) return makeTwoTone(src);
        IoSupplier<InputStream> vanilla = Minecraft.getInstance().getVanillaPackResources()
                .getResource(PackType.CLIENT_RESOURCES, src.vanilla());
        if (vanilla == null) {
            throw new IOException("Missing vanilla texture " + src.vanilla());
        }
        try (InputStream in = vanilla.get(); NativeImage image = NativeImage.read(in)) {
            int w = image.getWidth(), h = image.getHeight();
            // Every distinct shade in the vanilla texture, darkest first.
            java.util.TreeSet<Integer> shades = new java.util.TreeSet<>();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    if ((abgr >>> 24) != 0 && recolour(src, abgr)) shades.add(shade(abgr));
                }
            }
            java.util.List<Integer> order = new java.util.ArrayList<>(shades);
            int[] ramp = src.ramp();
            int steps = ramp.length;
            int[] ranks = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    ranks[y * w + x] = (abgr >>> 24) == 0 || !recolour(src, abgr) ? -1 : order.indexOf(shade(abgr));
                }
            }
            // Wood keeps vanilla's own contrast: shades map onto the ramp by brightness, not by rank.
            double lo = Double.MAX_VALUE, hi = -1;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    if ((abgr >>> 24) == 0) continue;
                    lo = Math.min(lo, luminance(abgr));
                    hi = Math.max(hi, luminance(abgr));
                }
            }
            // Each vanilla shade becomes one exact ramp colour: a clean palette swap, no blending or noise.
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    int alpha = abgr >>> 24;
                    if (alpha == 0) continue;
                    int i = ranks[y * w + x];
                    if (i < 0) continue;                                  // a kept pixel (e.g. the handle)
                    int step;
                    if (src.byBrightness()) {
                        step = (int) Math.round((luminance(abgr) - lo) / Math.max(1e-6, hi - lo) * (steps - 1));
                    } else {
                        step = order.size() == 1 ? steps - 1 : Math.round(i * (steps - 1) / (float) (order.size() - 1));
                    }
                    int rgb = ramp[step];
                    // NativeImage stores pixels as ABGR
                    image.setPixelRGBA(x, y, alpha << 24 | (rgb & 0xFF) << 16 | (rgb & 0xFF00) | (rgb >> 16 & 0xFF));
                }
            }
            return image.asByteArray();
        }
    }

    /**
     * Two parts recoloured separately: green pixels (leaves) onto {@code ramp}, the rest (the stem) onto
     * {@code otherRamp}, each spread over its ramp by brightness within its own part.
     */
    private byte[] makeTwoTone(Source src) throws IOException {
        IoSupplier<InputStream> vanilla = Minecraft.getInstance().getVanillaPackResources()
                .getResource(PackType.CLIENT_RESOURCES, src.vanilla());
        if (vanilla == null) throw new IOException("Missing vanilla texture " + src.vanilla());
        try (InputStream in = vanilla.get(); NativeImage image = NativeImage.read(in)) {
            int w = image.getWidth(), h = image.getHeight();
            double[] lo = {Double.MAX_VALUE, Double.MAX_VALUE}, hi = {-1, -1};
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    if ((abgr >>> 24) == 0) continue;
                    int part = green(abgr) ? 0 : 1;
                    lo[part] = Math.min(lo[part], luminance(abgr));
                    hi[part] = Math.max(hi[part], luminance(abgr));
                }
            }
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    int alpha = abgr >>> 24;
                    if (alpha == 0) continue;
                    int part = green(abgr) ? 0 : 1;
                    int[] ramp = part == 0 ? src.ramp() : src.otherRamp();
                    int step = (int) Math.round((luminance(abgr) - lo[part]) / Math.max(1e-6, hi[part] - lo[part]) * (ramp.length - 1));
                    int rgb = ramp[step];
                    image.setPixelRGBA(x, y, alpha << 24 | (rgb & 0xFF) << 16 | (rgb & 0xFF00) | (rgb >> 16 & 0xFF));
                }
            }
            return image.asByteArray();
        }
    }

    /** Whether a pixel is a leafy green: coloured enough, with a hue between yellow-green and cyan. */
    private static boolean green(int abgr) {
        float r = (abgr & 0xFF) / 255f, g = (abgr >> 8 & 0xFF) / 255f, b = (abgr >> 16 & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        if (max == 0 || (max - min) / max <= 0.15f) return false;
        float d = max - min, hue;
        if (max == r) hue = ((g - b) / d) % 6;
        else if (max == g) hue = (b - r) / d + 2;
        else hue = (r - g) / d + 4;
        hue = (hue < 0 ? hue + 6 : hue) / 6f;
        return hue > 0.17f && hue < 0.5f;
    }

    /** Whether a pixel takes the metal's colours: every pixel, or for gear only the grey ones. */
    private static boolean recolour(Source src, int abgr) {
        if (!src.metalOnly() && !src.colouredOnly()) return true;
        int r = abgr & 0xFF, g = abgr >> 8 & 0xFF, b = abgr >> 16 & 0xFF;
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        boolean grey = max == 0 || (max - min) / (float) max < 0.18f;
        return src.colouredOnly() ? !grey : grey;           // a bucket's liquid, or a tool's metal
    }

    /** A pixel's brightness, rounded so identical vanilla shades group together. */
    private static int shade(int abgr) {
        return (int) Math.round(luminance(abgr) * 10);
    }

    private static double luminance(int abgr) {
        int r = abgr & 0xFF, g = abgr >> 8 & 0xFF, b = abgr >> 16 & 0xFF;
        return 0.299 * r + 0.587 * g + 0.114 * b;
    }

    private IoSupplier<InputStream> supplier(ResourceLocation target) {
        return () -> {
            byte[] bytes = cache.get(target);
            if (bytes == null) {
                bytes = make(target);
                cache.put(target, bytes);
            }
            return new ByteArrayInputStream(bytes);
        };
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        return type == PackType.CLIENT_RESOURCES && TEXTURES.containsKey(location) ? supplier(location) : null;
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES || !Joulesmith.MODID.equals(namespace)) return;
        String prefix = path.endsWith("/") ? path : path + "/";
        for (ResourceLocation target : TEXTURES.keySet()) {
            if (target.getPath().startsWith(prefix)) {
                output.accept(target, supplier(target));
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? Set.of(Joulesmith.MODID) : Set.of();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
        if ("pack".equals(deserializer.getMetadataSectionName())) {
            return (T) new PackMetadataSection(Component.literal("Joulesmith metal textures"),
                    SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES));
        }
        return null;
    }

    @Override
    public PackLocationInfo location() {
        return INFO;
    }

    @Override
    public void close() {
    }
}
