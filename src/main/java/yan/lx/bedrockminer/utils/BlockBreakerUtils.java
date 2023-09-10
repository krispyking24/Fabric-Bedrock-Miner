package yan.lx.bedrockminer.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;


public class BlockBreakerUtils {
    public static boolean simpleBreakBlock(BlockPos pos) {
        return breakBlock(pos, Direction.UP);
    }

    public static boolean breakBlock(BlockPos blockPos, Direction direction) {
        var client = MinecraftClient.getInstance();
        var player = client.player;
        var world = client.world;
        var interactionManager = client.interactionManager;
        if (player == null || world == null || interactionManager == null || blockPos == null) {
            return false;
        }
        if (world.getBlockState(blockPos).isReplaceable()) return true;
        if (world.getBlockState(blockPos).getBlock().getHardness() < 0) return false;
        var slot = -1;
        var playerInventory = player.getInventory();
        for (int i = 0; i < playerInventory.size(); i++) {
            var itemStack = playerInventory.getStack(i);
            // 检查耐久是否发起警告(剩余耐久<=检查值)
            if (InventoryManagerUtils.isItemDamageWarning(itemStack, 5)) {
                continue;
            }
            slot = i;
            break;
        }
        if (slot != -1) {
            InventoryManagerUtils.switchToSlot(slot);
        }
        // 更新方块正在破坏进程
        if (interactionManager.updateBlockBreakingProgress(blockPos, direction)) {
            client.particleManager.addBlockBreakingParticles(blockPos, direction);
        }
        // 检查是否已经成功
        return world.getBlockState(blockPos).isReplaceable();
    }

}