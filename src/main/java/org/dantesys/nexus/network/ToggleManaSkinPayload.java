package org.dantesys.nexus.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.dantesys.nexus.Nexus;

public record ToggleManaSkinPayload() implements CustomPacketPayload {
    public static final Type<ToggleManaSkinPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Nexus.MODID, "toggle_mana_skin")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleManaSkinPayload> STREAM_CODEC =
            StreamCodec.unit(new ToggleManaSkinPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
