package yan.lx.bedrockminer.utils;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class BlockPlacerUtils {
    /**
     * 简单方块放置
     *
     * @param pos  待放置位置
     * @param item 待放置方块
     */
    public static void simpleBlockPlacement(BlockPos pos, ItemConvertible item) {
        if (pos == null || item == null) {
            return;
        }
        var minecraftClient = MinecraftClient.getInstance();
        var world = minecraftClient.world;
        var player = minecraftClient.player;
        ClientPlayNetworkHandler clientPlayNetworkHandler = minecraftClient.getNetworkHandler();
        if (world == null || player == null || clientPlayNetworkHandler == null) {
            return;
        }
        if (!world.getBlockState(pos).isReplaceable()) {
            return;
        }
        Direction direction = Direction.UP;
        var x = player.getX();
        var y = player.getY();
        var z = player.getZ();
        var yaw = player.getYaw();
        var pitch = 90f;
        minecraftClient.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, player.isOnGround()));
        InventoryManagerUtils.switchToItem(item);
        BlockHitResult hitResult = new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), direction, pos, false);
        placeBlockWithoutInteractingBlock(hitResult);
    }

    /**
     * 活塞放置
     *
     * @param pos       活塞放置坐标
     * @param direction 活塞放置方向
     */
    public static void pistonPlacement(BlockPos pos, Direction direction) {
        if (pos == null || direction == null) {
            return;
        }
        var minecraftClient = MinecraftClient.getInstance();
        var world = minecraftClient.world;
        var player = minecraftClient.player;
        ClientPlayNetworkHandler clientPlayNetworkHandler = minecraftClient.getNetworkHandler();
        if (world == null || player == null || clientPlayNetworkHandler == null) {
            return;
        }
        if (!world.getBlockState(pos).isReplaceable()) {
            return;
        }
        var x = player.getX();
        var y = player.getY();
        var z = player.getZ();
        var yaw = player.getYaw();
        var pitch = switch (direction) {
            case UP, NORTH, SOUTH, WEST, EAST -> 90f;
            case DOWN -> -90f;
        };
        minecraftClient.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, player.isOnGround()));
        Vec3d vec3d = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
        InventoryManagerUtils.switchToItem(Blocks.PISTON);
        BlockHitResult hitResult = new BlockHitResult(vec3d, Direction.UP, pos, false);
        placeBlockWithoutInteractingBlock(hitResult);
    }

    /**
     * 放置没有交互的方块
     */
    private static void placeBlockWithoutInteractingBlock(BlockHitResult hitResult) {
        var minecraftClient = MinecraftClient.getInstance();
        var world = minecraftClient.world;
        var player = minecraftClient.player;
        var interactionManager = minecraftClient.interactionManager;
        if (world == null || player == null || interactionManager == null) {
            return;
        }
        interactionManager.interactBlock(player, Hand.MAIN_HAND, hitResult);
        interactionManager.interactItem(player, Hand.MAIN_HAND);
    }
}
