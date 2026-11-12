package org.dantesys.nexus.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.dantesys.nexus.Nexus;

public record UseManaPayload() implements CustomPacketPayload {
    public static final Type<UseManaPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Nexus.MODID, "use_mana")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, UseManaPayload> STREAM_CODEC =
            StreamCodec.unit(new UseManaPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
