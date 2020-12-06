package blockbreaker.bedrockbreaker;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.server.v1_16_R2.PacketPlayInBlockDig;

/**
 * Reads incoming packets from all a player and keeps track of if the player is
 * currently digging the same block that was being digged when this object got
 * instantiated.
 */
public class PacketDigBlockReader extends ChannelDuplexHandler {
    private boolean isDiggingBlock = true;

    /**
     * Get if the player is currently digging the same block that they were digging
     * when this object was created
     * @return True if player is still digging the block, false otherwise
     */
    public boolean playerIsDiggingBlock() {
        return isDiggingBlock;
    }

    /**
     * Gets the players current dig state, and detects if the player is no longer digging
     *
     * @param c tf is this my dude
     * @param incomingPacket The packet sent from the client to the server
     */
    @Override
    public void channelRead(ChannelHandlerContext c, Object incomingPacket) throws Exception {
        if (incomingPacket instanceof PacketPlayInBlockDig ) {
            PacketPlayInBlockDig digPacket = (PacketPlayInBlockDig) incomingPacket;

            switch(digPacket.d()) {
                case ABORT_DESTROY_BLOCK:
                case SWAP_ITEM_WITH_OFFHAND:
                case DROP_ITEM:
                case DROP_ALL_ITEMS:
                case STOP_DESTROY_BLOCK:
                case RELEASE_USE_ITEM:
                    isDiggingBlock = false;
            }
        }

        super.channelRead(c, incomingPacket);
    }
}
