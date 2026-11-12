package org.dantesys.nexus.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.util.Ranks;

public class ModItems {
    public static final DeferredRegister.Items  ITEMS = DeferredRegister.createItems(Nexus.MODID);
    public static final DeferredItem<org.dantesys.nexus.relic.RuptyItem> RUPTY =
            ITEMS.registerItem("rupty", org.dantesys.nexus.relic.RuptyItem::new);

    public static final DeferredItem<Item> STEEL = ITEMS.registerSimpleItem("steel");
    public static final DeferredItem<Item> NUCLEO = ITEMS.registerSimpleItem("nucleo");
    public static final DeferredItem<Item> NUCLEO_FORJA = ITEMS.registerSimpleItem("nucleo_forja");
    public static final DeferredItem<Item> FRAGMENTO_IGNEA = ITEMS.registerSimpleItem("fragmento_ignea");
    public static final DeferredItem<Item> FRAGMENTO_VIDA = ITEMS.registerSimpleItem("fragmento_vida");
    public static final DeferredItem<Item> FRAGMENTO_TEMPESTADE = ITEMS.registerSimpleItem("fragmento_tempestade");
    public static final DeferredItem<Item> FRAGMENTO_METAL = ITEMS.registerSimpleItem("fragmento_metal");
    public static final DeferredItem<Item> FRAGMENTO_LUZ = ITEMS.registerSimpleItem("fragmento_luz");
    public static final DeferredItem<Item> FRAGMENTO_SOMBRIA = ITEMS.registerSimpleItem("fragmento_sombria");
    public static final DeferredItem<Item> ESMERALDA_IGNEA = ITEMS.registerSimpleItem("esmeralda_ignea");
    public static final DeferredItem<Item> ESMERALDA_VIDA = ITEMS.registerSimpleItem("esmeralda_vida");
    public static final DeferredItem<Item> ESMERALDA_TEMPESTADE = ITEMS.registerSimpleItem("esmeralda_tempestade");
    public static final DeferredItem<Item> ESMERALDA_METAL = ITEMS.registerSimpleItem("esmeralda_metal");
    public static final DeferredItem<Item> ESMERALDA_LUZ = ITEMS.registerSimpleItem("esmeralda_luz");
    public static final DeferredItem<Item> ESMERALDA_SOMBRIA = ITEMS.registerSimpleItem("esmeralda_sombria");

    public static final DeferredItem<Item> CRISTAL_MANA_S = registerCrystal("cristal_mana_s", Ranks.S);
    public static final DeferredItem<Item> CRISTAL_MANA_A = registerCrystal("cristal_mana_a", Ranks.A);
    public static final DeferredItem<Item> CRISTAL_MANA_B = registerCrystal("cristal_mana_b", Ranks.B);
    public static final DeferredItem<Item> CRISTAL_MANA_C = registerCrystal("cristal_mana_c", Ranks.C);
    public static final DeferredItem<Item> CRISTAL_MANA_D = registerCrystal("cristal_mana_d", Ranks.D);
    public static final DeferredItem<Item> CRISTAL_MANA_E = registerCrystal("cristal_mana_e", Ranks.E);
    public static final DeferredItem<Item> CRISTAL_MANA_F = registerCrystal("cristal_mana_f", Ranks.F);

    public static final DeferredItem<Item> MATRIZ_PRIMORDIAL = ITEMS.registerSimpleItem("matriz_primordial");
    public static final DeferredItem<Item> MATRIZ_SINTETICA = ITEMS.registerSimpleItem("matriz_sintetica");
    public static final DeferredItem<Item> ARCANO_EVOLUIDOR_INCOMUM = ITEMS.registerSimpleItem("arcano_evoluidor_incomum");
    public static final DeferredItem<Item> ARCANO_EVOLUIDOR_RARO = ITEMS.registerSimpleItem("arcano_evoluidor_raro");
    public static final DeferredItem<Item> ARCANO_EVOLUIDOR_EPICO = ITEMS.registerSimpleItem("arcano_evoluidor_epico");
    public static final DeferredItem<Item> ARCANO_EVOLUIDOR_MITICO = ITEMS.registerSimpleItem("arcano_evoluidor_mitico");
    public static final DeferredItem<Item> ARCANO_EVOLUIDOR_LENDARIO = ITEMS.registerSimpleItem("arcano_evoluidor_lendario");
    public static final DeferredItem<Item> ARCANO_EVOLUIDOR_UNICO = ITEMS.registerSimpleItem("arcano_evoluidor_unico");
    public static final DeferredItem<Item> STEEL_SWORD = ITEMS.registerItem(
            "steel_sword", properties -> new Item(properties.sword(ModToolMaterial.STEEL_MATERIA, 3.0F, -2.4F)));
    public static final DeferredItem<Item> STEEL_PICKAXE = ITEMS.registerItem(
            "steel_pickaxe", properties -> new Item(properties.pickaxe(ModToolMaterial.STEEL_MATERIA, 1.0F, -2.8F)));
    public static final DeferredItem<Item> STEEL_AXE = ITEMS.registerItem(
            "steel_axe", properties -> new Item(properties.axe(ModToolMaterial.STEEL_MATERIA, 5.5F, -3.1F)));
    public static final DeferredItem<Item> STEEL_SHOVEL = ITEMS.registerItem(
            "steel_shovel", properties -> new Item(properties.shovel(ModToolMaterial.STEEL_MATERIA, 1.5F, -3.0F)));
    public static final DeferredItem<Item> STEEL_HOE = ITEMS.registerItem(
            "steel_hoe", properties -> new Item(properties.hoe(ModToolMaterial.STEEL_MATERIA, -2.5F, 0.0F)));
    public static final DeferredItem<Item> STEEL_SPEAR = ITEMS.registerItem(
            "steel_spear", properties -> new Item(properties.spear(
                    ModToolMaterial.STEEL_MATERIA,
                    0.85F,
                    0.82F,
                    0.65F,
                    4.0F,
                    9.0F,
                    8.25F,
                    5.1F,
                    12.5F,
                    4.6F
            )));
    public static final DeferredItem<Item> STEEL_HELMET = ITEMS.registerItem(
            "steel_helmet", properties -> new Item(properties.humanoidArmor(
                    ModArmorMaterial.STEEL_ARMOR_MATERIAL, ArmorType.HELMET)));
    public static final DeferredItem<Item> STEEL_CHESTPLATE = ITEMS.registerItem(
            "steel_chestplate", properties -> new Item(properties.humanoidArmor(
                    ModArmorMaterial.STEEL_ARMOR_MATERIAL, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> STEEL_LEGGINGS = ITEMS.registerItem(
            "steel_leggings", properties -> new Item(properties.humanoidArmor(
                    ModArmorMaterial.STEEL_ARMOR_MATERIAL, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> STEEL_BOOTS = ITEMS.registerItem(
            "steel_boots", properties -> new Item(properties.humanoidArmor(
                    ModArmorMaterial.STEEL_ARMOR_MATERIAL, ArmorType.BOOTS)));

    public static void register(IEventBus bus){
        ITEMS.register(bus);
    }

    private static DeferredItem<Item> registerCrystal(String name, Ranks rank) {
        return ITEMS.registerItem(name,
                properties -> new RelicCrystalItem(properties, rank));
    }

    public static Item getCrystal(Ranks rank) {
        return switch (rank) {
            case F -> CRISTAL_MANA_F.get();
            case E -> CRISTAL_MANA_E.get();
            case D -> CRISTAL_MANA_D.get();
            case C -> CRISTAL_MANA_C.get();
            case B -> CRISTAL_MANA_B.get();
            case A -> CRISTAL_MANA_A.get();
            case S -> CRISTAL_MANA_S.get();
        };
    }
}
