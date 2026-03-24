package de.programmierin.revivegraves.mixin;

import de.programmierin.revivegraves.ghost.GhostChickenState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

/**
 * Intercepts entity tracking packets to disguise ghost players as chickens.
 * When a non-ghost player starts tracking a ghost player, this mixin replaces
 * the player spawn packet with a chicken spawn packet and sends empty tracker data.
 */
@Mixin(EntityTrackerEntry.class)
public abstract class EntityTrackerEntryMixin {

    @Shadow @Final private Entity entity;

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
}
