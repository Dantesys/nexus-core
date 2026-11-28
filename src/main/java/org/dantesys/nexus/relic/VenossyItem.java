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
        super(properties.sword(ModToolMaterial.RELIC, 3.0F, -1.8F), MatrizModos.ESPADA, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.venossy.mestre_dos_venenos"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.venossy.aura_de_assassino"), VenossyEffects.AURA_MANA, VenossyEffects::aura),
                RelicSkill.active(Component.translatable("relic.nexus.venossy.toxina_potente"), VenossyEffects.TOXIN_MANA, VenossyEffects::throwToxin),
                RelicSkill.active(Component.translatable("relic.nexus.venossy.veneno_paralisante"), VenossyEffects.PARALYZE_MANA, VenossyEffects::throwParalyze),
                new RelicSkill(Component.translatable("relic.nexus.venossy.sangue_toxico_2"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.venossy.estar_com_a_toxina_potente"),
                        VenossyEffects::canToxicBlood, VenossyEffects::toxicBlood, VenossyEffects.BLOOD_MANA));
    }

    @Override protected boolean allowWhileParalyzed(int slot) { return slot == 4; }

    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        return List.of(VenossyEffects.thrown(player) ? 1f : 0f);
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.venossy.skill." + slot + ".desc");
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
