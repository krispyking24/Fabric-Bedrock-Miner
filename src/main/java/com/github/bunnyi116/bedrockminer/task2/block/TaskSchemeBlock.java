package com.github.bunnyi116.bedrockminer.task2.block;

import com.github.bunnyi116.bedrockminer.task2.TaskBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public abstract class TaskSchemeBlock extends TaskBlock {
    public final Direction direction;
    public final Direction facing;

    public TaskSchemeBlock(ClientWorld world, BlockPos pos, Direction direction, Direction facing) {
        super(world, pos);
        this.direction = direction;
        this.facing = facing;
    }

    public boolean isNeedModifyLook() {
        return this.facing.getAxis().isHorizontal();
    }
}
