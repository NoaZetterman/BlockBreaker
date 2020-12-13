package blockbreaker.bedrockbreaker;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.util.Map;
import java.util.Set;

public class ItemDigConfiguration {
    private final boolean isHarvestable;
    private final boolean isBestTool;

    private final Set<Material> breakableMaterials;

    private final Map<String, Integer> exactEnchantments;
    private final Map<String, Integer> minEnchantments;

    public ItemDigConfiguration(boolean isHarvestable, boolean isBestTool, Set<Material> breakableMaterials, Map<String, Integer> exactEnchantments, Map<String, Integer> minEnchantments) {
        this.isHarvestable = isHarvestable;
        this.isBestTool = isBestTool;
        this.breakableMaterials = breakableMaterials;
        this.exactEnchantments = exactEnchantments;
        this.minEnchantments = minEnchantments;
    }

    public boolean isHarvestable() {
        return isHarvestable;
    }

    public boolean isBestTool() {
        return isBestTool;
    }

    /**
     *
     * @param activeEnchantments A Map of the items active enchantments
     * @param blockMaterial The material of the block to dig
     * @return True if the block can be broken, false otherwise
     */
    public boolean canBreakBlock(Map<Enchantment, Integer> activeEnchantments, Material blockMaterial) {
        if(breakableMaterials.contains(blockMaterial) || breakableMaterials.size() == 0) {
            return hasExactEnchantments(activeEnchantments) && hasMinEnchantment(activeEnchantments);
        } else {
            return false;
        }
    }

    private boolean hasMinEnchantment(Map<Enchantment, Integer> activeEnchantments) {
        if(minEnchantments.size() == 0) {
            return true;
        }

        for(Enchantment enchantment : activeEnchantments.keySet()) {
            String key = enchantment.getKey().getKey();
            if(minEnchantments.containsKey(key)
                    && minEnchantments.get(key).compareTo(activeEnchantments.get(enchantment)) <= 0) {
                return true;
            }
        }

        return false;
    }

    private boolean hasExactEnchantments(Map<Enchantment, Integer> activeEnchantments) {

        for(String enchantmentName : exactEnchantments.keySet()) {
            if(!activeEnchantments.containsKey(Enchantment.getByKey(NamespacedKey.minecraft(enchantmentName)))
                    || exactEnchantments.get(enchantmentName)
                    .compareTo(activeEnchantments.get(Enchantment.getByKey(NamespacedKey.minecraft(enchantmentName)))) != 0) {
                return false;
            }
        }

        return true;
    }
}
