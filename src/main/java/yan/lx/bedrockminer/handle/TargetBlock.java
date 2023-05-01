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

public class TargetBlock {
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

    private int count;
    private boolean hasTried;
    private Status status;
    private int recycleCount;
    private boolean retrying;
    private int retryMax;

    /**
     * 构造函数
     *
     * @param block    目标方块
     * @param blockPos 目标方块所在位置
     * @param world    目标方块所在世界
     */
    public TargetBlock(Block block, BlockPos blockPos, ClientWorld world) {
        // 赋值
        this.block = block;
        this.blockPos = blockPos;
        this.world = world;
        // 初始化
        this.id = UUID.randomUUID();
        this.pistonBlockPos = null;
        this.slimeBlockPos = null;
        this.redstoneTorchBlockPos = null;
        this.count = 0;
        this.hasTried = false;
        this.status = Status.INITIALIZATION;
        this.retrying = false;
        this.retryMax = 1;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public ClientWorld getWorld() {
        return world;
    }

    public void tick() {
        Debug.info();
        Debug.info("[%s][%s][当前处理状态]: %s", id, count, status);
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
                this.count = 0;
                this.recycleCount = 0;
                this.hasTried = false;
                this.status = Status.WAIT_GAME_UPDATE;  // 等待更新状态
                Debug.info("[%s][%s][状态处理][初始化]: 完成", id, count);
            }
            case FIND_PISTON_POSITION -> {
                pistonBlockPos = blockPos.up();
                // 检查活塞能否放置
                if (CheckingEnvironmentUtils.has2BlocksOfPlaceToPlacePiston(world, blockPos)) {
                    Debug.info("[%s][%s][状态处理][查找活塞位置]: 完成, %s", id, count, pistonBlockPos);
                } else {
                    Debug.info("[%s][%s][状态处理][查找活塞位置]: 失败", id, count);
                    MessageUtils.setOverlayMessageKey("bedrockminer.fail.place.piston");   // 无法放置活塞
                    status = Status.FAILED;    // 失败状态
                    return false;
                }
                status = Status.WAIT_GAME_UPDATE;  // 等待更新状态
            }
            case FIND_SLIME_POSITION -> {
                if (pistonBlockPos != null) {
                    var pos = pistonBlockPos.down();
                    if (world.getBlockState(pos).getMaterial().isReplaceable()) {
                        slimeBlockPos = pos;
                        status = Status.PLACE_SLIME_BLOCK;
                    }
                }
            }
            case FIND_REDSTONE_TORCH_POSITION -> {
                if (pistonBlockPos != null) {
                    var redstoneTorchBlockPosList = CheckingEnvironmentUtils.findNearbyFlatBlockToPlaceRedstoneTorch(world, pistonBlockPos);
                    if (redstoneTorchBlockPosList.size() > 0) {
                        for (var pos : redstoneTorchBlockPosList) {
                            // 优选：不用红石火把基座方块的位置且人物不在基座范围内，是可以放置的
                            var basePos = pos.down();
                            if (!world.getBlockState(basePos).isAir() && !CheckingEnvironmentUtils.isBlocked(basePos)) {
                                redstoneTorchBlockPos = pos;
                                break;
                            }
                            // 次选1：人物不在基座范围内，是可以放置的
                            if (!CheckingEnvironmentUtils.isBlocked(basePos)) {
                                redstoneTorchBlockPos = pos;
                                break;
                            }
                            // 次选2：人物不在基座范围内，是可以放置的
                            if (!world.getBlockState(basePos).isAir()) {
                                redstoneTorchBlockPos = pos;
                                break;
                            }
                        }
                        // 无优选，使用默认方法
                        if (redstoneTorchBlockPosList.size() > 0 && redstoneTorchBlockPos == null) {
                            redstoneTorchBlockPos = redstoneTorchBlockPosList.get(0);
                        }
                    }
                    if (redstoneTorchBlockPos != null) {
                        Debug.info("[%s][%s][状态处理][查找红石火把位置]: 成功, %s", id, count, redstoneTorchBlockPos);
                    } else {
                        // 查找可以放置红石火把基座位置
                        Debug.info("[%s][%s][状态处理][查找红石火把位置]: 失败, 准备尝试查找粘液块", id, count);
                        Debug.info("[%s][%s][状态处理][查找粘液块位置]: 准备, %s", id, count, slimeBlockPos);
                        slimeBlockPos = CheckingEnvironmentUtils.findPossibleSlimeBlockPos(world, blockPos);
                        if (slimeBlockPos != null) {
                            redstoneTorchBlockPos = slimeBlockPos.up();
                            status = Status.WAIT_GAME_UPDATE;  // 等待更新状态
                            Debug.info("[%s][%s][状态处理][查找粘液块位置]: 成功, %s", id, count, slimeBlockPos);
                            Debug.info("[%s][%s][状态处理][查找红石火把位置]: 成功, %s", id, count, redstoneTorchBlockPos);
                        } else {
                            MessageUtils.setOverlayMessageKey("bedrockminer.fail.place.redstonetorch"); // 无法放置红石火把(没有可放置的基座方块)
                            Debug.info("[%s][%s][状态处理][查找粘液块位置]: 失败", id, count);
                            Debug.info("[%s][%s][状态处理][查找红石火把位置]: 失败", id, count);
                            status = Status.FAILED;    // 失败状态
                            return false;
                        }
                    }
                } else {
                    status = Status.FAILED;    // 失败状态
                    return false;
                }

                status = Status.WAIT_GAME_UPDATE;  // 等待更新状态
            }
            case PLACE_PISTON -> {
                if (pistonBlockPos != null) {
                    if (!CheckingEnvironmentUtils.has2BlocksOfPlaceToPlacePiston(world, blockPos)) {
                        MessageUtils.setOverlayMessageKey("bedrockminer.fail.place.piston");   // 无法放置活塞
                        status = Status.FAILED;
                        Debug.info("[%s][%s][状态处理][放置活塞]: 无法放置", id, count);
                        return false;
                    }
                    InventoryManagerUtils.switchToItem(Blocks.PISTON);
                    BlockPlacerUtils.pistonPlacement(pistonBlockPos, Direction.UP);
                    if (world.getBlockState(pistonBlockPos).isOf(Blocks.PISTON)) {
                        Debug.info("[%s][%s][状态处理][放置活塞]: 放置成功", id, count);
                    } else {
                        MessageUtils.setOverlayMessageKey("bedrockminer.fail.place.piston");   // 无法放置活塞
                        Debug.info("[%s][%s][状态处理][放置活塞]: 放置失败", id, count);
                        status = Status.FAILED;
                        return false;
                    }
                }
                status = Status.WAIT_GAME_UPDATE;  // 等待更新状态
            }
            case PLACE_SLIME_BLOCK -> {
                if (slimeBlockPos != null) {
                    Debug.info("[%s][%s][状态处理][放置粘液块]: 放置准备, %s", id, count, slimeBlockPos);
                    BlockPlacerUtils.simpleBlockPlacement(slimeBlockPos, Blocks.SLIME_BLOCK);
                    if (world.getBlockState(slimeBlockPos).isOf(Blocks.SLIME_BLOCK)) {
                        Debug.info("[%s][%s][状态处理][放置粘液块]: 放置成功", id, count);
                    } else {
                        MessageUtils.setOverlayMessageKey("bedrockminer.fail.place.slimeBlock");
                        Debug.info("[%s][%s][状态处理][放置粘液块]: 放置失败", id, count);
                        status = Status.FAILED;
                        return false;
                    }
                }
                status = Status.WAIT_GAME_UPDATE;  // 等待更新状态
            }
            case PLACE_REDSTONE_TORCH -> {
                if (redstoneTorchBlockPos != null) {
                    if (!world.getBlockState(redstoneTorchBlockPos.down()).isAir()) {
                        if (Block.sideCoversSmallSquare(world, redstoneTorchBlockPos.down(), Direction.UP)) {
                            Debug.info("[%s][%s][状态处理][放置红石火把]: 准备放置, %s", id, count, redstoneTorchBlockPos);
                            BlockPlacerUtils.simpleBlockPlacement(redstoneTorchBlockPos, Blocks.REDSTONE_TORCH);
                            if (world.getBlockState(redstoneTorchBlockPos).isOf(Blocks.REDSTONE_TORCH)) {
                                Debug.info("[%s][%s][状态处理][放置红石火把]: 放置成功", id, count);
                            } else if (!world.getBlockState(redstoneTorchBlockPos).isOf(Blocks.REDSTONE_TORCH)) {
                                Debug.info("[%s][%s][状态处理][放置红石火把]: 放置状态不对,重新放置！", id, count);
                                if (BlockBreakerUtils.breakBlock(redstoneTorchBlockPos)) {
                                    BlockPlacerUtils.simpleBlockPlacement(redstoneTorchBlockPos, Blocks.REDSTONE_TORCH);
                                }
                            } else {
                                Debug.info("[%s][%s][状态处理][放置红石火把]: 放置失败", id, count);
                            }
                            status = Status.WAIT_GAME_UPDATE;
                            return false;
                        }
                    }
                }
                Debug.info("[%s][%s][状态处理][放置红石火把]: 放置失败, 红石位置未获取", id, count);
                status = Status.FIND_REDSTONE_TORCH_POSITION;
            }
            case EXTENDED_START -> {
                if (!hasTried && pistonBlockPos != null) {
                    Debug.info("[%s][%s][状态处理][扩展]：准备开始", id, count);
                    // 打掉活塞附近能充能的红石火把
                    var nearByRedstoneTorchPosList = CheckingEnvironmentUtils.findNearbyRedstoneTorch(world, pistonBlockPos);
                    for (BlockPos pos : nearByRedstoneTorchPosList) {
                        Debug.info("[%s][%s][状态处理][扩展]：打掉红石火把, %s", id, count, pos);
                        BlockBreakerUtils.instantBreakPistonBlock(pos);
                    }
                    // 打掉活塞
                    Debug.info("[%s][%s][状态处理][扩展]：打掉活塞, %s", id, count, pistonBlockPos);
                    if (BlockBreakerUtils.instantBreakPistonBlock(pistonBlockPos)) {
                        this.status = Status.FAILED;
                    }

                    // 放置朝下的活塞
                    Debug.info("[%s][%s][状态处理][扩展]：放置朝下的活塞, %s", id, count, pistonBlockPos);
                    BlockPlacerUtils.pistonPlacement(pistonBlockPos, Direction.DOWN);

                    hasTried = true;
                    Debug.info("[%s][%s][状态处理][扩展]：扩展完成", id, count);
                    status = Status.WAIT_GAME_UPDATE;  // 等待状态
                }
            }
            case PISTON_MOVING -> status = Status.WAIT_GAME_UPDATE;  // 等待更新状态
            case WAIT_GAME_UPDATE -> {
                return true;
            }
            case TIME_OUT -> {
                status = Status.ITEM_RECYCLING;
                return false;
            }
            case FAILED -> {
                status = Status.ITEM_RECYCLING;
                Debug.info("[%s][%s][状态处理][失败]：准备物品回收", id, count);
                return false;
            }
            case ITEM_RECYCLING -> {
                // 回收任务超时直接退出任务
                if (recycleCount++ > 25) {
                    status = Status.FINISH;
                }
                // 跳过3tick, 因为活塞位置可能在执行活塞回收任务
                if (recycleCount < 3) {
                    return false;
                }
                // 活塞
                if (pistonBlockPos != null) {
                    // 可能残留的活塞1
                    BlockPos pistonPos1 = pistonBlockPos.up();
                    if (world.getBlockState(pistonPos1).isOf(Blocks.PISTON)) {
                        Debug.info("[%s][%s][状态处理][物品回收][%s][活塞up]: %s", id, count, recycleCount, pistonPos1);
                        if (BlockBreakerUtils.breakPistonBlock(pistonPos1)) {
                            return false;
                        }
                    }
                    // 可能残留的活塞2
                    BlockPos pistonPos2 = pistonBlockPos.up().up();
                    if (world.getBlockState(pistonPos2).isOf(Blocks.PISTON)) {
                        Debug.info("[%s][%s][状态处理][物品回收][%s][活塞upup]: %s", id, count, recycleCount, pistonPos2);
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
                        Debug.info("[%s][%s][状态处理][物品回收][%s][活塞]: %s", id, count, recycleCount, pistonBlockPos);
                        // 破坏活塞
                        if (BlockBreakerUtils.breakPistonBlock(pistonBlockPos)){
                            pistonBlockPos = null;
                        }

                    } else if (blockState.isAir()) {
                        pistonBlockPos = null;
                    }
                }
                // 红石火把
                if (redstoneTorchBlockPos != null) {
                    var blockState = world.getBlockState(redstoneTorchBlockPos);
                    if (blockState.isOf(Blocks.REDSTONE_TORCH) || blockState.isOf(Blocks.REDSTONE_WALL_TORCH)) {
                        Debug.info("[%s][%s][状态处理][物品回收][%s][红石火把]: %s", id, count, recycleCount, redstoneTorchBlockPos);
                        if (BlockBreakerUtils.breakBlock(redstoneTorchBlockPos, Direction.UP)) {
                            redstoneTorchBlockPos = null;
                        }
                    } else if (blockState.isAir()) {
                        redstoneTorchBlockPos = null;
                    }
                }
                // 粘液块
                if (slimeBlockPos != null) {
                    var blockState = world.getBlockState(slimeBlockPos);
                    if (blockState.isOf(Blocks.SLIME_BLOCK)) {
                        Debug.info("[%s][%s][状态处理][物品回收][%s][粘液块]: %s", id, count, recycleCount, slimeBlockPos);
                        if (BlockBreakerUtils.breakBlock(slimeBlockPos, Direction.UP)) {
                            slimeBlockPos = null;
                        }
                    } else if (blockState.isAir()) {
                        slimeBlockPos = null;
                    }
                }

                if (pistonBlockPos == null && redstoneTorchBlockPos == null && slimeBlockPos == null) {
                    if (retrying) {
                        status = Status.RETRY;
                    } else {
                        status = Status.FINISH;
                    }
                }
                return false;
            }
            case RETRY -> {
                this.pistonBlockPos = null;
                this.slimeBlockPos = null;
                this.redstoneTorchBlockPos = null;
                this.count = 0;
                this.hasTried = false;
                this.status = Status.INITIALIZATION;
                this.retrying = false;
                Debug.info("[%s][%s][状态处理][重试]: 重新尝试", id, count);
            }
            case FINISH -> {
                return false;
            }
        }
        return true;
    }

    private void updateStatus() {
        if (count++ > 20) {
            Debug.info("[%s][%s][玩家交互更新]: 超时", id, count);
            status = Status.TIME_OUT;
            return;
        }

        // 游戏更新
        if (status == Status.WAIT_GAME_UPDATE) {
            // 检查目标方块是否存在(成功)
            if (world.getBlockState(blockPos).isAir()) {
                // 获取需要回收物品的清单
                Debug.info("[%s][%s][更新状态]: 目标方块(%s)已不存在, 准备执行回收任务", id, count, block.getName().getString());
                status = Status.ITEM_RECYCLING; // 物品回收状态
                return;
            }
            if (pistonBlockPos != null) {
                // 优先检查活塞当前是否还处于技术性方块(36号方块)
                if (world.getBlockState(pistonBlockPos).isOf(Blocks.MOVING_PISTON) || world.getBlockState(pistonBlockPos.up()).isOf(Blocks.MOVING_PISTON)) {
                    Debug.info("[%s][%s][更新状态]: 活塞移动中", id, count);
                    status = Status.PISTON_MOVING;
                    return;
                }
            }
            // 检查活塞位置
            if (pistonBlockPos == null) {
                Debug.info("[%s][%s][更新状态]: 活塞坐标位置未获取", id, count);
                status = Status.FIND_PISTON_POSITION;
                return;
            }
            // 检查红石火把位置
            if (redstoneTorchBlockPos == null) {
                Debug.info("[%s][%s][更新状态]: 红石火把位置未获取", id, count);
                status = Status.FIND_REDSTONE_TORCH_POSITION;
                return;
            }
            // 活塞存在
            if (world.getBlockState(pistonBlockPos).isOf(Blocks.PISTON)) {
                if (hasTried) {
                    if (retryMax-- > 0) {
                        retrying = true;
                    }
                } else {
                    var direction = world.getBlockState(pistonBlockPos).get(PistonBlock.FACING);
                    Debug.info("[%s][%s][更新状态]: 活塞已放置, %s", id, count, direction);
                    if (direction == Direction.UP) {
                        // 活塞已充能(扩展开始)
                        if (world.getBlockState(pistonBlockPos).get(PistonBlock.EXTENDED)) {
                            status = Status.EXTENDED_START;    // 扩展开始
                            Debug.info("[%s][%s][更新状态]: 条件充足,准备扩展", id, count);
                        }
                        // 活塞未充能, 活塞朝向：向上(红石火把可能被之前的任务给清理掉了,需要重新放置)
                        else {
                            if (world.getBlockState(pistonBlockPos).get(PistonBlock.FACING) == Direction.UP) {
                                if (slimeBlockPos != null && world.getBlockState(slimeBlockPos).getMaterial().isReplaceable()) {
                                    status = Status.PLACE_SLIME_BLOCK;  // 放置粘液块
                                    Debug.info("[%s][%s][更新状态]: 需要放置红石火把基座", id, count);
                                } else {
                                    status = Status.PLACE_REDSTONE_TORCH;  // 放置红石火把状态
                                    Debug.info("[%s][%s][更新状态]: 需要放置红石火把", id, count);
                                }

                            }
                        }
                    }
                }
            }
            // 活塞不存在
            else {
                this.status = Status.PLACE_PISTON;   // 放置活塞状态
                Debug.info("[%s][%s][更新状态]: 活塞未放置", id, count);
            }

        }

    }

    public Status getStatus() {
        return status;
    }
}


