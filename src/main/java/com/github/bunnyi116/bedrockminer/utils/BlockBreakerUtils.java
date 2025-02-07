package com.github.bunnyi116.bedrockminer.utils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static com.github.bunnyi116.bedrockminer.BedrockMiner.world;

public class BlockBreakerUtils {
    private static @Nullable Consumer<BlockPos> beforeBlockDestroyPacket;

    public static boolean updateBlockBreakingProgress(BlockPos blockPos) {
        return updateBlockBreakingProgress(blockPos, InteractionUtils.getClosestFace(blockPos));
    }

    private static boolean updateBlockBreakingProgress(BlockPos blockPos, Direction direction) {
        if (!InteractionUtils.isBlockWithinReach(blockPos, direction)) {
            return false;
        }
        ClientPlayerInteractionManagerUtils.updateBlockBreakingProgress(blockPos);
        return world.getBlockState(blockPos).isReplaceable();
    }

    private static void extracted(BlockPos blockPos) {
        if (beforeBlockDestroyPacket != null) {
            beforeBlockDestroyPacket.accept(blockPos);
            beforeBlockDestroyPacket = null;
        }
    }
}
