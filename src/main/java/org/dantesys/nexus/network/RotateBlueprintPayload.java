package org.dantesys.nexus.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.dantesys.nexus.Nexus;

/** Gira a planta da Krefty que esta na outra mao (90 graus por vez). */
public record RotateBlueprintPayload() implements CustomPacketPayload {
    public static final Type<RotateBlueprintPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Nexus.MODID, "rotate_blueprint"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RotateBlueprintPayload> STREAM_CODEC =
            StreamCodec.unit(new RotateBlueprintPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
