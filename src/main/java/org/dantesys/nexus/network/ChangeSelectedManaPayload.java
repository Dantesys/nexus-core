package org.dantesys.nexus.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.dantesys.nexus.Nexus;

public record ChangeSelectedManaPayload(boolean next) implements CustomPacketPayload {
    public static final Type<ChangeSelectedManaPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Nexus.MODID, "change_selected_mana")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ChangeSelectedManaPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    ChangeSelectedManaPayload::next,
                    ChangeSelectedManaPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
