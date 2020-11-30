package blockbreaker.bedrockbreaker;

import net.minecraft.server.v1_16_R2.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
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
        System.out.println("Player started digging bedrocc");
        //Create a bukkit runnable that runs each tick and checks if player is still breaking block (nms)
        //Below should do a check over a couple of configed properties
        if(event.getBlock().getType() == Material.BEDROCK && event.getItemInHand().getType() == Material.DIAMOND_PICKAXE) {
            int size=100;

            new BukkitRunnable() {
                private int ticksUntilNextBreakAnimation = 0;
                private final int ticksToBreak = size;
                int digState = 0;
                private final CraftPlayer player = (CraftPlayer) event.getPlayer();
                private final Block block = event.getBlock();
                private final ItemStack usedItem = event.getItemInHand();


                @Override
                public void run() {
                    ticksUntilNextBreakAnimation++;
                    /*if(!isBreakingBlock()) {
                        //Reset block break state
                        super.cancel();
                    }*/

                    if(ticksUntilNextBreakAnimation == Math.floorDiv(ticksToBreak,9)) {
                        ticksUntilNextBreakAnimation = 0;
                        digState++;
                        if(digState > 9) {
                            Material blockType = block.getType();
                            block.breakNaturally(usedItem);
                            block.getWorld().dropItem(block.getLocation(), new ItemStack(blockType,1));
                            this.cancel();
                        }
                        updateBlockBreakState();
                    }


                    //Get craftbukkit player
                }

                /**
                 * Updates the block breaking animation to digState
                 */
                private void updateBlockBreakState() {

                    PacketPlayOutBlockBreakAnimation action = new PacketPlayOutBlockBreakAnimation(0,
                            new BlockPosition(block.getX(),block.getY(),block.getZ()), digState);
                    player.getHandle().playerConnection.sendPacket(action);

                }

                /**
                 * Checks if the player is currently digging a block
                 * @return True if the player is currently breaking a block, false otherwise
                 */
                private boolean isBreakingBlock() {
                    //PacketPlayInBlockDig d = new PacketPlayInBlockDig();
                    //System.out.println("Dig type: " + d.d());
                    player.getHandle();
                    //player.blablabla
                    return true;
                }

                @Override
                public synchronized void cancel() throws IllegalStateException {
                    //Add logic for it he item should break or "reset"
                    super.cancel();
                }
            }.runTaskTimerAsynchronously(bedrockBreaker, 0, 1);
        }

    }
}
