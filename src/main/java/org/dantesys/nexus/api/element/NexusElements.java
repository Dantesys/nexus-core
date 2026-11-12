package org.dantesys.nexus.api.element;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.dantesys.nexus.Nexus;

/**
 * Registro de elementos ({@code nexus:element}). O core deixa vazio; os addons registram assim:
 * <pre>{@code
 * DeferredRegister<NexusElement> ELEMENTOS = DeferredRegister.create(NexusElements.REGISTRY_KEY, "meuaddon");
 * DeferredHolder<NexusElement, NexusElement> FOGO = ELEMENTOS.register("fogo", () -> new NexusElement(0xFFFF4400));
 * }</pre>
 * Status por elemento (afinidade, desbloqueio...) ficam a cargo de cada addon, com attachments próprios.
 */
@EventBusSubscriber(modid = Nexus.MODID)
public final class NexusElements {
    public static final ResourceKey<Registry<NexusElement>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(Nexus.MODID, "element"));

    public static final Registry<NexusElement> REGISTRY = new RegistryBuilder<>(REGISTRY_KEY)
            .sync(true)
            .create();

    private NexusElements() {
    }

    @SubscribeEvent
    static void registrar(NewRegistryEvent event) {
        event.register(REGISTRY);
    }
}
