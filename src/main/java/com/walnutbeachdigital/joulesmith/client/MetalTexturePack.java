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
 * It reads Minecraft's own iron ingot, iron nugget, raw iron, sugar, iron block and raw iron block
 * textures from the player's copy of the game and recolours each one through a metal's five-step
 * gradient, so ingots, nuggets, raw ore, dusts and blocks keep vanilla's shapes with rich,
 * hand-picked colours: light from the top-left, streaky metal grain and a few bright glints.
 * No Mojang texture is shipped inside the mod.
 */
public final class MetalTexturePack implements PackResources {
    private static final PackLocationInfo INFO = new PackLocationInfo(
            Joulesmith.MODID + "_metal_textures", Component.literal("Joulesmith metal textures"),
            PackSource.BUILT_IN, java.util.Optional.empty());

    private record Source(Metal metal, ResourceLocation vanilla) {}

    /** Joulesmith texture path -> the vanilla texture and metal it is made from. */
    private static final Map<ResourceLocation, Source> TEXTURES = new LinkedHashMap<>();
    private final Map<ResourceLocation, byte[]> cache = new ConcurrentHashMap<>();

    static {
        for (Metal m : Metal.values()) {
            String id = m.id();
            if (m.hasIngot()) {
                add(m, "item/" + id + "_ingot", "item/iron_ingot");
                add(m, "item/" + id + "_nugget", "item/iron_nugget");
                add(m, "block/" + id + "_block", "block/iron_block");
            }
            if (m.hasOre()) {
                add(m, "item/raw_" + id, "item/raw_iron");
                add(m, "block/raw_" + id + "_block", "block/raw_iron_block");
            }
            if (m.hasDust()) {
                add(m, "item/" + id + "_dust", "item/sugar");
            }
        }
    }

    private static void add(Metal metal, String path, String vanillaPath) {
        TEXTURES.put(Joulesmith.id("textures/" + path + ".png"),
                new Source(metal, ResourceLocation.withDefaultNamespace("textures/" + vanillaPath + ".png")));
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
            // Darkest and brightest visible pixels, and the shape's bounds.
            double lo = 255, hi = 0;
            int x0 = w, x1 = 0, y0 = h, y1 = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    if ((abgr >>> 24) == 0) continue;
                    double l = luminance(abgr);
                    lo = Math.min(lo, l);
                    hi = Math.max(hi, l);
                    x0 = Math.min(x0, x); x1 = Math.max(x1, x);
                    y0 = Math.min(y0, y); y1 = Math.max(y1, y);
                }
            }
            double span = Math.max(1, hi - lo);
            boolean block = target.getPath().startsWith("textures/block/");
            double light = block ? 0.08 : 0.26;     // blocks tile, so they get only a gentle sweep
            int seed = src.metal().ordinal() + 1;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    int alpha = abgr >>> 24;
                    if (alpha == 0) continue;
                    double base = (luminance(abgr) - lo) / span;
                    double v;
                    if (base < 0.12) {
                        v = 0;                                              // outline: darkest tone
                    } else {
                        // light from the top-left, streaky grain along the rows, a few bright glints
                        double diag = 1 - ((x - x0) / (double) Math.max(1, x1 - x0) + (y - y0) / (double) Math.max(1, y1 - y0)) / 2;
                        double streak = 0.6 * hash(x / 3, y, seed + 7) + 0.4 * hash(x, y, seed);
                        double t = 0.78 * base + light * diag + (streak - 0.5) * 0.24;
                        v = 0.08 + 0.80 * t;
                        if (base > 0.6 && hash(x, y, seed + 99) > 0.93) v = 1;
                    }
                    int rgb = src.metal().rampColor(v);
                    // NativeImage stores pixels as ABGR
                    image.setPixelRGBA(x, y, alpha << 24 | (rgb & 0xFF) << 16 | (rgb & 0xFF00) | (rgb >> 16 & 0xFF));
                }
            }
            return image.asByteArray();
        }
    }

    /** Repeatable pseudo-random number in [0, 1] for a pixel, so textures look the same every launch. */
    private static double hash(int x, int y, int seed) {
        long n = (x * 374761393L + y * 668265263L + seed * 2147483647L) & 0xFFFFFFFFL;
        n = ((n ^ (n >>> 13)) * 1274126177L) & 0xFFFFFFFFL;
        return ((n ^ (n >>> 16)) & 0xFFFF) / 65535.0;
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
