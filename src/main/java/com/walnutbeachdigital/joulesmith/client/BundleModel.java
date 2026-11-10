package com.walnutbeachdigital.joulesmith.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.transport.conduit.BundleView;
import com.walnutbeachdigital.joulesmith.transport.conduit.ConduitShapes;
import com.walnutbeachdigital.joulesmith.transport.conduit.ConduitShapes.Kind;
import com.walnutbeachdigital.joulesmith.transport.conduit.ConduitShapes.Piece;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

/**
 * Draws a conduit bundle from its {@link BundleView}: every conduit's core, arms and plates
 * ({@link ConduitShapes}), textured from the same tiles the old pipe and cable models used. Built once per
 * look and kept.
 *
 * Ducts and pipes use the pipe sheet: rows 0-5 are the side, running along the pipe; the 6 x 6 at (0, 6) is
 * the end cap; push and pull plates come from the attachment sheet. Cables use their whole tile, with the
 * 4-wide band in the middle running along the cable.
 */
public class BundleModel implements IDynamicBakedModel {
    private final BakedModel base;
    private final Map<Long, List<BakedQuad>> cache = new ConcurrentHashMap<>();

    public BundleModel(BakedModel base) {
        this.base = base;
    }

    private static final ChunkRenderTypeSet TYPES = ChunkRenderTypeSet.of(RenderType.cutout());

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return TYPES;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, RenderType renderType) {
        if (side != null) return List.of();
        BundleView view = data.get(BundleView.PROPERTY);
        if (view == null || view.entries().isEmpty()) return List.of();
        return cache.computeIfAbsent(view.key(), k -> build(view));
    }

    private static TextureAtlasSprite sprite(String name) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(Joulesmith.id("block/" + name));
    }

    private static List<BakedQuad> build(BundleView view) {
        List<BakedQuad> quads = new ArrayList<>();
        TextureAtlasSprite attach = sprite("pipe_attachment");
        for (BundleView.Entry e : view.entries()) {
            TextureAtlasSprite tex = sprite(e.type().id(e.tier()));
            boolean pipe = e.type().plates;
            for (Piece p : ConduitShapes.pieces(e.type(), e.connections())) {
                for (Direction face : Direction.values()) {
                    int n = face.getAxis().ordinal();
                    if (p.kind() == Kind.ARM && n == p.axis()) continue;          // open ends
                    boolean plate = p.kind() == Kind.PUSH || p.kind() == Kind.PULL;
                    quads.add(quad(p, face, plate ? attach : tex, p.kind(), pipe));
                }
            }
        }
        return List.copyOf(quads);
    }

    // corners of each face (Direction order), as max-or-min per axis; vertex 0 is the face's top-left
    private static final boolean[][][] CORNERS = {
            {{false, false, true}, {false, false, false}, {true, false, false}, {true, false, true}},   // down
            {{false, true, false}, {false, true, true}, {true, true, true}, {true, true, false}},      // up
            {{true, true, false}, {true, false, false}, {false, false, false}, {false, true, false}},  // north
            {{false, true, true}, {false, false, true}, {true, false, true}, {true, true, true}},      // south
            {{false, true, false}, {false, false, false}, {false, false, true}, {false, true, true}},  // west
            {{true, true, true}, {true, false, true}, {true, false, false}, {true, true, false}}};     // east

    /** The two axes lying in a face: for a face across x, (z, y); across y, (x, z); across z, (x, y). */
    private static int[] inPlane(int normal) {
        return normal == 0 ? new int[] {2, 1} : normal == 1 ? new int[] {0, 2} : new int[] {0, 1};
    }

    private static BakedQuad quad(Piece p, Direction face, TextureAtlasSprite sprite, Kind kind, boolean pipe) {
        QuadBakingVertexConsumer b = new QuadBakingVertexConsumer();
        b.setSprite(sprite);
        b.setDirection(face);
        b.setShade(true);
        b.setHasAmbientOcclusion(true);
        b.setTintIndex(-1);
        int n = face.getAxis().ordinal();
        int[] plane = inPlane(n);
        for (boolean[] corner : CORNERS[face.ordinal()]) {
            float[] v = new float[3];
            for (int i = 0; i < 3; i++) v[i] = corner[i] ? p.max()[i] : p.min()[i];
            float[] uv = uv(p, n, plane, corner, v, kind, pipe);
            b.addVertex(v[0] / 16f, v[1] / 16f, v[2] / 16f)
                    .setColor(255, 255, 255, 255)
                    .setUv(sprite.getU(uv[0] / 16f), sprite.getV(uv[1] / 16f))
                    .setUv2(0, 0)
                    .setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
        }
        return b.bakeQuad();
    }

    /** Where in the texture (0..16) one corner of a face samples. */
    private static float[] uv(Piece p, int normal, int[] plane, boolean[] corner, float[] v, Kind kind, boolean pipe) {
        int a = plane[0], b = plane[1];
        // 0..1 along each in-plane axis; y counts down from the top
        float ta = corner[a] ? 1 : 0, tb = corner[b] ? 1 : 0;
        if (a == 1) ta = 1 - ta;
        if (b == 1) tb = 1 - tb;
        switch (kind) {
            case PUSH, PULL -> {
                float u0 = kind == Kind.PUSH ? 0 : 8, depth = kind == Kind.PUSH ? 2 : 3;
                if (normal == p.axis()) return new float[] {u0 + ta * 8, tb * 8};       // the plate's face
                int cross = a == p.axis() ? b : a;                                       // an edge
                float tc = cross == a ? ta : tb, tl = cross == a ? tb : ta;
                return new float[] {u0 + tc * 8, 8 + tl * depth};
            }
            case ARM -> {
                int len = a == p.axis() ? a : b;
                float along = len == 1 ? 16 - v[1] : v[len];                             // absolute, so it lines up block to block
                float across = len == a ? tb : ta;
                return pipe ? new float[] {along, across * 6} : new float[] {6 + across * 4, along};
            }
            default -> {                                                                 // core and cable caps: the end cap
                return pipe ? new float[] {ta * 6, 6 + tb * 6} : new float[] {6 + ta * 4, 6 + tb * 4};
            }
        }
    }

    @Override public boolean useAmbientOcclusion() { return true; }
    @Override public boolean isGui3d() { return false; }
    @Override public boolean usesBlockLight() { return true; }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return base.getParticleIcon(); }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        BundleView view = data.get(BundleView.PROPERTY);
        if (view == null || view.entries().isEmpty()) return base.getParticleIcon();
        BundleView.Entry e = view.entries().get(0);
        return sprite(e.type().id(e.tier()));
    }

    @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }

    /** The bundle block's model location, to swap the plain one for this. */
    public static boolean isBundle(ResourceLocation id) {
        return id.getNamespace().equals(Joulesmith.MODID) && id.getPath().equals("conduit_bundle");
    }
}
