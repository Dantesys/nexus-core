package org.dantesys.nexus.items;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.tags.ModTags;

import java.util.EnumMap;

public final class ModArmorMaterial {
    public static final ResourceKey<EquipmentAsset> STEEL_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(Nexus.MODID, "steel")
    );

    public static final ArmorMaterial STEEL_ARMOR_MATERIAL = new ArmorMaterial(
            24,
            Util.make(new EnumMap<>(ArmorType.class), defense -> {
                defense.put(ArmorType.BOOTS, 3);
                defense.put(ArmorType.LEGGINGS, 6);
                defense.put(ArmorType.CHESTPLATE, 7);
                defense.put(ArmorType.HELMET, 2);
                defense.put(ArmorType.BODY, 6);
            }),
            10,
            SoundEvents.ARMOR_EQUIP_IRON,
            1.0F,
            0.0F,
            ModTags.Items.STEEL_INGOTS,
            STEEL_ASSET
    );

    private ModArmorMaterial() {
    }
}
