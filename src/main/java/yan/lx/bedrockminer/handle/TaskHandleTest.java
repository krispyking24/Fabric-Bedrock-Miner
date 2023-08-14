package yan.lx.bedrockminer.handle;

import com.google.common.collect.Queues;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.Debug;
import yan.lx.bedrockminer.model.TaskBlockInfo;
import yan.lx.bedrockminer.model.TaskSchemeInfo;
import yan.lx.bedrockminer.model.TaskSolutionInfo;
import yan.lx.bedrockminer.model.TaskTickInfo;
import yan.lx.bedrockminer.utils.BlockBreakerUtils;
import yan.lx.bedrockminer.utils.BlockPlacerUtils;
import yan.lx.bedrockminer.utils.BlockUtils;
import yan.lx.bedrockminer.utils.MessageUtils;

import java.util.Queue;

import static net.minecraft.block.Block.sideCoversSmallSquare;
import static yan.lx.bedrockminer.handle.TaskHandleState.*;
import static yan.lx.bedrockminer.model.TaskBlockInfo.*;

public class TaskHandleTest {
    protected final TaskDebug debug = new TaskDebug(this);
    public final TargetBlock targetBlock;
    public @Nullable TaskSolutionInfo solutionInfo;
    public final TaskTickInfo totalTick;
    public final TaskTickInfo handleTick;
    public final TaskTickInfo recycledTick;
    public TaskRunningState runningState;
    public TaskHandleState handleState;
    private final Queue<TaskBlockInfo> recycledQueues = Queues.newConcurrentLinkedQueue();
    private @Nullable ModifyLooKInfo modifyLooKInfo = null;
    private boolean hasTried;
    private boolean recycled;
    private int waitCount;
    private boolean finish;

    public TaskHandleTest(TargetBlock targetBlock) {
        this.targetBlock = targetBlock;
        this.totalTick = new TaskTickInfo(200);
        this.handleTick = new TaskTickInfo(40);
        this.recycledTick = new TaskTickInfo(100);
        this.onInit();
    }

    public TaskHandleTest(Block block, BlockPos pos, ClientWorld world) {
        this(new TargetBlock(block, pos, world));
    }

    private void onInit() {
        debug.begin();
        this.finish = false;
        this.runningState = TaskRunningState.RUNNING;
        this.handleState = INITIALIZATION;
        this.hasTried = false;
        this.handleTick.reset();
        this.recycledTick.reset();
        debug.end();
    }


    private void tickCounter() {
        if (recycled) {
            recycledTick.addOne();
        } else {
            handleTick.addOne();
        }
        totalTick.addOne();
    }

    public void tick() {
        debug.empty();
        debug.begin();
        // 退出前还原视角
        if (finish) {
            handleState = MODIFY_LOOK_RESTORE;
            onHandleStatus();
        }
        if (!finish && totalTick.isAllow()) {
            onUpdateStatus();
            onHandleStatus();
            debug.end();
            tickCounter();  // tick计数器
        }
    }

    private void onHandleStatus() {
        var client = MinecraftClient.getInstance();
        var player = client.player;
        if (player == null) return;
        debug.begin();
        switch (handleState) {
            case INITIALIZATION -> onInit();
            case FIND_SOLUTION -> {
                solutionInfo = TaskSchemeFinder.findAllPossible(targetBlock);
                handleState = TaskHandleState.SELECT_SOLUTION;
            }
            case SELECT_SOLUTION -> {
                if (solutionInfo != null && solutionInfo.peek() != null) {
                    handleState = TaskHandleState.WAIT_GAME_UPDATE;
                } else {
                    //TODO: 硬编码文本, 待处理
                    MessageUtils.setOverlayMessage(Text.literal("没有可以使用的方案"));
                    handleState = TaskHandleState.FAIL;
                }
            }
            case PLACE_PISTON -> {
                //TODO: 待实现
            }
            case PLACE_SLIME_BLOCK -> {
                //TODO: 待实现
            }
            case PLACE_REDSTONE_TORCH -> {
                //TODO: 待实现
            }
            case MODIFY_LOOK -> {
                if (modifyLooKInfo == null) {
                    handleState = MODIFY_LOOK_RESTORE;
                } else {
                    Direction facing = modifyLooKInfo.blockInfo.facing;
                    if (facing == null) {
                        handleState = MODIFY_LOOK_RESTORE;
                    } else {
                        float yaw = facing.getOpposite().asRotation();
                        float pitch = switch (facing) {
                            case UP -> 90F;
                            case DOWN -> -90F;
                            default -> 0F;
                        };
                        TaskModifyLookManager.set(yaw, pitch);
                    }
                    handleState = modifyLooKInfo.nextState;
                }
            }
            case MODIFY_LOOK_RESTORE -> TaskModifyLookManager.reset();
            case START_EXECUTION -> {
                if (solutionInfo == null) return;
                TaskSchemeInfo schemeInfo = solutionInfo.peek();
                if (schemeInfo == null) return;
                Piston piston = schemeInfo.piston;
                RedstoneTorch redstoneTorch = schemeInfo.redstoneTorch;
                BaseBlock baseBlock = schemeInfo.baseblock;
                if (!hasTried) {
                    modifyLooKInfo = new ModifyLooKInfo(piston, START_EXECUTION);
                }
                // 打掉活塞附近能充能的红石火把
                BlockPos[] nearByRedstoneTorchPosList = TaskSchemeFinder.findPistonNearbyRedstoneTorch(schemeInfo.piston);
                for (BlockPos pos : nearByRedstoneTorchPosList) {
                    debug.info("打掉红石火把");
                    BlockBreakerUtils.usePistonBreakBlock(pos);
                }
                debug.info("打掉活塞");
                if (BlockBreakerUtils.usePistonBreakBlock(schemeInfo.piston.pos)) {
                    handleState = FAIL;
                }
                debug.info("放置朝下的活塞");
                BlockPlacerUtils.placement(schemeInfo.piston.pos, Direction.DOWN, Items.PISTON);
                hasTried = true;
                handleState = WAIT_NETWORK_LATENCY;
            }
            case WAIT_GAME_UPDATE -> {
            }
            case WAIT_NETWORK_LATENCY -> {
                var playerListEntry = player.networkHandler.getPlayerListEntry(player.getUuid());
                var waitTick = 1;
                if (playerListEntry != null) {
                    var ping = playerListEntry.getLatency();
                    var tick = ping / 50 + 1;
                    waitTick += tick;
                    debug.info("ping: %, tick: %", ping, tick);
                }
                if (waitCount++ >= waitTick) {
                    handleState = WAIT_GAME_UPDATE;
                } else {
                    waitCount += waitTick;
                }
            }
            case TIMEOUT -> handleState = FAIL;
            case FAIL -> handleState = RECYCLED_ITEMS;
            case RECYCLED_ITEMS -> {
                while (!recycledQueues.isEmpty()) {
                    TaskBlockInfo blockInfo = recycledQueues.peek();
                    if (blockInfo.isAir()) {
                        recycledQueues.remove();
                        continue;
                    }
                    BlockBreakerUtils.usePistonBreakBlock(blockInfo.pos);
                }
            }
            case FINISH -> finish = true;
        }
        debug.end();
    }

    private void onUpdateStatus() {
        debug.begin();

        // 优先检测
        if (targetBlock.isAir()) {
            handleState = RECYCLED_ITEMS;
            debug.info("目标方块(%s)已不存在, 准备执行回收任务", targetBlock.getBlockName());
            debug.end();
            return;
        }

        // 超时检查
        if (!recycledTick.isAllow() || !handleTick.isAllow()) {
            handleState = FAIL;
            debug.info("执行已超时", targetBlock.getBlockName());
            debug.end();
            return;
        }

        // 游戏内
        if (handleState != TaskHandleState.WAIT_GAME_UPDATE) {
            debug.end();
            return;
        }

        // 检查方案查找情况
        if (solutionInfo == null) {
            handleState = FIND_SOLUTION;
            debug.end();
            return;
        }

        // 检查方案是否已经到结尾
        if (solutionInfo.isStop()) {
            handleState = RECYCLED_ITEMS;
            debug.end();
            return;
        }

        TaskSchemeInfo schemeInfo = solutionInfo.peek();
        if (schemeInfo != null) {
            Piston piston = schemeInfo.piston;
            RedstoneTorch redstoneTorch = schemeInfo.redstoneTorch;
            BaseBlock baseBlock = schemeInfo.baseblock;
            // 未尝试情况
            if (!hasTried) {
                // 放置基座
                if (baseBlock.isReplaceable()) {
                    debug.info("未放置粘液块, 准备放置！");
                    handleState = PLACE_SLIME_BLOCK;
                    debug.end();
                    return;
                }
                // 放置红石火把
                if (redstoneTorch.isReplaceable()) {
                    debug.info("未放置红石火把, 准备放置！");
                    handleState = PLACE_REDSTONE_TORCH;
                    debug.end();
                    return;
                }
                if (piston.isReplaceable()) {
                    debug.info("未放置活塞, 准备放置！");
                    handleState = PLACE_PISTON;
                    debug.end();
                    return;
                }
                // 检查红石火把放置情况
                if (redstoneTorch.isOf(Blocks.REDSTONE_TORCH) || redstoneTorch.isOf(Blocks.REDSTONE_WALL_TORCH)) {
                    if (redstoneTorch.facing == redstoneTorch.getFacing()) {
                        debug.info("红石火把已放置！");
                    } else {
                        debug.info("红石火把状态错误！原因：朝向错误。");
                        debug.end();
                        return;
                    }
                } else {
                    debug.info("红石火把状态错误！原因：当前位置不是红石火把, 而是\"%s\"", BlockUtils.getBlockName(redstoneTorch.getBlock()));
                    debug.end();
                    return;
                }
                // 检查基座放置红石火把的面是否可以放置
                if (sideCoversSmallSquare(baseBlock.world, baseBlock.pos, redstoneTorch.facing)) {
                    debug.info("无法放置红石火把, 原因：没有可以放置的完整表面");
                    debug.end();
                    return;
                }
                // 活塞放置情况
                if (piston.isOf(Blocks.PISTON) || piston.isOf(Blocks.STICKY_PISTON)) {
                    if (!piston.isExtended()) {
                        debug.info("活塞状态错误, 原因：未激活");
                        debug.end();
                        return;
                    }
                    if (piston.facing != piston.getFacing()) {
                        debug.info("活塞状态错误, 原因：朝向不正确");
                        debug.end();
                        return;
                    }
                    handleState = START_EXECUTION;
                    debug.info("准备充足, 准备执行！");
                }
            } else if (piston.isOf(Blocks.PISTON) || piston.isOf(Blocks.STICKY_PISTON)) {
                assert piston.facing != null;
                if (piston.facing.getOpposite() == piston.getFacing()) {
                    handleState = FAIL;
                    debug.info("失败了！");
                }
            }
        }
        debug.end();
    }

    public boolean isStop() {
        return this.runningState == TaskRunningState.STOP;
    }

    public record ModifyLooKInfo(TaskBlockInfo blockInfo, @Nullable TaskHandleState nextState) {
        public ModifyLooKInfo(TaskBlockInfo blockInfo) {
            this(blockInfo, null);
        }
    }


    public record TaskDebug(TaskHandleTest handle) {
        private void empty() {
            Debug.info();
        }

        private void info(String format, Object... args) {
            StackTraceElement[] temp = Thread.currentThread().getStackTrace();
            StackTraceElement a = temp[2];
            String methodName = a.getMethodName();
            Debug.info("[%s][%s][%s][%s][%s][%s]%s",
                    handle.targetBlock.pos,
                    handle.totalTick,
                    handle.handleTick,
                    handle.runningState,
                    handle.handleState,
                    methodName,
                    String.format(format, args)
            );
        }

        private void begin() {
            info("开始");
        }

        private void end() {
            info("结束");
        }
    }
}


