package de.programmierin.revivegraves.mixin;

import de.programmierin.revivegraves.ghost.GhostChickenState;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents ghost chicken players from picking up or attracting XP orbs.
 * Uses isInvisible() as a client-compatible check since ghost players are always invisible.
 */
@Mixin(ExperienceOrbEntity.class)
public class ExperienceOrbEntityMixin {

    @Shadow
    @Nullable
    private PlayerEntity target;

    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void revivegraves$preventGhostXpPickup(PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (serverPlayer.getEntityWorld() instanceof ServerWorld serverWorld) {
            GhostChickenState state = GhostChickenState.get(serverWorld.getServer());
            if (state.isGhost(serverPlayer.getUuid())) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "moveTowardsPlayer", at = @At("HEAD"), cancellable = true)
    private void revivegraves$preventGhostAttraction(CallbackInfo ci) {
        // Clear existing target if it's a ghost (invisible) player
        if (this.target != null && this.target.isInvisible()) {
            this.target = null;
        }

        // If no target, check if closest player is invisible (ghost) — skip attraction entirely
        if (this.target == null) {
            ExperienceOrbEntity self = (ExperienceOrbEntity) (Object) this;
            PlayerEntity closest = self.getEntityWorld().getClosestPlayer(self, 8.0);
            if (closest != null && closest.isInvisible()) {
                ci.cancel();
            }
        }
    }
}
