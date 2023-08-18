package yan.lx.bedrockminer.task;

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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TaskManager {
    private static final List<TaskSelectionInfo> selectionInfos = new ArrayList<>();
    private static final List<TaskHandler> handleTasks = new LinkedList<>();
    private static boolean working = false;

    public static void switchOnOff(Block block) {
        if (Config.INSTANCE.disable) return;
        if (checkIsAllowBlock(block)) {
            if (working) {
                clearTask();
                setWorking(false);
            } else {
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
                    for (var targetBlock : handleTasks) {
                        // 检查重复任务
                        if (targetBlock.pos.getManhattanDistance(pos) == 0) {
                            return;
                        }
                    }
                    handleTasks.add(new TaskHandler(world, world.getBlockState(pos).getBlock(), pos, false));
                }
            }
        }
    }

    public static void clearTask() {
        if (TaskModifyLookInfo.isModify()) {
            TaskModifyLookInfo.reset();
        }
        synchronized (handleTasks) {
            for (TaskHandler handler : handleTasks) {
                handler.onClear();
            }
            handleTasks.clear();
        }
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
        if (handleTasks.isEmpty()) return;
        if (interactionManager.getCurrentGameMode().isCreative()) return;   // 检查玩家模式
        if (reverseCheckInventoryItemConditionsAllow()) return;    // 检查物品条件
        // 使用迭代器, 安全删除列表
        var range = 4;
        for (int y = -range; y < range + 1; y++) {
            for (int x = -range; x < range + 1; x++) {
                for (int z = -range; z < range + 1; z++) {
                    BlockPos blockPos = player.getBlockPos().add(new BlockPos(x, y, z));
//                    // 选区模式
//                    for (TaskSelectionInfo selectionInfo : selectionInfos) {
//                        int x1 = selectionInfo.pos1.getX();
//                        int y1 = selectionInfo.pos1.getY();
//                        int z1 = selectionInfo.pos1.getZ();
//                        int x2 = selectionInfo.pos2.getX();
//                        int y2 = selectionInfo.pos2.getY();
//                        int z2 = selectionInfo.pos2.getZ();
//                        int minX = Math.min(x1, x2);
//                        int minY = Math.min(y1, y2);
//                        int minZ = Math.min(z1, z2);
//                        int maxX = Math.max(x1, x2);
//                        int maxY = Math.max(y1, y2);
//                        int maxZ = Math.max(z1, z2);
//                        if (blockPos.getX() > minX && blockPos.getX() < maxX
//                                || blockPos.getY() > minY && blockPos.getY() < maxY
//                                || blockPos.getZ() > minZ && blockPos.getZ() < maxZ) {
//                            if (checkIsAllowBlock(world.getBlockState(blockPos).getBlock())) {
//                                for (var targetBlock : handleTasks) {
//                                    // 检查重复任务
//                                    if (targetBlock.pos.getManhattanDistance(blockPos) == 0) {
//                                        return;
//                                    }
//                                }
//                                handleTasks.add(new TaskHandler(world, world.getBlockState(blockPos).getBlock(), blockPos, false));
//                            }
//                        }
//                    }

                    // 普通模式
                    var iterator = handleTasks.iterator();
                    while (iterator.hasNext()) {
                        var handler = iterator.next();
                        var pos = handler.pos;
                        // 检查是否有其他任务正在修改视角且不是那个任务
                        if (TaskModifyLookInfo.isModify() && TaskModifyLookInfo.getTaskHandler() != handler) {
                            continue;
                        }
                        // 检查正在执行的目标位置是否与任务坐标一致
                        if (blockPos.getX() != pos.getX() || blockPos.getY() != pos.getY() || blockPos.getZ() != pos.getZ()) {
                            continue;
                        }
                        // 玩家切换世界,距离目标方块太远时,删除缓存任务
                        if (handler.world != world) {
                            iterator.remove();
                            continue;
                        }
                        // 判断玩家与方块距离是否在处理范围内
                        handler.onTick();
                        if (handler.isSucceed()) {
                            iterator.remove();
                        }
                        break;

                    }
                }
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
