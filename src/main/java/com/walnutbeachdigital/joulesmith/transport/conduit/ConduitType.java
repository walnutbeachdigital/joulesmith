package com.walnutbeachdigital.joulesmith.transport.conduit;

import java.util.Locale;

/**
 * The four kinds of conduit a bundle can hold, one of each. Every kind has its own lane through the block:
 * a fixed corner of the block's centre (the four alternate corners of a cube), so seen along any axis the
 * four lanes sit in four different quarters and never cross, and the same kind always lines up with itself
 * in the next block.
 */
public enum ConduitType {
    ITEM("item_duct", 5, -1, -1, -1, true),
    FLUID("fluid_pipe", 5, 1, 1, -1, true),
    ENERGY("energy_cable", 5, 1, -1, 1, false),
    NETWORK("network_cable", 1, -1, 1, 1, false);

    private static final String[] TIER_PREFIX = {"", "", "reinforced_", "advanced_", "elite_", "ultimate_"};

    public final String baseId;
    public final int maxTier;
    /** Which side of the centre the lane runs on, along x, y and z (-1 or 1). */
    public final int[] lane;
    /** Whether the conduit ends in a push or pull plate on blocks (item ducts and fluid pipes) or a small cap. */
    public final boolean plates;

    ConduitType(String baseId, int maxTier, int x, int y, int z, boolean plates) {
        this.baseId = baseId;
        this.maxTier = maxTier;
        this.lane = new int[] {x, y, z};
        this.plates = plates;
    }

    /** The item (and texture) name of this conduit at a tier: item_duct, reinforced_item_duct, ... */
    public String id(int tier) {
        return TIER_PREFIX[Math.max(1, Math.min(5, tier))] + baseId;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Conduit create(int tier) {
        return switch (this) {
            case ITEM -> new ItemConduit(tier);
            case FLUID -> new FluidConduit(tier);
            case ENERGY -> new EnergyConduit(tier);
            case NETWORK -> new NetworkConduit();
        };
    }
}
