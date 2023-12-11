package yan.lx.bedrockminer.task;

import com.google.common.collect.Queues;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.BedrockMinerLang;
import yan.lx.bedrockminer.Debug;
import yan.lx.bedrockminer.config.Config;
import yan.lx.bedrockminer.utils.BlockBreakerUtils;
import yan.lx.bedrockminer.utils.BlockPlacerUtils;
import yan.lx.bedrockminer.utils.CheckingEnvironmentUtils;
import yan.lx.bedrockminer.utils.MessageUtils;

import java.util.List;
import java.util.Queue;
import java.util.UUID;

import static net.minecraft.block.Block.sideCoversSmallSquare;

public class TaskHandler {
    private final List<TaskHandler> handleTasks;    // 用来过滤选择方案
    public final UUID id;
    public final ClientWorld world;
    public final Block block;
    public final BlockPos pos;
    public TaskState state;
    public final TaskSeekSchemeInfo[] taskSchemes;
    public @Nullable Direction direction;
    public @Nullable TaskSeekBlockInfo piston;
    public @Nullable TaskSeekBlockInfo redstoneTorch;
    public @Nullable TaskSeekBlockInfo slimeBlock;
    public final Queue<BlockPos> recycledQueue;
    public @Nullable TaskState waitNextState;
    public boolean executeModify;
    public int totalTick;
    public int waitCount;
    public int waitCountDelta;
    public int retryCount;
    public int retryCountMax;
    public boolean executed;
    public boolean recycled;
    public int recycledCount;
    public boolean timeout;
    private int waitTick;

    public TaskHandler(List<TaskHandler> handleTasks, ClientWorld world, Block block, BlockPos pos, int retryCountMax) {
        this.handleTasks = handleTasks;
        this.id = UUID.randomUUID();
        this.world = world;
        this.block = block;
        this.pos = pos;
        this.state = TaskState.INITIALIZE;
        this.taskSchemes = TaskSeekSchemeTools.findAllPossible(pos, world);
        this.recycledQueue = Queues.newConcurrentLinkedQueue();
        this.totalTick = 0;
        this.retryCount = 0;
        this.retryCountMax = retryCountMax;
        onInit();
    }

    public void debug(String var1, Object... var2) {
        Debug.info("[{}] [{}] [{}] [{}] " + var1, id, retryCount, totalTick, state, var2);
    }

    public boolean isSucceed() {
        return state == TaskState.SUCCESS;
    }

    private void setWait(@Nullable TaskState nextState, int customDelta) {
        waitNextState = nextState;
        waitCountDelta = Math.max(customDelta, 1);
        state = TaskState.WAIT;
    }

    private void setModifyLook(TaskSeekBlockInfo blockInfo) {
        if (blockInfo != null) {
            setModifyLook(blockInfo.facing);
            blockInfo.modify = true;
        }
    }

    private void setModifyLook(Direction facing) {
        TaskModifyLookHandle.set(facing, this);
    }

    private void resetModifyLook() {
        TaskModifyLookHandle.reset();
    }

    private void onInit() {
        this.direction = null;
        this.piston = null;
        this.redstoneTorch = null;
        this.slimeBlock = null;
        this.recycledQueue.clear();
        this.executed = false;
        this.recycled = false;
        this.recycledCount = 0;
        this.waitNextState = null;
        this.totalTick = 0;
        this.waitCount = 0;
        this.waitCountDelta = 0;
        this.state = TaskState.WAIT_GAME_UPDATE;
        this.timeout = false;
        this.findPiston();
        this.placePiston();
    }

    public void onTick() {
        debug("开始处理！");
        if (recycled && recycledCount++ > 10) {
            state = TaskState.SUCCESS;
            debug("回收超时");
        }
        if (state == TaskState.SUCCESS) {
            return;
        }
        if (!timeout && totalTick > 20 + waitTick) {
            timeout = true;
            state = TaskState.TIMEOUT;
            debug("执行超时");
        }
        if (onHandleStatus()) {
            onUpdateStates();
        }
        totalTick += 1;
        debug("结束处理");
    }

    private void onUpdateStates() {
        if (piston != null && world.getBlockState(piston.pos).isOf(Blocks.MOVING_PISTON)) {
            debug("活塞正在移动");
            return;
        }
        if (!world.getBlockState(pos).isOf(block)) {
            state = TaskState.RECYCLED_ITEMS;
            debug("目标不存在");
        }
        if (!executed) {
            // 获取放置位置
            if (piston == null) {
                state = TaskState.FIND_PISTON;
                return;
            }
            if (redstoneTorch == null) {
                state = TaskState.FIND_REDSTONE_TORCH;
                return;
            }
            if (slimeBlock == null) {
                state = TaskState.FIND_SLIME_BLOCK;
                return;
            }
            if (world.getBlockState(piston.pos).isReplaceable()) {
                state = TaskState.PLACE_PISTON;
                return;
            } else if (!(world.getBlockState(piston.pos).getBlock() instanceof PistonBlock)) {
                findPiston();
                return;
            }
            if (world.getBlockState(slimeBlock.pos).isReplaceable()) {
                state = TaskState.PLACE_SLIME_BLOCK;
                return;
            } else if (!Block.sideCoversSmallSquare(world, slimeBlock.pos, slimeBlock.facing)) {
                findRedstoneTorch();
                return;
            }

            if (world.getBlockState(redstoneTorch.pos).isReplaceable()) {
                state = TaskState.PLACE_REDSTONE_TORCH;
                return;
            } else if (!(world.getBlockState(redstoneTorch.pos).getBlock() instanceof RedstoneTorchBlock)) {
                findRedstoneTorch();
                return;
            }

            if (world.getBlockState(piston.pos).getBlock() instanceof PistonBlock) {
                if (world.getBlockState(piston.pos).contains(PistonBlock.EXTENDED)) {
                    if (world.getBlockState(piston.pos).get(PistonBlock.EXTENDED)) {
                        state = TaskState.EXECUTE;
                    }
                }
            }
        }
    }

    private boolean onHandleStatus() {
        var networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        var player = MinecraftClient.getInstance().player;
        if (networkHandler != null && player != null) {
            switch (state) {
                case INITIALIZE -> onInit();
                case WAIT_GAME_UPDATE -> {
                    return true;
                }
                case WAIT -> {
                    waitTick = 0;
                    var playerListEntry = networkHandler.getPlayerListEntry(player.getUuid());
                    if (playerListEntry != null) {
                        int latency = playerListEntry.getLatency();
                        waitTick += latency / 50;
                    }
                    if (waitCount > (waitTick + waitCountDelta)) {
                        if (waitNextState != null) {
                            state = waitNextState;
                            waitNextState = null;
                        }
                        waitCount = 0;
                        waitCountDelta = 0;
                    } else {
                        waitCount += 1;
                    }
                }
                case FIND_PISTON -> {
                    return findPiston();
                }
                case FIND_REDSTONE_TORCH -> {
                    return findRedstoneTorch();
                }
                case FIND_SLIME_BLOCK -> {
                    return findSlimeBlock();
                }
                case PLACE_PISTON -> {
                    return placePiston();
                }
                case PLACE_REDSTONE_TORCH -> {
                    return placeRedstoneTorch();
                }
                case PLACE_SLIME_BLOCK -> {
                    return placeSlimeBlock();
                }
                case EXECUTE -> {
                    if (executed || direction == null || piston == null || redstoneTorch == null || slimeBlock == null) {
                        return true;
                    }
                    if (!executeModify && direction.getAxis().isHorizontal()) {
                        setModifyLook(direction.getOpposite());
                        executeModify = true;
                        return false;
                    } else {
                        // 打掉附近红石火把
                        BlockPos[] nearbyRedstoneTorch = TaskSeekSchemeTools.findPistonNearbyRedstoneTorch(piston.pos, world);
                        for (BlockPos pos : nearbyRedstoneTorch) {
                            if (world.getBlockState(pos).getBlock() instanceof RedstoneTorchBlock) {
                                BlockBreakerUtils.simpleBreakBlock(pos);
                            }
                        }
                        if (world.getBlockState(redstoneTorch.pos).getBlock() instanceof RedstoneTorchBlock) {
                            BlockBreakerUtils.simpleBreakBlock(redstoneTorch.pos);
                        }
                        BlockBreakerUtils.simpleBreakBlock(piston.pos);
                        BlockPlacerUtils.placement(piston.pos, direction.getOpposite(), Items.PISTON);
                        addRecycled(piston.pos);
                        if (executeModify) {
                            resetModifyLook();
                        }
                        executed = true;
                    }
                    setWait(TaskState.WAIT_GAME_UPDATE, 4);
                }
                case TIMEOUT -> state = TaskState.FAIL;
                case FAIL -> state = TaskState.RECYCLED_ITEMS;
                case RECYCLED_ITEMS -> {
                    if (!recycled) recycled = true;
                    if (!recycledQueue.isEmpty()) {
                        var blockPos = recycledQueue.peek();
                        BlockBreakerUtils.simpleBreakBlock(blockPos);
                        if (world.getBlockState(blockPos).isReplaceable()) {
                            recycledQueue.remove(blockPos);
                        }
                        return false;
                    } else if (world.getBlockState(pos).isOf(block)) {
                        if (retryCount < retryCountMax - 1) {
                            state = TaskState.INITIALIZE;
                            retryCount += 1;
                            return false;
                        }
                    }
                    state = TaskState.SUCCESS;
                }
            }
        }
        return false;
    }

    private boolean isTaskPos(TaskSeekBlockInfo piston, TaskSeekBlockInfo redstoneTorch, TaskSeekBlockInfo slimeBlock) {
        boolean b = false;
        for (TaskHandler handler : handleTasks) {
            if (handler.equals(this)) {
                continue;
            }
            // 红石火把
            if (handler.pos != null && handler.pos.equals(redstoneTorch.pos)) {
                b = true;
            }
            if (handler.piston != null && handler.piston.pos.equals(redstoneTorch.pos)) {
                b = true;
            }
            if (handler.piston != null && handler.piston.pos.offset(handler.piston.facing).equals(redstoneTorch.pos)) {
                b = true;
            }
            if (handler.redstoneTorch != null && handler.redstoneTorch.pos.equals(redstoneTorch.pos)) {
                b = true;
            }
            if (handler.slimeBlock != null && handler.slimeBlock.pos.equals(redstoneTorch.pos)) {
                b = true;
            }
            // 粘液块
//            if (handler.pos != null && handler.pos.equals(slimeBlock.pos)) {
//                b = true;
//            }
//            if (handler.piston != null && handler.piston.pos.equals(slimeBlock.pos)) {
//                b = true;
//            }
//            if (handler.redstoneTorch != null && handler.redstoneTorch.pos.equals(slimeBlock.pos)) {
//                b = true;
//            }
//            if (handler.slimeBlock != null && handler.slimeBlock.pos.equals(slimeBlock.pos)) {
//                b = true;
//            }
        }
        return b;
    }

    private boolean findPiston() {
        if (this.piston == null) {
            for (TaskSeekSchemeInfo taskSchemeInfo : taskSchemes) {
                Direction direction = taskSchemeInfo.direction;
                TaskSeekBlockInfo piston = taskSchemeInfo.piston;
                TaskSeekBlockInfo redstoneTorch = taskSchemeInfo.redstoneTorch;
                TaskSeekBlockInfo slimeBlock = taskSchemeInfo.slimeBlock;
                if (direction.getAxis().isHorizontal()) {
                    if (!Config.INSTANCE.horizontal){
                        continue;
                    }
                }
                if (direction.getAxis().isVertical()) {
                    if (!Config.INSTANCE.vertical){
                        continue;
                    }
                }
                if (World.isValid(piston.pos) && World.isValid(redstoneTorch.pos) && World.isValid(slimeBlock.pos)) {
                    if (isTaskPos(piston, redstoneTorch, slimeBlock)) {
                        continue;
                    }
                    BlockState pistonState = world.getBlockState(piston.pos);
                    BlockState pistonHeadState = world.getBlockState(piston.pos.offset(piston.facing));
                    // 检查活塞位置
                    if (!(pistonState.isReplaceable() && pistonHeadState.isReplaceable())) {
                        if (!(pistonState.getBlock() instanceof PistonBlock)) {
                            continue;
                        }
                    }
                    // 预检查, 避免无法放置导致失败
                    BlockState redstoneTorchState = world.getBlockState(redstoneTorch.pos);
                    if (!redstoneTorchState.isReplaceable()) {
                        if (!(redstoneTorchState.getBlock() instanceof RedstoneTorchBlock)) {
                            continue;
                        }
                    }
                    // 预检查, 避免无法放置导致失败
                    if (!(world.getBlockState(slimeBlock.pos).isReplaceable() || sideCoversSmallSquare(world, slimeBlock.pos, slimeBlock.facing))) {
                        continue;
                    }
                    if (!CheckingEnvironmentUtils.canPlace(slimeBlock.pos, Blocks.SLIME_BLOCK, slimeBlock.facing)) {
                        continue;
                    }
                    if (piston.facing.getAxis().isHorizontal()) {
                        if (!Config.INSTANCE.horizontal){
                            continue;
                        }
                    }
                    if (piston.facing.getAxis().isVertical()) {
                        if (!Config.INSTANCE.vertical){
                            continue;
                        }
                    }
                    this.direction = direction;
                    this.piston = piston;
                    this.redstoneTorch = null;
                    this.slimeBlock = null;
                    break;
                }
            }
        }
        if (this.piston == null) {
//            state = TaskState.FAIL;
            MessageUtils.setOverlayMessage(Text.literal(BedrockMinerLang.HANDLE_SEEK.getString().replace("%BlockPos%", pos.toShortString())));
        } else {
            state = TaskState.WAIT_GAME_UPDATE;
            return true;
        }
        return false;
    }

    private boolean findRedstoneTorch() {
        if (redstoneTorch == null || !(world.getBlockState(redstoneTorch.pos).isReplaceable() || sideCoversSmallSquare(world, redstoneTorch.pos, redstoneTorch.facing))) {
            for (TaskSeekSchemeInfo taskSchemeInfo : taskSchemes) {
                if (taskSchemeInfo.piston.equals(piston)) {
                    TaskSeekBlockInfo piston = taskSchemeInfo.piston;
                    TaskSeekBlockInfo redstoneTorch = taskSchemeInfo.redstoneTorch;
                    TaskSeekBlockInfo slimeBlock = taskSchemeInfo.slimeBlock;
                    if (World.isValid(piston.pos) && World.isValid(redstoneTorch.pos) && World.isValid(slimeBlock.pos)) {
                        if (isTaskPos(piston, redstoneTorch, slimeBlock)) {
                            continue;
                        }
                        BlockState pistonState = world.getBlockState(piston.pos);
                        BlockState pistonHeadState = world.getBlockState(piston.pos.offset(piston.facing));
                        // 检查活塞位置
                        if (!(pistonState.isReplaceable() && pistonHeadState.isReplaceable())) {
                            if (!(pistonState.getBlock() instanceof PistonBlock)) {
                                continue;
                            }
                        }
                        // 预检查, 避免无法放置导致失败
                        BlockState redstoneTorchState = world.getBlockState(redstoneTorch.pos);
                        if (!redstoneTorchState.isReplaceable()) {
                            if (!(redstoneTorchState.getBlock() instanceof RedstoneTorchBlock)) {
                                continue;
                            }
                        }
                        if (!(world.getBlockState(slimeBlock.pos).isReplaceable() || sideCoversSmallSquare(world, slimeBlock.pos, slimeBlock.facing))) {
                            continue;
                        }
                        if (!CheckingEnvironmentUtils.canPlace(slimeBlock.pos, Blocks.SLIME_BLOCK, slimeBlock.facing)) {
                            continue;
                        }
                        if (redstoneTorch.facing.getAxis().isHorizontal()) {
                            if (!Config.INSTANCE.horizontal){
                                continue;
                            }
                        }
                        if (redstoneTorch.facing.getAxis().isVertical()) {
                            if (!Config.INSTANCE.vertical){
                                continue;
                            }
                        }
                        this.redstoneTorch = redstoneTorch;
                        this.slimeBlock = null;
                        break;
                    }
                }
            }
        }
        if (this.redstoneTorch == null) {
//            state = TaskState.FAIL;
            MessageUtils.setOverlayMessage(Text.literal(BedrockMinerLang.HANDLE_SEEK.getString().replace("%BlockPos%", pos.toShortString())));
        } else {
            state = TaskState.WAIT_GAME_UPDATE;
            return true;
        }
        return false;
    }

    private boolean findSlimeBlock() {
        if (slimeBlock == null || !(world.getBlockState(slimeBlock.pos).isReplaceable()
                || sideCoversSmallSquare(world, slimeBlock.pos, slimeBlock.facing))) {
            for (TaskSeekSchemeInfo taskSchemeInfo : taskSchemes) {
                if (taskSchemeInfo.piston.equals(piston)) {
                    TaskSeekBlockInfo piston = taskSchemeInfo.piston;
                    if (taskSchemeInfo.redstoneTorch.equals(redstoneTorch)) {
                        TaskSeekBlockInfo redstoneTorch = taskSchemeInfo.redstoneTorch;
                        TaskSeekBlockInfo slimeBlock = taskSchemeInfo.slimeBlock;
                        if (World.isValid(piston.pos) && World.isValid(redstoneTorch.pos) && World.isValid(slimeBlock.pos)) {
                            if (isTaskPos(piston, redstoneTorch, slimeBlock)) {
                                continue;
                            }
                            BlockState pistonState = world.getBlockState(piston.pos);
                            BlockState pistonHeadState = world.getBlockState(piston.pos.offset(piston.facing));
                            // 检查活塞位置
                            if (!(pistonState.isReplaceable() && pistonHeadState.isReplaceable())) {
                                if (!(pistonState.getBlock() instanceof PistonBlock)) {
                                    continue;
                                }
                            }
                            // 预检查, 避免无法放置导致失败
                            BlockState redstoneTorchState = world.getBlockState(redstoneTorch.pos);
                            if (!redstoneTorchState.isReplaceable()) {
                                if (!(redstoneTorchState.getBlock() instanceof RedstoneTorchBlock)) {
                                    continue;
                                }
                            }
                            // 预检查, 避免无法放置导致失败
                            if (!(world.getBlockState(slimeBlock.pos).isReplaceable() || sideCoversSmallSquare(world, slimeBlock.pos, slimeBlock.facing))) {
                                continue;
                            }
                            if (!CheckingEnvironmentUtils.canPlace(slimeBlock.pos, Blocks.SLIME_BLOCK, slimeBlock.facing)) {
                                continue;
                            }
                            if (slimeBlock.facing.getAxis().isHorizontal()) {
                                if (!Config.INSTANCE.horizontal){
                                    continue;
                                }
                            }
                            if (slimeBlock.facing.getAxis().isVertical()) {
                                if (!Config.INSTANCE.vertical){
                                    continue;
                                }
                            }
                            this.slimeBlock = slimeBlock;
                            break;
                        }
                    }
                }
            }
        }
        if (this.slimeBlock == null) {
//            state = TaskState.FAIL;
            MessageUtils.setOverlayMessage(Text.literal(BedrockMinerLang.HANDLE_SEEK.getString().replace("%BlockPos%", pos.toShortString())));
        } else {
            state = TaskState.WAIT_GAME_UPDATE;
            return true;
        }
        return false;
    }

    private boolean placePiston() {
        if (piston == null) {
            return findPiston();
        }
        if (piston.isNeedModify() && !piston.modify) {
            setModifyLook(piston);
            return false;
        }
        BlockPlacerUtils.placement(piston.pos, piston.facing, Items.PISTON);
        addRecycled(piston.pos);
        setWait(TaskState.WAIT_GAME_UPDATE, 1);
        resetModifyLook();
        return true;
    }


    private boolean placeRedstoneTorch() {
        if (redstoneTorch == null) {
            return findPiston();
        }
        if (redstoneTorch.isNeedModify() && !redstoneTorch.modify) {
            setModifyLook(redstoneTorch);
            return false;
        }
        BlockPlacerUtils.placement(redstoneTorch.pos, redstoneTorch.facing, Items.REDSTONE_TORCH);
        addRecycled(redstoneTorch.pos);
        setWait(TaskState.WAIT_GAME_UPDATE, 3);
        resetModifyLook();
        return true;
    }

    private boolean placeSlimeBlock() {
        if (slimeBlock == null) {
            return findPiston();
        }
        if (slimeBlock.isNeedModify() && !slimeBlock.modify) {
            setModifyLook(slimeBlock);
            return false;
        }
        BlockPlacerUtils.placement(slimeBlock.pos, slimeBlock.facing, Items.SLIME_BLOCK);
        addRecycled(slimeBlock.pos);
        setWait(TaskState.WAIT_GAME_UPDATE, 1);
        resetModifyLook();
        return true;
    }

    private void addRecycled(BlockPos pos) {
        if (!recycledQueue.contains(pos)) {
            recycledQueue.add(pos);
        }
    }
}
