package com.walnutbeachdigital.joulesmith.gear.powered;

/**
 * The Railgun's tier 5 big sister: half a second to charge, 96 blocks, pierces six creatures for 40 damage
 * and bursts where it stops, burning and hurting everything within 4 blocks. It never breaks blocks.
 * 25,000 FE a shot.
 */
public class PlasmaCannonItem extends RailgunItem {
    public PlasmaCannonItem(Properties properties) {
        super(10_000_000, 25_000, 10, 96, 6, 40.0f, 4.0f, "tooltip.joulesmith.plasma_cannon", properties);
    }

    @Override
    public int transferRate() {
        return 40_000;
    }
}
