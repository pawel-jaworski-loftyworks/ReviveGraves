package de.programmierin.revivegraves.mixin.client;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Adds a ghost chicken flag to the player render state.
 * When set, the player will be rendered as a chicken in F5 mode.
 */
@Mixin(PlayerEntityRenderState.class)
public class PlayerEntityRenderStateMixin {
    @Unique
    public boolean revivegraves$isGhostChicken = false;
}
