package com.github.bunnyi116.bedrockminer.task2;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import com.github.bunnyi116.bedrockminer.task2.block.TaskTargetBlock;
import com.github.bunnyi116.bedrockminer.task2.block.scheme.TaskSchemeBaseBlockBlock;
import com.github.bunnyi116.bedrockminer.task2.block.scheme.TaskSchemePistonBlock;
import com.github.bunnyi116.bedrockminer.task2.block.scheme.TaskSchemeRedstoneTorchBlock;

import java.util.ArrayList;
import java.util.List;

public class Finder {
    public final static Direction[] DEFAULT_PISTON_DIRECTIONS = new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    public final static Direction[] DEFAULT_PISTON_FACINGS = new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public final static Direction[] DEFAULT_REDSTONE_TORCH_DIRECTIONS = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN};
    public final static Direction[] DEFAULT_REDSTONE_TORCH_FACINGS = new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public static TaskSchemePistonBlock[] findPiston(TaskTargetBlock targetBlock) {
        List<TaskSchemePistonBlock> pistons = new ArrayList<>();
        for (Direction pistonDirection : DEFAULT_PISTON_DIRECTIONS) {
            final BlockPos pistonPos = targetBlock.offset(pistonDirection);
            for (Direction pistonFacing : DEFAULT_PISTON_FACINGS) {
                final BlockPos pistonHeadPos = pistonPos.offset(pistonFacing);
                if (pistonHeadPos.equals(targetBlock.pos)) continue;    // 活塞头在目标方块位置
                pistons.add(new TaskSchemePistonBlock(targetBlock.world, pistonPos, pistonDirection, pistonFacing));
            }
        }
        return pistons.toArray(new TaskSchemePistonBlock[0]);
    }


    public static TaskSchemeRedstoneTorchBlock[] findRedstoneTorch(TaskTargetBlock target, TaskSchemePistonBlock piston) {
        List<TaskSchemeRedstoneTorchBlock> redstoneTorchs = new ArrayList<>();
        for (Direction redstoneTorchDirection : DEFAULT_REDSTONE_TORCH_DIRECTIONS) {
            if (redstoneTorchDirection.getAxis().isVertical()) continue;
            final BlockPos redstoneTorchPos = piston.offset(redstoneTorchDirection).up();
            if (redstoneTorchPos.equals(target.pos)) continue;  // 红石火把在目标方块位置
            if (redstoneTorchPos.equals(piston.offset(piston.facing))) continue;  // 红石火把在活塞头位置
            for (Direction redstoneTorchFacing : DEFAULT_REDSTONE_TORCH_FACINGS) {
                if (redstoneTorchDirection == Direction.UP && redstoneTorchFacing == Direction.UP) continue;    // 活塞上方
                if (redstoneTorchFacing == Direction.DOWN) continue;  // 红石火把不能倒放
                // 红石火把底座预检查
                BlockPos basePos = redstoneTorchPos.offset(redstoneTorchFacing.getOpposite());
                if (basePos.equals(piston.pos)) continue;  // 底座是活塞
                redstoneTorchs.add(new TaskSchemeRedstoneTorchBlock(piston.world, redstoneTorchPos, redstoneTorchDirection, redstoneTorchFacing));
            }
        }
        for (Direction redstoneTorchDirection : DEFAULT_REDSTONE_TORCH_DIRECTIONS) {
            final BlockPos redstoneTorchPos = piston.offset(redstoneTorchDirection);
            if (redstoneTorchPos.equals(target.pos)) continue;  // 红石火把在目标方块位置
            if (redstoneTorchPos.equals(piston.offset(piston.facing))) continue;  // 红石火把在活塞头位置
            for (Direction redstoneTorchFacing : DEFAULT_REDSTONE_TORCH_FACINGS) {
                if (redstoneTorchDirection == Direction.UP && redstoneTorchFacing == Direction.UP) continue;    // 活塞上方
                if (redstoneTorchFacing == Direction.DOWN) continue;  // 红石火把不能倒放
                // 红石火把底座预检查
                BlockPos basePos = redstoneTorchPos.offset(redstoneTorchFacing.getOpposite());
                if (basePos.equals(piston.pos)) continue;  // 底座是活塞
                redstoneTorchs.add(new TaskSchemeRedstoneTorchBlock(piston.world, redstoneTorchPos, redstoneTorchDirection, redstoneTorchFacing));
            }
        }
        return redstoneTorchs.toArray(new TaskSchemeRedstoneTorchBlock[0]);
    }

    public static TaskSchemeBaseBlockBlock findRedstoneTorchBaseBlock(TaskSchemeRedstoneTorchBlock redstoneTorch) {
        BlockPos basePos = redstoneTorch.offset(redstoneTorch.facing.getOpposite());
        return new TaskSchemeBaseBlockBlock(redstoneTorch.world, basePos, redstoneTorch.direction, redstoneTorch.facing);
    }
}
