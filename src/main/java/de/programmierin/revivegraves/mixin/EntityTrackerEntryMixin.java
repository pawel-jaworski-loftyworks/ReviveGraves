package de.programmierin.revivegraves.mixin;

import de.programmierin.revivegraves.ghost.GhostChickenState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Intercepts entity tracking to disguise ghost players as chickens.
 * - sendPackets: replaces player spawn with chicken spawn for other clients
 * - syncEntityData: prevents player DataTracker fields from reaching clients
 *   that have a ChickenEntity (would cause type mismatch crash)
 */
@Mixin(EntityTrackerEntry.class)
public abstract class EntityTrackerEntryMixin {

    @Shadow @Final private Entity entity;
    @Shadow @Nullable private List<DataTracker.SerializedEntry<?>> changedEntries;

    @Inject(method = "sendPackets", at = @At("HEAD"), cancellable = true)
    private void revivegraves$disguiseGhostChicken(
            ServerPlayerEntity player,
            Consumer<Packet<ClientPlayPacketListener>> sender,
            CallbackInfo ci
    ) {
        if (!(entity instanceof ServerPlayerEntity ghostPlayer)) return;
        if (!(ghostPlayer.getEntityWorld() instanceof ServerWorld serverWorld)) return;

        GhostChickenState state = GhostChickenState.get(serverWorld.getServer());
        if (!state.isGhost(ghostPlayer.getUuid())) return;

        // Don't disguise packets sent to the ghost player themselves
        if (player.getUuid().equals(ghostPlayer.getUuid())) return;

        ci.cancel();

        // Send a chicken spawn packet instead of a player spawn packet
        EntitySpawnS2CPacket spawnPacket = new EntitySpawnS2CPacket(
                ghostPlayer.getId(), ghostPlayer.getUuid(),
                ghostPlayer.getX(), ghostPlayer.getY(), ghostPlayer.getZ(),
                ghostPlayer.getPitch(), ghostPlayer.getYaw(),
                EntityType.CHICKEN, 0,
                ghostPlayer.getVelocity(), (double) ghostPlayer.getHeadYaw()
        );
        sender.accept(spawnPacket);

        // Send empty tracker data so the client uses chicken defaults
        sender.accept(new EntityTrackerUpdateS2CPacket(ghostPlayer.getId(), List.of()));
    }

    /**
     * Intercepts dirty DataTracker sync to prevent player-specific fields from being
     * broadcast to clients that have a ChickenEntity. Sends tracker data only to the
     * ghost player's own client. This prevents the field type mismatch crash
     * (e.g., PlayerEntity field 16 Byte vs ChickenEntity field 16 Boolean).
     */
    @Inject(method = "syncEntityData", at = @At("HEAD"), cancellable = true)
    private void revivegraves$suppressGhostDataSync(CallbackInfo ci) {
        if (!(entity instanceof ServerPlayerEntity ghostPlayer)) return;
        if (!(ghostPlayer.getEntityWorld() instanceof ServerWorld serverWorld)) return;

        GhostChickenState state = GhostChickenState.get(serverWorld.getServer());
        if (!state.isGhost(ghostPlayer.getUuid())) return;

        ci.cancel();

        // Still sync dirty DataTracker entries to the ghost's own client
        // Order matters: getChangedEntries() must be called before getDirtyEntries()
        // because getDirtyEntries() resets the dirty flags
        DataTracker dataTracker = entity.getDataTracker();
        this.changedEntries = dataTracker.getChangedEntries();
        List<DataTracker.SerializedEntry<?>> dirtyEntries = dataTracker.getDirtyEntries();
        if (dirtyEntries != null) {
            ghostPlayer.networkHandler.sendPacket(
                    new EntityTrackerUpdateS2CPacket(entity.getId(), dirtyEntries));
        }

        // Also sync attributes only to self (player attributes differ from chicken)
        Set<EntityAttributeInstance> tracked = ((LivingEntity) entity).getAttributes().getTracked();
        if (!tracked.isEmpty()) {
            ghostPlayer.networkHandler.sendPacket(
                    new EntityAttributesS2CPacket(entity.getId(), tracked));
            tracked.clear();
        }
    }
}
