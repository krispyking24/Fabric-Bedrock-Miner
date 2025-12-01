package com.github.bunnyi116.bedrockminer.util.player;

import com.github.bunnyi116.bedrockminer.task.Task;
import com.github.bunnyi116.bedrockminer.util.network.NetworkUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import static com.github.bunnyi116.bedrockminer.BedrockMiner.networkHandler;
import static com.github.bunnyi116.bedrockminer.BedrockMiner.player;

public class PlayerLookUtils {
    private static boolean modifyYaw = false;
    private static boolean modifyPitch = false;
    private static float yaw = 0F;
    private static float pitch = 0F;
    private static int ticks = 0;
    private static @Nullable Task task = null;

    public static Direction getPlacementDirection() {
        float currentYaw = PlayerLookUtils.modifyYaw ? PlayerLookUtils.yaw : (player != null ? player.getYRot() : 0F);
        float currentPitch = PlayerLookUtils.modifyPitch ? PlayerLookUtils.pitch : (player != null ? player.getXRot() : 0F);
        return getDirectionFromYawPitch(currentYaw, currentPitch);
    }

    // 静态工具方法：根据yaw和pitch计算方向
    public static Direction getDirectionFromYawPitch(float yaw, float pitch) {
        // 标准化yaw到0-360范围
        float normalizedYaw = Mth.wrapDegrees(yaw);
        if (normalizedYaw < 0) {
            normalizedYaw += 360;
        }
        // 根据pitch判断上下方向
        if (pitch <= -45) {
            return Direction.UP;
        } else if (pitch >= 45) {
            return Direction.DOWN;
        }
        // 根据yaw判断水平方向
        if (normalizedYaw >= 315 || normalizedYaw < 45) {
            return Direction.SOUTH;
        } else if (normalizedYaw >= 45 && normalizedYaw < 135) {
            return Direction.WEST;
        } else if (normalizedYaw >= 135 && normalizedYaw < 225) {
            return Direction.NORTH;
        } else {
            return Direction.EAST;
        }
    }

    // 获取当前水平方向（忽略上下看）
    public static Direction getPlacementHorizontalDirection() {
        float currentYaw = PlayerLookUtils.modifyYaw ? PlayerLookUtils.yaw : (player != null ? player.getYRot() : 0F);
        return getHorizontalDirectionFromYaw(currentYaw);
    }

    // 静态工具方法：根据yaw计算水平方向
    public static Direction getHorizontalDirectionFromYaw(float yaw) {
        float normalizedYaw = Mth.wrapDegrees(yaw);
        if (normalizedYaw < 0) {
            normalizedYaw += 360;
        }
        if (normalizedYaw >= 315 || normalizedYaw < 45) {
            return Direction.SOUTH;
        } else if (normalizedYaw >= 45 && normalizedYaw < 135) {
            return Direction.WEST;
        } else if (normalizedYaw >= 135 && normalizedYaw < 225) {
            return Direction.NORTH;
        } else {
            return Direction.EAST;
        }
    }

    public static float onModifyLookYaw(float yaw) {
        return PlayerLookUtils.modifyYaw ? PlayerLookUtils.yaw : yaw;
    }

    public static float onModifyLookPitch(float pitch) {
        return PlayerLookUtils.modifyPitch ? PlayerLookUtils.pitch : pitch;
    }

    public static ServerboundMovePlayerPacket getLookPacket(LocalPlayer player, float yaw, float pitch) {
        //#if MC > 12101
        return new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround(), false);
        //#else
        //$$ return new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround());
        //#endif

    }

    public static void sendLookPacket(ServerboundMovePlayerPacket packet) {
        NetworkUtils.sendPacket(packet);
    }

    public static void sendLookPacket(float yaw, float pitch) {
        PlayerLookUtils.sendLookPacket(PlayerLookUtils.getLookPacket(player, yaw, pitch));
    }

    private static ServerboundMovePlayerPacket getLookPacket(LocalPlayer player) {
        float yaw = PlayerLookUtils.modifyYaw ? PlayerLookUtils.yaw : player.getYRot();
        float pitch = PlayerLookUtils.modifyPitch ? PlayerLookUtils.pitch : player.getXRot();
        return getLookPacket(player, yaw, pitch);
    }

    public static void sendLookPacket() {
        if (networkHandler != null && player != null) {
            networkHandler.send(PlayerLookUtils.getLookPacket(player));
        }
    }

    public static void setYaw(float yaw) {
        PlayerLookUtils.yaw = yaw;
        PlayerLookUtils.modifyYaw = true;
    }

    public static void setPitch(float pitch) {
        PlayerLookUtils.pitch = pitch;
        PlayerLookUtils.modifyPitch = true;
    }

    public static void set(float yaw, float pitch) {
        PlayerLookUtils.setYaw(yaw);
        PlayerLookUtils.setPitch(pitch);
    }

    public static void set(Direction facing, Task task) {
        PlayerLookUtils.task = task;
        float yaw;
        switch (facing) {
            case SOUTH:
                yaw = 180F;
                break;
            case EAST:
                yaw = 90F;
                break;
            case NORTH:
                yaw = 0F;
                break;
            case WEST:
                yaw = -90F;
                break;
            default:
                yaw = player == null ? 0F : player.getYRot();
                break;
        }
        float pitch;
        switch (facing) {
            case UP:
                pitch = 90F;
                break;
            case DOWN:
                pitch = -90F;
                break;
            default:
                pitch = 0F;
                break;
        }
        PlayerLookUtils.set(yaw, pitch);
        PlayerLookUtils.sendLookPacket();
    }

    public static void reset() {
        PlayerLookUtils.modifyYaw = false;
        PlayerLookUtils.modifyPitch = false;
        PlayerLookUtils.task = null;
        PlayerLookUtils.sendLookPacket();
    }

    public static void tick() {
        if (PlayerLookUtils.isModify()) {   // 自动重置视角
            if (PlayerLookUtils.ticks++ > 20) {
                PlayerLookUtils.ticks = 0;
                PlayerLookUtils.reset();
            }
        }
    }

    public static boolean isModify() {
        return PlayerLookUtils.modifyYaw || PlayerLookUtils.modifyPitch;
    }

    public static @Nullable Task getTask() {
        return PlayerLookUtils.task;
    }
}