package yan.lx.bedrockminer.handle;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.Debug;
import yan.lx.bedrockminer.utils.*;

import java.util.UUID;

import static net.minecraft.block.Block.sideCoversSmallSquare;

public class TaskHandle {
    private UUID id;
    private final Block block;
    private final BlockPos blockPos;
    private final ClientWorld world;

    @Nullable
    private BlockPos pistonBlockPos;
    @Nullable
    private BlockPos redstoneTorchBlockPos;
    @Nullable
    private BlockPos slimeBlockPos;

    private int timeoutCount;
    private final int timeoutCountMax = 40;
    private boolean hasTried;
    private TaskStatus status;
    private int recycleCount;
    private boolean retrying;
    private int retryCount;
    private final int retryMax = 3;
    private int delayCount;
    private final int delayCountMax = 10;

    /**
     * 构造函数
     *
     * @param block    目标方块
     * @param blockPos 目标方块所在位置
     * @param world    目标方块所在世界
     */
    public TaskHandle(Block block, BlockPos blockPos, ClientWorld world) {
        // 赋值
        this.block = block;
        this.blockPos = blockPos;
        this.world = world;
        // 初始化
        this.id = UUID.randomUUID();
        this.status = TaskStatus.INITIALIZATION;

    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public ClientWorld getWorld() {
        return world;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void tick() {
        Debug.info();
        Debug.info("[%s][%s][当前处理状态]: %s", id, timeoutCount, status);
        if (handleStatus()) {
            updateStatus();
        }
    }

    private boolean handleStatus() {
        var minecraftClient = MinecraftClient.getInstance();
        var player = minecraftClient.player;
        if (player == null) return true;

        switch (status) {
            case INITIALIZATION -> {
                this.pistonBlockPos = null;
                this.slimeBlockPos = null;
                this.redstoneTorchBlockPos = null;
                this.timeoutCount = 0;
                this.hasTried = false;
                this.retrying = false;
                this.delayCount = 0;
                this.status = TaskStatus.WAIT_GAME_UPDATE;  // 等待更新状态
                Debug.info("[%s][%s][状态处理][初始化]: 完成", id, timeoutCount);
            }
            case FIND_PISTON_POSITION -> {
                return onFindPistonPosition();
            }
            case FIND_SLIME_POSITION -> {
                return onFindSlimePosition();
            }
            case FIND_REDSTONE_TORCH_POSITION -> {
                return onFindRedstoneTorchPosition();
            }
            case PLACE_PISTON -> {
                return onPlacePiston();
            }
            case PLACE_SLIME_BLOCK -> {
                return onPlaceSlimeBlock();
            }
            case PLACE_REDSTONE_TORCH -> {
                return onPlaceRedstoneTorch();
            }
            case PLACE_ERROR_PISTON -> BlockBreakerUtils.breakPistonBlock(pistonBlockPos);
            case PLACE_ERROR_REDSTONE_TORCH -> BlockBreakerUtils.breakPistonBlock(redstoneTorchBlockPos);
            case EXTENDED_START -> {
                if (!hasTried && pistonBlockPos != null) {
                    Debug.info("[%s][%s][状态处理][执行]：准备开始", id, timeoutCount);
                    // 打掉活塞附近能充能的红石火把
                    var nearByRedstoneTorchPosList = CheckingEnvironmentUtils.findNearbyRedstoneTorch(world, pistonBlockPos);
                    for (BlockPos pos : nearByRedstoneTorchPosList) {
                        Debug.info("[%s][%s][状态处理][执行]：打掉红石火把, %s", id, timeoutCount, pos);
                        BlockBreakerUtils.instantBreakPistonBlock(pos);
                    }
                    // 打掉活塞
                    Debug.info("[%s][%s][状态处理][执行]：打掉活塞, %s", id, timeoutCount, pistonBlockPos);
                    if (BlockBreakerUtils.instantBreakPistonBlock(pistonBlockPos)) {
                        this.status = TaskStatus.FAILED;
                    }

                    // 放置朝下的活塞
                    Debug.info("[%s][%s][状态处理][执行]：放置朝下的活塞, %s", id, timeoutCount, pistonBlockPos);
                    BlockPlacerUtils.pistonPlacement(pistonBlockPos, Direction.DOWN);

                    hasTried = true;
                    Debug.info("[%s][%s][状态处理][执行]：执行完成", id, timeoutCount);
                    status = TaskStatus.WAIT_GAME_UPDATE;  // 等待状态
                }
            }
            case PISTON_MOVING -> status = TaskStatus.WAIT_GAME_UPDATE;  // 等待更新状态
            case WAIT_GAME_UPDATE -> {
                return true;
            }
            case TIME_OUT -> {
                status = TaskStatus.ITEM_RECYCLING;
                return false;
            }
            case FAILED -> {
                status = TaskStatus.ITEM_RECYCLING;
                Debug.info("[%s][%s][状态处理][失败]：准备物品回收", id, timeoutCount);
                if (retryCount++ < retryMax) {
                    Debug.info("[%s][%s][状态处理][失败]: 准备重试", id, timeoutCount);
                    retrying = true;
                }
                return false;
            }
            case ITEM_RECYCLING -> {
                // 回收任务超时直接退出任务
                if (recycleCount++ < 40) {
                    // 跳过几tick, 因为活塞位置可能在执行活塞回收任务
                    if (recycleCount < 4) {
                        return false;
                    }
                    // 活塞
                    if (pistonBlockPos != null) {
                        // 可能残留的活塞1
                        BlockPos pistonPos1 = pistonBlockPos.up();
                        if (world.getBlockState(pistonPos1).isOf(Blocks.PISTON)) {
                            Debug.info("[%s][%s][状态处理][物品回收][%s][活塞up]: %s", id, timeoutCount, recycleCount, pistonPos1);
                            if (BlockBreakerUtils.breakPistonBlock(pistonPos1)) {
                                return false;
                            }
                        }
                        // 可能残留的活塞2
                        BlockPos pistonPos2 = pistonBlockPos.up().up();
                        if (world.getBlockState(pistonPos2).isOf(Blocks.PISTON)) {
                            Debug.info("[%s][%s][状态处理][物品回收][%s][活塞upup]: %s", id, timeoutCount, recycleCount, pistonPos2);
                            if (BlockBreakerUtils.breakPistonBlock(pistonPos1)) {
                                return false;
                            }
                        }

                        // 检查四周活塞臂
                        if (world.getBlockState(pistonBlockPos.west()).isOf(Blocks.PISTON_HEAD)) {
                            BlockBreakerUtils.instantBreakBlock(pistonBlockPos.west());
                            return false;
                        }
                        if (world.getBlockState(pistonBlockPos.south()).isOf(Blocks.PISTON_HEAD)) {
                            BlockBreakerUtils.instantBreakBlock(pistonBlockPos.south());
                            return false;
                        }
                        if (world.getBlockState(pistonBlockPos.north()).isOf(Blocks.PISTON_HEAD)) {
                            BlockBreakerUtils.instantBreakBlock(pistonBlockPos.south());
                            return false;
                        }
                        if (world.getBlockState(pistonBlockPos.east()).isOf(Blocks.PISTON_HEAD)) {
                            BlockBreakerUtils.instantBreakBlock(pistonBlockPos.south());
                            return false;
                        }

                        // 活塞
                        var blockState = world.getBlockState(pistonBlockPos);
                        if (blockState.isOf(Blocks.PISTON)) {
                            Debug.info("[%s][%s][状态处理][物品回收][%s][活塞]: %s", id, timeoutCount, recycleCount, pistonBlockPos);
                            // 破坏活塞
                            if (BlockBreakerUtils.breakPistonBlock(pistonBlockPos)) {
                                pistonBlockPos = null;
                                return false;
                            }

                        } else if (blockState.isAir()) {
                            pistonBlockPos = null;
                            return false;
                        }
                    }
                    // 红石火把
                    if (redstoneTorchBlockPos != null) {
                        var blockState = world.getBlockState(redstoneTorchBlockPos);
                        if (blockState.isOf(Blocks.REDSTONE_TORCH) || blockState.isOf(Blocks.REDSTONE_WALL_TORCH)) {
                            Debug.info("[%s][%s][状态处理][物品回收][%s][红石火把]: %s", id, timeoutCount, recycleCount, redstoneTorchBlockPos);
                            if (BlockBreakerUtils.breakBlock(redstoneTorchBlockPos, Direction.UP)) {
                                redstoneTorchBlockPos = null;
                                return false;
                            }
                        } else if (blockState.isAir()) {
                            redstoneTorchBlockPos = null;
                            return false;
                        }
                    }
                    // 粘液块
                    if (slimeBlockPos != null) {
                        var blockState = world.getBlockState(slimeBlockPos);
                        if (blockState.isOf(Blocks.SLIME_BLOCK)) {
                            Debug.info("[%s][%s][状态处理][物品回收][%s][粘液块]: %s", id, timeoutCount, recycleCount, slimeBlockPos);
                            if (BlockBreakerUtils.breakBlock(slimeBlockPos, Direction.UP)) {
                                slimeBlockPos = null;
                                return false;
                            }
                        } else if (blockState.isAir()) {
                            slimeBlockPos = null;
                            return false;
                        }
                    }
                    if (pistonBlockPos == null && redstoneTorchBlockPos == null && slimeBlockPos == null) {
                        if (retrying) {
                            status = TaskStatus.RETRY;
                        } else {
                            status = TaskStatus.FINISH;
                        }
                        return false;
                    }
                }
                if (retrying) {
                    status = TaskStatus.RETRY;
                    return false;
                }
                status = TaskStatus.FINISH;
                return false;
            }
            case RETRY -> {
                this.status = TaskStatus.INITIALIZATION;
                this.retrying = false;
                Debug.info("[%s][%s][状态处理][重试]: 重新尝试", id, timeoutCount);
            }
            case FINISH -> {
                return false;
            }
        }
        return true;
    }


    private void updateStatus() {
        // 检查超时
        if (timeoutCount++ > timeoutCountMax) {
            Debug.info("[%s][%s][玩家交互更新]: 超时", id, timeoutCount);
            status = TaskStatus.TIME_OUT;
            return;
        }

        // 检查目标方块
        {
            var blockState = world.getBlockState(blockPos);
            if (blockState.isAir()) {
                Debug.info("[%s][%s][更新状态]: 目标方块(%s)已不存在, 准备执行回收任务", id, timeoutCount, block.getName().getString());
                status = TaskStatus.ITEM_RECYCLING;
                return;
            }
        }

        // 检查活塞
        {
            if (pistonBlockPos == null) {
                Debug.info("[%s][%s][更新状态]: 活塞位置未获取", id, timeoutCount);
                status = TaskStatus.FIND_PISTON_POSITION;
                return;
            }
            Debug.info("[%s][%s][更新状态]: 活塞位置已获取", id, timeoutCount);

            // 检查活塞放置情况
            var pistonState = world.getBlockState(pistonBlockPos);

            // 检查活塞当前是否还处于技术性方块(36号方块)
            if (pistonState.isOf(Blocks.MOVING_PISTON)) {
                Debug.info("[%s][%s][更新状态]: 活塞移动中", id, timeoutCount);
                status = TaskStatus.PISTON_MOVING;
                return;
            }

            // 检查活塞状态
            if (pistonState.isOf(Blocks.PISTON)) {
                var direction = world.getBlockState(pistonBlockPos).get(PistonBlock.FACING);
                Debug.info("[%s][%s][更新状态]: 活塞已放置, %s", id, timeoutCount, direction);
                // 检查执行情况
                if (hasTried) {
                    Debug.info("[%s][%s][更新状态]: 已执行过, %s", id, timeoutCount, direction);
                    // 等待几个tick让活塞任务处理完成
                    if (delayCount++ > delayCountMax) {
                        status = TaskStatus.FAILED;
                    }
                    return;
                } else {
                    Debug.info("[%s][%s][更新状态]: 未执行过, %s", id, timeoutCount, direction);
                    if (direction == Direction.UP) {
                        // 活塞已充能(执行开始)
                        if (world.getBlockState(pistonBlockPos).get(PistonBlock.EXTENDED)) {
                            Debug.info("[%s][%s][更新状态]: 条件充足, 准备执行", id, timeoutCount);
                            status = TaskStatus.EXTENDED_START;
                            return;
                        }
                    } else {
                        Debug.info("[%s][%s][更新状态]: 活塞放置方向错误", id, timeoutCount);
                        status = TaskStatus.PLACE_ERROR_REDSTONE_TORCH;
                        return;
                    }
                }
            } else {
                if (!hasTried) {
                    Debug.info("[%s][%s][更新状态]: 活塞未放置", id, timeoutCount);
                    status = TaskStatus.PLACE_PISTON;
                    return;
                }
            }


        }

        // 检查红石火把基座
        {
            if (slimeBlockPos != null) {
                var slimeBlockState = world.getBlockState(slimeBlockPos);
                if (slimeBlockState.isReplaceable()) {
                    status = TaskStatus.PLACE_SLIME_BLOCK;
                    return;
                }
            }
        }

        // 检查红石火把
        {
            if (redstoneTorchBlockPos == null) {
                Debug.info("[%s][%s][更新状态]: 红石火把位置未获取", id, timeoutCount);
                status = TaskStatus.FIND_REDSTONE_TORCH_POSITION;
                return;
            }
            var redstoneTorchState = world.getBlockState(redstoneTorchBlockPos);
            var baseBlockState = world.getBlockState(redstoneTorchBlockPos.down());
            if (baseBlockState.isReplaceable()) {
                Debug.info("[%s][%s][更新状态]: 需要放置基座, 准备查找基座方块位置", id, timeoutCount);
                status = TaskStatus.FIND_SLIME_POSITION;
                return;
            }
            if (redstoneTorchState.isReplaceable()) {
                Debug.info("[%s][%s][更新状态]: 红石火把未放置", id, timeoutCount);
                status = TaskStatus.PLACE_REDSTONE_TORCH;
            } else if (redstoneTorchState.isOf(Blocks.REDSTONE_TORCH)) {
                Debug.info("[%s][%s][更新状态]: 红石火把已放置", id, timeoutCount);
            } else if (redstoneTorchState.isOf(Blocks.REDSTONE_WALL_TORCH)) {
                Debug.info("[%s][%s][更新状态]: 红石火把放置状态错误", id, timeoutCount);
                status = TaskStatus.PLACE_ERROR_REDSTONE_TORCH;
            }
        }
    }


    private boolean onFindPistonPosition() {
        var pos = blockPos.up();
        if (CheckingEnvironmentUtils.has2BlocksOfPlaceToPlacePiston(world, blockPos)) {
            Debug.info("[%s][%s][状态处理][查找活塞位置]: 完成, %s", id, timeoutCount, pos);
            pistonBlockPos = pos;
            status = TaskStatus.WAIT_GAME_UPDATE;  // 等待更新状态
            return true;
        }
        Debug.info("[%s][%s][状态处理][查找活塞位置]: 失败", id, timeoutCount);
        MessageUtils.setOverlayMessageKey("bedrockminer.fail.place.piston");   // 无法放置活塞
        status = TaskStatus.FAILED;    // 失败状态
        return false;
    }

    private boolean onFindSlimePosition() {
        if (redstoneTorchBlockPos != null) {
            var pos = redstoneTorchBlockPos.down();
            if (world.getBlockState(pos).getMaterial().isReplaceable()) {
                slimeBlockPos = pos;
                Debug.info("[%s][%s][状态处理][查找基座位置]: 成功, %s", id, timeoutCount, slimeBlockPos);
                status = TaskStatus.WAIT_GAME_UPDATE;
                return true;
            }
        }
        Debug.info("[%s][%s][状态处理][查找基座位置]: 失败", id, timeoutCount);
        status = TaskStatus.FAILED;    // 失败状态
        return false;
    }

    private boolean onFindRedstoneTorchPosition() {
        if (pistonBlockPos != null) {
            // 优选1
            var redstoneTorchBlockPosList = CheckingEnvironmentUtils.findNearbyFlatBlockToPlaceRedstoneTorch(world, pistonBlockPos);
            if (redstoneTorchBlockPosList.size() > 0 && redstoneTorchBlockPos == null) {
                for (var pos : redstoneTorchBlockPosList) {
                    if (sideCoversSmallSquare(world, pos.down(), Direction.UP)) {
                        Debug.info("[%s][%s][状态处理][查找红石火把位置]: 优选1, %s", id, timeoutCount, pos);
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
                        Debug.info("[%s][%s][状态处理][查找红石火把位置]: 优选2, 基座：%s", id, timeoutCount, pos);
                        Debug.info("[%s][%s][状态处理][查找红石火把位置]: 优选2, %s", id, timeoutCount, pos.up());
                        slimeBlockPos = pos;
                        redstoneTorchBlockPos = pos.up();
                        break;
                    }
                }
            }

            // 无优选，使用默认方法1
            if (redstoneTorchBlockPosList.size() > 0 && redstoneTorchBlockPos == null) {
                redstoneTorchBlockPos = redstoneTorchBlockPosList.get(0);
                Debug.info("[%s][%s][状态处理][查找红石火把位置]: 默认1, %s", id, timeoutCount, redstoneTorchBlockPos);
            }
            if (possibleSlimeBlockPosList.size() > 0 && redstoneTorchBlockPos == null) {
                slimeBlockPos = possibleSlimeBlockPosList.get(0);
                redstoneTorchBlockPos = possibleSlimeBlockPosList.get(0).up();
                Debug.info("[%s][%s][状态处理][查找红石火把位置]: 默认2, 基座：%s", id, timeoutCount, slimeBlockPos);
                Debug.info("[%s][%s][状态处理][查找红石火把位置]: 默认2, %s", id, timeoutCount, redstoneTorchBlockPos);
            }

            if (redstoneTorchBlockPos != null) {
                Debug.info("[%s][%s][状态处理][查找红石火把位置]: 成功, %s", id, timeoutCount, redstoneTorchBlockPos);
                status = TaskStatus.WAIT_GAME_UPDATE;  // 等待更新状态
                return true;
            }
            Debug.info("[%s][%s][状态处理][查找红石火把位置]: 失败", id, timeoutCount);
        }

        status = TaskStatus.FAILED;    // 失败状态
        return false;
    }

    private boolean onPlacePiston() {
        if (pistonBlockPos == null) {
            Debug.info("[%s][%s][状态处理][放置活塞]: 活塞位置未获取", id, timeoutCount);
            redstoneTorchBlockPos = null;
            slimeBlockPos = null;
            status = TaskStatus.FIND_PISTON_POSITION;  // 等待更新状态
            return false;
        }
        // 放置前再次确认是否可以放置
        if (!CheckingEnvironmentUtils.has2BlocksOfPlaceToPlacePiston(world, blockPos)) {
            MessageUtils.setOverlayMessageKey("bedrockminer.fail.place.piston");   // 无法放置活塞
            status = TaskStatus.FAILED;
            Debug.info("[%s][%s][状态处理][放置活塞]: 无法放置", id, timeoutCount);
            return false;
        }
        InventoryManagerUtils.switchToItem(Blocks.PISTON);
        BlockPlacerUtils.pistonPlacement(pistonBlockPos, Direction.UP);
        status = TaskStatus.WAIT_GAME_UPDATE;  // 等待更新状态
        return false;
    }

    private boolean onPlaceSlimeBlock() {
        if (slimeBlockPos == null) {
            Debug.info("[%s][%s][更新状态]: 基座方块未知未获取, 无法放置", id, timeoutCount);
            status = TaskStatus.FIND_SLIME_POSITION;
            return false;
        }
        // 实体检测
        if (!CheckingEnvironmentUtils.canPlace(slimeBlockPos, Blocks.SLIME_BLOCK, Direction.UP)) {
            Debug.info("[%s][%s][更新状态]: 基座方块无法放置, 可能放置位置有实体(玩家)站在该位置上", id, timeoutCount);
            status = TaskStatus.FAILED;
            return false;
        }
        Debug.info("[%s][%s][更新状态]: 放置基座方块, %s", id, timeoutCount, slimeBlockPos);
        BlockPlacerUtils.simpleBlockPlacement(slimeBlockPos, Blocks.SLIME_BLOCK);
        status = TaskStatus.WAIT_GAME_UPDATE;  // 等待更新状态
        return false;
    }

    private boolean onPlaceRedstoneTorch() {
        if (redstoneTorchBlockPos == null) {
            Debug.info("[%s][%s][状态处理][放置红石火把]: 红石火把未获取", id, timeoutCount);
            slimeBlockPos = null;
            status = TaskStatus.FIND_REDSTONE_TORCH_POSITION;
            return false;
        }
        Debug.info("[%s][%s][状态处理][放置红石火把]: 放置红石火把, %s", id, timeoutCount, redstoneTorchBlockPos);
        BlockPlacerUtils.simpleBlockPlacement(redstoneTorchBlockPos, Blocks.REDSTONE_TORCH);
        status = TaskStatus.WAIT_GAME_UPDATE;
        return false;
    }

}


