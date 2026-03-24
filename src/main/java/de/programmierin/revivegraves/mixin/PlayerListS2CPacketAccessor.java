package de.programmierin.revivegraves.mixin;

import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Accessor mixin for PlayerListS2CPacket to allow modifying the entries list.
 * Used to filter ghost players from tab list packets.
 */
@Mixin(PlayerListS2CPacket.class)
public interface PlayerListS2CPacketAccessor {

    @Accessor("entries")
    List<PlayerListS2CPacket.Entry> revivegraves$getEntries();

    @Mutable
    @Accessor("entries")
    void revivegraves$setEntries(List<PlayerListS2CPacket.Entry> entries);
}
