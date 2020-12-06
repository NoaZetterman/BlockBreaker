package blockbreaker.bedrockbreaker;

import io.netty.channel.Channel;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

/**
 * Adds and removes players from a packet reader that reads PacketPlayInBlockDig packets.
 */
public class PacketInjector {
    /**
     * Adds a player to read packets from PacketDigBlockReader to let the plugin read the players packets
     *
     * @param player The player to add
     * @param handlerName The name of the packet reader, must be unique
     * @return A PacketDigBlockReader used to check if player is still digging a block
     */
    public static PacketDigBlockReader addPlayer(Player player, String handlerName) {
        Channel channel = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel;

        PacketDigBlockReader packetDigBlockReader = null;
        if(channel.pipeline().get(handlerName) == null) {
            packetDigBlockReader = new PacketDigBlockReader();
            channel.pipeline().addBefore("packet_handler", handlerName, packetDigBlockReader);
        }

        return packetDigBlockReader;
    }

    /**
     * Removes a given player from reading player packets
     *
     * @param player The player name
     * @param handlerName The name of the packet handler, same as used when adding the player
     */
    public static void removePlayer(Player player, String handlerName) {
        Channel channel = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel;

        if (channel.pipeline().get(handlerName) != null) {
            channel.pipeline().remove(handlerName);
        }
    }
}
