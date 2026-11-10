package com.walnutbeachdigital.joulesmith.machine;

/** When a machine is allowed to run, based on the redstone signal it receives. */
public enum RedstoneMode {
    IGNORED, HIGH, LOW, NEVER,
    /** Each time a signal switches on, the machine runs one job (generators: one second). */
    PULSE;

    public boolean allows(boolean powered) {
        return switch (this) {
            case IGNORED -> true;
            case HIGH -> powered;
            case LOW -> !powered;
            case NEVER, PULSE -> false;              // PULSE is handled by the machine itself
        };
    }

    public RedstoneMode next() { return values()[(ordinal() + 1) % values().length]; }

    public String key() { return "gui.joulesmith.redstone." + name().toLowerCase(java.util.Locale.ROOT); }
}
