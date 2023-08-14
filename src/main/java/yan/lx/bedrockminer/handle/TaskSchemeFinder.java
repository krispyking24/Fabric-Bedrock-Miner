package yan.lx.bedrockminer.handle;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.model.TaskSchemeInfo;
import yan.lx.bedrockminer.model.TaskSolutionInfo;

import java.util.ArrayList;
import java.util.List;

import static yan.lx.bedrockminer.model.TaskBlockInfo.*;

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
        List<TaskSchemeInfo> schemes = new ArrayList<>();
        // 遍历所有方向, 获取所有可能得方案 (未检验过, 初步计算)
        for (Direction direction : Direction.values()) {
            switch (direction) {
                case DOWN, UP, NORTH, SOUTH, WEST, EAST -> {
                    Piston[] pistons = findPistonPossible(direction, target);
                    for (Piston piston : pistons) {
                        RedstoneTorch[] redstoneTorches = findRedstoneTorchPossible(direction, piston);
                        for (RedstoneTorch redstoneTorch : redstoneTorches) {
                            BaseBlock baseBlock = findBaseBlockPossible(redstoneTorch);
                            schemes.add(new TaskSchemeInfo(direction, target, piston, redstoneTorch, baseBlock));
                        }
                    }
                }
            }
        }
        var arr = schemes.toArray(TaskSchemeInfo[]::new);
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

    /**
     * 基础方块相关(粘液块)
     *
     * @param redstoneTorch 红石火把
     */
    private static BaseBlock findBaseBlockPossible(RedstoneTorch redstoneTorch) {
        BlockPos pos = redstoneTorch.pos.offset(redstoneTorch.facing);
        return new BaseBlock(pos, redstoneTorch.world);
    }

    /**
     * 获取所有可以执行的方案
     *
     * @param solutionInfo 解决方案
     */
    public static TaskSchemeInfo[] getAllCanExecuteScheme(TaskSolutionInfo solutionInfo) {
        //TODO: 待实现
//        for (TaskSchemeInfo schemeInfo : solutionInfo.schemes) {
//            ClientWorld world = schemeInfo.target.world;
//            TargetBlock target = schemeInfo.target;
//            Piston piston = schemeInfo.piston;
//            RedstoneTorch redstoneTorch = schemeInfo.redstoneTorch;
//            BaseBlock baseBlock = schemeInfo.baseblock;
//            // 过滤活塞
//            if (!piston.isReplaceable() || !piston.isReplaceable()) continue;
//
//            if (!world.getBlockState(piston.pos.offset(piston.facing)).isReplaceable()) continue;
//
//            if ((piston.isOf(Blocks.PISTON) || piston.isOf(Blocks.STICKY_PISTON)) && piston.facing != piston.getFacing()) {
//                continue;
//            }
//            // 过滤红石火把
//            if (!redstoneTorch.isReplaceable() || !(piston.isOf(Blocks.REDSTONE_TORCH) || piston.isOf(Blocks.REDSTONE_WALL_TORCH))) {
//                continue;
//            }
//            if (piston.isOf(Blocks.REDSTONE_TORCH) || piston.isOf(Blocks.REDSTONE_WALL_TORCH) || redstoneTorch.facing != redstoneTorch.getFacing()) {
//                continue;
//            }
//        }
        return null;
    }

    /**
     * 获取所有可以执行的方案
     *
     * @param schemes 解决方案
     */
    public static TaskSchemeInfo[] getOptimalSort(TaskSolutionInfo schemes) {
        //TODO: 待实现
        return null;
    }

    /**
     * 查找活塞附近的火把
     *
     * @param piston 活塞方块信息
     */
    public static BlockPos[] findPistonNearbyRedstoneTorch(Piston piston) {
        List<BlockPos> list = new ArrayList<>();
        // 查找活塞2格范围内的红石火把, 之所以是2格, 是为了避免强充能方块边上被激活
        int range = 2;
        for (Direction direction : Direction.values()) {
            for (int i = 0; i < range; i++) {
                BlockPos pos = piston.pos.offset(direction);
                BlockState blockState = piston.world.getBlockState(pos);
                if (blockState.isOf(Blocks.REDSTONE_BLOCK) || blockState.isOf(Blocks.REDSTONE_WALL_TORCH)) {
                    list.add(pos);
                }
            }
        }
        return list.toArray(BlockPos[]::new);
    }
}
