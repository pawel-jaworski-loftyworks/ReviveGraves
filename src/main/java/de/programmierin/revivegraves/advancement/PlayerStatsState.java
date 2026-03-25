package de.programmierin.revivegraves.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.*;

/**
 * Persistent state tracking per-player advancement stats:
 * death count, revive count, and cumulative ghost ticks.
 */
public class PlayerStatsState extends PersistentState {

	private final Map<UUID, PlayerStats> playerStats;

	public record PlayerStats(int deathCount, int reviveCount, long ghostTicks) {
		public static final PlayerStats EMPTY = new PlayerStats(0, 0, 0);

		public static final Codec<PlayerStats> CODEC = RecordCodecBuilder.create(instance ->
				instance.group(
						Codec.INT.fieldOf("deathCount").forGetter(PlayerStats::deathCount),
						Codec.INT.fieldOf("reviveCount").forGetter(PlayerStats::reviveCount),
						Codec.LONG.fieldOf("ghostTicks").forGetter(PlayerStats::ghostTicks)
				).apply(instance, PlayerStats::new)
		);
	}

	/**
	 * Internal record for codec serialization of a single player entry.
	 */
	private record StatsEntry(String uuid, PlayerStats stats) {
		public static final Codec<StatsEntry> CODEC = RecordCodecBuilder.create(instance ->
				instance.group(
						Codec.STRING.fieldOf("uuid").forGetter(StatsEntry::uuid),
						PlayerStats.CODEC.fieldOf("stats").forGetter(StatsEntry::stats)
				).apply(instance, StatsEntry::new)
		);
	}

	public static final Codec<PlayerStatsState> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
					StatsEntry.CODEC.listOf().optionalFieldOf("player_stats", List.of())
							.forGetter(PlayerStatsState::toEntryList)
			).apply(instance, PlayerStatsState::fromEntryList)
	);

	public static final PersistentStateType<PlayerStatsState> STATE_TYPE = new PersistentStateType<>(
			"revivegraves_stats",
			PlayerStatsState::new,
			CODEC,
			null
	);

	public PlayerStatsState() {
		this.playerStats = new HashMap<>();
	}

	private PlayerStatsState(Map<UUID, PlayerStats> stats) {
		this.playerStats = new HashMap<>(stats);
	}

	private List<StatsEntry> toEntryList() {
		List<StatsEntry> entries = new ArrayList<>();
		for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
			entries.add(new StatsEntry(entry.getKey().toString(), entry.getValue()));
		}
		return entries;
	}

	private static PlayerStatsState fromEntryList(List<StatsEntry> entries) {
		Map<UUID, PlayerStats> stats = new HashMap<>();
		for (StatsEntry entry : entries) {
			try {
				UUID uuid = UUID.fromString(entry.uuid());
				stats.put(uuid, entry.stats());
			} catch (IllegalArgumentException e) {
				// Skip invalid UUIDs
			}
		}
		return new PlayerStatsState(stats);
	}

	public static PlayerStatsState get(MinecraftServer server) {
		ServerWorld overworld = server.getOverworld();
		return overworld.getPersistentStateManager().getOrCreate(STATE_TYPE);
	}

	public PlayerStats getStats(UUID uuid) {
		return playerStats.getOrDefault(uuid, PlayerStats.EMPTY);
	}

	public int incrementDeathCount(UUID uuid) {
		PlayerStats current = getStats(uuid);
		PlayerStats updated = new PlayerStats(current.deathCount() + 1, current.reviveCount(), current.ghostTicks());
		playerStats.put(uuid, updated);
		markDirty();
		return updated.deathCount();
	}

	public int incrementReviveCount(UUID uuid) {
		PlayerStats current = getStats(uuid);
		PlayerStats updated = new PlayerStats(current.deathCount(), current.reviveCount() + 1, current.ghostTicks());
		playerStats.put(uuid, updated);
		markDirty();
		return updated.reviveCount();
	}

	public long addGhostTicks(UUID uuid, long ticks) {
		PlayerStats current = getStats(uuid);
		PlayerStats updated = new PlayerStats(current.deathCount(), current.reviveCount(), current.ghostTicks() + ticks);
		playerStats.put(uuid, updated);
		markDirty();
		return updated.ghostTicks();
	}
}
