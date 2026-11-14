package org.dantesys.nexus.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.dantesys.nexus.Nexus;

/** Ataque com a Rifty sem alvo na mira (Passo Dimensional). */
public record RiftyStepPayload() implements CustomPacketPayload {
    public static final Type<RiftyStepPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Nexus.MODID, "rifty_step"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RiftyStepPayload> STREAM_CODEC = StreamCodec.unit(new RiftyStepPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
