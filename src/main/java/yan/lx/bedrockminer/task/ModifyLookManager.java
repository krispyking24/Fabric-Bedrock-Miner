package yan.lx.bedrockminer.task;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Direction;
import yan.lx.bedrockminer.Debug;

public class ModifyLookManager {
    private static boolean modifyYaw = false;
    private static boolean modifyPitch = false;
    private static float yaw = 0F;
    private static float pitch = 0F;

    private static PlayerMoveC2SPacket getLookAndOnGroundPacket(ClientPlayerEntity player) {
        var yaw = modifyYaw ? ModifyLookManager.yaw : player.getYaw();
        var pitch = modifyPitch ? ModifyLookManager.pitch : player.getPitch();
        if (modifyYaw) Debug.info(yaw);
        if (modifyPitch) Debug.info(pitch);
        return new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, player.isOnGround());
    }

    public static float getYaw() {
        return yaw;
    }

    public static float getPitch() {
        return pitch;
    }

    public static void set(float yaw, float pitch) {
        ModifyLookManager.modifyYaw = true;
        ModifyLookManager.yaw = yaw;
        ModifyLookManager.modifyPitch = true;
        ModifyLookManager.pitch = pitch;
    }

    public static void reset() {
        modifyYaw = false;
        yaw = 0F;
        modifyPitch = false;
        pitch = 0F;
        set(yaw, pitch);
        // 发送一个还原视角的数据包
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler != null) {
            networkHandler.sendPacket(getLookAndOnGroundPacket(player));
        }
    }

    public static float onModifyLookYaw(float yaw) {
        return modifyYaw ? ModifyLookManager.yaw : yaw;
    }

    public static float onModifyLookPitch(float pitch) {
        return modifyPitch ? ModifyLookManager.pitch : pitch;
    }

    public static void set(Direction facing) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        float yaw = switch (facing) {
            case SOUTH -> 180F;
            case EAST -> 90F;
            case NORTH -> 0F;
            case WEST -> -90F;
            default -> player == null ? 0F : player.getYaw();
        };
        float pitch = switch (facing) {
            case UP -> 90F;
            case DOWN -> -90F;
            default -> 0F;
        };
        set(yaw, pitch);
        if (networkHandler != null) {
            networkHandler.sendPacket(getLookAndOnGroundPacket(player));
        }
    }
}
