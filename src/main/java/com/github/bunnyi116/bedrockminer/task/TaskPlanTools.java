package com.github.bunnyi116.bedrockminer.task;

import com.github.bunnyi116.bedrockminer.config.Config;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 任务方案查找器
 */
public class TaskPlanTools {

    public static TaskPlanItem[] findAllPossible(BlockPos targetPos, ClientWorld world) {
        final var schemes = new ArrayList<TaskPlanItem>();
        for (Direction direction : Config.INSTANCE.pistonDirections) {
            final var pistons = findPistonPossible(direction, targetPos);
            for (TaskPlan piston : pistons) {
                final var redstoneTorches = findRedstoneTorchPossible(direction, targetPos, piston);
                for (TaskPlan redstoneTorch : redstoneTorches) {
                    final var slimeBlock = findSlimeBlockPossible(redstoneTorch);
                    schemes.add(new TaskPlanItem(direction, piston, redstoneTorch, slimeBlock));
                }
            }
        }
        // 重新排序
        schemes.sort(Comparator
                .comparingInt((TaskPlanItem scheme) -> scheme.piston.level + scheme.redstoneTorch.level)
        );
        return schemes.toArray(TaskPlanItem[]::new);
    }

    private static TaskPlan[] findPistonPossible(Direction direction, BlockPos targetPos) {
        final var list = new ArrayList<TaskPlan>();
        final var pistonPos = targetPos.offset(direction);
        for (Direction pistonFacing : Config.INSTANCE.pistonFacings) {
            // 活塞臂在目标方块位置
            final var pistonHeadPos = pistonPos.offset(pistonFacing);
            if (pistonHeadPos.equals(targetPos))
                continue;
            int level = switch (pistonFacing) {
                case UP -> 0;
                case DOWN -> 1;
                case NORTH, SOUTH, WEST, EAST -> 2;
            };
            list.add(new TaskPlan(pistonPos, pistonFacing, level));
        }
        return list.toArray(TaskPlan[]::new);
    }

    private static TaskPlan[] findRedstoneTorchPossible(Direction direction, BlockPos targetPos, TaskPlan pistonInfo) {
        final var list = new ArrayList<TaskPlan>();
        final var pistonHeadPos = pistonInfo.pos.offset(pistonInfo.facing);

        // 活塞在目标方块上方，红石火把通过在目标方块下方，充能目标方块激活活塞
        if (direction == Direction.UP) {
            final var redstoneTorchPos = targetPos.offset(direction.getOpposite());
            for (Direction redstoneTorchFacing : Config.INSTANCE.redstoneTorchFacings) {
                final var basePos = redstoneTorchPos.offset(redstoneTorchFacing.getOpposite());
                if (basePos.equals(pistonInfo.pos) || basePos.equals(pistonHeadPos))
                    continue;

                // 红石火把无法倒置
                if (redstoneTorchFacing == Direction.DOWN)
                    continue;

                // 设置排序等级
                int level = switch (redstoneTorchFacing) {
                    case UP -> 0;
                    case NORTH, SOUTH, WEST, EAST -> 2;
                    default -> throw new IllegalStateException("Unexpected value: " + redstoneTorchFacing);
                };
                // 添加到方案
                list.add(new TaskPlan(1, redstoneTorchPos, redstoneTorchFacing, level));
            }
        }

        for (Direction redstoneTorchDirection : Config.INSTANCE.redstoneTorchDirections) {
            final var redstoneTorchPos = pistonInfo.pos.offset(redstoneTorchDirection);
            // 红石火把位置与活塞臂伸出的位置重叠
            if (pistonHeadPos.equals(redstoneTorchPos))
                continue;

            // 常规位置
            for (Direction redstoneTorchFacing : Config.INSTANCE.redstoneTorchFacings) {
                final var basePos = redstoneTorchPos.offset(redstoneTorchFacing.getOpposite());

                // 过滤红石火把附在活塞上位置
                if (basePos.equals(pistonInfo.pos) || basePos.equals(pistonHeadPos))
                    continue;

                // 红石火把无法倒置
                if (redstoneTorchFacing == Direction.DOWN)
                    continue;

                // 设置排序等级
                int level = switch (redstoneTorchFacing) {
                    case UP -> 0;
                    case NORTH, SOUTH, WEST, EAST -> 2;
                    default -> throw new IllegalStateException("Unexpected value: " + redstoneTorchFacing);
                };

                // 添加到方案
                if (!redstoneTorchPos.equals(targetPos)) {
                    list.add(new TaskPlan(redstoneTorchPos, redstoneTorchFacing, level));
                }

                var redstoneTorchPosUp = redstoneTorchPos.up();
                if (!redstoneTorchPosUp.equals(targetPos) && !redstoneTorchPosUp.equals(pistonInfo.pos)) {
                    // 过滤红石火把附在活塞上位置
                    final var baseUpPos = redstoneTorchPos.offset(redstoneTorchFacing.getOpposite());
                    if (baseUpPos.equals(pistonInfo.pos) || baseUpPos.equals(pistonHeadPos))
                        continue;
                    list.add(new TaskPlan(redstoneTorchPos.up(), redstoneTorchFacing, level + 1));
                }
            }
        }
        return list.toArray(TaskPlan[]::new);
    }

    private static TaskPlan findSlimeBlockPossible(TaskPlan redstoneTorchInfo) {
        final var pos = redstoneTorchInfo.pos;
        final var facing = redstoneTorchInfo.facing;
        return new TaskPlan(pos.offset(facing.getOpposite()), facing, facing.getAxis().isVertical() ? 0 : 1);
    }

    // 查找活塞附近的火把
    public static BlockPos[] findPistonNearbyRedstoneTorch(BlockPos pistonPos, ClientWorld world) {
        List<BlockPos> list = new ArrayList<>();
        // 查找活塞2格范围内的红石火把, 之所以是2格, 是为了避免强充能方块边上被激活
        int range = 2;
        for (Direction direction : Direction.values()) {
            for (int i = 0; i < range; i++) {
                BlockPos pos = pistonPos.offset(direction, i);
                BlockState blockState = world.getBlockState(pos);
                if (blockState.isOf(Blocks.REDSTONE_TORCH) || blockState.isOf(Blocks.REDSTONE_WALL_TORCH)) {
                    list.add(pos);
                }
            }
        }
        return list.toArray(BlockPos[]::new);
    }

}
