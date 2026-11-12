package org.dantesys.nexus.data;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.magic.MagicStatus;
import org.dantesys.nexus.relic.RelicCrystalProgress;

import java.util.function.Supplier;

public class ModAttachmentType {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Nexus.MODID);
    public static final Supplier<AttachmentType<org.dantesys.nexus.relic.RelicMastery>> RELIC_MASTERY = ATTACHMENT_TYPES.register(
            "relic_mastery", () -> AttachmentType.serializable(org.dantesys.nexus.relic.RelicMastery::new)
                    .copyOnDeath().sync(org.dantesys.nexus.relic.RelicMastery.STREAM_CODEC).build());
    public static final Supplier<AttachmentType<MagicStatus>> MAGIC_STATS = ATTACHMENT_TYPES.register(
            "magic_status", () -> AttachmentType.serializable(MagicStatus::new)
                    .copyOnDeath()
                    .sync(MagicStatus.STREAM_CODEC)
                    .build()
    );
    public static final Supplier<AttachmentType<RelicCrystalProgress>> RELIC_CRYSTAL_PROGRESS = ATTACHMENT_TYPES.register(
            "relic_crystal_progress", () -> AttachmentType.serializable(RelicCrystalProgress::new)
                    .copyOnDeath()
                    .build()
    );
    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }
}
