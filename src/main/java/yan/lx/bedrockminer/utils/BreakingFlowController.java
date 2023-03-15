package yan.lx.bedrockminer.utils;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.config.Config;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BreakingFlowController {
    private static List<String> defaultBlockBlacklist = new ArrayList<>();
    private static List<TargetBlock> cache = new LinkedList<>();
    private static boolean working = false;

    static {
        // 默认方块黑名单 (用于限制的服务器, 与自定义黑名单分离)
        defaultBlockBlacklist.add(BlockUtils.getId(Blocks.COMMAND_BLOCK));            // 普通命令方块
        defaultBlockBlacklist.add(BlockUtils.getId(Blocks.CHAIN_COMMAND_BLOCK));      // 连锁型命令方块
        defaultBlockBlacklist.add(BlockUtils.getId(Blocks.REPEATING_COMMAND_BLOCK));  // 循环型命令方块
        defaultBlockBlacklist.add(BlockUtils.getId(Blocks.STRUCTURE_VOID));           // 结构空位
        defaultBlockBlacklist.add(BlockUtils.getId(Blocks.STRUCTURE_BLOCK));          // 结构方块
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
            Messager.chat("bedrockminer.toggle.off");
            working = false;
        } else {
            if (checkIsAllowBlock(block)) {
                MinecraftClient minecraftClient = MinecraftClient.getInstance();
                // 判断玩家是否为创造
                if (minecraftClient.interactionManager != null && minecraftClient.interactionManager.getCurrentGameMode().isCreative()) {
                    Messager.chat("bedrockminer.fail.missing.survival");
                    return;
                }
                Messager.chat("bedrockminer.toggle.on");
                // 判断是否在服务器
                if (!minecraftClient.isInSingleplayer()) {
                    Messager.chat("bedrockminer.warn.multiplayer");
                }
                working = true;
            }
        }
    }

    public static void addTask(Block block, BlockPos pos, ClientWorld world) {
        var minecraftClient = MinecraftClient.getInstance();
        if (working) {
            // 判断部分开启条件
            String haveEnoughItems = InventoryManager.warningMessage();
            if (haveEnoughItems != null) {
                Messager.actionBar(haveEnoughItems);
                return;
            }

            if (minecraftClient.interactionManager != null && minecraftClient.interactionManager.getCurrentGameMode().isSurvivalLike()) {
                if (checkIsAllowBlock(block)) {
                    for (var targetBlock : cache) {
                        // 检查重复任务
                        if (targetBlock.getBlockPos().getManhattanDistance(pos) == 0) {
                            return;
                        }
                    }
                    var targetBlock = new TargetBlock(world.getBlockState(pos).getBlock(), pos, world);
                    cache.add(targetBlock);
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
        if (InventoryManager.warningMessage() != null) {
            return;
        }
        if (interactionManager.getCurrentGameMode().isCreative()) {
            return;
        }
        var count = 0;
        var iterator = cache.iterator();
        while (iterator.hasNext()) {
            var currentTask = iterator.next();
            // 玩家切换世界,距离目标方块太远时,删除缓存任务
            if (currentTask.getWorld() != world) {
                iterator.remove();
                break;
            }
            // 判断玩家与方块距离是否在处理范围内
            if (currentTask.getBlockPos().isWithinDistance(player.getPos(), 3.4f)) {
                if (count++ < 2) {
                    currentTask.tick();
                    if (currentTask.getStatus() == Status.FINISH) {
                        iterator.remove();
                    }
                } else {
                    return;
                }
            }

        }

    }


    private static boolean shouldAddNewTargetBlock(BlockPos pos) {
        for (TargetBlock targetBlock : cache) {
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
            Messager.chat("bedrockminer.toggle.on");
        } else {
            Messager.chat("bedrockminer.toggle.off");
        }
        BreakingFlowController.working = working;
    }
}
