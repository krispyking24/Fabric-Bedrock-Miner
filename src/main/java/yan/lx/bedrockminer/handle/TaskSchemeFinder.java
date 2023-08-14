package yan.lx.bedrockminer.handle;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.model.TaskBlockInfo;
import yan.lx.bedrockminer.model.TaskInfo;
import yan.lx.bedrockminer.model.TaskSolutionInfo;

import java.util.ArrayList;
import java.util.List;

import static yan.lx.bedrockminer.model.TaskInfo.BaseBlock.*;

/**
 * 任务方案查找器
 */
public class TaskSchemeFinder {
    /**
     * 查找所有可能放置情况(假设性的，未检查游戏中的环境)
     *
     * @param target 目标方块
     */
    public static TaskSolutionInfo findAllPossible(TargetBlock target) {
        List<TaskBlockInfo> schemes = new ArrayList<>();
        // 遍历所有方向, 获取所有可能得方案 (未检验过, 初步计算)
        for (Direction direction : Direction.values()) {
            switch (direction) {
                case DOWN, UP, NORTH, SOUTH, WEST, EAST -> {
                    Piston[] pistons = findPistonPossible(direction, target);
                    for (Piston piston : pistons) {
                        RedstoneTorch[] redstoneTorches = findRedstoneTorchPossible(direction, piston);
                        for (RedstoneTorch redstoneTorch : redstoneTorches) {
                            BaseBlock baseBlock = findBaseBlockPossible(redstoneTorch);
                            schemes.add(new TaskBlockInfo(direction, target, piston, redstoneTorch, baseBlock));
                        }
                    }
                }
            }
        }
        var arr = schemes.toArray(TaskBlockInfo[]::new);
        return new TaskSolutionInfo(arr, null);
    }

    /**
     * 活塞相关
     */

    private static Piston[] findPistonPossible(Direction direction, TargetBlock target) {
        List<Piston> list = new ArrayList<>();
        // 遍历活塞所有可以放的朝向(因为技术问题, 实际可能只会采用向上朝向)
        for (Direction facing : Direction.values()) {
            // 过滤朝着目标方块的方向
            if (facing == direction.getOpposite()) {
                continue;
            }
            list.add(new Piston(target.pos.offset(facing), target.world));
        }
        return list.toArray(Piston[]::new);
    }


    /**
     * 红石火把相关
     */
    private static RedstoneTorch[] findRedstoneTorchPossible(Direction direction, Piston piston) {
        List<RedstoneTorch> list = new ArrayList<>();
        for (Direction offsetFacing : Direction.values()) {
            // 常规位置
            BlockPos pos = piston.pos.offset(offsetFacing);
            BlockPos posUp = pos.up();
            // 通过强充能活塞四周方块激活活塞(方案方向相反隔2格位置)
            BlockPos posOffset2 = pos.offset(direction.getOpposite(), 2);
            // 活塞底部位置(方案向上的话,活塞底下是目标方块,需要过滤且过滤活塞臂伸出位置)
            @Nullable BlockPos posDown = null;
            if (direction != Direction.UP && piston.facing != Direction.DOWN) {
                posDown = pos.down();
            }
            // 添加正放(Direction.Up)
            list.add(new RedstoneTorch(pos, piston.world));
            list.add(new RedstoneTorch(posUp, piston.world));
            list.add(new RedstoneTorch(posOffset2, piston.world));
            if (posDown != null) {
                list.add(new RedstoneTorch(posDown, piston.world));
            }
            // 添加侧放(无法精准放置)
            list.add(new RedstoneTorch(pos, offsetFacing.getOpposite(), piston.world));
            list.add(new RedstoneTorch(posUp, offsetFacing.getOpposite(), piston.world));
            list.add(new RedstoneTorch(posOffset2, offsetFacing.getOpposite(), piston.world));
            if (posDown != null) {
                list.add(new RedstoneTorch(posDown, offsetFacing.getOpposite(), piston.world));
            }
        }
        return list.toArray(RedstoneTorch[]::new);
    }

    private static BaseBlock findBaseBlockPossible(RedstoneTorch redstoneTorch) {
        BlockPos pos = redstoneTorch.pos.offset(redstoneTorch.facing);
        return new BaseBlock(pos, redstoneTorch.world);
    }
}
