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
import org.dantesys.nexus.Nexus;
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
        return Equippable.builder(slot).setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Nexus.MODID, relic + "_dormente"))).build();
    }

    public SteelyItem(Properties properties) {
        super(properties.attributes(armor("steely", EquipmentSlotGroup.CHEST, 9, 4, 0.15)).component(DataComponents.EQUIPPABLE, equip(EquipmentSlot.CHEST, "steely")),
                MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.literal("Aço Arcano"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Corpo de Aço"), SteelyEffects.BODY_MANA, SteelyEffects::steelBody),
                RelicSkill.passive(Component.literal("Aço Duro"), 0, (p, s, c) -> false),
                new RelicSkill(Component.literal("Guarda-Costas"), RelicSkill.Kind.ACTIVE, Component.literal("Só um golem por vez."),
                        SteelyEffects::canGuard, SteelyEffects::guard, SteelyEffects.GUARD_MANA),
                new RelicSkill(Component.literal("Eu Sou o Iron Golem"), RelicSkill.Kind.ACTIVE,
                        Component.literal("O Guarda-Costas precisa estar pronto para uso."),
                        SteelyEffects::canTransform, SteelyEffects::transform, SteelyEffects.GOLEM_MANA));
    }

    @Override protected void applyStageComponents(ServerPlayer player, ItemStack stack, int stage) {
        setArmorAsset(stack, EquipmentSlot.CHEST, "steely", stage, false);
    }

    @Override protected boolean skillsAvailable(ServerPlayer player, ItemStack stack, int slot) {
        return !SteelyEffects.transformed(player);
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Vestido: reduz todo dano recebido (20% no nível 1 até 50% no 20), até 85% somando armadura e Resistência.";
            case 1 -> "Por 20s: Resistência II e imunidade a ataques mágicos (inclusive de relíquias). Finais e mortes garantidas ignoram.";
            case 2 -> "Devolve 15% do dano recebido (antes das reduções) a quem atacou, inclusive à distância.";
            case 3 -> "Invoca um golem de meio tamanho que causa o dobro de dano e te protege. Recarga conta da morte dele.";
            case 4 -> "Vira o Guarda-Costas por 3 min: ~50 corações de absorção, dano dobrado e socos que arremessam. Só soca.";
            default -> "";
        });
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
