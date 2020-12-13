package blockbreaker.bedrockbreaker;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Handles getting data configured in config.yml
 */
public class ConfigHelper {

    private final String hardnessKey = "hardness";

    private final String isHarvestableKey = "isHarvestable";
    private final String isBestToolKey = "isBestTool";
    private final String anyEnchantmentKey = "AnyEnchantment";
    private final String allEnchantmentKey = "AllEnchantment";
    private final String diggableBlocksKey = "DiggableBlocks";

    private final String itemsToDigWithKey = "itemsToDigWith";
    private final String blocksToDigKey = "blocksToDig";

    private HashMap<Material, ItemDigConfiguration> items;
    private HashMap<Material, Float> blocks;

    public ConfigHelper(FileConfiguration fileConfiguration) {
        setUpItems(fileConfiguration);
        setUpBlocks(fileConfiguration);
    }

    private void setUpItems(FileConfiguration fileConfiguration) {
        ConfigurationSection configurationSection = fileConfiguration.getConfigurationSection(itemsToDigWithKey);

        Set<String> itemsAsMap = configurationSection.getKeys(false);
        items = new HashMap<>(itemsAsMap.size());

        for(String itemName : itemsAsMap) {
            ConfigurationSection itemConfiguration = configurationSection.getConfigurationSection(itemName);;

            boolean isHarvestable = true;
            boolean isBestTool = true;

            Set<Material> breakableMaterials = new HashSet<>();

            Map<String, Integer> minEnchantmentMap = new HashMap<>();
            Map<String, Integer> exactEnchantmentMap = new HashMap<>();


            if(itemConfiguration.contains(isHarvestableKey)) {
                isHarvestable = itemConfiguration.getBoolean(isHarvestableKey);
            }

            if(itemConfiguration.contains(isBestToolKey)) {
                isBestTool = itemConfiguration.getBoolean(isBestToolKey);
            }

            if(itemConfiguration.contains(diggableBlocksKey)) {
                List<String> materialNames = itemConfiguration.getStringList(diggableBlocksKey);

                for(String materialName : materialNames) {
                    breakableMaterials.add(Material.getMaterial(materialName));
                }
            }


            if(itemConfiguration.contains(anyEnchantmentKey)) {
                ConfigurationSection anyEnchantmentSection = itemConfiguration.getConfigurationSection(anyEnchantmentKey);
                Set<String> enchantmentsWithChild = anyEnchantmentSection.getKeys(false);

                for(String name : enchantmentsWithChild) {
                    minEnchantmentMap.put(name, anyEnchantmentSection.getInt(name));
                }
            }

            if(itemConfiguration.contains(allEnchantmentKey)) {
                ConfigurationSection allEnchantmentSection = itemConfiguration.getConfigurationSection(allEnchantmentKey);
                Set<String> enchantmentsWithChild = allEnchantmentSection.getKeys(false);

                for(String name : enchantmentsWithChild) {
                    exactEnchantmentMap.put(name, allEnchantmentSection.getInt(name));
                }
            }

            items.put(Material.getMaterial(itemName), new ItemDigConfiguration(isHarvestable, isBestTool, breakableMaterials, exactEnchantmentMap, minEnchantmentMap));
        }
    }

    private void setUpBlocks(FileConfiguration fileConfiguration) {
        ConfigurationSection configurationSection = fileConfiguration.getConfigurationSection(blocksToDigKey);
        Set<String> blocksAsMap = fileConfiguration.getConfigurationSection(blocksToDigKey).getKeys(false);
        blocks = new HashMap<>(blocksAsMap.size());

        for(String materialName : blocksAsMap) {
            ConfigurationSection blockConfiguration = configurationSection.getConfigurationSection(materialName);

            Float hardness = null;

            if(blockConfiguration.contains(hardnessKey)) {
                hardness = (float) blockConfiguration.getDouble(hardnessKey);
            }

            blocks.put(Material.getMaterial(materialName), hardness);
        }
    }

    //ITEMS

    public boolean isHarvestable(ItemStack itemStack) {
        return items.get(itemStack.getType()).isHarvestable();
    }

    public boolean isBestTool(ItemStack itemStack) {
        return items.get(itemStack.getType()).isBestTool();
    }

    //BLOCKS

    public boolean hasCustomHardness(Block block) {
        return blocks.get(block.getType()) != null;
    }

    public float getCustomHardness(Block block) {
        return blocks.get(block.getType());
    }

    //ENCHANTS

    public boolean canBreakBlock(ItemStack item, Block blockToDig) {
        if(items.containsKey(item.getType()) && blocks.containsKey(blockToDig.getType())) {
            return items.get(item.getType()).canBreakBlock(item.getEnchantments(), blockToDig.getType());
        } else {
            return false;
        }
    }
}
