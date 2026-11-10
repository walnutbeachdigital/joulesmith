package com.walnutbeachdigital.joulesmith.registry;

import java.util.Map;

import com.walnutbeachdigital.joulesmith.Joulesmith;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Machine running sounds. Each is its own sound event (so resource packs can change it) that by default
 * plays a quiet vanilla sound: sounds.json in the assets says which, how loud and at what pitch.
 */
public final class JSSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Joulesmith.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> FURNACE = sound("machine.furnace");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRIND = sound("machine.grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAW = sound("machine.saw");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUM = sound("machine.hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLUID = sound("machine.fluid");
    public static final DeferredHolder<SoundEvent, SoundEvent> PUMP = sound("machine.pump");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURBINE = sound("machine.turbine");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHANT = sound("machine.enchant");
    public static final DeferredHolder<SoundEvent, SoundEvent> PRESS = sound("machine.press");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Joulesmith.id(name)));
    }

    /** Which sound a machine block makes while it runs, by its id; machines not listed run quietly. */
    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> BY_BLOCK = Map.ofEntries(
            Map.entry("electric_furnace", FURNACE), Map.entry("alloy_smelter", FURNACE), Map.entry("combustion_generator", FURNACE),
            Map.entry("boiler", FURNACE), Map.entry("geothermal_generator", FURNACE),
            Map.entry("crusher", GRIND), Map.entry("mob_grinder", GRIND), Map.entry("quarry", GRIND),
            Map.entry("sawmill", SAW), Map.entry("tree_farm", SAW),
            Map.entry("compressor", PRESS), Map.entry("wire_drawer", PRESS), Map.entry("block_breaker", PRESS),
            Map.entry("fluid_mixer", FLUID), Map.entry("ore_washer", FLUID), Map.entry("chemical_dissolver", FLUID),
            Map.entry("bio_reactor", FLUID), Map.entry("fertilizer_mixer", FLUID), Map.entry("crystallizer", FLUID),
            Map.entry("biofuel_generator", FLUID),
            Map.entry("pump", PUMP), Map.entry("advanced_pump", PUMP), Map.entry("fluid_drain", PUMP),
            Map.entry("steam_turbine", TURBINE), Map.entry("wind_turbine", TURBINE),
            Map.entry("centrifuge", HUM), Map.entry("circuit_assembler", HUM), Map.entry("hydroponic_bed", HUM),
            Map.entry("autocrafter", HUM),
            Map.entry("auto_enchanter", ENCHANT));

    public static SoundEvent forBlock(String id) {
        var holder = BY_BLOCK.get(id);
        return holder == null ? null : holder.get();
    }

    private JSSounds() {}
}
