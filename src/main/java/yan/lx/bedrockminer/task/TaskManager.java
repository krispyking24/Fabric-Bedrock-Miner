package yan.lx.bedrockminer.task;

import com.google.common.collect.Queues;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import yan.lx.bedrockminer.BedrockMinerLang;
import yan.lx.bedrockminer.config.Config;
import yan.lx.bedrockminer.utils.BlockUtils;
import yan.lx.bedrockminer.utils.InventoryManagerUtils;
import yan.lx.bedrockminer.utils.MessageUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class TaskManager {
    private static final List<TaskHandler> handleTaskCaches = new LinkedList<>();
    private static boolean working = false;

    public static void switchOnOff(Block block) {
        if (Config.INSTANCE.disable) return;
        if (working) {
            setWorking(false);
        } else {
            if (checkIsAllowBlock(block)) {
                var client = MinecraftClient.getInstance();
                // 检查玩家是否为创造
                if (client.interactionManager != null && client.interactionManager.getCurrentGameMode().isCreative()) {
                    MessageUtils.addMessage(BedrockMinerLang.FAIL_MISSING_SURVIVAL);
                    return;
                }
                setWorking(true);
                // 检查是否在服务器
                if (!client.isInSingleplayer()) {
                    MessageUtils.addMessage(BedrockMinerLang.WARN_MULTIPLAYER);
                }
            }
        }
    }

    public static void addTask(Block block, BlockPos pos, ClientWorld world) {
        if (Config.INSTANCE.disable) return;
        if (!working) return;
        var interactionManager = MinecraftClient.getInstance().interactionManager;
        if (interactionManager != null) {
            if (reverseCheckInventoryItemConditionsAllow()) return;
            // 仅生存执行
            if (interactionManager.getCurrentGameMode().isSurvivalLike()) {
                if (checkIsAllowBlock(block)) {
                    for (var targetBlock : handleTaskCaches) {
                        // 检查重复任务
                        if (targetBlock.pos.getManhattanDistance(pos) == 0) {
                            return;
                        }
                    }
                    handleTaskCaches.add(new TaskHandler(world, world.getBlockState(pos).getBlock(), pos));
                }
            }
        }
    }

    public static void clearTask() {
        handleTaskCaches.clear();
        MessageUtils.addMessage(BedrockMinerLang.COMMAND_TASK_CLEAR);
    }

    public static void tick() {
        if (Config.INSTANCE.disable) return;
        if (!working) return;
        var minecraftClient = MinecraftClient.getInstance();
        var world = minecraftClient.world;
        var player = minecraftClient.player;
        var interactionManager = minecraftClient.interactionManager;
        if (world == null || player == null || interactionManager == null) return;
        if (handleTaskCaches.isEmpty()) return;
        if (reverseCheckInventoryItemConditionsAllow()) return;    // 检查物品条件
        if (interactionManager.getCurrentGameMode().isCreative()) return;   // 检查玩家模式
        // 使用迭代器, 安全删除列表
        var iterator = handleTaskCaches.iterator();
        var tick = 0;
        while (iterator.hasNext()) {
            var currentTask = iterator.next();
            // 玩家切换世界,距离目标方块太远时,删除缓存任务
            if (currentTask.world != world) {
                iterator.remove();
                continue;
            }
            // 判断玩家与方块距离是否在处理范围内
            if (currentTask.pos.isWithinDistance(player.getEyePos(), 3.5F)) {
                currentTask.onTick();
                if (currentTask.isSucceed()) {
                    iterator.remove();
                }
                return;
            }
        }
    }

    public static boolean checkIsAllowBlock(Block block) {
        var minecraftClient = MinecraftClient.getInstance();
        var config = Config.INSTANCE;
        // 方块黑名单检查(服务器)
        if (!minecraftClient.isInSingleplayer()) {
            for (var defaultBlockBlack : config.blockBlacklistServer) {
                if (BlockUtils.getId(block).equals(defaultBlockBlack)) {
                    return false;
                }
            }
        }
        // 方块黑名单检查(用户自定义)
        for (var blockBlack : config.blockBlacklist) {
            if (BlockUtils.getId(block).equals(blockBlack)) {
                return false;
            }
        }
        // 方块白名单检查(用户自定义)
        for (var blockBlack : config.blockWhitelist) {
            if (BlockUtils.getId(block).equals(blockBlack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean reverseCheckInventoryItemConditionsAllow() {
        var client = MinecraftClient.getInstance();
        var msg = (Text) null;
        if (client.interactionManager != null && !client.interactionManager.getCurrentGameMode().isSurvivalLike()) {
            msg = BedrockMinerLang.FAIL_MISSING_SURVIVAL;
        }
        if (InventoryManagerUtils.getInventoryItemCount(Items.PISTON) < 2) {
            msg = BedrockMinerLang.FAIL_MISSING_PISTON;
        }
        if (InventoryManagerUtils.getInventoryItemCount(Items.REDSTONE_TORCH) < 1) {
            msg = BedrockMinerLang.FAIL_MISSING_REDSTONETORCH;
        }
        if (InventoryManagerUtils.getInventoryItemCount(Items.SLIME_BLOCK) < 1) {
            msg = BedrockMinerLang.FAIL_MISSING_SLIME;
        }
        if (!InventoryManagerUtils.canInstantlyMinePiston()) {
            msg = BedrockMinerLang.FAIL_MISSING_INSTANTMINE;
        }
        if (msg != null) {
            MessageUtils.setOverlayMessage(msg);
            return true;
        }
        return false;
    }

    public static boolean isWorking() {
        return working;
    }

    public static void setWorking(boolean working) {
        if (working) {
            MessageUtils.addMessage(BedrockMinerLang.TOGGLE_ON);
        } else {
            MessageUtils.addMessage(BedrockMinerLang.TOGGLE_OFF);
        }
        TaskManager.working = working;
    }
}
