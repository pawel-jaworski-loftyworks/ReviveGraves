package de.programmierin.revivegraves.mixin;

import de.programmierin.revivegraves.ghost.GhostChickenState;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Intercepts outgoing PlayerListS2CPacket to hide ghost players from the tab list
 * while preserving their GameProfile data (needed for gravestone skull skin resolution).
 * Ghost entries are kept but modified to listed=false instead of being removed.
 */
@Mixin(ServerCommonNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Inject(method = "sendPacket", at = @At("HEAD"))
    private void revivegraves$unlistGhostFromTabList(Packet<?> packet, CallbackInfo ci) {
        if (!(((Object) this) instanceof ServerPlayNetworkHandler handler)) return;

        ServerPlayerEntity receiver = handler.getPlayer();
        if (receiver == null) return;

        MinecraftServer server = receiver.getEntityWorld().getServer();
        if (server == null) return;

        if (!(packet instanceof PlayerListS2CPacket listPacket)) return;

        GhostChickenState state = GhostChickenState.get(server);
        Set<UUID> ghostUuids = state.getGhostPlayerUuids();
        if (ghostUuids.isEmpty()) return;

        List<PlayerListS2CPacket.Entry> originalEntries =
                ((PlayerListS2CPacketAccessor) listPacket).revivegraves$getEntries();

        boolean modified = false;
        List<PlayerListS2CPacket.Entry> newEntries = new ArrayList<>(originalEntries.size());
        for (PlayerListS2CPacket.Entry entry : originalEntries) {
            if (ghostUuids.contains(entry.profileId()) && entry.listed()) {
                // Keep the entry but set listed=false so the GameProfile is preserved
                // on the client (needed for gravestone skull skin) without showing in tab
                newEntries.add(new PlayerListS2CPacket.Entry(
                        entry.profileId(), entry.profile(), false, entry.latency(),
                        entry.gameMode(), entry.displayName(), entry.showHat(),
                        entry.listOrder(), entry.chatSession()));
                modified = true;
            } else {
                newEntries.add(entry);
            }
        }

        if (modified) {
            ((PlayerListS2CPacketAccessor) listPacket).revivegraves$setEntries(newEntries);
        }
    }
}
