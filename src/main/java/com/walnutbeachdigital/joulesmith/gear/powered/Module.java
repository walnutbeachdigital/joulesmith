package com.walnutbeachdigital.joulesmith.gear.powered;

import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

/**
 * An upgrade fitted to powered gear at the Modification Table, two per item. Some work as enchantments
 * (Silk Touch, Fortune, Sharpness, Looting, Fire Aspect); the rest are handled in {@link PoweredGearEvents}.
 */
public enum Module implements StringRepresentable {
    // tools (Power Drill, Chainsaw)
    SPEED(Kind.TOOL, null, 0),
    RANGE(Kind.TOOL, null, 0),
    SILK_TOUCH(Kind.TOOL, Enchantments.SILK_TOUCH, 1),
    FORTUNE(Kind.TOOL, Enchantments.FORTUNE, 3),
    AUTO_SMELT(Kind.TOOL, null, 0),
    MAGNET(Kind.TOOL_OR_WEAPON, null, 0),
    // the Energy Sword
    DAMAGE(Kind.WEAPON, Enchantments.SHARPNESS, 5),
    LOOTING(Kind.WEAPON, Enchantments.LOOTING, 3),
    FIRE(Kind.WEAPON, Enchantments.FIRE_ASPECT, 2),
    CHAIN_LIGHTNING(Kind.WEAPON, null, 0),
    LIFESTEAL(Kind.WEAPON, null, 0),
    // the Exo-Suit, each on its own piece
    NIGHT_VISION(Kind.HELMET, null, 0),
    WATER_BREATHING(Kind.HELMET, null, 0),
    SOLAR(Kind.HELMET, null, 0),
    JETPACK(Kind.CHESTPLATE, null, 0),
    SWIFTNESS(Kind.LEGGINGS, null, 0),
    STEP_ASSIST(Kind.LEGGINGS, null, 0),
    JUMP_BOOST(Kind.BOOTS, null, 0),
    FALL_PROTECTION(Kind.BOOTS, null, 0);

    /** What a module can be fitted to. */
    public enum Kind { TOOL, WEAPON, TOOL_OR_WEAPON, HELMET, CHESTPLATE, LEGGINGS, BOOTS }

    public static final Codec<Module> CODEC = StringRepresentable.fromEnum(Module::values);
    public static final StreamCodec<FriendlyByteBuf, Module> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(Module.class);

    public final Kind kind;
    /** The enchantment this module grants, or null. */
    public final ResourceKey<Enchantment> enchantment;
    public final int level;

    Module(Kind kind, ResourceKey<Enchantment> enchantment, int level) {
        this.kind = kind;
        this.enchantment = enchantment;
        this.level = level;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getSerializedName() {
        return id();
    }

    /** Modules that can't be fitted together. */
    public boolean conflicts(Module other) {
        return (this == SILK_TOUCH && other == FORTUNE) || (this == FORTUNE && other == SILK_TOUCH) || (this == other && this != RANGE);   // a second Range module widens to 5x5
    }
}
