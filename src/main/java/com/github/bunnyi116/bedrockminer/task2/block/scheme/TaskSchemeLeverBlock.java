package com.github.bunnyi116.bedrockminer.task2.block.scheme;

import com.github.bunnyi116.bedrockminer.task2.block.TaskSchemeBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class TaskSchemeLeverBlock extends TaskSchemeBlock {
    public TaskSchemeLeverBlock(ClientWorld world, BlockPos pos, Direction direction, Direction facing) {
        super(world, pos, direction, facing);
    }

    @Override
    public boolean isNeedModifyLook() {
        return true;
    }

    public @Nullable Direction getBlockStateFacing() {
        if (this.getBlock() instanceof LeverBlock) {
            return switch (this.get(LeverBlock.FACE)) {
                case FLOOR -> Direction.DOWN;
                case WALL -> this.get(LeverBlock.FACING);
                case CEILING -> Direction.UP;
            };
        }
        return null;
    }
}
