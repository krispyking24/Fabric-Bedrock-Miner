package yan.lx.bedrockminer.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.Debug;
import yan.lx.bedrockminer.network.PacketManager;

public class BlockPlacerUtils {

    /**
     * 活塞放置
     *
     * @param blockPos 活塞放置坐标
     * @param facing   活塞放置方向
     * @param item     使用的物品
     */
    public static void placement(BlockPos blockPos, Direction facing, @Nullable Item item) {
        if (blockPos == null || facing == null) return;
        var client = MinecraftClient.getInstance();
        var world = client.world;
        var player = client.player;
        var networkHandler = client.getNetworkHandler();
        var interactionManager = client.interactionManager;
        if (world == null || player == null || networkHandler == null || interactionManager == null) return;
        if (!world.getBlockState(blockPos).isReplaceable()) return;
        var yaw = switch (facing) {
            case SOUTH -> 180F;
            case EAST -> 90F;
            case NORTH -> 0F;
            case WEST -> -90F;
            default -> player.getYaw();
        };
        var pitch = switch (facing) {
            case UP -> 90F;
            case DOWN -> -90F;
            default -> 0F;
        };
        // 模拟选中位置(凭空放置)
        var blockCenterPos = Vec3d.ofCenter(blockPos);
        var hitPos = blockPos.offset(facing.getOpposite());
        var hitVec3d = hitPos.toCenterPos().offset(facing, 0.5F);   // 放置面中心坐标
        var hitResult = new BlockHitResult(hitVec3d, facing, blockPos, false);
        var distance = player.getEyePos().squaredDistanceTo(blockCenterPos);
        if (distance > ServerPlayNetworkHandler.MAX_BREAK_SQUARED_DISTANCE) {
            Debug.info("玩家位置离目标方块位置超过限制%s, 当前距离目标方块：%s", ServerPlayNetworkHandler.MAX_BREAK_SQUARED_DISTANCE, distance);
            return;
        }
        var spacing = hitVec3d.subtract(blockCenterPos); // 选中放置面与目标方块中心位置的间距
        var maxRange = 1.0000001D;
        if (!(Math.abs(spacing.getX()) < maxRange && Math.abs(spacing.getY()) < maxRange && Math.abs(spacing.getZ()) < maxRange)) {
            Debug.info("选中放置面与目标方块中心位置的间距超过限制%s！%s, %s", maxRange, blockPos.toShortString(), hitVec3d);
            return;
        }
        if (item != null) {
            InventoryManagerUtils.switchToItem(item);
        }
        PacketManager.write = false;
        // 发送修改视角数据包
        PacketManager.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, player.isOnGround()));
        // 发送交互方块数据包
        try (PendingUpdateManager pendingUpdateManager = world.getPendingUpdateManager().incrementSequence()) {
            int sequence = pendingUpdateManager.getSequence();
            PacketManager.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
        }
        PacketManager.write = true;
    }

    public static void placement(BlockPos blockPos, Direction facing) {
        placement(blockPos, facing, null);
    }

    public static void simpleBlockPlacement(BlockPos blockPos) {
        simpleBlockPlacement(blockPos, null);
    }

    public static void simpleBlockPlacement(BlockPos blockPos, @Nullable Item item) {
        placement(blockPos, Direction.UP, item);
    }
}
