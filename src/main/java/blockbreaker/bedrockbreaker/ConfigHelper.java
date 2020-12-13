package blockbreaker.bedrockbreaker;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Handles getting data from the configuration file config.yml
 */
public class ConfigHelper {

    private final String hardnessKey = "hardness";

    private final String isHarvestableKey = "isHarvestable";
    private final String isBestToolKey = "isBestTool";
    private final String minEnchantmentKey = "MinEnchantment";
    private final String exactEnchantmentKey = "ExactEnchantment";
    private final String diggableBlocksKey = "DiggableBlocks";

    private final String itemsToDigWithKey = "itemsToDigWith";
    private final String blocksToDigKey = "blocksToDig";


    //private final FileConfiguration fileConfiguration;
    private HashMap<Material, ItemDigConfiguration> items;
    private HashMap<Material, Float> blocks;

    public ConfigHelper(FileConfiguration fileConfiguration) {
        setUpItems(fileConfiguration);
        setUpBlocks(fileConfiguration);
    }

    private void setUpItems(FileConfiguration fileConfiguration) {
        //new EnchantmentWrapper("fortune")
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


            if(itemConfiguration.contains(minEnchantmentKey)) {
                ConfigurationSection minEnchantmentSection = itemConfiguration.getConfigurationSection(minEnchantmentKey);
                Set<String> enchantmentsWithChild = minEnchantmentSection.getKeys(false);

                for(String name : enchantmentsWithChild) {
                    minEnchantmentMap.put(name, minEnchantmentSection.getInt(name));
                }
            }

            if(itemConfiguration.contains(exactEnchantmentKey)) {
                ConfigurationSection exactEnchantmentSection = itemConfiguration.getConfigurationSection(exactEnchantmentKey);
                Set<String> enchantmentsWithChild = exactEnchantmentSection.getKeys(false);

                for(String name : enchantmentsWithChild) {
                    //TODO: Retrieve a int list instead of just an int
                    exactEnchantmentMap.put(name, exactEnchantmentSection.getInt(name));
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

    public boolean toolExists(ItemStack itemStack) {
        return items.containsKey(itemStack.getType());
    }

    public boolean isHarvestable(ItemStack itemStack) {
        return items.get(itemStack.getType()).isHarvestable();
    }

    public boolean isBestTool(ItemStack itemStack) {
        return items.get(itemStack.getType()).isBestTool();
    }

    //BLOCKS

    public boolean blockExists(Block block) {
        return blocks.containsKey(block.getType());
    }

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
