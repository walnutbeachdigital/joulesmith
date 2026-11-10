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
 * It reads Minecraft's own copper ingot, iron nugget, raw iron and raw iron block
 * textures from the player's copy of the game and swaps each of their shades for one step of a
 * metal's eight-step colour ramp, so ingots, nuggets, raw ore and raw blocks keep vanilla's shapes with
 * clean, hand-picked palettes. Ingots are made from the copper ingot, whose top face is shiny.
 * No Mojang texture is shipped inside the mod.
 */
public final class MetalTexturePack implements PackResources {
    private static final PackLocationInfo INFO = new PackLocationInfo(
            Joulesmith.MODID + "_metal_textures", Component.literal("Joulesmith metal textures"),
            PackSource.BUILT_IN, java.util.Optional.empty());

    /** {@code metalOnly}: recolour only the grey (metal) pixels and keep coloured ones, like a tool's wooden handle. */
    private record Source(Metal metal, ResourceLocation vanilla, boolean metalOnly) {}

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
    }

    private static void addMetal(Metal metal, String path, String vanillaPath) {
        TEXTURES.put(Joulesmith.id("textures/" + path + ".png"),
                new Source(metal, ResourceLocation.withDefaultNamespace("textures/" + vanillaPath + ".png"), true));
    }

    private static void add(Metal metal, String path, String vanillaPath) {
        TEXTURES.put(Joulesmith.id("textures/" + path + ".png"),
                new Source(metal, ResourceLocation.withDefaultNamespace("textures/" + vanillaPath + ".png"), false));
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
            Metal metal = src.metal();
            int steps = metal.rampSize();
            int[] ranks = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    ranks[y * w + x] = (abgr >>> 24) == 0 || !recolour(src, abgr) ? -1 : order.indexOf(shade(abgr));
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
                    int step = order.size() == 1 ? steps - 1 : Math.round(i * (steps - 1) / (float) (order.size() - 1));
                    int rgb = metal.rampStep(step);
                    // NativeImage stores pixels as ABGR
                    image.setPixelRGBA(x, y, alpha << 24 | (rgb & 0xFF) << 16 | (rgb & 0xFF00) | (rgb >> 16 & 0xFF));
                }
            }
            return image.asByteArray();
        }
    }

    /** Whether a pixel takes the metal's colours: every pixel, or for gear only the grey ones. */
    private static boolean recolour(Source src, int abgr) {
        if (!src.metalOnly()) return true;
        int r = abgr & 0xFF, g = abgr >> 8 & 0xFF, b = abgr >> 16 & 0xFF;
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        return max == 0 || (max - min) / (float) max < 0.18f;
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
