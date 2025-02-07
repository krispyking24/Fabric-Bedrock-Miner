package com.github.bunnyi116.bedrockminer.task2.block.scheme;

import net.minecraft.block.Block;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import com.github.bunnyi116.bedrockminer.task2.block.TaskSchemeBlock;

public class TaskSchemeBaseBlockBlock extends TaskSchemeBlock {
    public TaskSchemeBaseBlockBlock(ClientWorld world, BlockPos pos, Direction direction, Direction facing) {
        super(world, pos, direction, facing);
    }

    public boolean sideCoversSmallSquare() {
        return Block.sideCoversSmallSquare(this.world, this.pos, this.facing);
    }

    @Override
    public boolean isNeedModifyLook() {
        return false;
    }
}
