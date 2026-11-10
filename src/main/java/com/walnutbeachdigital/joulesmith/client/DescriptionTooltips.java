package com.walnutbeachdigital.joulesmith.client;

import com.walnutbeachdigital.joulesmith.Joulesmith;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Adds a one-line "what it does" (lang key desc.joulesmith.&lt;id&gt;) under the name of Joulesmith blocks and parts. */
public final class DescriptionTooltips {
    public static void add(ItemTooltipEvent event) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!Joulesmith.MODID.equals(id.getNamespace())) return;
        String key = "desc." + Joulesmith.MODID + "." + id.getPath();
        if (!Language.getInstance().has(key)) return;
        int at = Math.min(1, event.getToolTip().size());          // right under the name
        event.getToolTip().add(at, Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    private DescriptionTooltips() {}
}
