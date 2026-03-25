package de.programmierin.revivegraves.loot;

import de.programmierin.revivegraves.ReviveGraves;
import de.programmierin.revivegraves.item.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;

public class ModLootTableModifiers {
    // Drop chances (will be configurable in a future version)
    private static final float END_CITY_CHANCE = 0.01f;       // 1%
    private static final float OMINOUS_VAULT_CHANCE = 0.03f;  // 3%

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin()) return;

            if (LootTables.END_CITY_TREASURE_CHEST == key) {
                tableBuilder.pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(END_CITY_CHANCE))
                        .with(ItemEntry.builder(ModItems.REVIVE_TOKEN))
                );
                ReviveGraves.LOGGER.debug("Injected Revive Token into End City treasure loot table");
            }

            if (LootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_CHEST == key) {
                tableBuilder.pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(OMINOUS_VAULT_CHANCE))
                        .with(ItemEntry.builder(ModItems.REVIVE_TOKEN))
                );
                ReviveGraves.LOGGER.debug("Injected Revive Token into Ominous Trial Vault loot table");
            }
        });
    }
}
