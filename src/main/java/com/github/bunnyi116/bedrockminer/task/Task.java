package com.github.bunnyi116.bedrockminer.task;

import com.github.bunnyi116.bedrockminer.BedrockMiner;
import com.github.bunnyi116.bedrockminer.Debug;
import com.github.bunnyi116.bedrockminer.I18n;
import com.github.bunnyi116.bedrockminer.config.Config;
import com.github.bunnyi116.bedrockminer.util.*;
import com.google.common.collect.Queues;
import net.minecraft.block.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Queue;

import static com.github.bunnyi116.bedrockminer.BedrockMiner.player;
import static net.minecraft.block.Block.sideCoversSmallSquare;

public class Task {
    public final ClientWorld world;
    public final Block block;
    public final BlockPos pos;

    private TaskState currentState;
    private TaskState lastState;
    private @Nullable TaskState nextState;

    public final TaskPlanItem[] planItems;
    public @Nullable TaskPlanItem planItem;

    public final Queue<BlockPos> recycledQueue;
    public boolean executeModify;
    private int tickTotalCount;
    private int tickInternalCount;
    private int ticksTotalMax;
    private int ticksTimeoutMax;
    private int tickWaitMax;
    public int retryCount = 0;
    public int retryCountMax = 1;
    public boolean retry;
    public boolean executed;
    public boolean recycled;
    public boolean timeout;

    public Task(ClientWorld world, Block block, BlockPos pos) {
        this.world = world;
        this.block = block;
        this.pos = pos;
        this.planItems = TaskPlanTools.findAllPossible(pos, world);
        this.recycledQueue = Queues.newConcurrentLinkedQueue();
        this.init();
    }

    public boolean canInteractWithBlockAt() {
        if (this.world == BedrockMiner.world) {
            if (ClientPlayerInteractionManagerUtils.canInteractWithBlockAt(pos, 1.0F)) {
                if (planItem != null) {
                    return planItem.canInteractWithBlockAt();
                }
                return true;
            }
        }
        return false;
    }

    private void setWait(@Nullable TaskState nextState, int tickWaitMax) {
        this.nextState = nextState;
        this.tickWaitMax = Math.max(tickWaitMax, 1);
        this.currentState = TaskState.WAIT_CUSTOM;
    }

    private void setModifyLook(TaskPlan blockInfo) {
        if (blockInfo != null) {
            debug("修改视角");
            setModifyLook(blockInfo.facing);
            blockInfo.modify = true;
        }
    }

    private void setModifyLook(Direction facing) {
        PlayerLookManager.INSTANCE.set(facing, this);
    }

    private void resetModifyLook() {
        if (PlayerLookManager.INSTANCE.isModify()) {
            PlayerLookManager.INSTANCE.reset();
        }
    }

    public void tick() {
        if (this.currentState == TaskState.COMPLETE) {
            return;
        }
        this.lastState = this.currentState; // 先将现有状态记录（debug输出）
        debug("开始");
        if (this.tickTotalCount >= this.ticksTotalMax) {
            this.currentState = TaskState.COMPLETE;
        }
        if (!this.timeout && this.tickTotalCount >= this.ticksTimeoutMax) {
            this.timeout = true;
            this.currentState = TaskState.TIMEOUT;
        }
        this.tickInternalCount = 0;
        while (tickInternalCount < 10) {
            this.lastState = this.currentState;
            switch (this.currentState) {
                case INITIALIZE -> this.init();
                case WAIT_GAME_UPDATE -> this.updateStates();
                case WAIT_CUSTOM -> this.waitCustom();
                case FIND -> this.find();
                case PLACE_PISTON -> this.placePiston();
                case PLACE_REDSTONE_TORCH -> this.placeRedstoneTorch();
                case PLACE_SLIME_BLOCK -> this.placeSlimeBlock();
                case EXECUTE -> this.execute();
                case RETRY -> {
                    retry = true;
                    if (!recycledQueue.isEmpty()) {
                        currentState = TaskState.RECYCLED_ITEMS;
                        return;
                    }
                    if (retryCount < retryCountMax) {
                        retryCount++;
                        debug("任务物品回收已完成, 超时重试: %s", retryCount);
                        currentState = TaskState.INITIALIZE;
                    } else {
                        currentState = TaskState.COMPLETE;
                    }
                }
                case TIMEOUT -> {
                    debug("任务已超时");
                    currentState = TaskState.RETRY;
                }
                case FAIL -> {
                    debug("任务已失败");
                    currentState = TaskState.RETRY;
                }
                case RECYCLED_ITEMS -> this.recycledItems();
                case COMPLETE -> debug("任务已完成");
            }
            if (this.lastState == this.currentState) {  // 开始状态与结束状态一致, 避免无意义的内循环
                debug("状态一致，无需内部循环");
                break;
            }

            if (this.currentState.isExclusiveTick()) {
                if (this.lastState.isExclusiveTick()) {
                    if (this.lastState == TaskState.WAIT_GAME_UPDATE) {
                        debug("避免浪费TICK");
                        continue;
                    }
                    debug("独占TICK运行");
                    break;
                }
            }
            ++tickInternalCount;
        }
        debug("结束\r\n");
        ++tickTotalCount;
    }

    private void placeSlimeBlock() {
        if (planItem == null) {
            this.currentState = TaskState.FIND;
            return;
        }
        BlockPlacerUtils.placement(planItem.slimeBlock.pos, planItem.slimeBlock.facing, Items.SLIME_BLOCK);
        this.addRecycled(planItem.slimeBlock.pos);
        this.resetModifyLook();
        this.currentState = TaskState.WAIT_GAME_UPDATE;
    }

    private void placeRedstoneTorch() {
        if (planItem == null) {
            this.currentState = TaskState.FIND;
            return;
        }
        debug("红石火把");
        if (planItem.redstoneTorch.isNeedModify() && !planItem.redstoneTorch.modify) {
            setModifyLook(planItem.redstoneTorch);
            return;
        }
        BlockPlacerUtils.placement(planItem.redstoneTorch.pos, planItem.redstoneTorch.facing, Items.REDSTONE_TORCH);
        this.addRecycled(planItem.redstoneTorch.pos);
        this.setWait(TaskState.WAIT_GAME_UPDATE, Config.INSTANCE.taskShortWait ? 1 : 2);
        this.resetModifyLook();
    }

    private void placePiston() {
        if (planItem == null) {
            this.currentState = TaskState.FIND;
            return;
        }
        debug("放置活塞");
        var placeBlockState = Blocks.PISTON.getDefaultState().with(PistonBlock.FACING, planItem.piston.facing);
        if (BlockPlacerUtils.canPlace(world, planItem.piston.pos, placeBlockState)) {
            if (planItem.piston.isNeedModify() && !planItem.piston.modify) {
                setModifyLook(planItem.piston);
                return;
            }
            BlockPlacerUtils.placement(planItem.piston.pos, planItem.piston.facing, Items.PISTON);
            this.addRecycled(planItem.piston.pos);
            this.setWait(TaskState.WAIT_GAME_UPDATE, Config.INSTANCE.taskShortWait ? 1 : 3);
            this.resetModifyLook();
        } else {
            this.planItem = null;
            this.currentState = TaskState.FIND;
        }
    }

    private void find() {
        if (this.planItem == null) {
            debug("查找方案");
            for (TaskPlanItem item : planItems) {
                if (!item.isWorldValid()) {
                    continue;
                }
                final var pistonPos = item.piston.pos;
                final var pistonFacing = item.piston.facing;
                final var pistonHeadPos = pistonPos.offset(pistonFacing);
                final var pistonState = world.getBlockState(pistonPos);
                final var pistonHeadState = world.getBlockState(pistonHeadPos);
                final var pistonDefaultState = Blocks.PISTON.getDefaultState().with(PistonBlock.FACING, pistonFacing);
                final var pistonHeadDefaultState = Blocks.PISTON_HEAD.getDefaultState().with(PistonHeadBlock.FACING, pistonFacing);
                if (!BlockPlacerUtils.canPlace(world, pistonPos, pistonDefaultState) || !BlockPlacerUtils.canPlace(world, pistonHeadPos, pistonHeadDefaultState)) {
                    if (!(pistonState.isOf(Blocks.PISTON) && pistonHeadState.isOf(Blocks.PISTON_HEAD))) {
                        continue;
                    }
                }
                final var redstoneTorchState = world.getBlockState(item.redstoneTorch.pos);
                if (!redstoneTorchState.isReplaceable()) {  // 如果该位置已存在方块
                    // 当前位置方块类型
                    if (!(redstoneTorchState.getBlock() instanceof RedstoneTorchBlock
                            || redstoneTorchState.getBlock() instanceof WallRedstoneTorchBlock
                    )) {
                        continue;
                    }
                }
                if (BlockPlacerUtils.canPlace(world, item.slimeBlock.pos, Blocks.SLIME_BLOCK.getDefaultState()) || sideCoversSmallSquare(world, item.slimeBlock.pos, item.slimeBlock.facing)) {// 特殊放置方案类型1, 需要检查目标方块是否能被充能
                    if (item.redstoneTorch.type == 1 && !world.getBlockState(pos).isSolidBlock(world, pos)) {
                        continue;
                    }
                    this.planItem = item;
                    break;
                }

            }
        }
        if (this.planItem == null) {
            this.currentState = TaskState.FAIL;
            MessageUtils.setOverlayMessage(Text.literal(I18n.HANDLE_SEEK.getString().replace("%BlockPos%", pos.toShortString())));
        } else {
            this.debug("目标: %s", pos);
            this.debug("方案: %s", this.planItem.direction);
            this.debug("活塞: %s", this.planItem.piston);
            this.debug("底座: %s", this.planItem.slimeBlock);
            this.debug("红石火把: %s", this.planItem.redstoneTorch);
            this.currentState = TaskState.WAIT_GAME_UPDATE;
        }
    }

    private void recycledItems() {
        if (!recycledQueue.isEmpty()) {
            var blockPos = recycledQueue.peek();
            var blockState = world.getBlockState(blockPos);
            debug("任务物品正在回收: (%s) --> %s", blockPos.toShortString(), blockState.getBlock().getName().getString());
            if (blockState.getBlock().getHardness() < 0) {
                recycledQueue.remove(blockPos);
                recycledItems();
            }
            var instant = world.getBlockState(blockPos).calcBlockBreakingDelta(player, world, blockPos) >= ClientPlayerInteractionManagerUtils.BREAKING_PROGRESS_MAX;
            if (!instant) {
                InventoryManagerUtils.autoSwitch(blockState);
            }
            ClientPlayerInteractionManagerUtils.updateBlockBreakingProgress(blockPos);
            if (blockState.isReplaceable()) {
                recycledQueue.remove(blockPos);
            }
            if (instant && !recycledQueue.isEmpty()) {
                recycledItems();
            }
        }
        if (recycledQueue.isEmpty()) {
            debug("任务物品回收已完成");
            if (retry) {
                currentState = TaskState.RETRY;
            } else {
                currentState = TaskState.COMPLETE;
            }
        }

    }

    private void execute() {
        if (executed || player == null || planItem == null) {
            return;
        }
        if (!executeModify && planItem.direction.getAxis().isHorizontal()) {
            this.setModifyLook(planItem.direction.getOpposite());
            this.executeModify = true;
            return;
        } else {
            // 切换到工具
            if (world.getBlockState(planItem.piston.pos).calcBlockBreakingDelta(player, world, planItem.piston.pos) < 1F) {
                InventoryManagerUtils.autoSwitch(world.getBlockState(planItem.piston.pos));
                this.setWait(TaskState.EXECUTE, 1);
                return;
            }
            // 打掉附近红石火把
            final var nearbyRedstoneTorch = TaskPlanTools.findPistonNearbyRedstoneTorch(planItem.piston.pos, world);
            for (final var pos : nearbyRedstoneTorch) {
                if (world.getBlockState(pos).getBlock() instanceof RedstoneTorchBlock) {
                    ClientPlayerInteractionManagerUtils.updateBlockBreakingProgress(pos);
                }
            }
            if (world.getBlockState(planItem.redstoneTorch.pos).getBlock() instanceof RedstoneTorchBlock) {
                ClientPlayerInteractionManagerUtils.updateBlockBreakingProgress(planItem.redstoneTorch.pos);
            }
            ClientPlayerInteractionManagerUtils.updateBlockBreakingProgress(planItem.piston.pos);
            BlockPlacerUtils.placement(planItem.piston.pos, planItem.direction.getOpposite(), Items.PISTON);
            this.addRecycled(planItem.piston.pos);
            if (this.executeModify) {
                this.resetModifyLook();
            }
            this.executed = true;
        }
        this.setWait(TaskState.WAIT_GAME_UPDATE, 4);
    }

    private void waitCustom() {
        if (--this.tickWaitMax <= 0) {
            this.currentState = this.nextState == null ? TaskState.WAIT_GAME_UPDATE : this.nextState;
            this.tickWaitMax = 0;
            this.debug("等待已结束, 状态设置为: %s", this.currentState);
        } else {
            ++this.ticksTotalMax;
            ++this.ticksTimeoutMax;
            this.debug("剩余等待TICK: %s", tickWaitMax);
        }
    }

    private void updateStates() {
        if (!world.getBlockState(pos).isOf(block)) {
            this.currentState = TaskState.RECYCLED_ITEMS;
            this.debugUpdateStates("目标不存在");
            return;
        }
        if (this.planItem == null) {
            this.currentState = TaskState.FIND;
            this.debugUpdateStates("没有正在执行的放置方案, 准备查找可执行方案");
            return;
        }
        if (world.getBlockState(planItem.piston.pos).isOf(Blocks.MOVING_PISTON)) {
            this.debugUpdateStates("活塞正在移动");
            return;
        }
        if (!this.executed) {
            debugUpdateStates("任务未执行过");
            // 活塞
            if (world.getBlockState(this.planItem.piston.pos).isReplaceable()) {
                this.debugUpdateStates("[%s] [%s] 活塞未放置且该位置可放置物品,设置放置状态", this.planItem.piston.pos.toShortString(), this.planItem.piston.facing);
                this.currentState = TaskState.PLACE_PISTON;
                return;
            }
            if (world.getBlockState(this.planItem.piston.pos).getBlock() instanceof PistonBlock) {
                if (world.getBlockState(this.planItem.piston.pos).get(PistonBlock.FACING) != this.planItem.piston.facing) {
                    this.debugUpdateStates("[%s] [%s] 活塞已放置, 但放置方向不正确", this.planItem.piston.pos.toShortString(), this.planItem.piston.facing);
                    this.currentState = TaskState.FAIL;
                    return;
                }
            }
            // 底座
            if (world.getBlockState(this.planItem.slimeBlock.pos).isReplaceable()) {
                this.debugUpdateStates("[%s] [%s] 底座未放置且该位置可放置物品,设置放置状态", this.planItem.slimeBlock.pos.toShortString(), this.planItem.slimeBlock.facing);
                this.currentState = TaskState.PLACE_SLIME_BLOCK;
                return;
            }
            if (!Block.sideCoversSmallSquare(world, this.planItem.slimeBlock.pos, this.planItem.slimeBlock.facing)) {
                this.debugUpdateStates("[%s] [%s] 底座已放置, 但不是完整的方块", this.planItem.slimeBlock.pos.toShortString(), this.planItem.slimeBlock.facing);
                this.currentState = TaskState.FAIL;
                return;
            }
            // 红石火把
            if (world.getBlockState(this.planItem.redstoneTorch.pos).isReplaceable()) {
                this.debugUpdateStates("[%s] [%s] 红石火把未放置且该位置可放置物品,设置放置状态", this.planItem.redstoneTorch.pos.toShortString(), this.planItem.redstoneTorch.facing);
                this.currentState = TaskState.PLACE_REDSTONE_TORCH;
                return;
            }
            if (world.getBlockState(this.planItem.redstoneTorch.pos).getBlock() instanceof RedstoneTorchBlock) {
                boolean b = false;
                if (world.getBlockState(this.planItem.redstoneTorch.pos).getBlock() instanceof WallRedstoneTorchBlock) {
                    if (world.getBlockState(this.planItem.redstoneTorch.pos).get(WallRedstoneTorchBlock.FACING) != this.planItem.redstoneTorch.facing) {
                        b = true;
                    }
                } else if (this.planItem.redstoneTorch.facing != Direction.UP) {
                    b = true;
                }
                if (b) {
                    this.debugUpdateStates("[%s] [%s] 红石火把已放置, 但放置状态与方案不一致", this.planItem.redstoneTorch.pos.toShortString(), this.planItem.redstoneTorch.facing);
                    this.currentState = TaskState.FAIL;
                }
            }
            if (world.getBlockState(this.planItem.piston.pos).getBlock() instanceof PistonBlock) {
                if (world.getBlockState(this.planItem.piston.pos).contains(PistonBlock.EXTENDED)) {
                    if (world.getBlockState(this.planItem.piston.pos).get(PistonBlock.EXTENDED)) {
                        this.debugUpdateStates("[%s] [%s] 条件已充足, 准备开始尝试", this.planItem.piston.pos.toShortString(), this.planItem.piston.facing);
                        this.currentState = TaskState.EXECUTE;
                        return;
                    }
                }
            }
            this.debugUpdateStates("？？？");
        }
    }

    private void init() {
        this.nextState = null;
        this.tickTotalCount = 0;
        this.ticksTotalMax = 100;
        this.ticksTimeoutMax = 45;
        this.tickWaitMax = 0;
        this.planItem = null;
        this.recycledQueue.clear();
        this.executed = false;
        this.recycled = false;
        this.timeout = false;
        this.currentState = TaskState.WAIT_GAME_UPDATE;
        this.retry = false;
    }

    private void debug(String var1, Object... var2) {
        Debug.write("[{}/{}] [{}:{}/{}] [{} -> {}] {}",
                retryCount, retryCountMax,
                tickTotalCount, tickInternalCount, ticksTotalMax,
                lastState, currentState,
                String.format(var1, var2));
    }

    private void debugUpdateStates(String var1, Object... var2) {
        Debug.write("[{}/{}] [{}:{}/{}] [{} -> {}] [状态更新] {}",
                retryCount, retryCountMax,
                tickTotalCount, tickInternalCount, ticksTotalMax,
                lastState, currentState,
                String.format(var1, var2));
    }

    private void addRecycled(BlockPos pos) {
        if (!recycledQueue.contains(pos)) {
            recycledQueue.add(pos);
        }
    }

    public boolean isComplete() {
        return currentState == TaskState.COMPLETE || tickTotalCount >= ticksTotalMax;
    }

    public TaskState getCurrentState() {
        return currentState;
    }
}
