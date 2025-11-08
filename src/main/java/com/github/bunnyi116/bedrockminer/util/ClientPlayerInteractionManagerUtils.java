package com.github.bunnyi116.bedrockminer.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.github.bunnyi116.bedrockminer.BedrockMiner.*;

@Environment(EnvType.CLIENT)
public class ClientPlayerInteractionManagerUtils {
    public static final float BREAKING_PROGRESS_MAX = 0.7F;

    private static BlockPos currentBreakingPos = new BlockPos(-1, -1, -1);
    private static ItemStack selectedStack;
    private static float currentBreakingProgress;
    private static boolean breakingBlock;
    private static int lastSelectedSlot;
    private static int breakingTicks;
    private static int breakingTickMax;


    public static boolean canInteractWithBlockAt(BlockPos pos, double additionalRange) {
        double d = PlayerUtils.getBlockInteractionRange() + additionalRange;
        return (new Box(pos)).squaredMagnitude(player.getEyePos()) < d * d;
    }


    private static void syncSelectedSlot() {
        int i = PlayerInventoryUtils.getSelectedSlot();
        if (i != lastSelectedSlot) {
            lastSelectedSlot = i;
            networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(lastSelectedSlot));
        }
    }

    private static boolean isCurrentlyBreaking(BlockPos pos) {
        ItemStack itemStack = player.getMainHandStack();
        return pos.equals(currentBreakingPos) && ItemStack.areItemsAndComponentsEqual(itemStack, selectedStack);
    }

    private static int getBlockBreakingProgress() {
        return currentBreakingProgress > 0.0F ? (int) (currentBreakingProgress * 10.0F) : -1;
    }

    private static boolean attackBlock(BlockPos pos, Direction direction, @Nullable Runnable beforeBreaking, @Nullable Runnable afterBreaking) {
        if (player.isBlockBreakingRestricted(world, pos, gameMode)) {
            return false;
        } else if (!world.getWorldBorder().contains(pos)) {
            return false;
        } else {
            if (gameMode.isCreative()) {
                breakingBlock = true;
                sendSequencedPacket((sequence) -> {
                    interactionManager.breakBlock(pos);
                    return new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, direction, sequence);
                }, beforeBreaking, afterBreaking);
                breakingBlock = false;
            } else if (!breakingBlock || !isCurrentlyBreaking(pos)) {
                if (breakingBlock) {
                    networkHandler.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, currentBreakingPos, direction));
                    breakingBlock = false;
                }
                BlockState blockState = world.getBlockState(pos);
                var calcBlockBreakingDelta = blockState.calcBlockBreakingDelta(player, player.getEntityWorld(), pos);
                if (calcBlockBreakingDelta >= BREAKING_PROGRESS_MAX) {
                    breakingBlock = true;
                    sendSequencedPacket((sequence) -> {
                        if (!blockState.isAir()) {
                            interactionManager.breakBlock(pos);
                        }
                        return new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, direction, sequence);
                    }, beforeBreaking, afterBreaking);
                    breakingBlock = false;
                } else {
                    sendSequencedPacket((sequence) -> {
                        if (!blockState.isAir() && currentBreakingProgress == 0.0F) {
                            blockState.onBlockBreakStart(world, pos, player);
                        }
                        breakingBlock = true;
                        currentBreakingPos = pos;
                        selectedStack = player.getMainHandStack();
                        currentBreakingProgress = 0.0F;
                        world.setBlockBreakingInfo(player.getId(), currentBreakingPos, getBlockBreakingProgress());
                        return new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, direction, sequence);
                    });
                }
            }
            ++breakingTickMax;
            return true;
        }
    }

    public static boolean updateBlockBreakingProgress(BlockPos pos, Direction direction, @Nullable Runnable beforeBreaking, @Nullable Runnable afterBreaking) {
        syncSelectedSlot();
        if (gameMode.isCreative() && world.getWorldBorder().contains(pos)) {
            breakingBlock = true;
            sendSequencedPacket((sequence) -> {
                interactionManager.breakBlock(pos);
                return new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, direction, sequence);
            }, beforeBreaking, afterBreaking);
            breakingBlock = false;
            ++breakingTickMax;
            return true;
        } else if (breakingBlock && isCurrentlyBreaking(pos)) {
            BlockState blockState = world.getBlockState(pos);
            if (blockState.isAir()) {
                breakingBlock = false;
                return false;
            } else {
                breakingBlock = true;
                currentBreakingProgress += blockState.calcBlockBreakingDelta(player, player.getEntityWorld(), pos);
                if (currentBreakingProgress >= BREAKING_PROGRESS_MAX) {
                    sendSequencedPacket((sequence) -> {
                        interactionManager.breakBlock(pos);
                        return new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, direction, sequence);
                    }, beforeBreaking, afterBreaking);
                    currentBreakingProgress = 0.0F;
                    breakingBlock = false;
                }
                world.setBlockBreakingInfo(player.getId(), currentBreakingPos, getBlockBreakingProgress());
                ++breakingTickMax;
                return true;
            }
        } else {
            return attackBlock(pos, direction, beforeBreaking, afterBreaking);
        }
    }

    public static void updateBlockBreakingProgress(BlockPos pos) {
        updateBlockBreakingProgress(pos, getClosestFace(pos), null, null);
    }

    public static void sendSequencedPacket(SequencedPacketCreator packetCreator, @Nullable Runnable beforeSending, @Nullable Runnable afterSending) {
        try (PendingUpdateManager pendingUpdateManager = world.getPendingUpdateManager().incrementSequence()) {
            int i = pendingUpdateManager.getSequence();
            Packet<ServerPlayPacketListener> packet = packetCreator.predict(i);
            if (beforeSending != null) {
                beforeSending.run();
            }
            networkHandler.sendPacket(packet);
            if (afterSending != null) {
                afterSending.run();
            }
        }
    }

    public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        sendSequencedPacket(packetCreator, null, null);
    }

    public static Direction getClosestFace(BlockPos targetPos) {
        Vec3d playerPos = player.getEyePos();
        Vec3d targetCenterPos = targetPos.toCenterPos();
        Direction closestFace = null;
        double closestDistanceSquared = Double.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            double offsetX = direction.getOffsetX() * 0.5;
            double offsetY = direction.getOffsetY() * 0.5;
            double offsetZ = direction.getOffsetZ() * 0.5;
            Vec3d facePos = targetCenterPos.add(offsetX, offsetY, offsetZ);
            double distanceSquared = playerPos.squaredDistanceTo(facePos);
            // 更新最近的面
            if (distanceSquared < closestDistanceSquared) {
                closestDistanceSquared = distanceSquared;
                closestFace = direction;
            }
        }
        return closestFace;
    }

    private static List<Vec3d> getFacePoints(Vec3d center, Direction side) {
        List<Vec3d> points = new ArrayList<>();
        double halfSize = 0.5; // 方块的一半边长
        // 获取偏移方向
        double offsetX = side.getOffsetX() * halfSize;
        double offsetY = side.getOffsetY() * halfSize;
        double offsetZ = side.getOffsetZ() * halfSize;
        // 面的中心点
        Vec3d faceCenter = center.add(offsetX, offsetY, offsetZ);
        points.add(faceCenter);
        // 面的四个角
        if (side.getAxis() == Direction.Axis.Y) { // 顶部/底部面
            points.add(faceCenter.add(halfSize, 0, halfSize));
            points.add(faceCenter.add(halfSize, 0, -halfSize));
            points.add(faceCenter.add(-halfSize, 0, halfSize));
            points.add(faceCenter.add(-halfSize, 0, -halfSize));
        } else if (side.getAxis() == Direction.Axis.X) { // 左/右面
            points.add(faceCenter.add(0, halfSize, halfSize));
            points.add(faceCenter.add(0, halfSize, -halfSize));
            points.add(faceCenter.add(0, -halfSize, halfSize));
            points.add(faceCenter.add(0, -halfSize, -halfSize));
        } else if (side.getAxis() == Direction.Axis.Z) { // 前/后面
            points.add(faceCenter.add(halfSize, halfSize, 0));
            points.add(faceCenter.add(halfSize, -halfSize, 0));
            points.add(faceCenter.add(-halfSize, halfSize, 0));
            points.add(faceCenter.add(-halfSize, -halfSize, 0));
        }
        return points;
    }

    public static void resetBreaking() {
        breakingTicks = 0;
        breakingTickMax = 200;
        breakingBlock = false;
    }

    public static void autoResetBreaking() {
        if (!breakingBlock && breakingTicks > 0) {  // 如果未在破坏, 但是破坏TICK已有累计, 先进行初始化
            resetBreaking();
        }
        if (breakingBlock && breakingTicks++ > breakingTickMax) {
            resetBreaking();
        }
    }

    public static boolean isBreakingBlock() {
        return breakingBlock;
    }
}
