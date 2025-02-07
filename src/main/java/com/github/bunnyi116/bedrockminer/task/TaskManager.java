package com.github.bunnyi116.bedrockminer.task;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import com.github.bunnyi116.bedrockminer.config.Config;
import com.github.bunnyi116.bedrockminer.utils.BlockUtils;
import com.github.bunnyi116.bedrockminer.utils.InventoryManagerUtils;
import com.github.bunnyi116.bedrockminer.utils.MessageUtils;

import java.util.LinkedList;
import java.util.List;

import static com.github.bunnyi116.bedrockminer.BedrockMiner.*;
import static com.github.bunnyi116.bedrockminer.I18n.*;
import static com.github.bunnyi116.bedrockminer.utils.InteractionUtils.getClosestFace;
import static com.github.bunnyi116.bedrockminer.utils.InteractionUtils.isBlockWithinReach;

public class TaskManager {
    private static final List<TaskHandler> handleTasks = new LinkedList<>();
    private static @Nullable TaskHandler lastTask = null;
    private static int lastTaskTick = 0;
    private static final int lastTaskTickMax = 40;
    private static boolean working = false;

    public static void switchOnOff(Block block) {
        if (isDisabled())
            return;

        if (!checkIsAllowBlock(block))
            return;

        if (isWorking()) {
            clearTask();
            setWorking(false);
        } else {
            // 检查玩家是否为创造
            if (client.interactionManager != null && client.interactionManager.getCurrentGameMode().isCreative()) {
                MessageUtils.addMessage(FAIL_MISSING_SURVIVAL);
                return;
            }
            setWorking(true);
            // 检查是否在服务器
            if (!client.isInSingleplayer()) {
                MessageUtils.addMessage(WARN_MULTIPLAYER);
            }
        }
    }

    public static void addTask(Block block, BlockPos pos, ClientWorld world) {
        if (isDisabled() || !isWorking())
            return;

        if (reverseCheckInventoryItemConditionsAllow())
            return;

        // 仅生存执行
        if (gameMode.isSurvivalLike()) {
            if (!checkIsAllowBlock(block)) {
                return;
            }
            var task = new TaskHandler(world, world.getBlockState(pos).getBlock(), pos);
            var config = Config.INSTANCE;
            if (config.floorsBlacklist != null && !config.floorsBlacklist.isEmpty()) {
                if (config.floorsBlacklist.contains(pos.getY())) {
                    var msg = FLOOR_BLACK_LIST_WARN.getString().replace("(#floor#)", String.valueOf(pos.getY()));
                    MessageUtils.setOverlayMessage(Text.literal(msg));
                    return;
                }
            }
            for (var targetBlock : handleTasks) {
                if (targetBlock.pos.equals(pos)) {
                    return;
                }
            }
            handleTasks.add(task);
        }
    }

    public static void clearTask() {
        if (TaskPlayerLookManager.isModify()) {
            TaskPlayerLookManager.reset();
        }
        handleTasks.clear();
        MessageUtils.addMessage(COMMAND_TASK_CLEAR);
    }

    public static void tick() {
        if (isDisabled() || !working) {
            TaskPlayerLookManager.onTick();
            return;
        }
        if (handleTasks.isEmpty()
                || gameMode.isCreative() // 检查玩家模式
                || (reverseCheckInventoryItemConditionsAllow()) // 检查物品条件
        ) {

            if (!(lastTask != null && (lastTask.getState() == TaskState.RECYCLED_ITEMS || world.getBlockState(lastTask.pos).isAir()))) {
                return;
            }
        }

        // 检查任务是否完成, 重置任务
        if (lastTask != null && !handleTasks.contains(lastTask)) {
            lastTask = null;
        }
        if (lastTask != null) {
            // 检查与目标方块距离
            if (isBlockWithinReach(lastTask.pos, getClosestFace(lastTask.pos), 1F)) {
                // 玩家切换世界
                if (lastTask.world == world) {
                    // 执行任务
                    lastTask.tick();
                    // 任务完成, 删除当前任务
                    if (lastTask.isComplete()) {
                        handleTasks.remove(lastTask);
                        lastTask = null;
                    }
                }
            } else if (lastTaskTick++ >= lastTaskTickMax) {
                lastTask = null;
                lastTaskTick = 0;
            }
        }

        // 使用迭代器, 安全删除列表
        var iterator = handleTasks.iterator();
        while (iterator.hasNext()) {
            var handler = iterator.next();
            // 检查是否有其他任务正在修改视角且不是那个任务
            if (TaskPlayerLookManager.isModify() && TaskPlayerLookManager.getTaskHandler() != handler) {
                continue;
            }
            // 检查与目标方块距离
            if (!isBlockWithinReach(handler.pos, getClosestFace(handler.pos), -1)) {
                continue;
            }
            // 玩家切换世界,距离目标方块太远时,删除缓存任务
            if (handler.world != world) {
                iterator.remove();
                continue;
            }
            lastTask = handler;
            return;
        }
    }

    public static boolean checkIsAllowBlock(Block block) {
        var minecraftClient = MinecraftClient.getInstance();
        var config = Config.INSTANCE;
        // 方块黑名单检查(服务器)
        if (!minecraftClient.isInSingleplayer()) {
            for (var defaultBlockBlack : config.blockBlacklistServer) {
                if (BlockUtils.getBlockId(block).equals(defaultBlockBlack)) {
                    return false;
                }
            }
        }
        // 方块白名单检查(用户自定义)
        for (var blockBlack : config.blockWhitelist) {
            if (BlockUtils.getBlockId(block).equals(blockBlack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean reverseCheckInventoryItemConditionsAllow() {
        var client = MinecraftClient.getInstance();
        var msg = (Text) null;
        if (client.interactionManager != null && !client.interactionManager.getCurrentGameMode().isSurvivalLike()) {
            msg = FAIL_MISSING_SURVIVAL;
        }
        if (InventoryManagerUtils.getInventoryItemCount(Items.PISTON) < 2) {
            msg = FAIL_MISSING_PISTON;
        }
        if (InventoryManagerUtils.getInventoryItemCount(Items.REDSTONE_TORCH) < 1) {
            msg = FAIL_MISSING_REDSTONETORCH;
        }
        if (InventoryManagerUtils.getInventoryItemCount(Items.SLIME_BLOCK) < 1) {
            msg = FAIL_MISSING_SLIME;
        }
        if (!InventoryManagerUtils.canInstantlyMinePiston()) {
            msg = FAIL_MISSING_INSTANTMINE;
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
            MessageUtils.addMessage(TOGGLE_ON);
        } else {
            MessageUtils.addMessage(TOGGLE_OFF);
        }
        TaskManager.working = working;
    }

    private static boolean isDisabled() {
        return Config.INSTANCE.disable;
    }
}
