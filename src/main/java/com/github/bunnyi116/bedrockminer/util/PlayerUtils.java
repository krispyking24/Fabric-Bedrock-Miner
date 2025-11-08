package com.github.bunnyi116.bedrockminer.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class PlayerUtils {
    /**
     * 判断玩家是否位于可与指定方块交互的距离范围内。
     * 用于检测破坏、放置或右键交互是否有效。
     */
    public static boolean canInteractWithBlockAt(BlockPos blockPos, double additionalRange) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return false;
        }
        double blockPosX = blockPos.getX();
        double blockPosY = blockPos.getY();
        double blockPosZ = blockPos.getZ();
        //#if MC >= 12005
        double distance = getBlockInteractionRange() + additionalRange;
        double eyePosX = player.getX();
        double eyePosY = player.getEyeY();
        double eyePosZ = player.getZ();
        double dx = Math.max(Math.max(blockPosX - eyePosX, eyePosX - (blockPosX + 1)), 0);
        double dy = Math.max(Math.max(blockPosY - eyePosY, eyePosY - (blockPosY + 1)), 0);
        double dz = Math.max(Math.max(blockPosZ - eyePosZ, eyePosZ - (blockPosZ + 1)), 0);
        return (dx * dx + dy * dy + dz * dz) < (distance * distance);
        //#elseif MC >= 11900
        //$$ double distance = Math.max(getBlockInteractionRange(), 5) + additionalRange;
        //$$ double eyePosX = player.getX();
        //$$ double eyePosY = player.getEyeY();
        //$$ double eyePosZ = player.getZ();
        //$$ double dx = eyePosX - (blockPosX + 0.5);
        //$$ double dy = eyePosY - (blockPosY + 0.5);
        //$$ double dz = eyePosZ - (blockPosZ + 0.5);
        //$$ return (dx * dx + dy * dy + dz * dz) < (distance * distance);
        //#else
        //$$ // MC <= 1.18.2
        //$$ double distance = Math.max(getBlockInteractionRange(), 5) + additionalRange;
        //$$ double dx = player.getX() - (blockPosX + 0.5);
        //$$ double dy = player.getY() - (blockPosY + 0.5) + 1.5;
        //$$ double dz = player.getZ() - (blockPosZ + 0.5);
        //$$ return (dx * dx + dy * dy + dz * dz) < (distance * distance);
        //#endif
    }

    /**
     * 获取方块交互范围
     */
    public static double getBlockInteractionRange() {
        //#if MC>=12005
        if (MinecraftClient.getInstance().player != null) {
            return MinecraftClient.getInstance().player.getBlockInteractionRange();
        }
        //#else
        //$$ if (MinecraftClient.getInstance().interactionManager != null) {
        //$$    return MinecraftClient.getInstance().interactionManager.getReachDistance();
        //$$ }
        //#endif
        return 4.5F;
    }
}
