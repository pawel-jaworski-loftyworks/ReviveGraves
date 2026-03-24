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
 * Intercepts outgoing packets on ServerCommonNetworkHandler to filter ghost players
 * from PlayerListS2CPacket (tab list) entries. This prevents ghost-chicken players
 * from appearing in other players' tab lists.
 */
@Mixin(ServerCommonNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void revivegraves$filterGhostFromTabList(Packet<?> packet, CallbackInfo ci) {
        // Only apply to ServerPlayNetworkHandler instances (not config/login handlers)
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

        List<PlayerListS2CPacket.Entry> filteredEntries = new ArrayList<>();
        for (PlayerListS2CPacket.Entry entry : originalEntries) {
            if (!ghostUuids.contains(entry.profileId())) {
                filteredEntries.add(entry);
            }
        }

        // If all entries were ghosts, cancel the packet entirely
        if (filteredEntries.isEmpty()) {
            ci.cancel();
            return;
        }

        // If some entries were filtered, replace the list
        if (filteredEntries.size() != originalEntries.size()) {
            ((PlayerListS2CPacketAccessor) listPacket).revivegraves$setEntries(filteredEntries);
        }
    }
}
