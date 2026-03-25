package de.programmierin.revivegraves.mixin;

import de.programmierin.revivegraves.ghost.GhostChickenState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents ghost chicken players from picking up items entirely.
 * Without this, items get picked up for one tick then cleared — destroying them.
 */
@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void revivegraves$preventGhostPickup(PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (serverPlayer.getEntityWorld() instanceof ServerWorld serverWorld) {
            GhostChickenState state = GhostChickenState.get(serverWorld.getServer());
            if (state.isGhost(serverPlayer.getUuid())) {
                ci.cancel();
            }
        }
    }
}
