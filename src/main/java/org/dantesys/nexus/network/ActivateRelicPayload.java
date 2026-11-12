package org.dantesys.nexus.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.dantesys.nexus.Nexus;

public record ActivateRelicPayload() implements CustomPacketPayload {
    public static final Type<ActivateRelicPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Nexus.MODID, "activate_relic"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActivateRelicPayload> STREAM_CODEC =
            StreamCodec.unit(new ActivateRelicPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
