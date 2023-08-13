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
     * 构造器
     *
     * @param block 方块本体
     * @param pos   方块位置
     * @param world 方块所在世界
     */
    public TaskInfo(Block block, BlockPos pos, ClientWorld world) {
        this.block = block;
        this.pos = pos;
        this.world = world;
    }

    /**
     * 目标方块
     */
    public static class TargetTask extends TaskInfo {
        public TargetTask(Block block, BlockPos pos, ClientWorld world) {
            super(block, pos, world);
        }
    }

    /**
     * 活塞
     */
    public static class Piston extends TaskInfo {
        public Piston(boolean sticky, BlockPos pos, ClientWorld world) {
            super(sticky ? Blocks.STICKY_PISTON : Blocks.PISTON, pos, world);
        }

        public Piston(BlockPos pos, ClientWorld world) {
            this(false, pos, world);
        }
    }

    /**
     * 红石火把
     */
    public static class RedstoneTorch extends TaskInfo {
        public RedstoneTorch(BlockPos pos, ClientWorld world) {
            super(Blocks.REDSTONE_TORCH, pos, world);
        }
    }

    /**
     * 基座方块（红石火把的附着方块）
     */
    public static class BaseTask extends TaskInfo {
        public BaseTask(Block block, BlockPos pos, ClientWorld world) {
            super(block, pos, world);
        }

        public BaseTask(BlockPos pos, ClientWorld world) {
            this(Blocks.SLIME_BLOCK, pos, world);
        }
    }
}
