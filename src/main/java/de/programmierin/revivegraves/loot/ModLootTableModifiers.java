package de.programmierin.revivegraves.loot;

import de.programmierin.revivegraves.ReviveGraves;
import de.programmierin.revivegraves.config.ModConfig;
import de.programmierin.revivegraves.item.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;

public class ModLootTableModifiers {
	public static void register() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (!source.isBuiltin()) return;
			if (!ModConfig.INSTANCE.loot.enabled) return;

			if (LootTables.END_CITY_TREASURE_CHEST == key) {
				tableBuilder.pool(LootPool.builder()
						.rolls(ConstantLootNumberProvider.create(1))
						.conditionally(RandomChanceLootCondition.builder(ModConfig.INSTANCE.loot.endCityChance))
						.with(ItemEntry.builder(ModItems.REVIVE_TOKEN))
				);
				ReviveGraves.LOGGER.debug("Injected Revive Token into End City treasure loot table (chance: {})",
						ModConfig.INSTANCE.loot.endCityChance);
			}

			if (LootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_CHEST == key) {
				tableBuilder.pool(LootPool.builder()
						.rolls(ConstantLootNumberProvider.create(1))
						.conditionally(RandomChanceLootCondition.builder(ModConfig.INSTANCE.loot.ominousVaultChance))
						.with(ItemEntry.builder(ModItems.REVIVE_TOKEN))
				);
				ReviveGraves.LOGGER.debug("Injected Revive Token into Ominous Trial Vault loot table (chance: {})",
						ModConfig.INSTANCE.loot.ominousVaultChance);
			}
		});
	}
}
