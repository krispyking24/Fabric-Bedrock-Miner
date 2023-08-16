package yan.lx.bedrockminer.task;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import yan.lx.bedrockminer.model.BlockInfo;
import yan.lx.bedrockminer.model.SchemeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务方案查找器
 */
public class SchemeFinder {
    /**
     * 查找所有可能放置情况(假设性的，未检查游戏中的环境)
     */
    public static SchemeInfo[] findAllPossible(BlockPos targetPos) {
        List<SchemeInfo> schemes = new ArrayList<>();
        // 遍历所有方向, 获取所有可能得方案 (未检验过, 初步计算)
        for (Direction direction : Direction.values()) {
            switch (direction) {
                case DOWN, UP, NORTH, SOUTH, WEST, EAST -> {
                    BlockInfo[] pistons = findPistonPossible(direction, new BlockInfo(targetPos, direction));
                    for (BlockInfo piston : pistons) {
                        BlockInfo[] redstoneTorches = findRedstoneTorchPossible(direction, piston);
                        for (BlockInfo redstoneTorch : redstoneTorches) {
                            BlockInfo slimeBlock = findSlimeBlockPossible(redstoneTorch);
                            schemes.add(new SchemeInfo(piston, redstoneTorch, slimeBlock));
                        }
                    }
                }
            }
        }
        return schemes.toArray(SchemeInfo[]::new);
    }


    private static BlockInfo[] findPistonPossible(Direction direction, BlockInfo targetInfo) {
        List<BlockInfo> list = new ArrayList<>();
        // 遍历活塞所有可以放的朝向(因为技术问题, 实际可能只会采用向上朝向)
        for (Direction facing : Direction.values()) {
            // 过滤朝着目标方块的方向
            if (facing == direction.getOpposite()) {
                continue;
            }
            list.add(new BlockInfo(targetInfo.pos, facing));
        }
        return list.toArray(BlockInfo[]::new);
    }

    private static BlockInfo[] findRedstoneTorchPossible(Direction direction, BlockInfo pistonInfo) {
        List<BlockInfo> list = new ArrayList<>();
        for (Direction offsetFacing : Direction.values()) {
            // 常规位置1
            BlockPos pos = pistonInfo.pos.offset(offsetFacing);
            list.add(new BlockInfo(pos, offsetFacing.getOpposite()));
            list.add(new BlockInfo(pos, Direction.UP));
            // 常规位置2
            BlockPos posUp = pos.up();
            list.add(new BlockInfo(posUp, offsetFacing.getOpposite()));
            list.add(new BlockInfo(posUp, Direction.UP));
            // 活塞底部位置(方案向上的话,活塞底下是目标方块,需要过滤且过滤活塞臂伸出位置)
            if (direction != Direction.UP && pistonInfo.facing != Direction.DOWN) {
                BlockPos posDown = pos.down();
                list.add(new BlockInfo(posDown, Direction.UP));
                list.add(new BlockInfo(posDown, offsetFacing.getOpposite()));
            }
        }
        return list.toArray(BlockInfo[]::new);
    }

    private static BlockInfo findSlimeBlockPossible(BlockInfo redstoneTorchInfo) {
        BlockPos pos = redstoneTorchInfo.pos.offset(redstoneTorchInfo.facing);
        return new BlockInfo(pos, Direction.UP);
    }

    /**
     * 获取所有可以执行的方案
     */
    public static SchemeInfo[] getAllCanExecuteScheme(ClientWorld world, Block block, BlockPos pos, SchemeInfo[] schemeInfos) {
        //TODO: 待实现
        for (SchemeInfo schemeInfo : schemeInfos) {
            var piston = schemeInfo.piston;
            var redstoneTorch = schemeInfo.redstoneTorch;
            var slimeBlock = schemeInfo.slimeBlock;
        }
        return null;
    }

    /**
     * 获取所有可以执行的方案
     */
    public static SchemeInfo[] getOptimalSort(SchemeInfo[] schemeInfos) {
        //TODO: 待实现
        return null;
    }

    /**
     * 查找活塞附近的火把
     */
    public static BlockPos[] findPistonNearbyRedstoneTorch(BlockPos pistonPos, ClientWorld world) {
        List<BlockPos> list = new ArrayList<>();
        // 查找活塞2格范围内的红石火把, 之所以是2格, 是为了避免强充能方块边上被激活
        int range = 2;
        for (Direction direction : Direction.values()) {
            for (int i = 0; i < range; i++) {
                BlockPos pos = pistonPos.offset(direction);
                BlockState blockState = world.getBlockState(pos);
                if (blockState.isOf(Blocks.REDSTONE_BLOCK) || blockState.isOf(Blocks.REDSTONE_WALL_TORCH)) {
                    list.add(pos);
                }
            }
        }
        return list.toArray(BlockPos[]::new);
    }


}
