package com.walnutbeachdigital.joulesmith.automation;

import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

/**
 * A Mob Imprint: right-click a mob to record what kind it is (the mob is unharmed). The Mob Duplicator
 * grows copies of the recorded kind. Sneak and use it in the air to wipe it. Bosses can't be imprinted.
 */
public class MobImprintItem extends Item {
    private static final String KEY = "joulesmith_mob";

    public MobImprintItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    public static Optional<EntityType<?>> mob(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(KEY)) return Optional.empty();
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(KEY));
        return id == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(id);
    }

    public static boolean canImprint(LivingEntity entity) {
        return entity instanceof Mob && entity.getType().canSummon() && !entity.getType().is(Tags.EntityTypes.BOSSES);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (mob(stack).isPresent()) return InteractionResult.PASS;
        if (!canImprint(target)) {
            if (!player.level().isClientSide) player.displayClientMessage(Component.translatable("message.joulesmith.imprint.refused"), true);
            return InteractionResult.FAIL;
        }
        if (!player.level().isClientSide) {
            ItemStack filled = stack.split(1);
            CompoundTag tag = new CompoundTag();
            tag.putString(KEY, BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());
            filled.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            if (!player.getInventory().add(filled)) player.drop(filled, false);
            player.displayClientMessage(Component.translatable("message.joulesmith.imprint.taken", target.getType().getDescription()), true);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && mob(stack).isPresent()) {
            stack.remove(DataComponents.CUSTOM_DATA);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return mob(stack).isPresent();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        mob(stack).ifPresentOrElse(
                type -> tooltip.add(Component.translatable("tooltip.joulesmith.imprint.mob", type.getDescription()).withStyle(ChatFormatting.AQUA)),
                () -> tooltip.add(Component.translatable("tooltip.joulesmith.imprint.blank").withStyle(ChatFormatting.GRAY)));
    }
}
