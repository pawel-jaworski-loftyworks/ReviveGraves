package de.programmierin.revivegraves.ghost;

/**
 * Duck-type interface injected into PlayerEntityRenderState via mixin.
 * Used to pass the ghost chicken flag between mixins without cross-mixin casting.
 */
public interface GhostChickenRenderState {
    boolean revivegraves$isGhostChicken();
    void revivegraves$setGhostChicken(boolean value);
}
