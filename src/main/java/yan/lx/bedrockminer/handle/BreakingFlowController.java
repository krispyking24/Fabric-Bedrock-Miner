package yan.lx.bedrockminer.handle;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import yan.lx.bedrockminer.config.Config;
import yan.lx.bedrockminer.utils.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BreakingFlowController {
    private static final List<String> defaultBlockBlacklist = new ArrayList<>();
    private static final List<TargetBlock> handleTaskCaches = new LinkedList<>();
    private static boolean working = false;

    static {
        // 默认方块黑名单 (用于限制的服务器, 与自定义黑名单分离)
        defaultBlockBlacklist.add(BlockUtils.getId(Blocks.AIR));                    // 屏障
    }

    public static boolean checkIsAllowBlock(Block block) {
        var minecraftClient = MinecraftClient.getInstance();
        var config = Config.getInstance();
        // 服务器方块黑名单检查
        if (!minecraftClient.isInSingleplayer()) {
            for (var defaultBlockBlack : defaultBlockBlacklist) {
                if (BlockUtils.getId(block).equals(defaultBlockBlack)) {
                    return false;
                }
            }
        }
        // 用户自定义方块黑名单检查
        for (var blockBlack : config.blockBlacklist) {
            if (BlockUtils.getId(block).equals(blockBlack)) {
                return false;
            }
        }
        // 用户自定义方块白名单检查
        for (var blockBlack : config.blockWhitelist) {
            if (BlockUtils.getId(block).equals(blockBlack)) {
                return true;
            }
        }
        return false;
    }

    public static void switchOnOff(Block block) {
        if (working) {
            MessageUtils.addMessageKey("bedrockminer.toggle.off");
            working = false;
        } else {
            if (checkIsAllowBlock(block)) {
                MinecraftClient minecraftClient = MinecraftClient.getInstance();
                // 判断玩家是否为创造
                if (minecraftClient.interactionManager != null && minecraftClient.interactionManager.getCurrentGameMode().isCreative()) {
                    MessageUtils.addMessageKey("bedrockminer.fail.missing.survival");
                    return;
                }
                MessageUtils.addMessageKey("bedrockminer.toggle.on");
                // 判断是否在服务器
                if (!minecraftClient.isInSingleplayer()) {
                    MessageUtils.addMessageKey("bedrockminer.warn.multiplayer");
                }
                working = true;
            }
        }
    }

    public static void addTask(Block block, BlockPos pos, ClientWorld world) {
        var minecraftClient = MinecraftClient.getInstance();
        if (working) {
            // 判断部分开启条件
            String haveEnoughItems = InventoryManagerUtils.warningMessage();
            if (haveEnoughItems != null) {
                MessageUtils.setOverlayMessageKey(haveEnoughItems);
                return;
            }
            // 仅生存执行
            if (minecraftClient.interactionManager != null && minecraftClient.interactionManager.getCurrentGameMode().isSurvivalLike()) {
                if (checkIsAllowBlock(block)) {
                    for (var targetBlock : handleTaskCaches) {
                        // 检查重复任务
                        if (targetBlock.getBlockPos().getManhattanDistance(pos) == 0) {
                            return;
                        }
                    }
                    var targetBlock = new TargetBlock(world.getBlockState(pos).getBlock(), pos, world);
                    handleTaskCaches.add(targetBlock);
                }
            }
        }
    }

    public static void tick() {
        if (!working) return;
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ClientWorld world = minecraftClient.world;
        PlayerEntity player = minecraftClient.player;
        ClientPlayerInteractionManager interactionManager = minecraftClient.interactionManager;
        if (world == null || player == null || interactionManager == null) {
            return;
        }
        // 运行更新程序
        updater(world, player, interactionManager);
    }

    public static void updater(ClientWorld world, PlayerEntity player, ClientPlayerInteractionManager interactionManager) {
        // 检查使用环境
        if (InventoryManagerUtils.warningMessage() != null) {
            return;
        }
        // 检查玩家模式
        if (interactionManager.getCurrentGameMode().isCreative()) {
            return;
        }
        // 使用迭代器, 安全删除列表
        var iterator = handleTaskCaches.iterator();
        var count = 0;
        while (iterator.hasNext()) {
            var currentTask = iterator.next();
            // 玩家切换世界,距离目标方块太远时,删除缓存任务
            if (currentTask.getWorld() != world) {
                iterator.remove();
                break;
            }
            // 判断玩家与方块距离是否在处理范围内
            if (currentTask.getBlockPos().isWithinDistance(player.getEyePos(), 3.5f)) {
                // 为了tick内部能打印出完成状态, 所以才放在tick前面
                if (currentTask.getStatus() == Status.FINISH) {
                    iterator.remove();
                }
                currentTask.tick();
                if (++count > 1) {
                    return;
                }
            }
        }
    }

    private static boolean shouldAddNewTargetBlock(BlockPos pos) {
        for (TargetBlock targetBlock : handleTaskCaches) {
            if (targetBlock.getBlockPos().getManhattanDistance(pos) == 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean isWorking() {
        return working;
    }

    public static void setWorking(boolean working) {
        if (working) {
            MessageUtils.addMessageKey("bedrockminer.toggle.on");
        } else {
            MessageUtils.addMessageKey("bedrockminer.toggle.off");
        }
        BreakingFlowController.working = working;
    }
}
