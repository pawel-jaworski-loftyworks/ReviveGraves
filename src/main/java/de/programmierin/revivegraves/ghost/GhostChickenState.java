package de.programmierin.revivegraves.ghost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import de.programmierin.revivegraves.config.ModConfig;

import java.util.EnumSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.*;

/**
 * Persistent state manager for ghost chicken players.
 * Tracks which players are ghosts and stores their gravestone locations
 * for cross-dimension revive support.
 */
public class GhostChickenState extends PersistentState {

    private static final Identifier SPEED_MODIFIER_ID = Identifier.of("revivegraves", "ghost_chicken_speed");

    private final Map<UUID, GraveLocation> gravestoneLocations;
    private final Set<UUID> ghostPlayers;

    /**
     * Stores gravestone dimension + position for cross-dimension support.
     */
    public record GraveLocation(String dimension, BlockPos pos) {
        public static final Codec<GraveLocation> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("dimension").forGetter(GraveLocation::dimension),
                        BlockPos.CODEC.fieldOf("pos").forGetter(GraveLocation::pos)
                ).apply(instance, GraveLocation::new)
        );

        public ServerWorld resolveWorld(MinecraftServer server) {
            RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(dimension()));
            return server.getWorld(dimKey);
        }
    }

    /**
     * Internal record for codec serialization of a single ghost entry.
     */
    private record GhostEntry(String uuid, Optional<GraveLocation> location) {
        public static final Codec<GhostEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("uuid").forGetter(GhostEntry::uuid),
                        GraveLocation.CODEC.optionalFieldOf("location").forGetter(GhostEntry::location)
                ).apply(instance, GhostEntry::new)
        );
    }

    public static final Codec<GhostChickenState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    GhostEntry.CODEC.listOf().optionalFieldOf("ghost_players", List.of())
                            .forGetter(GhostChickenState::toEntryList)
            ).apply(instance, GhostChickenState::fromEntryList)
    );

    public static final PersistentStateType<GhostChickenState> STATE_TYPE = new PersistentStateType<>(
            "revivegraves_ghost",
            GhostChickenState::new,
            CODEC,
            null
    );

    public GhostChickenState() {
        this.ghostPlayers = new HashSet<>();
        this.gravestoneLocations = new HashMap<>();
    }

    private GhostChickenState(Map<UUID, GraveLocation> locations, Set<UUID> ghosts) {
        this.gravestoneLocations = new HashMap<>(locations);
        this.ghostPlayers = new HashSet<>(ghosts);
    }

    /**
     * Converts internal state to a list of entries for codec serialization.
     */
    private List<GhostEntry> toEntryList() {
        return ghostPlayers.stream()
                .map(uuid -> new GhostEntry(uuid.toString(), Optional.ofNullable(gravestoneLocations.get(uuid))))
                .toList();
    }

    /**
     * Reconstructs state from a list of entries (codec deserialization).
     */
    private static GhostChickenState fromEntryList(List<GhostEntry> entries) {
        Map<UUID, GraveLocation> locations = new HashMap<>();
        Set<UUID> ghosts = new HashSet<>();
        for (GhostEntry entry : entries) {
            try {
                UUID uuid = UUID.fromString(entry.uuid());
                ghosts.add(uuid);
                entry.location().ifPresent(loc -> locations.put(uuid, loc));
            } catch (IllegalArgumentException e) {
                // Skip invalid UUIDs
            }
        }
        GhostChickenState state = new GhostChickenState(locations, ghosts);
        return state;
    }

    /**
     * Retrieves (or creates) the ghost chicken state from the overworld's persistent state manager.
     */
    public static GhostChickenState get(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        return overworld.getPersistentStateManager().getOrCreate(STATE_TYPE);
    }

    public boolean isGhost(UUID uuid) {
        return ghostPlayers.contains(uuid);
    }

    public GraveLocation getGravestoneLocation(UUID uuid) {
        return gravestoneLocations.get(uuid);
    }

    public void addGhost(UUID uuid, String dimension, BlockPos gravestonePos) {
        ghostPlayers.add(uuid);
        gravestoneLocations.put(uuid, new GraveLocation(dimension, gravestonePos));
        markDirty();
    }

    public void removeGhost(UUID uuid) {
        ghostPlayers.remove(uuid);
        gravestoneLocations.remove(uuid);
        markDirty();
    }

    /**
     * Marks the gravestone as expired (removes location reference but keeps ghost state).
     * The player remains a ghost until revived by other means or the server handles cleanup.
     */
    public void setGravestoneExpired(UUID uuid) {
        gravestoneLocations.remove(uuid);
        markDirty();
    }

    public Set<UUID> getGhostPlayerUuids() {
        return Collections.unmodifiableSet(ghostPlayers);
    }

    /**
     * Applies ghost state to a player: Adventure mode, invulnerable, invisible, slow speed.
     */
    public static void applyGhostState(ServerPlayerEntity player) {
        player.changeGameMode(GameMode.ADVENTURE);
        player.setInvulnerable(true);

        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.INVISIBILITY,
                StatusEffectInstance.INFINITE,
                0, true, false, false
        ));

        EntityAttributeInstance speedAttr = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER_ID);
            double basePlayerSpeed = 0.1;
            double modifier = (ModConfig.INSTANCE.ghost.speedMultiplier * basePlayerSpeed) - basePlayerSpeed;
            speedAttr.addTemporaryModifier(new EntityAttributeModifier(
                    SPEED_MODIFIER_ID, modifier, EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }

        // Hide ghost from tab list but keep profile/skin data in playerListEntries
        // (needed for gravestone skull rendering on other clients).
        // DON'T remove from player list — just set listed=false via UPDATE_LISTED.
        // This keeps the GameProfile available for skin resolution.
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server != null) {
            // Create UPDATE_LISTED packet with listed=false
            PlayerListS2CPacket unlistPacket = new PlayerListS2CPacket(
                    EnumSet.of(PlayerListS2CPacket.Action.UPDATE_LISTED), List.of(player));
            // entryFromPlayer creates entries with listed=true, override to false
            var accessor = (de.programmierin.revivegraves.mixin.PlayerListS2CPacketAccessor) (Object) unlistPacket;
            List<PlayerListS2CPacket.Entry> unlisted = accessor.revivegraves$getEntries().stream()
                    .map(e -> new PlayerListS2CPacket.Entry(
                            e.profileId(), e.profile(), false, e.latency(),
                            e.gameMode(), e.displayName(), e.showHat(), e.listOrder(), e.chatSession()))
                    .toList();
            accessor.revivegraves$setEntries(unlisted);

            for (ServerPlayerEntity onlinePlayer : server.getPlayerManager().getPlayerList()) {
                onlinePlayer.networkHandler.sendPacket(unlistPacket);
            }
        }
    }

    /**
     * Removes ghost state from a player: removes invulnerability, invisibility, and speed modifier.
     */
    public static void removeGhostState(ServerPlayerEntity player) {
        player.setInvulnerable(false);
        player.removeStatusEffect(StatusEffects.INVISIBILITY);

        EntityAttributeInstance speedAttr = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER_ID);
        }

        // Re-list player in tab (set listed=true)
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server != null) {
            PlayerListS2CPacket relistPacket = new PlayerListS2CPacket(
                    EnumSet.of(PlayerListS2CPacket.Action.UPDATE_LISTED), List.of(player));
            // Entry from player will have listed=true by default — which is what we want
            for (ServerPlayerEntity onlinePlayer : server.getPlayerManager().getPlayerList()) {
                onlinePlayer.networkHandler.sendPacket(relistPacket);
            }
        }
    }

}
