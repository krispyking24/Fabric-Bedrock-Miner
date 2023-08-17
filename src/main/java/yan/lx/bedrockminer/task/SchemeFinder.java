package yan.lx.bedrockminer.task;

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
            BlockInfo[] pistons = findPistonPossible(direction, targetPos);
            for (BlockInfo piston : pistons) {
                BlockInfo[] redstoneTorches = findRedstoneTorchPossible(piston);
                for (BlockInfo redstoneTorch : redstoneTorches) {
                    BlockInfo slimeBlock = findSlimeBlockPossible(piston.direction, redstoneTorch);
                    schemes.add(new SchemeInfo(direction, piston, redstoneTorch, slimeBlock));
                }
            }
        }
        return schemes.toArray(SchemeInfo[]::new);
    }

    private static BlockInfo[] findPistonPossible(Direction direction, BlockPos targetPos) {
        List<BlockInfo> list = new ArrayList<>();
        BlockPos pos = targetPos.offset(direction);
        for (Direction facing : Direction.values()) {
            // 过滤朝着目标方块的方向
            if (direction == facing.getOpposite()) continue;
            int level = switch (facing) {
                case UP -> 0;
                case DOWN -> 1;
                case NORTH -> 2;
                case SOUTH -> 3;
                case WEST -> 4;
                case EAST -> 5;
            };
            list.add(new BlockInfo(direction, pos, facing, level));
        }
        return list.toArray(BlockInfo[]::new);
    }

    private static BlockInfo[] findRedstoneTorchPossible(BlockInfo pistonInfo) {
        List<BlockInfo> list = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            // 过滤与活塞臂退出的位置
            if (direction == pistonInfo.facing) continue;
            // 红石火把的方向集合, 因为红石火把没有倒着放, 所以去掉了他
            List<Direction> facings = List.of(Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
            BlockPos pos = pistonInfo.pos.offset(direction);
            // 活塞底部位置, 并且过滤活塞伸出方向位置
            for (Direction facing : facings) {
                if (direction == facing) continue;
                // 方案在上活塞底下是目标方块, 所以过滤
                if (direction == Direction.UP) continue;
                // 活塞朝下, 那么活塞下面就被活塞臂占位, 所以过滤
                if (pistonInfo.facing == Direction.DOWN) continue;
                int level = switch (facing) {
                    case UP -> 0;
                    case NORTH -> 1;
                    case SOUTH -> 2;
                    case WEST -> 3;
                    case EAST -> 4;
                    default -> throw new IllegalStateException("Unexpected value: " + facing);
                };
                list.add(new BlockInfo(direction, pos, facing, level));
            }
            // 常规位置
            for (Direction facing : facings) {
                // 过滤活塞测方向
                if (direction == facing) continue;
                // 过滤垂直方向
                if (direction.getAxis().isVertical()) continue;
                // 过滤红石火把附在活塞面上位置
                if (facing == pistonInfo.facing) continue;
                int level = switch (facing) {
                    case UP -> 0;
                    case NORTH -> 1;
                    case SOUTH -> 2;
                    case WEST -> 3;
                    case EAST -> 4;
                    default -> throw new IllegalStateException("Unexpected value: " + facing);
                };
                list.add(new BlockInfo(direction, pos, facing, level));
                list.add(new BlockInfo(direction, pos.up(), facing, level));

            }


        }
        return list.toArray(BlockInfo[]::new);
    }

    private static BlockInfo findSlimeBlockPossible(Direction direction, BlockInfo redstoneTorchInfo) {
        BlockPos pos = redstoneTorchInfo.pos;
        Direction facing = redstoneTorchInfo.facing;
        return new BlockInfo(direction, pos.offset(facing.getOpposite()), facing, facing == Direction.UP ? 0 : 1);
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
