package com.github.bunnyi116.bedrockminer.task2.block.scheme;

import net.minecraft.block.PistonBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import com.github.bunnyi116.bedrockminer.task2.block.TaskSchemeBlock;

public class TaskSchemePistonBlock extends TaskSchemeBlock {
    public TaskSchemePistonBlock(ClientWorld world, BlockPos pos, Direction direction, Direction facing) {
        super(world, pos, direction, facing);
    }

    public @Nullable Direction getBlockStateFacing() {
        if (this.getBlock() instanceof PistonBlock) {
            return this.get(PistonBlock.FACING);
        }
        return null;
    }
}
