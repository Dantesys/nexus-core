package org.dantesys.nexus.relic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.ArmorType;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModArmorMaterial;
import org.dantesys.nexus.util.MatrizModos;

/** Steely: peitoral acima do netherite. */
public final class SteelyItem extends RelicItem {
    static ItemAttributeModifiers armor(String name, EquipmentSlotGroup group, double armor, double toughness, double knockback) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(Identifier.fromNamespaceAndPath(Nexus.MODID, name + "_armadura"), armor, AttributeModifier.Operation.ADD_VALUE), group)
                .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(Identifier.fromNamespaceAndPath(Nexus.MODID, name + "_dureza"), toughness, AttributeModifier.Operation.ADD_VALUE), group)
                .add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(Identifier.fromNamespaceAndPath(Nexus.MODID, name + "_repulsao"), knockback, AttributeModifier.Operation.ADD_VALUE), group)
                .build();
    }

    static Equippable equip(EquipmentSlot slot, String relic) {
        return Equippable.builder(slot).setEquipSound(ModArmorMaterial.RELIC_ARMOR_MATERIAL.equipSound()).setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Nexus.MODID, relic + "_dormente"))).build();
    }

    public SteelyItem(Properties properties) {
        super(properties.humanoidArmor(ModArmorMaterial.RELIC_ARMOR_MATERIAL, ArmorType.CHESTPLATE)
                        .attributes(armor("steely", EquipmentSlotGroup.CHEST, 9, 4, 0.15))
                        .component(DataComponents.EQUIPPABLE, equip(EquipmentSlot.CHEST, "steely")),
                MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.steely.aco_arcano"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.steely.corpo_de_aco"), SteelyEffects.BODY_MANA, SteelyEffects::steelBody),
                RelicSkill.passive(Component.translatable("relic.nexus.steely.aco_duro"), 0, (p, s, c) -> false),
                new RelicSkill(Component.translatable("relic.nexus.steely.guarda_costas"), RelicSkill.Kind.ACTIVE, Component.translatable("relic.nexus.steely.so_um_golem_por_vez"),
                        SteelyEffects::canGuard, SteelyEffects::guard, SteelyEffects.GUARD_MANA),
                new RelicSkill(Component.translatable("relic.nexus.steely.eu_sou_o_iron_golem_2"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.steely.o_guarda_costas_precisa_estar"),
                        SteelyEffects::canTransform, SteelyEffects::transform, SteelyEffects.GOLEM_MANA));
    }

    @Override protected void applyStageComponents(ServerPlayer player, ItemStack stack, int stage) {
        setArmorAsset(stack, EquipmentSlot.CHEST, "steely", stage, false);
    }

    @Override protected boolean skillsAvailable(ServerPlayer player, ItemStack stack, int slot) {
        return !SteelyEffects.transformed(player);
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.steely.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> SteelyEffects.BODY_COOLDOWN;
            case 4 -> SteelyEffects.GOLEM_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
