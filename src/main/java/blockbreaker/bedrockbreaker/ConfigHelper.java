package blockbreaker.bedrockbreaker;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles getting data from the configuration file config.yml
 */
public class ConfigHelper {

    private final String hardnessKey = "hardness";

    private final String isHarvestableKey = "isHarvestable";
    private final String isBestToolKey = "isBestTool";

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
        ConfigurationSection configurationSection = fileConfiguration.getConfigurationSection(itemsToDigWithKey);

        Set<String> itemsAsMap = configurationSection.getKeys(false);
        items = new HashMap<>(itemsAsMap.size());

        for(String materialName : itemsAsMap) {
            ConfigurationSection itemConfiguration = configurationSection.getConfigurationSection(materialName);;

            boolean isHarvestable = true;
            boolean isBestTool = true;

            if(itemConfiguration.contains(isHarvestableKey)) {
                isHarvestable = itemConfiguration.getBoolean(isHarvestableKey);
            }

            if(itemConfiguration.contains(isBestToolKey)) {
                isBestTool = itemConfiguration.getBoolean(isBestToolKey);
            }

            items.put(Material.getMaterial(materialName), new ItemDigConfiguration(isHarvestable, isBestTool));
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
}
