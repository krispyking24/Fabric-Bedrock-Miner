package yan.lx.bedrockminer.network;

import com.google.common.collect.Queues;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;

import java.util.Queue;

public class PacketManager {
    public static Queue<Packet<?>> packets = Queues.newConcurrentLinkedQueue();
    public static boolean write = false;

    public static void sendPacket(Packet<?> packet) {
        packets.add(packet);
    }

    public static void tick() {
        var networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (PacketManager.write && networkHandler != null) {
            Queue<Packet<?>> queue = PacketManager.packets;
            synchronized (queue) {
                Packet<?> packet;
                if ((packet = queue.poll()) != null) {
                    var connection = networkHandler.getConnection();
                    if (connection != null) {
                        connection.send(packet);
                    }
                }
            }
            if (PacketManager.packets.size() == 0) {
                PacketManager.write = false;
            }
        }
    }
}
