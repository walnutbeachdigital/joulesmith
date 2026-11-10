package com.walnutbeachdigital.joulesmith.gear.powered;

import java.util.Comparator;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Hold use to charge for a second, release to fire: a straight shot up to 64 blocks that passes through
 * up to four creatures (24 damage each) and stops at the first solid block. 5,000 FE a shot.
 */
public class RailgunItem extends Item implements PoweredItem {
    public static final int CAPACITY = 2_000_000, SHOT_COST = 5_000, CHARGE_TICKS = 20, RANGE = 64, PIERCE = 4;
    public static final float DAMAGE = 24.0f;

    private final int capacity, shotCost, chargeTicks, range, pierce;
    private final float damage, splash;
    private final String tooltipKey;

    public RailgunItem(Properties properties) {
        this(CAPACITY, SHOT_COST, CHARGE_TICKS, RANGE, PIERCE, DAMAGE, 0, "tooltip.joulesmith.railgun", properties);
    }

    /** {@code splash}: radius of the blast where the shot stops (0 for none); it hurts creatures, never blocks. */
    protected RailgunItem(int capacity, int shotCost, int chargeTicks, int range, int pierce, float damage, float splash,
                          String tooltipKey, Properties properties) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
        this.shotCost = shotCost;
        this.chargeTicks = chargeTicks;
        this.range = range;
        this.pierce = pierce;
        this.damage = damage;
        this.splash = splash;
        this.tooltipKey = tooltipKey;
    }

    @Override public int capacity() { return capacity; }
    @Override public int transferRate() { return 10_000; }
    @Override public boolean accepts(Module module) { return false; }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72_000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!PoweredItem.has(stack, shotCost, player)) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        if (!level.isClientSide) level.playSound(null, player, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.5f, 1.8f);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        int charged = getUseDuration(stack, user) - timeLeft;
        if (charged < chargeTicks || level.isClientSide || !(level instanceof ServerLevel server)) return;
        if (!PoweredItem.use(stack, shotCost, user)) return;
        Vec3 start = user.getEyePosition();
        Vec3 end = start.add(user.getLookAngle().scale(range));
        var block = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        Vec3 from = start, to = end;
        List<Entity> hits = level.getEntities(user, new AABB(start, end).inflate(1.0),
                        e -> e.isPickable() && e.isAlive() && e.getBoundingBox().inflate(0.3).clip(from, to).isPresent())
                .stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(from))).limit(pierce).toList();
        var source = user instanceof Player p ? level.damageSources().playerAttack(p) : level.damageSources().mobAttack(user);
        for (Entity e : hits) e.hurt(source, damage);
        if (splash > 0) {
            Vec3 at = end;
            for (Entity e : level.getEntities(user, new AABB(at, at).inflate(splash), e -> e instanceof LivingEntity && e.isAlive() && !hits.contains(e))) {
                float falloff = (float) Math.max(0.25, 1 - e.position().distanceTo(at) / splash);
                e.hurt(source, damage * 0.6f * falloff);
                e.igniteForSeconds(4);
            }
            server.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 2, 0.5, 0.5, 0.5, 0);
            server.sendParticles(ParticleTypes.DRAGON_BREATH, at.x, at.y, at.z, 40, splash / 2, splash / 3, splash / 2, 0.02);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 1.4f);
        }
        Vec3 step = end.subtract(start);
        int points = (int) (step.length() * 3);
        for (int i = 2; i < points; i++) {
            Vec3 at = start.add(step.scale(i / (double) points)).add(0, -0.15, 0);
            server.sendParticles(splash > 0 ? ParticleTypes.WITCH : ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, user, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.2f, 0.6f);
        if (user instanceof Player p) p.getCooldowns().addCooldown(this, 10);
    }

    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return PoweredItem.barWidth(stack); }
    @Override public int getBarColor(ItemStack stack) { return BAR_COLOR; }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
        PoweredItem.appendTooltip(stack, tooltip);
    }
}
