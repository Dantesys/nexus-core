package org.dantesys.nexus.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.magic.ManaItemReceiver;
import org.dantesys.nexus.magic.ManaReceiver;
import org.dantesys.nexus.magic.MagicStatus;
import org.dantesys.nexus.relic.RelicItem;

public class ModNetwork {
    private static final String VERSION = "6";
    private static final double MANA_TRANSFER_RANGE = 10.0D;
    private static final int BASE_MANA_PER_SECOND = 50;
    private static final int MAX_RESONANCE = 1000;

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(SelectRelicSkillPayload.TYPE, SelectRelicSkillPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    ItemStack stack = RelicItem.controlledRelic(player);
                    if (stack.getItem() instanceof RelicItem relic) relic.selectSkill(player, stack, payload.next());
                });
        registrar.playToServer(
                ToggleManaSkinPayload.TYPE,
                ToggleManaSkinPayload.STREAM_CODEC,
                ModNetwork::handleToggleManaSkin
        );
        registrar.playToServer(
                UseManaPayload.TYPE,
                UseManaPayload.STREAM_CODEC,
                ModNetwork::handleUseMana
        );
        registrar.playToServer(RotateBlueprintPayload.TYPE, RotateBlueprintPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    ItemStack off = player.getOffhandItem();
                    if (!(off.getItem() instanceof org.dantesys.nexus.items.PlantaItem)) return;
                    org.dantesys.nexus.items.PlantaItem.rotate(off);
                    int degrees = org.dantesys.nexus.items.PlantaItem.rotation(off).ordinal() * 90;
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.nexus.modnetwork.planta_girada", degrees), true);
                });
        registrar.playToServer(RiftyStepPayload.TYPE, RiftyStepPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    ItemStack stack = player.getMainHandItem();
                    if (stack.getItem() instanceof org.dantesys.nexus.relic.RiftyItem relic) relic.triggerPassive(player, stack, 0);
                });
        registrar.playToServer(
                ActivateRelicPayload.TYPE,
                ActivateRelicPayload.STREAM_CODEC,
                ModNetwork::handleActivateRelic
        );
    }

    private static void handleActivateRelic(ActivateRelicPayload payload,
            net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        ItemStack stack = RelicItem.controlledRelic(player);
        if (stack.getItem() instanceof RelicItem relic) relic.activate(player, stack);
    }

    private static void handleToggleManaSkin(ToggleManaSkinPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
        status.setManaSkinActive(!status.isManaSkinActive() && status.getMana() > 0);
        player.syncData(ModAttachmentType.MAGIC_STATS);
    }

    private static void handleUseMana(UseManaPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
        int available = status.getMana();
        long gameTime = player.level().getGameTime();
        if (!status.isCreated() || available <= 0 || !status.startManaUseTick(gameTime)) {
            return;
        }

        int resonance = Mth.clamp(status.getRessonancia(), 0, MAX_RESONANCE);
        boolean carregandoItem = player.getMainHandItem().getItem() instanceof ManaItemReceiver;

        int manaPerSecond = BASE_MANA_PER_SECOND
                + BASE_MANA_PER_SECOND * resonance / MAX_RESONANCE;
        int tickInSecond = (int) Math.floorMod(gameTime, 20L);
        int transferLimit = manaPerSecond * (tickInSecond + 1) / 20
                - manaPerSecond * tickInSecond / 20;
        available = Math.min(available, transferLimit);

        // Segurando um item que aceita mana, carrega o item; senão, transfere para o bloco mirado.
        int accepted = carregandoItem
                ? chargeHeldItem(player, available)
                : transferNonElementalMana(player, status, available, gameTime);

        accepted = Math.clamp(accepted, 0, available);
        if (accepted > 0 && status.useMana(accepted)) {
            player.syncData(ModAttachmentType.MAGIC_STATS);
        }
    }

    private static int transferNonElementalMana(
            ServerPlayer player,
            MagicStatus status,
            int available,
            long gameTime
    ) {
        HitResult hit = player.pick(MANA_TRANSFER_RANGE, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return 0;
        }

        if (player.level().getBlockEntity(blockHit.getBlockPos()) instanceof ManaReceiver receiver) {
            int accepted = receiver.receiveNonElementalMana(available);
            if (accepted > 0) {
                renderManaBeam(player, status, blockHit.getLocation(), gameTime);
            }
            return accepted;
        }
        return 0;
    }

    private static void renderManaBeam(
            ServerPlayer player,
            MagicStatus status,
            Vec3 destination,
            long gameTime
    ) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 origin = player.getEyePosition().add(player.getLookAngle().scale(0.25D));
        Vec3 beam = destination.subtract(origin);
        double length = beam.length();
        if (length <= 0.0D) {
            return;
        }
        Vec3 direction = beam.scale(1.0D / length);

        for (double distance = 0.0D; distance <= length; distance += 0.25D) {
            Vec3 particlePos = origin.add(direction.scale(distance));
            level.sendParticles(
                    ParticleTypes.ENCHANT,
                    particlePos.x,
                    particlePos.y,
                    particlePos.z,
                    1,
                    0.02D,
                    0.02D,
                    0.02D,
                    0.0D
            );
        }

        if (!status.startManaBeamImpact(gameTime)) {
            return;
        }

        int affinity = Mth.clamp(status.getAfinidade(),
                0,
                MAX_RESONANCE
        );
        double pushDistance = 1.0D + affinity / (double) MAX_RESONANCE;
        float damage = 1.0F + affinity / (float) MAX_RESONANCE;
        AABB beamArea = new AABB(origin, destination).inflate(0.6D);

        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                beamArea,
                entity -> entity != player && entity.isAlive()
        )) {
            Vec3 relative = target.getBoundingBox().getCenter().subtract(origin);
            double alongBeam = Mth.clamp(relative.dot(direction), 0.0D, length);
            Vec3 closestPoint = origin.add(direction.scale(alongBeam));
            if (target.getBoundingBox().getCenter().distanceToSqr(closestPoint) > 0.75D * 0.75D) {
                continue;
            }

            target.hurt(level.damageSources().playerAttack(player), damage);
            target.push(
                    direction.x * pushDistance,
                    Math.max(0.15D, direction.y * pushDistance),
                    direction.z * pushDistance
            );
            target.needsSync = true;
        }
    }

    private static int chargeHeldItem(ServerPlayer player, int available) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.isEmpty() && stack.getItem() instanceof ManaItemReceiver receiver) {
            int accepted = receiver.receiveMana(stack, null, available);
            if (accepted > 0) {
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            }
            return accepted;
        }
        return 0;
    }
}
