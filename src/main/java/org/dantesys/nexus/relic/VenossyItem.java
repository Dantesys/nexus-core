package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.items.ModToolMaterial;
import org.dantesys.nexus.util.MatrizModos;

import java.util.List;

/** Venossy: adaga do assassino. */
public final class VenossyItem extends RelicItem {
    public VenossyItem(Properties properties) {
        super(properties.sword(ModToolMaterial.STEEL_MATERIA, 3.0F, -1.8F), MatrizModos.ESPADA, 0, 0, 2,
                RelicSkill.passive(Component.literal("Mestre dos Venenos"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Aura de Assassino"), VenossyEffects.AURA_MANA, VenossyEffects::aura),
                RelicSkill.active(Component.literal("Toxina Potente"), VenossyEffects.TOXIN_MANA, VenossyEffects::throwToxin),
                RelicSkill.active(Component.literal("Veneno Paralisante"), VenossyEffects.PARALYZE_MANA, VenossyEffects::throwParalyze),
                new RelicSkill(Component.literal("Sangue Tóxico"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Estar com a Toxina Potente e o Veneno Paralisante em você."),
                        VenossyEffects::canToxicBlood, VenossyEffects::toxicBlood, VenossyEffects.BLOOD_MANA));
    }

    @Override protected boolean allowWhileParalyzed(int slot) { return slot == 4; }

    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        return List.of(VenossyEffects.thrown(player) ? 1f : 0f);
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Imune a veneno. Seus golpes envenenam (I, II no nível 10, III no 20), até mortos-vivos, e o veneno pode matar.";
            case 1 -> "Aura de 3 blocos por 10s: inimigos nela levam o dobro do seu veneno e dos seus golpes.";
            case 2 -> "Lança a adaga (volta sozinha): toxina que tira meio coração a cada 5s até a morte. Só o totem remove.";
            case 3 -> "Lança a adaga (volta sozinha): o alvo fica totalmente paralisado por 5s, até bosses.";
            case 4 -> "Com os dois venenos em você, tira-os e o próximo golpe aplica versões amplificadas e sem cura.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> VenossyEffects.AURA_COOLDOWN;
            case 2 -> VenossyEffects.TOXIN_COOLDOWN;
            case 3 -> VenossyEffects.PARALYZE_COOLDOWN;
            case 4 -> VenossyEffects.BLOOD_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
