package yan.lx.bedrockminer.handle;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.BedrockMinerLang;
import yan.lx.bedrockminer.Debug;
import yan.lx.bedrockminer.utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskHandle {
    public final UUID id;
    public final Block block;
    public final BlockPos blockPos;
    public final ClientWorld world;
    private @Nullable BlockPos pistonBlockPos;
    private @Nullable BlockPos redstoneTorchBlockPos;
    private @Nullable BlockPos slimeBlockPos;
    private boolean hasTried;
    private boolean retrying;
    private TaskStatus status;
    private int ticks;
    private int waitTickCount;
    private int retryCount;
    private int recycleCount;
    private int waitCount;

    public TaskHandle(Block block, BlockPos blockPos, ClientWorld world) {
        this.id = UUID.randomUUID();
        this.block = block;
        this.blockPos = blockPos;
        this.world = world;
        this.status = TaskStatus.INITIALIZATION;
    }

    public boolean isFinish() {
        return this.status == TaskStatus.FINISH;
    }

    public void tick() {
        Debug.info();
        Debug.info("[%s][%s][状态开始]: %s", id, ticks, status);
        if (handleStatus()) {
            updateStatus();
        }
        Debug.info("[%s][%s][状态结束]: %s", id, ticks, status);
        ++ticks;
    }

    private boolean handleStatus() {
        var client = MinecraftClient.getInstance();
        var player = client.player;
        if (player == null) return true;
        switch (status) {
            case INITIALIZATION -> {
                Debug.info("[%s][%s][状态处理][初始化]: 准备", id, ticks);
                this.pistonBlockPos = null;
                this.redstoneTorchBlockPos = null;
                this.slimeBlockPos = null;
                this.hasTried = false;
                this.retrying = false;
                this.ticks = 0;
                this.waitTickCount = 0;
                this.status = TaskStatus.WAIT;
                Debug.info("[%s][%s][状态处理][初始化]: 完成", id, ticks);
            }
            case FIND_PISTON_POSITION -> {
                if (pistonBlockPos == null) {
                    pistonBlockPos = blockPos.up();
                }
                if (!CheckingEnvironmentUtils.has2BlocksOfPlaceToPlacePiston(world, blockPos)) {
                    Debug.info("[%s][%s][状态处理][查找活塞位置]: 失败", id, ticks);
                    MessageUtils.setOverlayMessage(BedrockMinerLang.FAIL_PLACE_PISTON);   // 无法放置活塞
                    this.status = TaskStatus.FAILED;
                } else {
                    Debug.info("[%s][%s][状态处理][查找活塞位置]: 完成, %s", id, ticks, pistonBlockPos);
                    this.status = TaskStatus.WAIT;
                }
            }
            case FIND_SLIME_POSITION -> {
                if (redstoneTorchBlockPos == null) {
                    Debug.info("[%s][%s][状态处理][查找粘液块位置]: 失败,未正确获取红石火把位置", id, ticks);
                    status = TaskStatus.FAILED;
                } else {
                    var pos = redstoneTorchBlockPos.down();
                    if (world.getBlockState(pos).isReplaceable()) {
                        slimeBlockPos = pos;
                        Debug.info("[%s][%s][状态处理][查找粘液块位置]: 成功, %s", id, ticks, slimeBlockPos);
                        status = TaskStatus.WAIT;
                    }
                }
            }
            case FIND_REDSTONE_TORCH_POSITION -> {
                if (pistonBlockPos != null) {
                    // 优选1(直接可以放置红石火把的位置)
                    var redstoneTorchBlockPosList = CheckingEnvironmentUtils.findNearbyFlatBlockToPlaceRedstoneTorch(world, pistonBlockPos);
                    if (redstoneTorchBlockPosList.size() > 0 && redstoneTorchBlockPos == null) {
                        for (var pos : redstoneTorchBlockPosList) {
                            if (Block.sideCoversSmallSquare(world, pos.down(), Direction.UP)) {
                                Debug.info("[%s][%s][状态处理][查找红石火把位置]: 优选1, %s", id, ticks, pos);
                                redstoneTorchBlockPos = pos;
                                break;
                            }
                        }
                    }
                    // 优选2
                    var possibleSlimeBlockPosList = CheckingEnvironmentUtils.findPossibleSlimeBlockPos(world, pistonBlockPos);
                    if (possibleSlimeBlockPosList.size() > 0 && redstoneTorchBlockPos == null) {
                        for (var pos : possibleSlimeBlockPosList) {
                            if (CheckingEnvironmentUtils.canPlace(pos, Blocks.SLIME_BLOCK, Direction.UP)) {
                                Debug.info("[%s][%s][状态处理][查找红石火把位置]: 优选2, 基座：%s", id, ticks, pos);
                                Debug.info("[%s][%s][状态处理][查找红石火把位置]: 优选2, %s", id, ticks, pos.up());
                                slimeBlockPos = pos;
                                redstoneTorchBlockPos = pos.up();
                                break;
                            }
                        }
                    }
                    // 无优选，使用默认方法1
                    if (redstoneTorchBlockPosList.size() > 0 && redstoneTorchBlockPos == null) {
                        redstoneTorchBlockPos = redstoneTorchBlockPosList.get(0);
                        Debug.info("[%s][%s][状态处理][查找红石火把位置]: 默认1, %s", id, ticks, redstoneTorchBlockPos);
                    }
                    // 无优选，使用默认方法2
                    if (possibleSlimeBlockPosList.size() > 0 && redstoneTorchBlockPos == null) {
                        slimeBlockPos = possibleSlimeBlockPosList.get(0);
                        redstoneTorchBlockPos = possibleSlimeBlockPosList.get(0).up();
                        Debug.info("[%s][%s][状态处理][查找红石火把位置]: 默认2, 基座：%s", id, ticks, slimeBlockPos);
                        Debug.info("[%s][%s][状态处理][查找红石火把位置]: 默认2, %s", id, ticks, redstoneTorchBlockPos);
                    }
                    // 最终方案
                    if (redstoneTorchBlockPos == null) {
                        Debug.info("[%s][%s][状态处理][查找红石火把位置]: 失败", id, ticks);
                        status = TaskStatus.FAILED;
                    } else {
                        Debug.info("[%s][%s][状态处理][查找红石火把位置]: 成功, %s", id, ticks, redstoneTorchBlockPos);
                        this.status = TaskStatus.WAIT;
                    }
                }
            }
            case PLACE_PISTON -> {
                if (pistonBlockPos == null) {
                    this.status = TaskStatus.FIND_PISTON_POSITION;
                    this.redstoneTorchBlockPos = null;
                    this.slimeBlockPos = null;
                    Debug.info("[%s][%s][状态处理][放置活塞]: 活塞位置未获取", id, ticks);
                    return false;
                }
                if (!CheckingEnvironmentUtils.has2BlocksOfPlaceToPlacePiston(world, blockPos)) {
                    Debug.info("[%s][%s][状态处理][放置活塞]: 放置失败, 该位置可能无法放置或有实体存在, %s", id, ticks, pistonBlockPos);
                    MessageUtils.setOverlayMessage(BedrockMinerLang.FAIL_PLACE_PISTON);
                    this.status = TaskStatus.FAILED;
                } else {
                    BlockPlacerUtils.placement(pistonBlockPos, Direction.UP, Items.PISTON);
                    Debug.info("[%s][%s][状态处理][放置活塞]: 完成", id, ticks);
                    this.status = TaskStatus.WAIT;
                }
            }
            case PLACE_SLIME_BLOCK -> {
                if (slimeBlockPos == null) {
                    Debug.info("[%s][%s][状态处理][放置基座方块]: 基座方块未知未获取, 无法放置", id, ticks);
                    status = TaskStatus.FIND_SLIME_POSITION;
                    return false;
                }
                if (!CheckingEnvironmentUtils.canPlace(slimeBlockPos, Blocks.SLIME_BLOCK, Direction.UP)) {
                    Debug.info("[%s][%s][状态处理][放置基座方块]: 放置失败, 该位置可能无法放置或有实体存在, %s", id, ticks, slimeBlockPos);
                    MessageUtils.setOverlayMessage(BedrockMinerLang.FAIL_PLACE_SLIMEBLOCK);
                }
                Debug.info("[%s][%s][状态处理][放置基座方块]: 放置, %s", id, ticks, slimeBlockPos);
                BlockPlacerUtils.simpleBlockPlacement(slimeBlockPos, Items.SLIME_BLOCK);
                status = TaskStatus.WAIT;
            }
            case PLACE_REDSTONE_TORCH -> {
                if (redstoneTorchBlockPos == null) {
                    Debug.info("[%s][%s][状态处理][放置红石火把]: 红石火把未获取", id, ticks);
                    slimeBlockPos = null;
                    status = TaskStatus.FIND_REDSTONE_TORCH_POSITION;
                    return false;
                }
                Debug.info("[%s][%s][状态处理][放置红石火把]: 放置红石火把, %s", id, ticks, redstoneTorchBlockPos);
                BlockPlacerUtils.simpleBlockPlacement(redstoneTorchBlockPos, Items.REDSTONE_TORCH);
                status = TaskStatus.WAIT;
                return true;
            }
            case PLACE_ERROR_PISTON -> {
                if (BlockBreakerUtils.usePistonBreakBlock(pistonBlockPos)) {
                    this.status = TaskStatus.WAIT;
                }
            }
            case PLACE_ERROR_REDSTONE_TORCH -> {
                if (BlockBreakerUtils.simpleBreakBlock(redstoneTorchBlockPos)) {
                    this.status = TaskStatus.WAIT;
                }
            }
            case EXTENDED_START -> {
                // 同 Tick 执行任务
                if (!hasTried && pistonBlockPos != null) {
                    Debug.info("[%s][%s][状态处理][执行]：准备开始", id, ticks);
                    // 打掉活塞附近能充能的红石火把
                    var nearByRedstoneTorchPosList = CheckingEnvironmentUtils.findNearbyRedstoneTorch(world, pistonBlockPos);
                    for (BlockPos pos : nearByRedstoneTorchPosList) {
                        Debug.info("[%s][%s][状态处理][执行]：打掉红石火把, %s", id, ticks, pos);
                        BlockBreakerUtils.usePistonBreakBlock(pos);
                    }
                    // 打掉活塞
                    Debug.info("[%s][%s][状态处理][执行]：打掉活塞, %s", id, ticks, pistonBlockPos);
                    if (BlockBreakerUtils.usePistonBreakBlock(pistonBlockPos)) {
                        this.status = TaskStatus.FAILED;
                    }
                    // 放置朝下的活塞
                    Debug.info("[%s][%s][状态处理][执行]：放置朝下的活塞, %s", id, ticks, pistonBlockPos);
                    BlockPlacerUtils.placement(pistonBlockPos, Direction.DOWN, Items.PISTON);
                    Debug.info("[%s][%s][状态处理][执行]：执行完成", id, ticks);
                    hasTried = true;
                    this.status = TaskStatus.WAIT;
                }
            }
            case PISTON_MOVING -> this.status = TaskStatus.WAIT;
            case WAIT -> {
                // ping延迟补偿
                var playerListEntry = player.networkHandler.getPlayerListEntry(player.getUuid());
                var waitTick = 1;
                if (playerListEntry != null) {
                    var ping = playerListEntry.getLatency();
                    var tick = ping / 50; // 至少延迟1tick (1000ms/50mspt=20tick)
                    Debug.info("[%s][%s][状态处理][等待]：延迟时间, %s", id, ticks, ping);
                    Debug.info("[%s][%s][状态处理][等待]：延迟总刻, %s", id, ticks, tick);
                    waitTick += tick;
                }
                Debug.info("[%s][%s][状态处理][等待]：等待总刻, %s", id, ticks, waitTick);
                if (waitCount++ >= waitTick) {
                    this.status = TaskStatus.WAIT_GAME_UPDATE;
                } else {
                    waitTickCount += waitTick;
                }
            }
            case WAIT_GAME_UPDATE -> {
                if (waitCount > 0) {
                    waitCount = 0;
                }
                return true;
            }
            case TIME_OUT -> this.status = TaskStatus.FAILED;
            case FAILED -> {
                this.status = TaskStatus.ITEM_RECYCLING;
                Debug.info("[%s][%s][状态处理][失败]：准备物品回收", id, ticks);
                if (retryCount++ < 1) {
                    Debug.info("[%s][%s][状态处理][失败]: 准备重试", id, ticks);
                    retrying = true;
                }
                return false;
            }
            case ITEM_RECYCLING -> {
                if (pistonBlockPos != null && BlockBreakerUtils.usePistonBreakBlock(pistonBlockPos)) {
                    pistonBlockPos = null;
                    Debug.info("[%s][%s][状态处理][物品回收][活塞]: 成功", id, ticks);
                } else if (redstoneTorchBlockPos != null && BlockBreakerUtils.usePistonBreakBlock(redstoneTorchBlockPos)) {
                    redstoneTorchBlockPos = null;
                    Debug.info("[%s][%s][状态处理][物品回收][粘液块]: 成功", id, ticks);
                } else if (slimeBlockPos != null && BlockBreakerUtils.usePistonBreakBlock(slimeBlockPos)) {
                    slimeBlockPos = null;
                    Debug.info("[%s][%s][状态处理][物品回收][粘液块]: 成功", id, ticks);
                }

                if (recycleCount++ > 20 || (pistonBlockPos == null && redstoneTorchBlockPos == null && slimeBlockPos == null)) {
                    if (retrying) {
                        this.status = TaskStatus.RETRY;
                    } else {
                        this.status = TaskStatus.FINISH;
                    }
                }
            }
            case RETRY -> {
                this.retrying = false;
                this.status = TaskStatus.INITIALIZATION;
                Debug.info("[%s][%s][状态处理][重试]: 重新尝试", id, ticks);
            }
            case FINISH -> Debug.info("[%s][%s][状态处理]: 完成", id, ticks);
        }
        return false;
    }


    private void updateStatus() {
        // 延迟等待
        if (status == TaskStatus.WAIT) {
            return;
        }
        // 检查超时
        if (ticks > 40 + waitTickCount) {
            Debug.info("[%s][%s][玩家交互更新]: 超时", id, ticks);
            status = TaskStatus.TIME_OUT;
            return;
        }
        // 检查目标方块
        var blockState = world.getBlockState(blockPos);
        if (blockState.isAir()) {
            Debug.info("[%s][%s][更新状态]: 目标方块(%s)已不存在, 准备执行回收任务", id, ticks, block.getName().getString());
            status = TaskStatus.ITEM_RECYCLING;
            return;
        }
        // 检查活塞
        if (pistonBlockPos == null) {
            Debug.info("[%s][%s][更新状态]: 活塞位置未获取", id, ticks);
            status = TaskStatus.FIND_PISTON_POSITION;
            return;
        } else {
            Debug.info("[%s][%s][更新状态]: 活塞位置已获取", id, ticks);
            var pistonState = world.getBlockState(pistonBlockPos);
            // 检查活塞当前是否还处于技术性方块(36号方块)
            if (pistonState.isOf(Blocks.MOVING_PISTON)) {
                Debug.info("[%s][%s][更新状态]: 活塞移动中", id, ticks);
                status = TaskStatus.PISTON_MOVING;
                return;
            }
            // 检查活塞状态
            if (pistonState.isOf(Blocks.PISTON)) {
                var direction = world.getBlockState(pistonBlockPos).get(PistonBlock.FACING);
                Debug.info("[%s][%s][更新状态]: 活塞已放置, %s", id, ticks, direction);
                // 检查执行情况
                if (hasTried) {
                    Debug.info("[%s][%s][更新状态]: 已执行过, %s", id, ticks, direction);
                    return;
                } else {
                    Debug.info("[%s][%s][更新状态]: 未执行过, %s", id, ticks, direction);
                    if (direction == Direction.UP) {
                        // 活塞已充能(执行开始)
                        if (world.getBlockState(pistonBlockPos).get(PistonBlock.EXTENDED)) {
                            Debug.info("[%s][%s][更新状态]: 条件充足, 准备执行", id, ticks);
                            status = TaskStatus.EXTENDED_START;
                            return;
                        }
                    } else {
                        Debug.info("[%s][%s][更新状态]: 活塞放置方向错误", id, ticks);
                        status = TaskStatus.PLACE_ERROR_REDSTONE_TORCH;
                        return;
                    }
                }
            } else {
                if (!hasTried) {
                    Debug.info("[%s][%s][更新状态]: 活塞未放置", id, ticks);
                    status = TaskStatus.PLACE_PISTON;
                    return;
                }
            }
        }
        // 检查红石火把基座
        if (!hasTried) {
            if (slimeBlockPos != null) {
                var slimeBlockState = world.getBlockState(slimeBlockPos);
                if (slimeBlockState.isReplaceable()) {
                    status = TaskStatus.PLACE_SLIME_BLOCK;
                    return;
                }
            }
        }
        // 检查红石火把
        if (!hasTried) {
            if (redstoneTorchBlockPos == null) {
                Debug.info("[%s][%s][更新状态]: 红石火把位置未获取", id, ticks);
                status = TaskStatus.FIND_REDSTONE_TORCH_POSITION;
                return;
            }
            var redstoneTorchState = world.getBlockState(redstoneTorchBlockPos);
            var baseBlockState = world.getBlockState(redstoneTorchBlockPos.down());
            if (baseBlockState.isReplaceable()) {
                Debug.info("[%s][%s][更新状态]: 需要放置基座, 准备查找基座方块位置", id, ticks);
                status = TaskStatus.FIND_SLIME_POSITION;
                return;
            }
            if (redstoneTorchState.isReplaceable()) {
                Debug.info("[%s][%s][更新状态]: 红石火把未放置", id, ticks);
                status = TaskStatus.PLACE_REDSTONE_TORCH;
            } else if (redstoneTorchState.isOf(Blocks.REDSTONE_TORCH)) {
                Debug.info("[%s][%s][更新状态]: 红石火把已放置", id, ticks);
            } else if (redstoneTorchState.isOf(Blocks.REDSTONE_WALL_TORCH)) {
                Debug.info("[%s][%s][更新状态]: 红石火把放置状态错误", id, ticks);
                status = TaskStatus.PLACE_ERROR_REDSTONE_TORCH;
            }
        }
    }
}


