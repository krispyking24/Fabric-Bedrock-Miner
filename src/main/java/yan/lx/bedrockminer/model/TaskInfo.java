package yan.lx.bedrockminer.model;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.Nullable;

/**
 * 方块基础信息
 */
public class TaskInfo {
    /**
     * 方块本体
     */
    public final Block block;
    /**
     * 方块位置
     */
    public final BlockPos pos;

    /**
     * 方块所在世界
     */
    public final ClientWorld world;

    /**
     * 方块的朝向
     */
    public @Nullable Direction facing = null;

    /**
     * 完整的构造器
     *
     * @param block  方块本体
     * @param pos    方块位置
     * @param world  方块所在世界
     * @param facing 方块的朝向
     */
    public TaskInfo(Block block, BlockPos pos, @Nullable Direction facing, ClientWorld world) {
        this.block = block;
        this.pos = pos;
        this.world = world;
        this.facing = facing;
    }

    public TaskInfo(Block block, BlockPos pos, ClientWorld world) {
        this(block, pos, null, world);
    }

    /**
     * 目标方块
     */
    public static class TargetBlock extends TaskInfo {
        public TargetBlock(Block block, BlockPos pos, ClientWorld world) {
            super(block, pos, world);
        }
    }

    /**
     * 获取当前方块在世界方块位置中的状态
     */
    public BlockState getBlockState() {
        return world.getBlockState(pos);
    }

    /**
     * 在世界方块位置上是否与当前方块相同
     */
    public boolean isOf(Block block) {
        return getBlockState().isOf(block);
    }

    /**
     * 当前方块在世界方块位置上是否为空气
     */
    public boolean isAir() {
        return getBlockState().isAir();
    }

    /**
     * 当前当前方块在世界方块位置上是否为可以被替换
     */
    public boolean isReplaceable() {
        return getBlockState().isReplaceable();
    }

    /**
     * 获取当前方块在世界方块位置中的朝向
     */
    public @Nullable Direction getFacing() {
        BlockState blockState = getBlockState();
        if (blockState.contains(Properties.FACING)) {
            return blockState.get(Properties.FACING);
        }
        return null;
    }

    /**
     * 获得曼哈顿距离
     *
     * @param vec 另一点的三维坐标（三维向量）
     */
    public int getManhattanDistance(Vec3i vec) {
        return pos.getManhattanDistance(vec);
    }

    /**
     * 获取平方距离（直线距离）
     *
     * @param vec 另一点的三维坐标（三维向量）
     */
    public double getSquaredDistance(Vec3i vec) {
        return pos.getSquaredDistance(vec);
    }

    /**
     * 是否在范围内
     *
     * @param vec      另一点的三维坐标（三维向量）
     * @param distance 判断是否在范围的数值
     */
    public boolean isWithinDistance(Vec3i vec, double distance) {
        return getSquaredDistance(vec) < MathHelper.square(distance);
    }

    /**
     * 当前朝向是否与另一个朝向一致
     *
     * @param facing 朝向
     */
    public boolean isFacingConsistent(Direction facing) {
        return this.facing == facing;
    }

    /**
     * 当前朝向是否与世界中的方块朝向一致
     */
    public boolean isFacingConsistent() {
        return isFacingConsistent(getFacing());
    }

    /**
     * 活塞
     */
    public static class Piston extends TaskInfo {
        protected final boolean sticky;

        public Piston(boolean sticky, BlockPos pos, @Nullable Direction facing, ClientWorld world) {
            super(sticky ? Blocks.STICKY_PISTON : Blocks.PISTON, pos, facing, world);
            this.sticky = sticky;
        }

        public Piston(boolean sticky, BlockPos pos, ClientWorld world) {
            this(sticky, pos, null, world);
        }

        public Piston(BlockPos pos, @Nullable Direction direction, ClientWorld world) {
            this(false, pos, direction, world);
        }

        public Piston(BlockPos pos, ClientWorld world) {
            this(false, pos, null, world);
        }

        public boolean isSticky() {
            return sticky;
        }
    }

    /**
     * 红石火把
     */
    public static class RedstoneTorch extends TaskInfo {
        protected final boolean wall;

        protected RedstoneTorch(boolean wall, BlockPos pos, @Nullable Direction facing, ClientWorld world) {
            super(wall ? Blocks.REDSTONE_WALL_TORCH : Blocks.REDSTONE_TORCH, pos, facing, world);
            this.wall = wall;
        }

        public RedstoneTorch(BlockPos pos, Direction facing, ClientWorld world) {
            this(true, pos, facing, world);
        }

        public RedstoneTorch(BlockPos pos, ClientWorld world) {
            this(false, pos, null, world);
        }

        public boolean isWall() {
            return wall;
        }
    }

    /**
     * 基座方块（建议使用粘液块）
     */
    public static class BaseBlock extends TaskInfo {
        protected BaseBlock(Block block, BlockPos pos, @Nullable Direction facing, ClientWorld world) {
            super(block, pos, facing, world);
        }

        protected BaseBlock(Block block, BlockPos pos, ClientWorld world) {
            this(block, pos, null, world);
        }

        public BaseBlock(BlockPos pos, ClientWorld world) {
            this(Blocks.SLIME_BLOCK, pos, world);
        }
    }
}
