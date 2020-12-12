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


public class BlockEvents implements Listener {

    private final BedrockBreaker bedrockBreaker;
    private final ConfigHelper configHelper;

    public BlockEvents(BedrockBreaker bedrockBreaker) {
        this.bedrockBreaker = bedrockBreaker;

        configHelper = new ConfigHelper(bedrockBreaker.getConfig());
    }

    //Triggers when the player starts to dig a block
    @EventHandler
    public void onPlayerStartDigBedrock(BlockDamageEvent event) {
        if(configHelper.toolExists(event.getItemInHand()) && configHelper.blockExists(event.getBlock())) {
            //Check if insta break
            //TODO: ADD 1/4s delay between breaking blocks unless insta break I guess

            new BukkitRunnable() {
                private float percentDigged = 0;
                private int digState = 0;
                private final Player player = event.getPlayer();
                private final Block block = event.getBlock();
                private final BlockPosition blockPosition = new BlockPosition(block.getX(),block.getY(),block.getZ());
                private final ItemStack usedItem = event.getItemInHand();
                private long time;

                private PacketDigBlockReader blockReader = null;

                @Override
                public void run() {
                    if(blockReader == null) {
                        time = System.nanoTime();

                        //Use the runnables taskid to initialize a new PacketInjector, it should then be completely unique.
                        blockReader = PacketInjector.addPlayer(event.getPlayer(), String.valueOf(this.getTaskId()));
                    } else if(!blockReader.playerIsDiggingBlock()) {
                        cancel();
                    } else {

                        float ticksToBreak = getTotalDigTimeInTicks(player, usedItem, block);
                        percentDigged += 1 / ticksToBreak;

                        //Animations are always cut into 9 parts, therefore digState/9
                        if (percentDigged * 10 >= digState) {
                            digState = (int) Math.floor(percentDigged * 10); //Changed to floor
                            if (percentDigged >= 1) {
                                cancel();
                            } else {
                                updateBlockBreakState();
                            }
                        }
                    }
                }

                /**
                 * Updates the block breaking animation for players in the same world
                 * as the block
                 */
                private void updateBlockBreakState() {
                    PacketPlayOutBlockBreakAnimation action = new PacketPlayOutBlockBreakAnimation(0, blockPosition, digState);
                    for(Player onlinePlayer : block.getWorld().getPlayers()) {
                        ((CraftPlayer)onlinePlayer).getHandle().playerConnection.sendPacket(action);
                    }

                }

                @Override
                public synchronized void cancel() throws IllegalStateException {
                    digState = -1; //Or wth is it?
                    updateBlockBreakState();

                    if(blockReader.playerIsDiggingBlock()) {
                        //Remove block and drop it
                        Material blockType = block.getType();

                        //Break naturally is weird with items that can already break
                        block.breakNaturally(usedItem, true);

                        if(itemIsBreakableByDigging(usedItem) && usedItem.getItemMeta() instanceof Damageable) {
                            Damageable itemMeta = (Damageable) usedItem.getItemMeta();
                            int itemDamage = itemMeta.getDamage();
                            float durabilityEnchantmentLevel = usedItem.getEnchantmentLevel(Enchantment.DURABILITY);

                            //Calculate durability change
                            int durabilityChange = Math.random()*100 < (100/(durabilityEnchantmentLevel+1)) ? 1 : 0;
                            itemMeta.setDamage(itemDamage + durabilityChange);

                            usedItem.setItemMeta((ItemMeta) itemMeta);
                        }
                        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(blockType,1));

                    }

                    PacketInjector.removePlayer(player, String.valueOf(this.getTaskId()));
                    long endTime = System.nanoTime() - time;
                    System.out.println("Start:   " + time + " " + event.getBlock().getX());
                    System.out.println("End:   " + System.nanoTime() + " " + event.getBlock().getX());
                    System.out.println("Time diff: " + endTime + " " + event.getBlock().getX());
                    super.cancel();
                }
            }.runTaskTimer(bedrockBreaker, 0, 1);
        }
    }

    /**
     * See https://minecraft.gamepedia.com/Breaking#Calculation for algorithm used
     *
     * @param player The player that is breaking the block
     * @param itemUsedToBreak The item digging the block
     * @param blockToBreak The block to break
     * @return The total ticks it takes to break this block
     */
    float getTotalDigTimeInTicks(Player player, ItemStack itemUsedToBreak, Block blockToBreak) {
        boolean isHarvestable = configHelper.isHarvestable(itemUsedToBreak);

        float seconds = 5;

        if(isHarvestable) {
            if (configHelper.hasCustomHardness(blockToBreak)) {
                seconds = configHelper.getCustomHardness(blockToBreak) * 1.5f;
            } else {
                seconds = blockToBreak.getType().getHardness() * 1.5f;
            }
        }

        float speedMultiplier = 1;

        if(configHelper.isBestTool(itemUsedToBreak)) {
            speedMultiplier = getSpeedMultiplier(itemUsedToBreak);

            //Check if player has efficiency
            if (itemUsedToBreak.getEnchantments().containsKey(Enchantment.DIG_SPEED)) {
                speedMultiplier += Math.pow(itemUsedToBreak.getEnchantments().get(Enchantment.DIG_SPEED)+1, 2) + 1;
            }

            //Reset multiplayer if item is not harvestable.
            if(!isHarvestable) {
                speedMultiplier = 1;
            }
        }

        //Check if player has haste
        if(player.hasPotionEffect(PotionEffectType.FAST_DIGGING)) {
            speedMultiplier *= 1 + 0.2*player.getPotionEffect(PotionEffectType.FAST_DIGGING).getAmplifier();
        }

        //Check if player has mining fatigue
        if(player.hasPotionEffect(PotionEffectType.SLOW_DIGGING)) {
            speedMultiplier /= Math.pow(3,player.getPotionEffect(PotionEffectType.SLOW_DIGGING).getAmplifier());
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

        return seconds*20;
    }

    /**
     * Speed multipliers are as defined on https://minecraft.gamepedia.com/Breaking#Speed
     * All tools except hoes are affected. The values are as follows
     * 1(not a tool), 2(wood), 4(stone), 6(iron), 8(dia), 9(nether), 12(gold)
     * @param item The item to get a speed multiplier of
     * @return A speed multiplier of a value mentioned above
     */
    int getSpeedMultiplier(ItemStack item) {
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
