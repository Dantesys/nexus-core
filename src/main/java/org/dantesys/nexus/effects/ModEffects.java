package org.dantesys.nexus.effects;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.dantesys.nexus.Nexus;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT,Nexus.MODID);

    public static final Holder<MobEffect> PELE_MANA = EFFECTS.register("pele_mana", () -> new PeleMana(
            //Can be either BENEFICIAL, NEUTRAL or HARMFUL. Used to determine the potion tooltip color of this effect.
            MobEffectCategory.BENEFICIAL,
            //The color of the effect particles in RGB format.
            0xffffff
    ));

    /** Rifty: atravessa blocos como andaimes e fica imune a dano fisico. */
    public static final Holder<MobEffect> SUBESPACO = EFFECTS.register("subespaco",
            () -> new Subespaco(MobEffectCategory.BENEFICIAL, 0x6a2fbf));

    // ---------- Efeitos das reliquias (lote Eterny..Voltrix) ----------
    private static Holder<MobEffect> nexus(String name, MobEffectCategory category, int color) {
        return EFFECTS.register(name, () -> new NexusEffect(category, color));
    }
    private static Holder<MobEffect> damaging(String name, int color, String damageType, int interval, float damage, boolean byLevel) {
        return EFFECTS.register(name, () -> new NexusEffect(MobEffectCategory.HARMFUL, color,
                org.dantesys.nexus.relic.RelicUtil.damageType(damageType), interval, damage, byLevel));
    }
    /** Eterny: sobrevive a uma morte. */
    public static final Holder<MobEffect> VIDA_EXTRA = nexus("vida_extra", MobEffectCategory.BENEFICIAL, 0xffd75e);
    /** Phoenixy: fogo, lava e queimadura curam. */
    public static final Holder<MobEffect> FOGO_DA_VIDA = nexus("fogo_da_vida", MobEffectCategory.BENEFICIAL, 0xff9a2e);
    /** Venossy/Voltrix/Grimy: nao anda, nao pula, nao ataca, nao usa itens. */
    public static final Holder<MobEffect> PARALISIA = EFFECTS.register("paralisia", () -> new NexusEffect(MobEffectCategory.HARMFUL, 0x8a8a8a)
            .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(Nexus.MODID, "paralisia_velocidade"), -1.0,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(Nexus.MODID, "paralisia_pulo"), -1.0,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.FLYING_SPEED,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(Nexus.MODID, "paralisia_voo"), -1.0,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    /** Draconix: 1 coracao/s, nao apaga com agua e ignora Resistencia ao Fogo. */
    public static final Holder<MobEffect> FOGO_DRACONICO = damaging("fogo_draconico", 0x9b3cff, "fogo_draconico", 20, 2.0F, false);
    /** Phoenixy: fogo dourado, 1 coracao/s, nao apaga com agua. */
    public static final Holder<MobEffect> FOGO_FENIX = damaging("fogo_fenix", 0xffc21a, "fogo_fenix", 20, 2.0F, false);
    /** Venossy: veneno que tambem afeta mortos-vivos e pode matar. */
    public static final Holder<MobEffect> VENENO_ASSASSINO = damaging("veneno_assassino", 0x4e9331, "veneno_assassino", 25, 1.0F, true);
    /** Venossy: meio coracao a cada 5s ate a morte; so o totem remove. */
    public static final Holder<MobEffect> TOXINA = damaging("toxina", 0x2d5a1e, "toxina", 100, 1.0F, false);
    /** Venossy (final): 1,5 coracao/s ate a morte, sem cura. */
    public static final Holder<MobEffect> TOXINA_AMPLIFICADA = damaging("toxina_amplificada", 0x1a3d10, "toxina", 20, 3.0F, false);

    public static void register(IEventBus bus){
        EFFECTS.register(bus);
    }
}
