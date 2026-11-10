package com.walnutbeachdigital.joulesmith.registry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.gear.AreaToolItem;
import com.walnutbeachdigital.joulesmith.gear.powered.ChainsawItem;
import com.walnutbeachdigital.joulesmith.gear.powered.EnergyCellItem;
import com.walnutbeachdigital.joulesmith.gear.powered.EnergySwordItem;
import com.walnutbeachdigital.joulesmith.gear.powered.ExoArmorItem;
import com.walnutbeachdigital.joulesmith.gear.powered.PowerDrillItem;
import com.walnutbeachdigital.joulesmith.material.Metal;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Material gear: copper and bronze (between stone and iron) and steel (between iron and diamond, much
 * tougher), each a full set of tools and armor, plus the steel Hammer and Excavator that dig 3x3.
 */
public final class JSGear {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Joulesmith.MODID);

    /** One metal's gear stats. */
    public record GearSet(Metal metal, Tier tier, DeferredHolder<ArmorMaterial, ArmorMaterial> armor, int armorDurability) {}

    public static final List<GearSet> SETS = new ArrayList<>();
    /** Every tool and armor piece, by name (e.g. "steel_pickaxe"), in creative-tab order. */
    public static final Map<String, DeferredItem<? extends Item>> ITEMS = new java.util.LinkedHashMap<>();

    public static final GearSet COPPER = set(Metal.COPPER, BlockTags.INCORRECT_FOR_STONE_TOOL, 190, 5.0f, 1.5f, 13,
            new int[] {1, 4, 5, 2}, 11, 9, 0f);
    public static final GearSet BRONZE = set(Metal.BRONZE, BlockTags.INCORRECT_FOR_STONE_TOOL, 230, 5.5f, 1.5f, 16,
            new int[] {2, 4, 5, 2}, 14, 12, 0f);
    public static final GearSet STEEL = set(Metal.STEEL, BlockTags.INCORRECT_FOR_IRON_TOOL, 900, 7.0f, 2.5f, 12,
            new int[] {2, 6, 7, 3}, 25, 11, 1.0f);

    public static final DeferredItem<AreaToolItem> STEEL_HAMMER = JSItems.ITEMS.register("steel_hammer",
            () -> new AreaToolItem(STEEL.tier(), BlockTags.MINEABLE_WITH_PICKAXE, new Item.Properties()
                    .attributes(DiggerItem.createAttributes(STEEL.tier(), 4.0f, -3.2f))));
    public static final DeferredItem<AreaToolItem> STEEL_EXCAVATOR = JSItems.ITEMS.register("steel_excavator",
            () -> new AreaToolItem(STEEL.tier(), BlockTags.MINEABLE_WITH_SHOVEL, new Item.Properties()
                    .attributes(DiggerItem.createAttributes(STEEL.tier(), 3.0f, -3.2f))));

    static {
        JSItems.TAB_ORDER.add(STEEL_HAMMER);
        JSItems.TAB_ORDER.add(STEEL_EXCAVATOR);
    }

    // ---- powered gear: holds FE instead of durability
    /** Every item that holds power, for the energy capability. */
    public static final List<DeferredItem<? extends Item>> POWERED = new ArrayList<>();

    public static final DeferredItem<EnergyCellItem> BASIC_ENERGY_CELL = powered("basic_energy_cell",
            () -> new EnergyCellItem(100_000, 100, new Item.Properties()));
    public static final DeferredItem<EnergyCellItem> REINFORCED_ENERGY_CELL = powered("reinforced_energy_cell",
            () -> new EnergyCellItem(400_000, 250, new Item.Properties()));
    public static final DeferredItem<EnergyCellItem> ELITE_ENERGY_CELL = powered("elite_energy_cell",
            () -> new EnergyCellItem(4_000_000, 1_000, new Item.Properties()));
    public static final DeferredItem<EnergyCellItem> ULTIMATE_ENERGY_CELL = powered("ultimate_energy_cell",
            () -> new EnergyCellItem(16_000_000, 2_000, new Item.Properties()));
    /** The Advanced Energy Cell (kept as "energy_cell" so existing worlds keep theirs). */
    public static final DeferredItem<EnergyCellItem> ENERGY_CELL = powered("energy_cell",
            () -> new EnergyCellItem(1_000_000, 500, new Item.Properties()));
    public static final DeferredItem<PowerDrillItem> POWER_DRILL = powered("power_drill", () -> new PowerDrillItem(new Item.Properties()
            .attributes(DiggerItem.createAttributes(Tiers.DIAMOND, 1.0f, -2.8f))));
    public static final DeferredItem<ChainsawItem> CHAINSAW = powered("chainsaw", () -> new ChainsawItem(new Item.Properties()
            .attributes(DiggerItem.createAttributes(Tiers.DIAMOND, 5.0f, -3.0f))));
    public static final DeferredItem<EnergySwordItem> ENERGY_SWORD = powered("energy_sword", () -> new EnergySwordItem(new Item.Properties()));
    public static final DeferredItem<com.walnutbeachdigital.joulesmith.gear.powered.OmniToolItem> OMNI_TOOL = powered("omni_tool",
            () -> new com.walnutbeachdigital.joulesmith.gear.powered.OmniToolItem(new Item.Properties().fireResistant()
                    .attributes(DiggerItem.createAttributes(Tiers.NETHERITE, 4.0f, -2.6f))));
    public static final DeferredItem<com.walnutbeachdigital.joulesmith.gear.powered.RailgunItem> RAILGUN = powered("railgun",
            () -> new com.walnutbeachdigital.joulesmith.gear.powered.RailgunItem(new Item.Properties().fireResistant()));
    public static final DeferredItem<com.walnutbeachdigital.joulesmith.network.WirelessTerminalItem> WIRELESS_TERMINAL = powered("wireless_terminal",
            () -> new com.walnutbeachdigital.joulesmith.network.WirelessTerminalItem(new Item.Properties()));
    public static final DeferredItem<com.walnutbeachdigital.joulesmith.gear.powered.PlasmaCannonItem> PLASMA_CANNON = powered("plasma_cannon",
            () -> new com.walnutbeachdigital.joulesmith.gear.powered.PlasmaCannonItem(new Item.Properties().fireResistant()));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> EXO = ARMOR_MATERIALS.register("exo", () -> {
        Map<ArmorItem.Type, Integer> def = new EnumMap<>(ArmorItem.Type.class);
        def.put(ArmorItem.Type.BOOTS, 2);
        def.put(ArmorItem.Type.LEGGINGS, 5);
        def.put(ArmorItem.Type.CHESTPLATE, 6);
        def.put(ArmorItem.Type.HELMET, 2);
        def.put(ArmorItem.Type.BODY, 6);
        return new ArmorMaterial(def, 0, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.EMPTY,
                List.of(new ArmorMaterial.Layer(Joulesmith.id("exo"))), 2.0f, 0f);
    });
    public static final DeferredItem<ExoArmorItem> EXO_HELMET = powered("exo_helmet", () -> new ExoArmorItem(EXO, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final DeferredItem<ExoArmorItem> EXO_CHESTPLATE = powered("exo_chestplate", () -> new ExoArmorItem(EXO, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final DeferredItem<ExoArmorItem> EXO_LEGGINGS = powered("exo_leggings", () -> new ExoArmorItem(EXO, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final DeferredItem<ExoArmorItem> EXO_BOOTS = powered("exo_boots", () -> new ExoArmorItem(EXO, ArmorItem.Type.BOOTS, new Item.Properties()));

    /** Elite (tier 4, four module slots) and Ultimate (tier 5, six) Exo-Suits. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ELITE_EXO = exoMaterial("elite_exo", new int[] {3, 6, 8, 3}, 3.0f, 0.05f);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ULTIMATE_EXO = exoMaterial("ultimate_exo", new int[] {3, 7, 9, 4}, 4.0f, 0.15f);
    public static final List<DeferredItem<ExoArmorItem>> HIGHER_EXO = new ArrayList<>();
    static {
        for (var t : List.of(java.util.Map.entry("elite", 4), java.util.Map.entry("ultimate", 5))) {
            var material = t.getValue() == 4 ? ELITE_EXO : ULTIMATE_EXO;
            for (ArmorItem.Type type : new ArmorItem.Type[] {ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS}) {
                int tier = t.getValue();
                HIGHER_EXO.add(powered(t.getKey() + "_exo_" + type.getName(), () -> new ExoArmorItem(tier, material, type,
                        tier == 5 ? new Item.Properties().fireResistant() : new Item.Properties())));
            }
        }
    }

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> exoMaterial(String id, int[] defense, float toughness, float knockback) {
        return ARMOR_MATERIALS.register(id, () -> {
            Map<ArmorItem.Type, Integer> def = new EnumMap<>(ArmorItem.Type.class);
            def.put(ArmorItem.Type.BOOTS, defense[0]);
            def.put(ArmorItem.Type.LEGGINGS, defense[1]);
            def.put(ArmorItem.Type.CHESTPLATE, defense[2]);
            def.put(ArmorItem.Type.HELMET, defense[3]);
            def.put(ArmorItem.Type.BODY, defense[2]);
            return new ArmorMaterial(def, 0, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.EMPTY,
                    List.of(new ArmorMaterial.Layer(Joulesmith.id(id))), toughness, knockback);
        });
    }

    private static <I extends Item> DeferredItem<I> powered(String name, java.util.function.Supplier<I> factory) {
        DeferredItem<I> item = JSItems.ITEMS.register(name, factory);
        POWERED.add(item);
        JSItems.TAB_ORDER.add(item);
        return item;
    }

    /** {@code defense}: boots, leggings, chestplate, helmet. */
    private static GearSet set(Metal metal, net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> incorrect,
                               int uses, float speed, float damage, int enchant, int[] defense, int durability, int armorEnchant, float toughness) {
        String id = metal.id();
        var ingot = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/" + id));
        Tier tier = new SimpleTier(incorrect, uses, speed, damage, enchant, () -> Ingredient.of(ingot));
        DeferredHolder<ArmorMaterial, ArmorMaterial> material = ARMOR_MATERIALS.register(id, () -> {
            Map<ArmorItem.Type, Integer> def = new EnumMap<>(ArmorItem.Type.class);
            def.put(ArmorItem.Type.BOOTS, defense[0]);
            def.put(ArmorItem.Type.LEGGINGS, defense[1]);
            def.put(ArmorItem.Type.CHESTPLATE, defense[2]);
            def.put(ArmorItem.Type.HELMET, defense[3]);
            def.put(ArmorItem.Type.BODY, defense[2]);
            return new ArmorMaterial(def, armorEnchant, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(ingot),
                    List.of(new ArmorMaterial.Layer(Joulesmith.id(id))), toughness, 0f);
        });
        GearSet set = new GearSet(metal, tier, material, durability);
        tool(id + "_sword", () -> new SwordItem(tier, new Item.Properties().attributes(SwordItem.createAttributes(tier, 3, -2.4f))));
        tool(id + "_pickaxe", () -> new PickaxeItem(tier, new Item.Properties().attributes(DiggerItem.createAttributes(tier, 1.0f, -2.8f))));
        tool(id + "_axe", () -> new AxeItem(tier, new Item.Properties().attributes(DiggerItem.createAttributes(tier, 6.0f, -3.1f))));
        tool(id + "_shovel", () -> new ShovelItem(tier, new Item.Properties().attributes(DiggerItem.createAttributes(tier, 1.5f, -3.0f))));
        tool(id + "_hoe", () -> new HoeItem(tier, new Item.Properties().attributes(DiggerItem.createAttributes(tier, -1.0f, -2.0f))));
        armor(id + "_helmet", material, ArmorItem.Type.HELMET, durability);
        armor(id + "_chestplate", material, ArmorItem.Type.CHESTPLATE, durability);
        armor(id + "_leggings", material, ArmorItem.Type.LEGGINGS, durability);
        armor(id + "_boots", material, ArmorItem.Type.BOOTS, durability);
        SETS.add(set);
        return set;
    }

    private static void tool(String name, java.util.function.Supplier<? extends Item> factory) {
        DeferredItem<? extends Item> item = JSItems.ITEMS.register(name, factory);
        ITEMS.put(name, item);
        JSItems.TAB_ORDER.add(item);
    }

    private static void armor(String name, DeferredHolder<ArmorMaterial, ArmorMaterial> material, ArmorItem.Type type, int durability) {
        tool(name, () -> new ArmorItem(material, type, new Item.Properties().durability(type.getDurability(durability))));
    }

    private JSGear() {}
}
