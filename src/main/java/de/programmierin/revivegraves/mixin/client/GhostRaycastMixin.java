package de.programmierin.revivegraves.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Modifies the standingEyeHeight field on the local player when they are a ghost chicken.
 * This is the single source of truth for eye height — both the Camera (for rendering)
 * and getCameraPosVec (for raycasting) read from this field.
 *
 * Targets Entity because standingEyeHeight is a private field on Entity.
 * Only applies to the local client player who is a ghost.
 */
@Mixin(Entity.class)
public abstract class GhostRaycastMixin {

    private static final float CHICKEN_EYE_HEIGHT = 0.35f;

    @Shadow
    private float standingEyeHeight;

    @Unique
    private boolean revivegraves$wasGhost = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void revivegraves$lowerGhostEyeHeight(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        if ((Object) this != client.player) return;

        boolean isGhost = client.interactionManager.getCurrentGameMode() == GameMode.ADVENTURE
                && client.player.hasStatusEffect(StatusEffects.INVISIBILITY);

        if (isGhost) {
            this.standingEyeHeight = CHICKEN_EYE_HEIGHT;
            this.revivegraves$wasGhost = true;
        } else if (this.revivegraves$wasGhost) {
            this.revivegraves$wasGhost = false;
            ((Entity) (Object) this).calculateDimensions();
        }
    }
}
