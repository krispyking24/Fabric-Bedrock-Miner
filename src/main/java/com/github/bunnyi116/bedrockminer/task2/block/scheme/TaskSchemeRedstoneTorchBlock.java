package com.github.bunnyi116.bedrockminer.task2.block.scheme;

import com.github.bunnyi116.bedrockminer.task2.block.TaskSchemeBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.WallRedstoneTorchBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class TaskSchemeRedstoneTorchBlock extends TaskSchemeBlock {
    public TaskSchemeRedstoneTorchBlock(ClientWorld world, BlockPos pos, Direction direction, Direction facing) {
        super(world, pos, direction, facing);
    }

    @Override
    public boolean isNeedModifyLook() {
        return true;
    }

    public @Nullable Direction getBlockStateFacing() {
        if (this.getBlock() instanceof WallRedstoneTorchBlock) {
            return this.get(WallRedstoneTorchBlock.FACING);
        }
        if (this.getBlock() instanceof RedstoneTorchBlock) {
            return Direction.UP;
        }
        return null;
    }
}
