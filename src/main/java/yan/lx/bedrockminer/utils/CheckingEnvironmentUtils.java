package yan.lx.bedrockminer.utils;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.block.Block.sideCoversSmallSquare;

public class CheckingEnvironmentUtils {

    public static List<BlockPos> findNearbyFlatBlockToPlaceRedstoneTorch(ClientWorld world, BlockPos blockPos) {
        var list = new ArrayList<BlockPos>();
        if ((sideCoversSmallSquare(world, blockPos.down().east(), Direction.UP) && (world.getBlockState(blockPos.east()).getMaterial().isReplaceable()) || world.getBlockState(blockPos.east()).isOf(Blocks.REDSTONE_TORCH))) {
            list.add(blockPos.east());
        }
        if ((sideCoversSmallSquare(world, blockPos.down().west(), Direction.UP) && (world.getBlockState(blockPos.west()).getMaterial().isReplaceable()) || world.getBlockState(blockPos.west()).isOf(Blocks.REDSTONE_TORCH))) {
            list.add(blockPos.west());
        }
        if ((sideCoversSmallSquare(world, blockPos.down().north(), Direction.UP) && (world.getBlockState(blockPos.north()).getMaterial().isReplaceable()) || world.getBlockState(blockPos.north()).isOf(Blocks.REDSTONE_TORCH))) {
            list.add(blockPos.north());
        }
        if ((sideCoversSmallSquare(world, blockPos.down().south(), Direction.UP) && (world.getBlockState(blockPos.south()).getMaterial().isReplaceable()) || world.getBlockState(blockPos.south()).isOf(Blocks.REDSTONE_TORCH))) {
            list.add(blockPos.south());
        }
        return list;
    }

    public static BlockPos findPossibleSlimeBlockPos(ClientWorld world, BlockPos blockPos) {
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos newBlockPos = blockPos.offset(direction);
            if (!world.getBlockState(newBlockPos).getMaterial().isReplaceable()) {
                continue;
            }
            if (CheckingEnvironmentUtils.isBlocked(newBlockPos)) {
                continue;
            }
            return newBlockPos;
        }
        return null;
    }

    public static boolean has2BlocksOfPlaceToPlacePiston(ClientWorld world, BlockPos blockPos) {
        BlockPos pos1 = blockPos.up();          // 活塞位置
        BlockPos pos2 = blockPos.up().up();     // 活塞臂位置
        // 获取硬度,应该是活塞位置处有其他方块吧？
        if (world.getBlockState(pos1).getHardness(world, pos1) == 0) {
            BlockBreakerUtils.breakPistonBlock(pos1);
        }
        // 判断活塞位置和活塞臂位置是否可以放置
        return world.getBlockState(pos1).getMaterial().isReplaceable() && world.getBlockState(pos2).getMaterial().isReplaceable();
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

    public static boolean isBlocked(BlockPos blockPos) {
        var player = MinecraftClient.getInstance().player;
        if (player != null) {
            var slimeItem = Blocks.SLIME_BLOCK.asItem();
            var context = new ItemPlacementContext(player, Hand.MAIN_HAND, slimeItem.getDefaultStack(),
                    new BlockHitResult(blockPos.toCenterPos(), Direction.UP, blockPos, false));
            return !context.canPlace();
        }
        return true;
    }
}
