package com.walnutbeachdigital.joulesmith.transport.conduit;

import java.util.List;

import com.walnutbeachdigital.joulesmith.transport.PipeConnection;

import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * What a bundle looks like: each conduit's kind, tier and how its six sides are joined. Packed into one
 * number too, so shapes and models can be built once per look and shared.
 */
public record BundleView(List<Entry> entries, long key) {
    public static final ModelProperty<BundleView> PROPERTY = new ModelProperty<>();
    public static final BundleView EMPTY = new BundleView(List.of(), 0);

    public record Entry(ConduitType type, int tier, PipeConnection[] connections) {}

    public static BundleView of(Iterable<Conduit> conduits) {
        java.util.ArrayList<Entry> entries = new java.util.ArrayList<>();
        long key = 0;
        for (Conduit c : conduits) {
            PipeConnection[] conns = c.connections().clone();
            entries.add(new Entry(c.type, c.tier(), conns));
            long part = 1L | (long) c.tier() << 1;          // 4 bits: there, and the tier
            for (int i = 0; i < 6; i++) part |= (long) conns[i].ordinal() << (4 + i * 2);
            key |= part << (c.type.ordinal() * 16);
        }
        return new BundleView(List.copyOf(entries), key);
    }

    public Entry get(ConduitType type) {
        for (Entry e : entries) if (e.type() == type) return e;
        return null;
    }
}
