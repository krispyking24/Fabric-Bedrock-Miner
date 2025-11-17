package com.github.bunnyi116.bedrockminer.util;

import com.github.bunnyi116.bedrockminer.util.network.NetworkUtils;
import com.github.bunnyi116.bedrockminer.util.player.PlayerUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import static com.github.bunnyi116.bedrockminer.BedrockMiner.*;
import static net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.*;

@Environment(EnvType.CLIENT)
public class ClientPlayerInteractionManagerUtils {
    public static final float BREAKING_PROGRESS_MAX = 0.7F;
    private static boolean breakingBlock;
    private static int breakingTicks;
    private static int breakingTickMax;


    public static boolean attackBlock(BlockPos pos, Direction direction, @Nullable Runnable beforeBreaking, @Nullable Runnable afterBreaking) {
        interactionManager.ensureHasSentCarriedItem();

        if (player.blockActionRestricted(world, pos, gameMode)) {
            return false;
        }
        if (!world.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        if (gameMode.isCreative()) {
            setBreakingBlock(true);
            NetworkUtils.sendSequencedPacket((sequence) -> {
                interactionManager.destroyBlock(pos);
                return new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, pos, direction, sequence);
            }, beforeBreaking, afterBreaking);
            setBreakingBlock(false);
        } else if (!(breakingBlock || interactionManager.isDestroying) || !interactionManager.sameDestroyTarget(pos)) {
            if ((breakingBlock || interactionManager.isDestroying)) {
                NetworkUtils.sendPacket(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, interactionManager.destroyBlockPos, direction));
                setBreakingBlock(false);
            }
            BlockState blockState = world.getBlockState(pos);
            float calcBlockBreakingDelta = PlayerUtils.calcBlockBreakingDelta(blockState);
            if (calcBlockBreakingDelta >= BREAKING_PROGRESS_MAX) {
                setBreakingBlock(true);
                NetworkUtils.sendSequencedPacket((sequence) -> {
                    if (!blockState.isAir()) {
                        interactionManager.destroyBlock(pos);
                    }
                    return new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, pos, direction, sequence);
                }, beforeBreaking, afterBreaking);
                setBreakingBlock(false);
            } else {
                NetworkUtils.sendSequencedPacket((sequence) -> {
                    if (!blockState.isAir() && interactionManager.destroyProgress == 0.0F) {
                        blockState.attack(world, pos, player);
                    }
                    setBreakingBlock(true);
                    interactionManager.destroyBlockPos = pos;
                    interactionManager.destroyingItem = player.getMainHandItem();
                    interactionManager.destroyProgress = 0.0F;
                    world.destroyBlockProgress(player.getId(), interactionManager.destroyBlockPos, getBlockBreakingProgress());
                    return new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, pos, direction, sequence);
                });
            }
        }
        ++breakingTickMax;
        return true;
    }

    public static boolean attackBlock(BlockPos pos) {
        return attackBlock(pos, PlayerUtils.getClosestFace(pos), null, null);
    }

    public static boolean updateBlockBreakingProgress(BlockPos pos, Direction direction, @Nullable Runnable beforeBreaking, @Nullable Runnable afterBreaking) {
        interactionManager.ensureHasSentCarriedItem();
        if (gameMode.isCreative() && world.getWorldBorder().isWithinBounds(pos)) {
            setBreakingBlock(true);
            NetworkUtils.sendSequencedPacket((sequence) -> {
                interactionManager.destroyBlock(pos);
                return new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, pos, direction, sequence);
            }, beforeBreaking, afterBreaking);
            setBreakingBlock(false);
            ++breakingTickMax;
            return true;
        }
        if ((breakingBlock || interactionManager.isDestroying()) && interactionManager.sameDestroyTarget(pos)) {
            BlockState blockState = world.getBlockState(pos);
            if (blockState.isAir()) {
                setBreakingBlock(false);
                return false;
            }
            setBreakingBlock(true);
            interactionManager.destroyProgress += PlayerUtils.calcBlockBreakingDelta(blockState);
            if (interactionManager.destroyProgress >= BREAKING_PROGRESS_MAX) {
                NetworkUtils.sendSequencedPacket((sequence) -> {
                    interactionManager.destroyBlock(pos);
                    return new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, pos, direction, sequence);
                }, beforeBreaking, afterBreaking);
                interactionManager.destroyProgress = 0.0F;
                setBreakingBlock(false);
            }
            world.destroyBlockProgress(player.getId(), interactionManager.destroyBlockPos, getBlockBreakingProgress());
            ++breakingTickMax;
            return true;
        }
        return attackBlock(pos, direction, beforeBreaking, afterBreaking);
    }


    public static void updateBlockBreakingProgress(BlockPos pos) {
        updateBlockBreakingProgress(pos, PlayerUtils.getClosestFace(pos), null, null);
    }

    public static int getBlockBreakingProgress() {
        return interactionManager.destroyProgress > 0.0F ? (int) (interactionManager.destroyProgress * 10.0F) : -1;
    }

    public static void resetBreaking() {
        breakingTicks = 0;
        breakingTickMax = 200;
        setBreakingBlock(false);
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

    public static void setBreakingBlock(boolean breakingBlock) {
        ClientPlayerInteractionManagerUtils.breakingBlock = breakingBlock;
        interactionManager.isDestroying = breakingBlock;
    }
}
