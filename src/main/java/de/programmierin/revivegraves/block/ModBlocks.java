package de.programmierin.revivegraves.block;

import de.programmierin.revivegraves.ReviveGraves;
import de.programmierin.revivegraves.block.custom.GravestoneBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block GRAVESTONE = registerBlockWithoutBlockItem("gravestone",
            new GravestoneBlock(
                    AbstractBlock.Settings.create()
                            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(ReviveGraves.MOD_ID, "gravestone")))
                            .strength(-1.0f, 3600000.0f)
                            .dropsNothing()
                            .nonOpaque()
                            .luminance(state -> 6)
            )
    );

    private static Block registerBlockWithoutBlockItem(String name, Block block) {
        return Registry.register(Registries.BLOCK,
                Identifier.of(ReviveGraves.MOD_ID, name), block);
    }

    // Hidden BlockItem for advancement icon only — not in any creative tab, not craftable
    public static final Item GRAVESTONE_ITEM = Registry.register(Registries.ITEM,
            Identifier.of(ReviveGraves.MOD_ID, "gravestone"),
            new BlockItem(GRAVESTONE, new Item.Settings()
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ReviveGraves.MOD_ID, "gravestone")))));

    public static void registerModBlocks() {
        ReviveGraves.LOGGER.info("Registering Mod Blocks for {}", ReviveGraves.MOD_ID);
    }
}
