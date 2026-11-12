package org.dantesys.nexus.gui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.gui.menu.BolaCristalMenu;
import org.dantesys.nexus.gui.menu.InfusorBasicoMenu;
import org.dantesys.nexus.gui.menu.ForjaFullMenu;

import java.util.function.Supplier;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, Nexus.MODID);

    public static final Supplier<MenuType<BolaCristalMenu>> BOLA_CRISTAL_MENU = MENUS.register("bola_cristal_menu", () -> IMenuTypeExtension.create(BolaCristalMenu::fromNetwork));
    public static final Supplier<MenuType<InfusorBasicoMenu>> INFUSOR_BASICO_MENU = MENUS.register("infusor_basico_menu", () -> IMenuTypeExtension.create(InfusorBasicoMenu::fromNetwork));
    public static final Supplier<MenuType<ForjaFullMenu>> FORJA_FULL_MENU = MENUS.register("forja_full_menu", () -> IMenuTypeExtension.create(ForjaFullMenu::fromNetwork));

    public static void register(IEventBus bus){
        MENUS.register(bus);
    }
}
