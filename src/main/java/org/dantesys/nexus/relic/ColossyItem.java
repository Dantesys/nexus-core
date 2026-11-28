package org.dantesys.nexus.relic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.dantesys.nexus.util.MatrizModos;

/** Colossy: calca do gigante. So vale vestida. */
public final class ColossyItem extends RelicItem {
    public ColossyItem(Properties properties) {
        super(properties.humanoidArmor(org.dantesys.nexus.items.ModArmorMaterial.RELIC_ARMOR_MATERIAL, net.minecraft.world.item.equipment.ArmorType.LEGGINGS)
                        .component(DataComponents.EQUIPPABLE, SteelyItem.equip(EquipmentSlot.LEGS, "colossy")),
                MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.colossy.gigantismo"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.colossy.soco_terremoto"), ColossyEffects.QUAKE_MANA, ColossyEffects::quake),
                RelicSkill.passive(Component.translatable("relic.nexus.colossy.fome_de_gigante"), 0, ColossyEffects::eat),
                RelicSkill.active(Component.translatable("relic.nexus.colossy.pisao_esmagador"), ColossyEffects.STOMP_MANA, ColossyEffects::stomp),
                new RelicSkill(Component.translatable("relic.nexus.colossy.gancho"), RelicSkill.Kind.ACTIVE, Component.translatable("relic.nexus.colossy.com_60_da_vida_ou"),
                        ColossyEffects::canHook, ColossyEffects::armHook, ColossyEffects.HOOK_MANA));
    }

    @Override protected void applyStageComponents(ServerPlayer player, ItemStack stack, int stage) {
        setArmorAsset(stack, EquipmentSlot.LEGS, "colossy", stage, false);
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.colossy.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        boolean armed = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr(ColossyEffects.HOOK_TAG, false);
        int ticks = switch (slot) {
            case 1 -> ColossyEffects.QUAKE_COOLDOWN;
            case 2 -> ColossyEffects.EAT_COOLDOWN;
            case 3 -> ColossyEffects.STOMP_COOLDOWN;
            case 4 -> armed ? 0 : ColossyEffects.HOOK_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
