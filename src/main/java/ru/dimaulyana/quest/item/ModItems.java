package ru.dimaulyana.quest.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import ru.dimaulyana.quest.DimaUlyanaQuestMod;

public class ModItems {
    // 1. Невыбрасываемый Компас Удачи с быстрой рулеткой ("Деп ресурсов")
    public static final Item FORTUNE_COMPASS = registerItem("fortune_compass",
        new FortuneCompassItem(new FabricItemSettings().maxCount(1).fireproof().rarity(Rarity.EPIC)));

    // 2. Шесть ступеней сюжетных кирок (от Земляной до Хроно-Звёздного Бура)
    public static final Item DIRT_PICKAXE = registerItem("dirt_pickaxe",
        new StoryPickaxeItem(ToolMaterials.STONE, 2, -2.6f, 1,
            new FabricItemSettings().maxDamage(320).rarity(Rarity.COMMON)));

    public static final Item BASALT_CRUSHER_PICKAXE = registerItem("basalt_crusher_pickaxe",
        new StoryPickaxeItem(ToolMaterials.IRON, 3, -2.5f, 2,
            new FabricItemSettings().maxDamage(750).rarity(Rarity.UNCOMMON)));

    public static final Item EMERALD_RESONANCE_PICKAXE = registerItem("emerald_resonance_pickaxe",
        new StoryPickaxeItem(ToolMaterials.DIAMOND, 4, -2.4f, 3,
            new FabricItemSettings().maxDamage(1450).rarity(Rarity.RARE)));

    public static final Item STORM_DIAMOND_PICKAXE = registerItem("storm_diamond_pickaxe",
        new StoryPickaxeItem(ToolMaterials.DIAMOND, 5, -2.3f, 4,
            new FabricItemSettings().maxDamage(2400).rarity(Rarity.RARE)));

    public static final Item MAGMA_COLOSSUS_PICKAXE = registerItem("magma_colossus_pickaxe",
        new StoryPickaxeItem(ToolMaterials.NETHERITE, 6, -2.2f, 5,
            new FabricItemSettings().maxDamage(4200).fireproof().rarity(Rarity.EPIC)));

    public static final Item CHRONO_STELLAR_DRILL = registerItem("chrono_stellar_drill",
        new StoryPickaxeItem(ToolMaterials.NETHERITE, 9, -2.0f, 6,
            new FabricItemSettings().maxDamage(9999).fireproof().rarity(Rarity.EPIC)));

    public static final ItemGroup QUEST_ITEM_GROUP = Registry.register(Registries.ITEM_GROUP,
        new Identifier(DimaUlyanaQuestMod.MOD_ID, "quest_group"),
        FabricItemGroup.builder()
            .displayName(Text.translatable("itemgroup.dima_ulyana_quest"))
            .icon(() -> new ItemStack(FORTUNE_COMPASS))
            .entries((displayContext, entries) -> {
                entries.add(FORTUNE_COMPASS);
                entries.add(DIRT_PICKAXE);
                entries.add(BASALT_CRUSHER_PICKAXE);
                entries.add(EMERALD_RESONANCE_PICKAXE);
                entries.add(STORM_DIAMOND_PICKAXE);
                entries.add(MAGMA_COLOSSUS_PICKAXE);
                entries.add(CHRONO_STELLAR_DRILL);
            }).build());

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(DimaUlyanaQuestMod.MOD_ID, name), item);
    }

    public static void registerModItems() {
        DimaUlyanaQuestMod.LOGGER.info("Зарегистрированы все сюжетные кирки и Компас Удачи!");
    }
}
