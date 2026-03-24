package de.programmierin.revivegraves.ghost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.server.MinecraftServer;
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
    private static final double GHOST_SPEED_MODIFIER = 0.03;

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
        List<GhostEntry> entries = new ArrayList<>();
        for (UUID uuid : ghostPlayers) {
            GraveLocation loc = gravestoneLocations.get(uuid);
            entries.add(new GhostEntry(uuid.toString(), Optional.ofNullable(loc)));
        }
        return entries;
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
            speedAttr.addTemporaryModifier(new EntityAttributeModifier(
                    SPEED_MODIFIER_ID, GHOST_SPEED_MODIFIER, EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }

        // Remove ghost from all clients' tab lists
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server != null) {
            PlayerRemoveS2CPacket removePacket = new PlayerRemoveS2CPacket(List.of(player.getUuid()));
            for (ServerPlayerEntity onlinePlayer : server.getPlayerManager().getPlayerList()) {
                onlinePlayer.networkHandler.sendPacket(removePacket);
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

        // Re-add player to all clients' tab lists
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server != null) {
            PlayerListS2CPacket addPacket = PlayerListS2CPacket.entryFromPlayer(List.of(player));
            for (ServerPlayerEntity onlinePlayer : server.getPlayerManager().getPlayerList()) {
                onlinePlayer.networkHandler.sendPacket(addPacket);
            }
        }
    }

    public static Identifier getSpeedModifierId() {
        return SPEED_MODIFIER_ID;
    }
}
