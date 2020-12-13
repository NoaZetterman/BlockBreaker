package blockbreaker.bedrockbreaker;

import net.minecraft.server.v1_16_R2.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.UUID;

/**
 * Handles digging with customizable items on customized blocks.
 *
 */
public class DigBlock implements Listener {

    private final BedrockBreaker bedrockBreaker;
    private final ConfigHelper configHelper;

    private HashSet<UUID> playersWithDigDelay = new HashSet<>();

    public DigBlock(BedrockBreaker bedrockBreaker) {
        this.bedrockBreaker = bedrockBreaker;

        configHelper = new ConfigHelper(bedrockBreaker.getConfig());
    }

    //Triggers when the player starts to dig a block
    @EventHandler
    public void onPlayerStartDigBlock(BlockDamageEvent event) {
        //Check if block and item exists in configuration
        if(configHelper.canBreakBlock(event.getItemInHand(), event.getBlock())) {

            new BukkitRunnable() {
                private float portionDigged = 0;
                private int digState = 0;

                private boolean isinstabreak = false;

                private final Player player = event.getPlayer();
                private final Block block = event.getBlock();
                private final BlockPosition blockPosition = new BlockPosition(block.getX(),block.getY(),block.getZ());
                private final ItemStack usedItem = event.getItemInHand();

                private PacketDigBlockReader blockReader = null;

                @Override
                public void run() {
                    if(blockReader == null) {
                        if(getTotalDigTimeInTicks(player, usedItem, block) == 0) {
                            isinstabreak = true;
                            portionDigged = 1;

                            cancel();
                        } else {
                            //Use the runnables taskid to initialize a new PacketInjector, it should then be completely unique.
                            blockReader = PacketInjector.addPlayer(event.getPlayer(), String.valueOf(this.getTaskId()));
                        }
                    } else if(!playersWithDigDelay.contains(player.getUniqueId())) {
                        if (!blockReader.playerIsDiggingBlock()) {
                            cancel();
                        } else {

                            float ticksToBreak = getTotalDigTimeInTicks(player, usedItem, block);
                            if(ticksToBreak == 0) {
                                portionDigged = 1;
                            } else {
                                portionDigged += 1 / ticksToBreak;
                            }

                            //There are a total of 10 different dig states (0-9)
                            if (digState < Math.floor(portionDigged * 10)) {
                                if (portionDigged >= 1 && digState > 0) {
                                    cancel();
                                } else {
                                    digState = (int) Math.floor(portionDigged * 10);
                                    updateBlockBreakAnimation();
                                }
                            }
                        }
                    }
                }

                /**
                 * Updates the block breaking animation for players in the same world
                 * as the block
                 */
                private void updateBlockBreakAnimation() {
                    PacketPlayOutBlockBreakAnimation action = new PacketPlayOutBlockBreakAnimation(0, blockPosition, digState);
                    for(Player onlinePlayer : block.getWorld().getPlayers()) {
                        ((CraftPlayer)onlinePlayer).getHandle().playerConnection.sendPacket(action);
                    }

                }

                @Override
                public synchronized void cancel() throws IllegalStateException {
                    digState = -1; //Any number not 0-9 sets the block to not show a dig animation
                    updateBlockBreakAnimation();

                    //If percentDigged is 0 then it is insta break
                    if(portionDigged >= 1 && block.getType() != Material.AIR) {
                        if (!isinstabreak) {
                            addPlayerToDigDelay(player);

                            if (blockReader.playerIsDiggingBlock()) {
                                breakBlock(player, usedItem, block);
                            }

                            PacketInjector.removePlayer(player, String.valueOf(this.getTaskId()));
                        } else {
                            breakBlock(player, usedItem, block);
                        }
                    }

                    super.cancel();
                }
            }.runTaskTimer(bedrockBreaker, 0, 1);
        }
    }

    /**
     * Breaks a block using an item. Sets drop and durability of item if needed.
     *
     * @param player The player that's breaking the block
     * @param usedItem The item used to break the block
     * @param block The block to break
     */
    private void breakBlock(Player player, ItemStack usedItem, Block block) {
        Material blockType = block.getType();

        if (block.getDrops(usedItem).size() == 0) {
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(blockType, 1));
        }

        block.breakNaturally(usedItem, true);

        if (itemIsBreakableByDigging(usedItem) && usedItem.getItemMeta() instanceof Damageable) {
            Damageable itemMeta = (Damageable) usedItem.getItemMeta();
            int itemDamage = itemMeta.getDamage();
            float durabilityEnchantmentLevel = usedItem.getEnchantmentLevel(Enchantment.DURABILITY);

            //Calculate durability change
            int durabilityChange = Math.random() * 100 < (100 / (durabilityEnchantmentLevel + 1)) ? 1 : 0;
            itemMeta.setDamage(itemDamage + durabilityChange);
            if (itemMeta.getDamage() >= usedItem.getType().getMaxDurability()) {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            } else {
                usedItem.setItemMeta((ItemMeta) itemMeta);
            }
        }
    }

    /**
     * Adds a player to have a dig delay of 5 ticks (0.25s), this makes
     * the player unable to actively dig another block (the block animation does not change)
     *
     * @param player The player to add a dig delay to
     */
    private void addPlayerToDigDelay(Player player) {
        playersWithDigDelay.add(player.getUniqueId());

        new BukkitRunnable() {

            @Override
            public void run() {
                playersWithDigDelay.remove(player.getUniqueId());
            }
        }.runTaskLaterAsynchronously(bedrockBreaker, 5);
    }

    /**
     * Calculates how long it takes to dig a block with a given item, for a player which may have
     * effects active
     *
     * See https://minecraft.gamepedia.com/Breaking#Calculation for algorithm used
     *
     * @param player The player that is breaking the block
     * @param itemUsedToBreak The item digging the block
     * @param blockToBreak The block to break
     * @return The total ticks it takes to break this block
     */
    private float getTotalDigTimeInTicks(Player player, ItemStack itemUsedToBreak, Block blockToBreak) {
        boolean isHarvestable = configHelper.isHarvestable(itemUsedToBreak);

        float seconds = 5;
        float hardness = blockToBreak.getType().getHardness();

        if(isHarvestable) {
            if (configHelper.hasCustomHardness(blockToBreak)) {
                hardness = configHelper.getCustomHardness(blockToBreak);
            }

            seconds = hardness * 1.5f;
        }

        float speedMultiplier = 1;

        if(configHelper.isBestTool(itemUsedToBreak)) {
            speedMultiplier = getSpeedMultiplier(itemUsedToBreak);

            //Check if player has efficiency
            if (itemUsedToBreak.getEnchantments().containsKey(Enchantment.DIG_SPEED)) {
                speedMultiplier += Math.pow(itemUsedToBreak.getEnchantments().get(Enchantment.DIG_SPEED), 2) + 1;
            }

            //Reset multiplier if item is not harvestable.
            if(!isHarvestable) {
                speedMultiplier = 1;
            }
        }

        //Check if player has haste
        if(player.hasPotionEffect(PotionEffectType.FAST_DIGGING)) {
            speedMultiplier *= 1 + 0.2 * (player.getPotionEffect(PotionEffectType.FAST_DIGGING).getAmplifier() + 1);
        }

        //Check if player has mining fatigue
        if(player.hasPotionEffect(PotionEffectType.SLOW_DIGGING)) {
            speedMultiplier /= Math.pow(3, player.getPotionEffect(PotionEffectType.SLOW_DIGGING).getAmplifier() + 1);
        }

        seconds /= speedMultiplier;


        //Check if player is in water and does not have aqua affiliation on helmet
        if(player.isInWater() && (player.getInventory().getHelmet() == null ||
                !player.getInventory().getHelmet().getEnchantments().containsKey(Enchantment.WATER_WORKER))) {
            seconds *= 5;
        }

        //Check if player is not standing on a block
        if(!player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid()
                || (player.isInWater() && player.getVelocity().getY() != -0.005)) {
            //Player has -0.005 velocity in y direction when standing still, maybe rework this :v
            seconds *= 5;
        }

        //Check if the block should be instabreaked
        if(speedMultiplier > hardness*30) {
            seconds = 0;
        }

        return seconds*20;
    }

    /**
     * Speed multipliers are as defined on https://minecraft.gamepedia.com/Breaking#Speed
     * All tools except hoes are affected. The values are as follows
     * 1(not a tool), 2(wood), 4(stone), 6(iron), 8(dia), 9(nether), 12(gold)
     *
     * @param item The item to get a speed multiplier of
     * @return A speed multiplier of a value mentioned above
     */
    private int getSpeedMultiplier(ItemStack item) {
        Material material = item.getType();

        switch(material) {
            case NETHERITE_AXE:
            case NETHERITE_PICKAXE:
            case NETHERITE_SHOVEL:
            case NETHERITE_SWORD:
                return 9;
            case DIAMOND_AXE:
            case DIAMOND_PICKAXE:
            case DIAMOND_SHOVEL:
            case DIAMOND_SWORD:
                return 8;
            case GOLDEN_AXE:
            case GOLDEN_PICKAXE:
            case GOLDEN_SHOVEL:
            case GOLDEN_SWORD:
                return 12;
            case IRON_AXE:
            case IRON_PICKAXE:
            case IRON_SHOVEL:
            case IRON_SWORD:
                return 6;
            case STONE_AXE:
            case STONE_PICKAXE:
            case STONE_SHOVEL:
            case STONE_SWORD:
                return 4;
            case WOODEN_AXE:
            case WOODEN_PICKAXE:
            case WOODEN_SHOVEL:
            case WOODEN_SWORD:
                return 2;
            default:
                return 1;
        }
    }

    /**
     * Checks if an item is breakable by digging
     *
     * @param itemStack The itemstack to check
     * @return True if durability can be changed when breaking a block, false otherwise
     */
    private boolean itemIsBreakableByDigging(ItemStack itemStack) {
        Material itemMaterial = itemStack.getType();

        switch(itemMaterial) {
            case NETHERITE_AXE:
            case NETHERITE_PICKAXE:
            case NETHERITE_SHOVEL:
            case NETHERITE_SWORD:
            case DIAMOND_AXE:
            case DIAMOND_PICKAXE:
            case DIAMOND_SHOVEL:
            case DIAMOND_SWORD:
            case GOLDEN_AXE:
            case GOLDEN_PICKAXE:
            case GOLDEN_SHOVEL:
            case GOLDEN_SWORD:
            case IRON_AXE:
            case IRON_PICKAXE:
            case IRON_SHOVEL:
            case IRON_SWORD:
            case STONE_AXE:
            case STONE_PICKAXE:
            case STONE_SHOVEL:
            case STONE_SWORD:
            case WOODEN_AXE:
            case WOODEN_PICKAXE:
            case WOODEN_SHOVEL:
            case WOODEN_SWORD:
                return true;
            default:
                return false;
        }
    }
}
