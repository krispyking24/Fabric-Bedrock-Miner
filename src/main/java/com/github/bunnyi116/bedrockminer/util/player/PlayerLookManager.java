package com.github.bunnyi116.bedrockminer.util.player;

import com.github.bunnyi116.bedrockminer.task.Task;
import com.github.bunnyi116.bedrockminer.util.network.NetworkUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.jetbrains.annotations.Nullable;

import static com.github.bunnyi116.bedrockminer.BedrockMiner.networkHandler;
import static com.github.bunnyi116.bedrockminer.BedrockMiner.player;

public class PlayerLookManager {
    public static PlayerLookManager INSTANCE = new PlayerLookManager();

    private boolean modifyYaw = false;
    private boolean modifyPitch = false;
    private float yaw = 0F;
    private float pitch = 0F;
    private int ticks = 0;
    private @Nullable Task task = null;

    public float onModifyLookYaw(float yaw) {
        return this.modifyYaw ? this.yaw : yaw;
    }

    public float onModifyLookPitch(float pitch) {
        return this.modifyPitch ? this.pitch : pitch;
    }

    public static ServerboundMovePlayerPacket getLookPacket(LocalPlayer player, float yaw, float pitch) {
        //#if MC > 12101
        return new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround(), false);
        //#else
        //$$ return new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround());
        //#endif

    }

    public static void sendLookPacket(ClientPacketListener networkHandler, ServerboundMovePlayerPacket packet) {
        NetworkUtils.sendPacket(packet);
    }

    public static void sendLookPacket(ClientPacketListener networkHandler, float yaw, float pitch) {
        final ServerboundMovePlayerPacket packet = PlayerLookManager.getLookPacket(player, yaw, pitch);
        PlayerLookManager.sendLookPacket(networkHandler, packet);
    }

    private ServerboundMovePlayerPacket getLookPacket(LocalPlayer player) {
        var yaw = this.modifyYaw ? this.yaw : player.getYRot();
        var pitch = this.modifyPitch ? this.pitch : player.getXRot();
        return getLookPacket(player, yaw, pitch);
    }

    public void sendLookPacket() {
        NetworkUtils.sendPacket(this.getLookPacket(player));
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
        this.modifyYaw = true;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        this.modifyPitch = true;
    }


    public void set(float yaw, float pitch) {
        this.setYaw(yaw);
        this.setPitch(pitch);
    }

    public void set(Direction facing, Task task) {
        this.task = task;
        final var yaw = switch (facing) {
            case SOUTH -> 180F;
            case EAST -> 90F;
            case NORTH -> 0F;
            case WEST -> -90F;
            default -> player == null ? 0F : player.getYRot();
        };
        final var pitch = switch (facing) {
            case UP -> 90F;
            case DOWN -> -90F;
            default -> 0F;
        };
        this.set(yaw, pitch);
        this.sendLookPacket();
    }

    public void reset() {
        this.modifyYaw = false;
        this.modifyPitch = false;
        this.task = null;
        this.sendLookPacket();
    }

    public void tick() {
        if (this.isModify()) {   // 自动重置视角
            if (this.ticks++ > 20) {
                this.ticks = 0;
                this.reset();
            }
        }
    }

    public boolean isModify() {
        return this.modifyYaw || this.modifyPitch;
    }

    public @Nullable Task getTask() {
        return this.task;
    }
}
