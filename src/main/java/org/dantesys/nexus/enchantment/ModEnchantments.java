package org.dantesys.nexus.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.tags.ModTags;

public class ModEnchantments {


    public static final ResourceKey<Enchantment> DISTURBIO_MANA =
            ResourceKey.create(
                    Registries.ENCHANTMENT,
                    Identifier.fromNamespaceAndPath(Nexus.MODID, "disturbio_mana")
            );

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        var items = context.lookup(Registries.ITEM);
        var nucleo = items.getOrThrow(ModTags.Items.NUCLEO);
        var exclusiveSet = context.lookup(Registries.ENCHANTMENT)
                .getOrThrow(ModTags.Enchantments.NUCLEO_EXCLUSIVE);



        register(context, DISTURBIO_MANA, Enchantment.enchantment(
                Enchantment.definition(
                        nucleo,
                        nucleo,
                        5,
                        10,
                        Enchantment.dynamicCost(12, 4),
                        Enchantment.dynamicCost(30, 6),
                        2,
                        EquipmentSlotGroup.ANY
                )
        ).exclusiveWith(exclusiveSet));
    }

    private static void register(
            BootstrapContext<Enchantment> registry,
            ResourceKey<Enchantment> key,
            Enchantment.Builder builder
    ) {
        registry.register(key, builder.build(key.identifier()));
    }
}
