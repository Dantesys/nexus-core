package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.core.component.DataComponents;
import org.dantesys.nexus.util.MatrizModos;

import java.util.List;
import java.util.Optional;

/** Bastry: escudo que guarda o dano bloqueado e o devolve. Identidade: refletir projeteis. */
public final class BastryItem extends RelicItem {
    public BastryItem(Properties properties) {
        super(properties.delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(
                        0.25F, 1.0F, List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
                        new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
                        Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
                        Optional.of(SoundEvents.SHIELD_BLOCK), Optional.of(SoundEvents.SHIELD_BREAK))),
                MatrizModos.ESPADA, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.bastry.carga"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.bastry.campo_reflexivo"), BastryEffects.FIELD_MANA, BastryEffects::field),
                RelicSkill.active(Component.translatable("relic.nexus.bastry.counter"), BastryEffects.COUNTER_MANA, BastryEffects::counter),
                RelicSkill.active(Component.translatable("relic.nexus.bastry.pele_de_aco"), BastryEffects.STEEL_MANA, BastryEffects::steelSkin),
                new RelicSkill(Component.translatable("relic.nexus.bastry.intocavel"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.bastry.capacidade_rank_a_ou_maior"),
                        BastryEffects::canUntouchable, BastryEffects::untouchable, BastryEffects.UNTOUCHABLE_MANA));
    }

    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.BLOCK; }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.bastry.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> BastryEffects.FIELD_COOLDOWN;
            case 2 -> BastryEffects.COUNTER_COOLDOWN;
            case 3 -> BastryEffects.STEEL_COOLDOWN;
            case 4 -> BastryEffects.UNTOUCHABLE_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }

    /** custom_model_data: [estagio, carga, limite]. */
    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        return List.of((float) Math.floor(BastryEffects.charge(player)), (float) Math.floor(BastryEffects.cap(player)));
    }

    @Override public Component hudInfo(ItemStack stack) {
        return Component.translatable("relic.nexus.bastry.carga_2", (int) modelValue(stack, 1, 0), (int) modelValue(stack, 2, 0));
    }
}
