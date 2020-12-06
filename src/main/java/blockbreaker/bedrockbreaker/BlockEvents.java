package blockbreaker.bedrockbreaker;

import net.minecraft.server.v1_16_R2.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;


public class BlockEvents implements Listener {

    private final BedrockBreaker bedrockBreaker;
    //BLock damage event - detect when player starts digging a block
    /*@EventHandler
    public void onDigBlockEvent(BlockEvent e) {
        System.out.println("A block event");
    }*/

    public BlockEvents(BedrockBreaker bedrockBreaker) {
        this.bedrockBreaker = bedrockBreaker;
    }

    //Triggers when the player starts to dig a block
    @EventHandler
    public void onPlayerStartDigBedrock(BlockDamageEvent event) {
        //Create a bukkit runnable that runs each tick and checks if player is still breaking block (nms)
        //Below should do a check over a couple of configed properties
        if(event.getBlock().getType() == Material.BEDROCK && event.getItemInHand().getType() == Material.DIAMOND_PICKAXE) {
            int size=100;

            new BukkitRunnable() {
                private int ticksUntilNextBreakAnimation = 0;
                private final int ticksToBreak = size;
                private int digState = 0;
                private final Player player = event.getPlayer();
                private final Block block = event.getBlock();
                private final BlockPosition blockPosition = new BlockPosition(block.getX(),block.getY(),block.getZ());
                private final ItemStack usedItem = event.getItemInHand();

                private PacketDigBlockReader blockReader;

                @Override
                public void run() {
                    if(ticksUntilNextBreakAnimation == 0 && digState == 0) {
                        //Use the runnables taskid to initialize a new PacketInjector, it should then be completely unique.
                        blockReader = PacketInjector.addPlayer(event.getPlayer(), String.valueOf(this.getTaskId()));
                    }

                    ticksUntilNextBreakAnimation++;

                    if(!blockReader.playerIsDiggingBlock()) {
                        cancel();
                    }

                    //Animations are always cut into 9 parts, therefore ticksToBreak/9
                    if(ticksUntilNextBreakAnimation == Math.floorDiv(ticksToBreak,9)) {
                        ticksUntilNextBreakAnimation = 0;
                        digState++;
                        if(digState > 9) {
                            cancel();
                        }
                        updateBlockBreakState();
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
                    if(blockReader.playerIsDiggingBlock()) {
                        //Remove block and drop it
                        Material blockType = block.getType();
                        block.breakNaturally(usedItem);
                        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(blockType,1));

                    } else {
                        //Reset block digging animation
                        digState = 0;
                        updateBlockBreakState();
                    }

                    PacketInjector.removePlayer(player, String.valueOf(this.getTaskId()));
                    super.cancel();
                }
            }.runTaskTimer(bedrockBreaker, 0, 1);
        }

    }
}
