package org.dantesys.nexus.relic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.dantesys.nexus.util.MatrizModos;

/** Colossy: calca do gigante. So vale vestida. */
public final class ColossyItem extends RelicItem {
    public ColossyItem(Properties properties) {
        super(properties.attributes(SteelyItem.armor("colossy", EquipmentSlotGroup.LEGS, 6, 3, 0.1))
                        .component(DataComponents.EQUIPPABLE, SteelyItem.equip(EquipmentSlot.LEGS, "colossy")),
                MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.literal("Gigantismo"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Soco Terremoto"), ColossyEffects.QUAKE_MANA, ColossyEffects::quake),
                RelicSkill.passive(Component.literal("Fome de Gigante"), 0, ColossyEffects::eat),
                RelicSkill.active(Component.literal("Pisão Esmagador"), ColossyEffects.STOMP_MANA, ColossyEffects::stomp),
                new RelicSkill(Component.literal("Gancho"), RelicSkill.Kind.ACTIVE, Component.literal("Com 60% da vida ou menos."),
                        ColossyEffects::canHook, ColossyEffects::armHook, ColossyEffects.HOOK_MANA));
    }

    @Override protected void applyStageComponents(ServerPlayer player, ItemStack stack, int stage) {
        setArmorAsset(stack, EquipmentSlot.LEGS, "colossy", stage, false);
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Vestida: fica maior e ganha, na mesma proporção, vida, dano, defesa e pulo (10% a 30%), +0,5 bloco de alcance.";
            case 1 -> "Soca o chão: dano numa área de 4 blocos e Lentidão II por 3s.";
            case 2 -> "Atacar um animal pacífico sem dono o come: recupera 50% da vida e toda a fome (recarga 1 min).";
            case 3 -> "Pisão à frente (3x3): dano e encolhe os atingidos pela metade por 10s.";
            case 4 -> "Com pouca vida, o próximo golpe vira um gancho que lança o alvo ~20 blocos para cima e o atordoa ao cair.";
            default -> "";
        });
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
