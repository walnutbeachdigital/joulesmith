package com.walnutbeachdigital.joulesmith.registry;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.item.filter.FilterMenu;
import com.walnutbeachdigital.joulesmith.logic.LogicMenu;
import com.walnutbeachdigital.joulesmith.machine.MachineMenu;
import com.walnutbeachdigital.joulesmith.storage.crate.CrateMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** One menu type serves every Joulesmith machine; the Filter Card has its own. */
public final class JSMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Joulesmith.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MACHINE =
            MENUS.register("machine", () -> IMenuTypeExtension.create(MachineMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FilterMenu>> FILTER =
            MENUS.register("filter", () -> IMenuTypeExtension.create(FilterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<com.walnutbeachdigital.joulesmith.network.TerminalMenu>> TERMINAL =
            MENUS.register("network_terminal", () -> IMenuTypeExtension.create(com.walnutbeachdigital.joulesmith.network.TerminalMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CrateMenu>> CRATE =
            MENUS.register("crate", () -> IMenuTypeExtension.create(CrateMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<LogicMenu>> LOGIC =
            MENUS.register("logic", () -> IMenuTypeExtension.create(LogicMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<com.walnutbeachdigital.joulesmith.gear.table.ModificationTableMenu>> MODIFICATION_TABLE =
            MENUS.register("modification_table", () -> IMenuTypeExtension.create(com.walnutbeachdigital.joulesmith.gear.table.ModificationTableMenu::new));

    private JSMenus() {}
}
