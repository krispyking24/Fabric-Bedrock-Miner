package yan.lx.bedrockminer.handle;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import yan.lx.bedrockminer.Debug;

public class TaskModifyLookManager {
    private static boolean modifyYaw = false;
    private static boolean modifyPitch = false;
    private static float yaw = 0F;
    private static float pitch = 0F;

    public static PlayerMoveC2SPacket getLookAndOnGroundPacket(ClientPlayerEntity player) {
        var yaw = modifyYaw ? TaskModifyLookManager.yaw : player.getYaw();
        var pitch = modifyPitch ? TaskModifyLookManager.pitch : player.getPitch();
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
        TaskModifyLookManager.modifyYaw = true;
        TaskModifyLookManager.yaw = yaw;
        TaskModifyLookManager.modifyPitch = true;
        TaskModifyLookManager.pitch = pitch;
    }

    public static void reset() {
        modifyYaw = false;
        yaw = 0F;
        modifyPitch = false;
        pitch = 0F;
    }

    public static float onModifyLookYaw(float yaw) {
        return modifyYaw ? TaskModifyLookManager.yaw : yaw;
    }

    public static float onModifyLookPitch(float pitch) {
        return modifyPitch ? TaskModifyLookManager.pitch : pitch;
    }
}
