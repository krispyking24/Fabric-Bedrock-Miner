package yan.lx.bedrockminer.utils;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.Debug;
import yan.lx.bedrockminer.model.ItemRecycleInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class TargetBlock {
    private final Block block;
    private final BlockPos blockPos;
    private final ClientWorld world;

    @Nullable
    private BlockPos pistonBlockPos;
    @Nullable
    private BlockPos redstoneTorchBlockPos;
    @Nullable
    private BlockPos slimeBlockPos;


    private int tickTimes;
    private boolean hasTried;
    private int tickRecycleTimes;
    private int retryMax;
    private Status status;

    private boolean isRecyclingTask;
    private final Queue<ItemRecycleInfo> recyclingTask;

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
        this.pistonBlockPos = null;
        this.slimeBlockPos = null;
        this.redstoneTorchBlockPos = null;
        this.tickTimes = 0;
        this.tickRecycleTimes = 0;
        this.retryMax = 1;
        this.hasTried = false;
        this.status = Status.INITIALIZATION;
        this.isRecyclingTask = false;
        this.recyclingTask = new LinkedList<>();
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public ClientWorld getWorld() {
        return world;
    }

    /**
     * 更新处理程序
     */
    public boolean updater() {
        Debug.info();
        Debug.info("[%s][当前处理状态]: %s", tickTimes, status);

        switch (this.status) {
            case INITIALIZATION, WAIT_GAME_UPDATE, PISTON_MOVING, EXTENDED_START,
                    PLACE_REDSTONE_TORCH, PLACE_SLIME_BLOCK, PLACE_PISTON,
                    FIND_REDSTONE_TORCH_POSITION, FIND_PISTON_POSITION -> {
                // 超时更新
                if (tickTimes > 40) {
                    Debug.info("[%s][玩家交互更新]: 超时", tickTimes);
                    status = Status.TIME_OUT;
                    return false;
                }
                Debug.info("[%s][玩家交互更新]: 准备获取更新状态", tickTimes);
                updateStatus();             // 更新当前状态
            }
            case TIME_OUT, FAILED, ITEM_RECYCLING, FINISH -> Debug.info("[%s][玩家交互更新]: 跳过更新状态", tickTimes);
        }


        Debug.info("[%s][玩家交互更新]: 准备处理状态", tickTimes);
        boolean finish = updateHandler();     // 状态处理
        Debug.info("[%s][玩家交互更新]: 处理状态完成", tickTimes);
        if (finish) {
            Debug.info("[%s][玩家交互更新]: 当前任务结束", tickTimes);
        }
        tickTimes++;
        return finish;
    }


    private boolean updateHandler() {
        switch (status) {
            case INITIALIZATION -> {
                Debug.info("[%s][状态处理][初始化]: 准备", tickTimes);
                this.tickTimes = 0;
                this.tickRecycleTimes = 0;
                this.hasTried = false;
                this.isRecyclingTask = false;
                this.status = Status.WAIT_GAME_UPDATE;  // 等待更新状态
                Debug.info("[%s][状态处理][初始化]: 完成", tickTimes);
            }
            case FIND_PISTON_POSITION -> {
                Debug.info("[%s][状态处理][查找活塞位置]: 准备", tickTimes);
                this.pistonBlockPos = blockPos.up();
                // 检查活塞能否放置
                if (CheckingEnvironment.has2BlocksOfPlaceToPlacePiston(world, blockPos)) {
                    Debug.info("[%s][状态处理][查找活塞位置]: 完成, %s", tickTimes, pistonBlockPos);
                    this.status = Status.WAIT_GAME_UPDATE;  // 等待更新状态
                } else {
                    Messager.actionBar("[%s][状态处理]bedrockminer.fail.place.piston");   // 无法放置活塞
                    this.status = Status.FAILED;  // 失败状态
                }
            }
            case FIND_REDSTONE_TORCH_POSITION -> {
                Debug.info("[%s][状态处理][查找红石火把位置]: 准备", tickTimes);
                this.redstoneTorchBlockPos = CheckingEnvironment.findNearbyFlatBlockToPlaceRedstoneTorch(world, blockPos);
                if (redstoneTorchBlockPos != null) {
                    Debug.info("[%s][状态处理][查找红石火把位置]: 成功, %s", tickTimes, redstoneTorchBlockPos);
                    this.status = Status.WAIT_GAME_UPDATE;  // 等待更新状态
                } else {
                    // 查找可以放置红石火把基座位置
                    Debug.info("[%s][状态处理][查找红石火把位置]: 失败, 准备尝试查找粘液块");
                    Debug.info("[%s][状态处理][查找粘液块位置]: 准备, %s", tickTimes, this.slimeBlockPos);
                    this.slimeBlockPos = CheckingEnvironment.findPossibleSlimeBlockPos(world, blockPos);
                    if (this.slimeBlockPos != null) {
                        this.redstoneTorchBlockPos = this.slimeBlockPos.up();
                        this.status = Status.WAIT_GAME_UPDATE;  // 等待更新状态
                        Debug.info("[%s][状态处理][查找粘液块位置]: 成功, %s", tickTimes, this.slimeBlockPos);
                        Debug.info("[%s][状态处理][查找红石火把位置]: 成功, %s", tickTimes, this.redstoneTorchBlockPos);
                    } else {
                        Messager.actionBar("bedrockminer.fail.place.redstonetorch"); // 无法放置红石火把(没有可放置的基座方块)
                        this.status = Status.FAILED;    // 失败状态
                        Debug.info("[%s][状态处理][查找粘液块位置]: 失败", tickTimes);
                    }
                }

            }
            case PLACE_PISTON -> {
                if (pistonBlockPos != null) {
                    Debug.info("[%s][状态处理][放置活塞]: 放置准备, %s", tickTimes, pistonBlockPos);
                    if (!CheckingEnvironment.has2BlocksOfPlaceToPlacePiston(world, blockPos)) {
                        Messager.actionBar("bedrockminer.fail.place.piston");   // 无法放置活塞
                        this.status = Status.FAILED;
                        Debug.info("[%s][状态处理][放置活塞]: 无法放置", tickTimes);
                        return false;
                    }

                    InventoryManager.switchToItem(Blocks.PISTON);
                    BlockPlacer.pistonPlacement(pistonBlockPos, Direction.UP);
                    if (world.getBlockState(pistonBlockPos).isOf(Blocks.PISTON)) {
                        Debug.info("[%s][状态处理][放置活塞]: 放置成功", tickTimes);
                    } else {
                        Debug.info("[%s][状态处理][放置活塞]: 放置失败", tickTimes);
                        this.status = Status.FAILED;
                        return false;
                    }
                }
                this.status = Status.WAIT_GAME_UPDATE;  // 等待更新状态
            }
            case PLACE_SLIME_BLOCK -> {
                if (slimeBlockPos != null) {
                    Debug.info("[%s][状态处理][放置粘液块]: 放置准备, %s", tickTimes, slimeBlockPos);
                    BlockPlacer.simpleBlockPlacement(slimeBlockPos, Blocks.SLIME_BLOCK);
                    if (world.getBlockState(slimeBlockPos).isOf(Blocks.SLIME_BLOCK)) {
                        Debug.info("[%s][状态处理][放置粘液块]: 放置成功", tickTimes);
                    } else {
                        Debug.info("[%s][状态处理][放置粘液块]: 放置失败", tickTimes);
                    }
                }
                this.status = Status.WAIT_GAME_UPDATE;  // 等待更新状态
            }
            case PLACE_REDSTONE_TORCH -> {
                if (slimeBlockPos != null && !world.getBlockState(slimeBlockPos).isOf(Blocks.SLIME_BLOCK)) {
                    Debug.info("[%s][状态处理][放置红石火把]: 需要放置粘液块, %s", tickTimes, slimeBlockPos);
                    this.status = Status.PLACE_SLIME_BLOCK;  // 放置粘液块
                    return false;
                }
                Debug.info("[%s][状态处理][放置红石火把]: 准备放置, %s", tickTimes, redstoneTorchBlockPos);
                BlockPlacer.simpleBlockPlacement(redstoneTorchBlockPos, Blocks.REDSTONE_TORCH);

                if (!world.getBlockState(redstoneTorchBlockPos).isAir()) {
                    Debug.info("[%s][状态处理][放置红石火把]: 放置成功", tickTimes);
                } else {
                    Debug.info("[%s][状态处理][放置红石火把]: 放置失败", tickTimes);
                }
                this.status = Status.WAIT_GAME_UPDATE;  // 等待更新状态
            }
            case EXTENDED_START -> {
                if (!hasTried && pistonBlockPos != null) {
                    Debug.info("[%s][状态处理][扩展]：准备开始", tickTimes);
                    // 打掉活塞附近能充能的红石火把
                    ArrayList<BlockPos> nearByRedstoneTorchPosList = CheckingEnvironment.findNearbyRedstoneTorch(world, pistonBlockPos);
                    for (BlockPos pos : nearByRedstoneTorchPosList) {
                        Debug.info("[%s][状态处理][扩展]：打掉红石火把, %s", tickTimes, pos);
                        BlockBreaker.breakBlock(pos, Direction.UP);
                    }
                    // 打掉活塞
                    Debug.info("[%s][状态处理][扩展]：打掉活塞, %s", tickTimes, pistonBlockPos);
                    if (BlockBreaker.breakPistonBlock(pistonBlockPos)) {
                        this.status = Status.FAILED;
                    }

                    // 放置朝下的活塞
                    Debug.info("[%s][状态处理][扩展]：放置朝下的活塞, %s", tickTimes, pistonBlockPos);
                    BlockPlacer.pistonPlacement(pistonBlockPos, Direction.DOWN);

                    hasTried = true;
                    Debug.info("[%s][状态处理][扩展]：扩展完成", tickTimes);
                    this.status = Status.WAIT_GAME_UPDATE;  // 等待状态
                }
            }
            case PISTON_MOVING -> this.status = Status.WAIT_GAME_UPDATE;  // 等待更新状态
            case WAIT_GAME_UPDATE -> {
                return false;
            }
            case TIME_OUT -> status = Status.ITEM_RECYCLING;
            case FAILED -> {
                if (retryMax-- > 0) {
                    Debug.info("[%s][状态处理][失败]：准备重试", tickTimes);
                    this.status = Status.INITIALIZATION;
                } else {
                    status = Status.ITEM_RECYCLING;
                    Debug.info("[%s][状态处理][失败]：准备物品回收", tickTimes);
                }
            }
            case ITEM_RECYCLING -> {
                // 回收任务超时直接退出任务
                if (tickRecycleTimes++ > 40) {
                    this.status = Status.FINISH;
                }
                // 检查需要回收的物品
                if (!isRecyclingTask) {
                    boolean isRecyclePistonBlock = pistonBlockPos != null && world.getBlockState(pistonBlockPos).isOf(Blocks.PISTON);
                    boolean isRecycleRedstoneTorchBlock = redstoneTorchBlockPos != null && (world.getBlockState(redstoneTorchBlockPos).isOf(Blocks.REDSTONE_TORCH) || world.getBlockState(redstoneTorchBlockPos).isOf(Blocks.REDSTONE_WALL_TORCH));
                    boolean isRecycleSlimeBlock = slimeBlockPos != null && world.getBlockState(slimeBlockPos).isOf(Blocks.SLIME_BLOCK);
                    if (slimeBlockPos != null) {
                        Debug.info("[%s][状态处理][物品回收][%s][粘液块][BlockState]: %s", tickTimes, tickRecycleTimes, world.getBlockState(slimeBlockPos));
                        Debug.info("[%s][状态处理][物品回收][%s][粘液块][Block]: %s", tickTimes, tickRecycleTimes, world.getBlockState(slimeBlockPos).getBlock().getName());
                    }

                    // 粘液块
                    if (isRecycleSlimeBlock) {
                        recyclingTask.offer(new ItemRecycleInfo(Blocks.SLIME_BLOCK, slimeBlockPos));
                        Debug.info("[%s][状态处理][物品回收][%s][开始添加][粘液块]: %s", tickTimes, tickRecycleTimes, slimeBlockPos);
                    }

                    // 红石火把
                    if (isRecycleRedstoneTorchBlock) {
                        recyclingTask.offer(new ItemRecycleInfo(Blocks.REDSTONE_TORCH, redstoneTorchBlockPos));
                        Debug.info("[%s][状态处理][物品回收][%s][开始添加][红石火把]: %s", tickTimes, tickRecycleTimes, redstoneTorchBlockPos);
                    }

                    // 活塞
                    if (isRecyclePistonBlock) {
                        recyclingTask.offer(new ItemRecycleInfo(Blocks.PISTON, pistonBlockPos));
                        Debug.info("[%s][状态处理][物品回收][%s][开始添加][活塞]: %s", tickTimes, tickRecycleTimes, pistonBlockPos);
                    }

                    // 可能残留的活塞
                    if (pistonBlockPos != null) {
                        BlockPos pistonPos1 = pistonBlockPos.up();
                        if (world.getBlockState(pistonPos1).isOf(Blocks.PISTON)) {
                            recyclingTask.offer(new ItemRecycleInfo(Blocks.PISTON, pistonPos1));
                            Debug.info("[%s][状态处理][物品回收][%s][开始添加][活塞up]: %s", tickTimes, tickRecycleTimes, pistonPos1);
                        }
                        BlockPos pistonPos2 = pistonBlockPos.up().up();
                        if (world.getBlockState(pistonPos2).isOf(Blocks.PISTON)) {
                            recyclingTask.offer(new ItemRecycleInfo(Blocks.PISTON, pistonPos2));
                            Debug.info("[%s][状态处理][物品回收][%s][开始添加][活塞upup]: %s", tickTimes, tickRecycleTimes, pistonPos2);
                        }
                    }

                    isRecyclingTask = true;
                    return false;
                }
                // 处理物品回收
                if (recyclingTask.size() > 0) {
                    var itemRecycleInfo = recyclingTask.peek();
                    if (world.getBlockState(itemRecycleInfo.blockPos()).isAir()) {
                        if (world.getBlockState(itemRecycleInfo.blockPos()).isOf(Blocks.MOVING_PISTON)) {
                            Debug.info("[%s][状态处理][物品回收][%s][正在回收][%s]: 活塞移动中, %s", tickTimes, tickRecycleTimes, itemRecycleInfo.getName(), itemRecycleInfo.blockPos());
                            return false;
                        }
                        recyclingTask.remove(itemRecycleInfo);
                        Debug.info("[%s][状态处理][物品回收][%s][回收成功][%s]: %s", tickTimes, tickRecycleTimes, itemRecycleInfo.getName(), itemRecycleInfo.blockPos());
                        return false;
                    }
                    Debug.info("[%s][状态处理][物品回收][%s][开始回收][%s]: %s", tickTimes, tickRecycleTimes, itemRecycleInfo.getName(), itemRecycleInfo.blockPos());
                    BlockBreaker.breakBlock(itemRecycleInfo.blockPos(), Direction.UP, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);
                    return false;
                }
                this.status = Status.FINISH;
            }
            case FINISH -> {
                return true;
            }
        }
        return false;
    }

    private void updateStatus() {
        // 游戏更新
        if (this.status == Status.WAIT_GAME_UPDATE) {
            // 检查活塞位置
            if (pistonBlockPos == null) {
                Debug.info("[%s][更新状态]: 活塞坐标位置未获取", tickTimes);
                status = Status.FIND_PISTON_POSITION;
                return;
            }
            // 检查红石火把位置
            if (redstoneTorchBlockPos == null) {
                Debug.info("[%s][更新状态]: 红石火把位置未获取", tickTimes);
                status = Status.FIND_REDSTONE_TORCH_POSITION;
                return;
            }

            // 优先检查活塞当前是否还处于技术性方块(36号方块)
            if (world.getBlockState(pistonBlockPos).isOf(Blocks.MOVING_PISTON) || world.getBlockState(pistonBlockPos.up()).isOf(Blocks.MOVING_PISTON)) {
                Debug.info("[%s][更新状态]: 活塞移动中", tickTimes);
                status = Status.PISTON_MOVING;
                return;
            }

            // 检查目标方块是否存在(成功)
            if (world.getBlockState(blockPos).isAir()) {
                // 获取需要回收物品的清单
                Debug.info("[%s][更新状态]: 目标方块(%s)已不存在, 准备执行回收任务", tickTimes, block.getName().getString());
                status = Status.ITEM_RECYCLING; // 物品回收状态
                return;
            }

            // 未扩展过
            if (!hasTried) {
                // 活塞存在
                if (world.getBlockState(pistonBlockPos).isOf(Blocks.PISTON)) {
                    Debug.info("[%s][更新状态]: 活塞已放置", tickTimes);

                    Direction direction = world.getBlockState(pistonBlockPos).get(PistonBlock.FACING);
                    switch (direction) {
                        case UP -> {
                            // 活塞已充能(扩展开始)
                            if (world.getBlockState(pistonBlockPos).get(PistonBlock.EXTENDED)) {
                                this.status = Status.EXTENDED_START;    // 扩展开始
                                Debug.info("[%s][更新状态]: 条件充足,准备扩展", tickTimes);
                            }
                            // 活塞未充能, 活塞朝向：向上(红石火把可能被之前的任务给清理掉了,需要重新放置)
                            else {
                                if (world.getBlockState(pistonBlockPos).get(PistonBlock.FACING) == Direction.UP) {
                                    this.status = Status.PLACE_REDSTONE_TORCH;  // 放置红石火把状态
                                    Debug.info("[%s][更新状态]: 需要放置红石火把", tickTimes);
                                }
                            }
                        }
                        case DOWN, NORTH, SOUTH, WEST, EAST -> this.status = Status.FAILED;  // 放置失败,失败状态
                    }

                }
                // 活塞不存在
                else {
                    this.status = Status.PLACE_PISTON;   // 放置活塞状态
                    Debug.info("[%s][更新状态]: 活塞未放置", tickTimes);
                }
            }
        }

    }
}


