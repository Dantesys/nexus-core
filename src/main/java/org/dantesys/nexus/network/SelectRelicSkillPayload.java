package org.dantesys.nexus.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.dantesys.nexus.Nexus;

public record SelectRelicSkillPayload(boolean next) implements CustomPacketPayload {
    public static final Type<SelectRelicSkillPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Nexus.MODID, "select_relic_skill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectRelicSkillPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public SelectRelicSkillPayload decode(RegistryFriendlyByteBuf buf) {
            return new SelectRelicSkillPayload(buf.readBoolean());
        }
        @Override public void encode(RegistryFriendlyByteBuf buf, SelectRelicSkillPayload value) {
            buf.writeBoolean(value.next());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
