package yan.lx.bedrockminer.utils;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import yan.lx.bedrockminer.Debug;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.block.Block.sideCoversSmallSquare;

public class CheckingEnvironmentUtils {

    /*** 找到附近的平地放置红石火把
     * @param world 客户端世界
     * @param pistonBlockPos 活塞位置
     * @return 可以直接放置红石火把的位置
     */
    public static List<BlockPos> findNearbyFlatBlockToPlaceRedstoneTorch(ClientWorld world, BlockPos pistonBlockPos) {
        var list = new ArrayList<BlockPos>();
        for (Direction direction : Direction.Type.HORIZONTAL) {
            var blockPos = pistonBlockPos.offset(direction);
            var blockState = world.getBlockState(blockPos);
            if (!sideCoversSmallSquare(world, blockPos.down(), Direction.UP)) {
                continue;
            }
            if (blockState.isReplaceable() || blockState.isOf(Blocks.REDSTONE_TORCH)) {
                list.add(blockPos);
            }
        }
        return list;
    }

    /**
     * 查找可能放置粘液块的位置
     *
     * @param world
     * @param pistonBlockPos
     * @return
     */
    public static List<BlockPos> findPossibleSlimeBlockPos(ClientWorld world, BlockPos pistonBlockPos) {
        var list = new ArrayList<BlockPos>();
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos redTorchPos = pistonBlockPos.offset(direction);
            BlockPos BaseBlockPos = redTorchPos.down();
            if (!world.getBlockState(redTorchPos).isReplaceable() || !world.getBlockState(BaseBlockPos).isReplaceable()) {
                continue;
            }
            list.add(BaseBlockPos);
        }
        return list;
    }

    public static boolean has2BlocksOfPlaceToPlacePiston(ClientWorld world, BlockPos blockPos) {
        BlockPos pos1 = blockPos.up();          // 活塞位置
        BlockPos pos2 = blockPos.up().up();     // 活塞臂位置
        // 获取硬度, 打掉0硬度值的方块
        if (world.getBlockState(pos1).getHardness(world, pos1) < 45f) {
            BlockBreakerUtils.breakPistonBlock(pos1);
        }
        if (world.getBlockState(pos2).getHardness(world, pos2) < 45f) {
            BlockBreakerUtils.breakPistonBlock(pos2);
        }
        // 判断活塞位置和活塞臂位置是否可以放置
        return world.getBlockState(pos1).isReplaceable() && world.getBlockState(pos2).isReplaceable();
    }

    public static List<BlockPos> findNearbyRedstoneTorch(ClientWorld world, BlockPos pistonBlockPos) {
        List<BlockPos> list = new ArrayList<>();
        if (world.getBlockState(pistonBlockPos.east()).isOf(Blocks.REDSTONE_TORCH)) {
            list.add(pistonBlockPos.east());
        }
        if (world.getBlockState(pistonBlockPos.west()).isOf(Blocks.REDSTONE_TORCH)) {
            list.add(pistonBlockPos.west());
        }
        if (world.getBlockState(pistonBlockPos.south()).isOf(Blocks.REDSTONE_TORCH)) {
            list.add(pistonBlockPos.south());
        }
        if (world.getBlockState(pistonBlockPos.north()).isOf(Blocks.REDSTONE_TORCH)) {
            list.add(pistonBlockPos.north());
        }
        return list;
    }

    public static boolean canPlace(BlockPos blockPos, Block block, Direction direction) {
        var player = MinecraftClient.getInstance().player;
        var world = MinecraftClient.getInstance().world;
        if (player != null && world != null) {
            // 放置检测
            var item = block.asItem();
            var context = new ItemPlacementContext(player, Hand.MAIN_HAND, item.getDefaultStack(), new BlockHitResult(blockPos.toCenterPos(), direction, blockPos, false));
            // 实体碰撞箱
            boolean b = true;
            for (var entity : world.getEntities()) {
                if (entity.collidesWithStateAtPos(blockPos, block.getDefaultState())) {
                    b = false;
                }
            }
            return context.canPlace() && b;
        }
        return false;
    }
}
