package com.walnutbeachdigital.joulesmith.gear.powered;

import com.walnutbeachdigital.joulesmith.Joulesmith;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** The modules and Exo-Suit effects that aren't enchantments: smelting, magnet, damage soaking, armor modules. */
public final class PoweredGearEvents {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    /** FE per tick each active armor module uses, and the jetpack while flying. */
    private static final int FIRE_COST_PER_POINT = 200, FEED_COST = 1_000, REGEN_COST = 400;
    private static final int MODULE_COST = 4, JETPACK_COST = 60, SOLAR = 25, FALL_COST_PER_BLOCK = 100;

    private static final ResourceLocation SWIFTNESS = Joulesmith.id("exo_swiftness");
    private static final ResourceLocation JUMP = Joulesmith.id("exo_jump_boost");
    private static final ResourceLocation STEP = Joulesmith.id("exo_step_assist");
    private static final ResourceLocation JETPACK = Joulesmith.id("exo_jetpack");
    /** FE per tick the Jetpack uses while you're in the air. */
    private static final int JETPACK_THRUST_COST = 30;

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PoweredGearEvents::blockDrops);
        NeoForge.EVENT_BUS.addListener(PoweredGearEvents::mobDrops);
        NeoForge.EVENT_BUS.addListener(PoweredGearEvents::incomingDamage);
        NeoForge.EVENT_BUS.addListener(PoweredGearEvents::fall);
        NeoForge.EVENT_BUS.addListener(PoweredGearEvents::playerTick);
        NeoForge.EVENT_BUS.addListener(PoweredGearEvents::afterDamage);
    }

    // ---------------------------------------------------------------- tools and weapon

    private static void blockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (tool.isEmpty() || !(tool.getItem() instanceof PoweredItem)) return;
        if (PoweredItem.hasModule(tool, Module.AUTO_SMELT)) {
            Level level = event.getLevel();
            for (ItemEntity drop : event.getDrops()) {
                ItemStack stack = drop.getItem();
                level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level).ifPresent(recipe -> {
                    ItemStack result = recipe.value().getResultItem(level.registryAccess());
                    if (!result.isEmpty()) drop.setItem(result.copyWithCount(Math.min(result.getMaxStackSize(), result.getCount() * stack.getCount())));
                });
            }
        }
        if (PoweredItem.hasModule(tool, Module.MAGNET) && event.getBreaker() instanceof Player player) {
            pull(event.getDrops(), player);
        }
    }

    private static void mobDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon != null && weapon.getItem() instanceof PoweredItem && PoweredItem.hasModule(weapon, Module.MAGNET)) {
            pull(event.getDrops(), player);
        }
    }

    /** Moves drops onto the player, ready to pick up. */
    private static void pull(java.util.Collection<ItemEntity> drops, Player player) {
        for (ItemEntity drop : drops) {
            drop.setPos(player.getX(), player.getY() + 0.5, player.getZ());
            drop.setDeltaMovement(0, 0, 0);
            drop.setNoPickUpDelay();
        }
    }

    private static final ThreadLocal<Boolean> CHAINING = ThreadLocal.withInitial(() -> false);
    private static final int CHAIN_TARGETS = 3, CHAIN_COST = 300;
    private static final float CHAIN_DAMAGE = 4.0f, CHAIN_RANGE = 5.0f, LIFESTEAL = 0.2f;

    /** Energy Sword modules that act on a hit that landed: Lifesteal heals, Chain Lightning arcs onward. */
    private static void afterDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (CHAINING.get() || !(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker.level().isClientSide) return;
        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon == null || !(weapon.getItem() instanceof PoweredItem) || event.getNewDamage() <= 0) return;
        if (PoweredItem.hasModule(weapon, Module.LIFESTEAL)) attacker.heal(event.getNewDamage() * LIFESTEAL);
        if (PoweredItem.hasModule(weapon, Module.CHAIN_LIGHTNING) && attacker.level() instanceof net.minecraft.server.level.ServerLevel level) {
            LivingEntity victim = event.getEntity();
            var nearby = level.getEntitiesOfClass(LivingEntity.class,
                    victim.getBoundingBox().inflate(CHAIN_RANGE),
                    e -> e != victim && e != attacker && e.isAlive() && e instanceof net.minecraft.world.entity.monster.Enemy);
            CHAINING.set(true);
            try {
                int hits = 0;
                for (LivingEntity next : nearby) {
                    if (hits >= CHAIN_TARGETS || !PoweredItem.use(weapon, CHAIN_COST, attacker)) break;
                    next.hurt(level.damageSources().indirectMagic(attacker, attacker), CHAIN_DAMAGE);
                    spark(level, victim.position().add(0, victim.getBbHeight() / 2, 0), next.position().add(0, next.getBbHeight() / 2, 0));
                    hits++;
                }
            } finally {
                CHAINING.set(false);
            }
        }
    }

    private static void spark(net.minecraft.server.level.ServerLevel level, net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 to) {
        for (int i = 0; i <= 8; i++) {
            var p = from.lerp(to, i / 8.0);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
        }
    }

    // ---------------------------------------------------------------- Exo-Suit

    private static void incomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        float amount = event.getAmount();
        float absorbed = 0;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack piece = entity.getItemBySlot(slot);
            if (!(piece.getItem() instanceof ExoArmorItem)) continue;
            if (event.getSource().is(DamageTypeTags.IS_FIRE) && PoweredItem.hasModule(piece, Module.FIRE_SHIELD)
                    && PoweredItem.use(piece, (int) Math.ceil(amount * FIRE_COST_PER_POINT), entity)) {
                event.setAmount(0);
                return;
            }
            ExoArmorItem exo = (ExoArmorItem) piece.getItem();
            float rate = (float) com.walnutbeachdigital.joulesmith.JSConfig.get(com.walnutbeachdigital.joulesmith.JSConfig.EXO_ABSORB)
                    * exo.absorbScale() * (PoweredItem.hasModule(piece, Module.PLATING) ? 1.25f : 1f);
            float share = Math.min(amount - absorbed, amount * rate);
            int cost = (int) Math.ceil(share * ExoArmorItem.FE_PER_POINT);
            if (PoweredItem.use(piece, cost, entity)) absorbed += share;
        }
        if (absorbed > 0) event.setAmount(amount - absorbed);
    }

    private static void fall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        ItemStack boots = entity.getItemBySlot(EquipmentSlot.FEET);
        if (event.getDistance() <= 3 || !(boots.getItem() instanceof ExoArmorItem)
                || !PoweredItem.hasModule(boots, Module.FALL_PROTECTION)) return;
        int cost = (int) (event.getDistance() * FALL_COST_PER_BLOCK);
        if (entity.level().isClientSide ? PoweredItem.has(boots, cost, entity) : PoweredItem.use(boots, cost, entity)) {
            event.setDamageMultiplier(0);
        }
    }

    /** Armor modules, every tick on both sides (attributes), using power on the server only. */
    private static void playerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean server = !player.level().isClientSide;
        if (server && player.level().getGameTime() % 4 == 0) {
            for (ItemStack worn : com.walnutbeachdigital.joulesmith.compat.curios.CuriosCompat.equipped(player)) {
                if (worn.getItem() instanceof EnergyCellItem cell) cell.chargeGear(worn, player);
            }
        }
        ItemStack head = exo(player, EquipmentSlot.HEAD), chest = exo(player, EquipmentSlot.CHEST),
                legs = exo(player, EquipmentSlot.LEGS), feet = exo(player, EquipmentSlot.FEET);

        // the solar helmet charges the whole suit in daylight under open sky
        if (server && head != null && PoweredItem.hasModule(head, Module.SOLAR) && player.level().isDay()
                && !player.level().isRaining() && player.level().canSeeSky(player.blockPosition().above())) {
            int left = SOLAR;
            for (ItemStack piece : new ItemStack[] {head, chest, legs, feet}) {
                if (piece == null || left <= 0) continue;
                int room = ((PoweredItem) piece.getItem()).capacity() - PoweredItem.energy(piece);
                int add = Math.min(room, left);
                if (add > 0) {
                    PoweredItem.setEnergy(piece, PoweredItem.energy(piece) + add);
                    left -= add;
                }
            }
        }

        if (active(head, Module.NIGHT_VISION, player, server)) effect(player, MobEffects.NIGHT_VISION, 300, 220);
        if (server && player.level().getGameTime() % 80 == 0) {
            // Feeder: a point of food (and a little saturation) every four seconds while hungry
            if (head != null && PoweredItem.hasModule(head, Module.FEEDER) && player.getFoodData().needsFood()
                    && PoweredItem.use(head, FEED_COST, player)) {
                player.getFoodData().eat(1, 0.5f);
            }
            // Regeneration: any piece; heals while hurt
            for (ItemStack piece : new ItemStack[] {head, chest, legs, feet}) {
                if (piece != null && PoweredItem.hasModule(piece, Module.REGENERATION) && player.getHealth() < player.getMaxHealth()
                        && PoweredItem.use(piece, REGEN_COST, player)) {
                    player.heal(1.0f);
                    break;
                }
            }
        }
        if (active(head, Module.WATER_BREATHING, player, server && player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER))) {
            effect(player, MobEffects.WATER_BREATHING, 60, 40);
        }
        modifier(player, Attributes.MOVEMENT_SPEED, SWIFTNESS, active(legs, Module.SWIFTNESS, player, server && player.isSprinting()),
                0.35, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(player, Attributes.JUMP_STRENGTH, JUMP, active(feet, Module.JUMP_BOOST, player, false),
                0.17, AttributeModifier.Operation.ADD_VALUE);
        modifier(player, Attributes.STEP_HEIGHT, STEP, active(legs, Module.STEP_ASSIST, player, false),
                0.5, AttributeModifier.Operation.ADD_VALUE);

        // Flight (tier 5 module): creative-style flight while charged, using power while flying
        boolean flight = chest != null && PoweredItem.hasModule(chest, Module.FLIGHT)
                && PoweredItem.has(chest, com.walnutbeachdigital.joulesmith.JSConfig.get(com.walnutbeachdigital.joulesmith.JSConfig.JETPACK_COST), player);
        if (flight && server && player.getAbilities().flying && !player.getAbilities().instabuild) {
            PoweredItem.use(chest, com.walnutbeachdigital.joulesmith.JSConfig.get(com.walnutbeachdigital.joulesmith.JSConfig.JETPACK_COST), player);
        }
        modifier(player, NeoForgeMod.CREATIVE_FLIGHT, JETPACK, flight, 1.0, AttributeModifier.Operation.ADD_VALUE);

        // Jetpack (tier 3 module): hold jump to climb slowly; falls are cushioned. Power is used while airborne.
        boolean jetpack = !flight && chest != null && PoweredItem.hasModule(chest, Module.JETPACK)
                && PoweredItem.has(chest, JETPACK_THRUST_COST, player);
        if (jetpack && !player.onGround() && !player.getAbilities().flying) {
            if (server) {
                player.fallDistance = 0;
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) NoFlyKick.reset(sp);
                if (player.level().getGameTime() % 2 == 0) PoweredItem.use(chest, JETPACK_THRUST_COST * 2, player);
            } else if (JetpackInput.thrusting(player)) {
                var v = player.getDeltaMovement();
                player.setDeltaMovement(v.x, Math.min(v.y + 0.12, 0.3), v.z);
            }
        }
    }

    private static ItemStack exo(Player player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        return stack.getItem() instanceof ExoArmorItem ? stack : null;
    }

    /** Whether a piece has this module and the power to run it; uses that power (on the server) when {@code drain}. */
    private static boolean active(ItemStack piece, Module module, Player player, boolean drain) {
        if (piece == null || !PoweredItem.hasModule(piece, module) || !PoweredItem.has(piece, MODULE_COST, player)) return false;
        if (drain) PoweredItem.use(piece, MODULE_COST, player);
        return true;
    }

    private static void effect(Player player, Holder<MobEffect> effect, int duration, int renewBelow) {
        if (player.level().isClientSide) return;
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getDuration() < renewBelow) {
            player.addEffect(new MobEffectInstance(effect, duration, 0, true, false, false));
        }
    }

    private static void modifier(Player player, Holder<Attribute> attribute, ResourceLocation id, boolean on, double amount,
                                 AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        if (on) {
            if (!instance.hasModifier(id)) instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
        } else if (instance.hasModifier(id)) {
            instance.removeModifier(id);
        }
    }

    private PoweredGearEvents() {}
}
