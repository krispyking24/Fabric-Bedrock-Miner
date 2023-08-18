package yan.lx.bedrockminer.task;

import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.BedrockMinerLang;
import yan.lx.bedrockminer.Debug;
import yan.lx.bedrockminer.utils.BlockBreakerUtils;
import yan.lx.bedrockminer.utils.BlockPlacerUtils;
import yan.lx.bedrockminer.utils.BlockUtils;
import yan.lx.bedrockminer.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.block.Block.sideCoversSmallSquare;

public class TaskHandler {
    public final ClientWorld world;
    public final Block block;
    public final BlockPos pos;
    public final boolean selection;
    public final TaskSchemeInfo[] taskSchemeInfos;
    public @Nullable TaskSchemeInfo currentScheme;
    public TaskState state;
    private int retryCount;
    private boolean timeout;
    private boolean fail;
    private boolean executed;
    private boolean executedModifyLook;
    private int totalTick;
    private int waitTick;
    private int waitCount;
    private @Nullable TaskState nextState;
    private final int recycledItemsTickMaxCount = 20;
    private boolean recycledItems;



    public TaskHandler(ClientWorld world, Block block, BlockPos pos, boolean selection) {
        this.world = world;
        this.block = block;
        this.pos = pos;
        this.taskSchemeInfos = TaskSchemeFinder.findAllPossible(pos);
        this.selection = selection;
        this.state = TaskState.INITIALIZE;
        this.nextState = null;
    }

    public void onTick() {
        if (state == TaskState.SUCCESS) {
            return;
        }
        int totalMaxTick = 70;
        if (!timeout && totalTick > (recycledItems ? totalMaxTick + recycledItemsTickMaxCount : totalMaxTick)) {
            timeout = true;
            state = TaskState.TIMEOUT;
        }
        switch (state) {
            case INITIALIZE -> onInit();
            case SELECT_SCHEME -> onSelectScheme();
            case PLACE_SCHEME_BLOCK -> onPlaceSchemeBlocks();
            case WAIT_GAME_UPDATE -> onWaitGameUpdate();
            case WAIT -> onWait();
            case EXECUTE -> onExecute();
            case TIMEOUT -> onTimeoutHandle();
            case FAIL -> onFail();
            case RECYCLED_ITEMS -> onRecycledItems();
        }
        totalTick++;
    }

    private void onInit() {
        this.totalTick = 0;
        this.timeout = false;
        this.recycledItems = false;
        this.state = TaskState.WAIT_GAME_UPDATE;
        this.executed = false;
        this.executedModifyLook = false;
        this.waitCount = 0;
        this.nextState = null;
        Debug.info("[%s] 初始化", totalTick);
    }

    private void onSelectScheme() {
        if (!world.getBlockState(pos).isOf(block)) return;
        List<TaskSchemeInfo> list = new ArrayList<>();
        for (TaskSchemeInfo taskSchemeInfo : taskSchemeInfos) {
            TaskBlockInfo piston = taskSchemeInfo.piston;
            TaskBlockInfo redstoneTorch = taskSchemeInfo.redstoneTorch;
            TaskBlockInfo slimeBlock = taskSchemeInfo.slimeBlock;
            // 边界检查
            if (!World.isValid(piston.pos)) {
                Debug.info("[%s] 活塞超出世界边界", totalTick);
                continue;
            }
            if (!World.isValid(redstoneTorch.pos)) {
                Debug.info("[%s] 红石火把超出世界边界", totalTick);
                continue;
            }
            if (!World.isValid(slimeBlock.pos)) {
                Debug.info("[%s] 底座超出世界边界", totalTick);
                continue;
            }
            // 检查活塞
            BlockState pistonState = world.getBlockState(piston.pos);
            BlockState pistonHeadState = world.getBlockState(piston.pos.offset(piston.facing));
            if (!(pistonState.isReplaceable() && pistonHeadState.isReplaceable())) {
                continue;
            }
            // 检查红石火把
            if (!world.getBlockState(redstoneTorch.pos).isReplaceable()) {
                continue;
            }
            // 检查粘液块
            if (!(world.getBlockState(slimeBlock.pos).isReplaceable() || sideCoversSmallSquare(world, slimeBlock.pos, slimeBlock.facing))) {
                continue;
            }
            if (world.getBlockState(slimeBlock.pos).isReplaceable()) {
                if (!canPlaceEntity(Blocks.SLIME_BLOCK.getDefaultState(), slimeBlock.pos)) {
                    continue;
                }
                // slimeBlock.level += 1;
            }
            list.add(taskSchemeInfo);
//            // 配置过滤
//            if (Config.INSTANCE.handleFacingDown && schemeInfo.direction == Direction.DOWN) {
//                list.add(schemeInfo);
//            }
//            if (Config.INSTANCE.handleFacingUP && schemeInfo.direction == Direction.UP) {
//                list.add(schemeInfo);
//            }
//            if (Config.INSTANCE.handleFacingNorth && schemeInfo.direction == Direction.NORTH) {
//                list.add(schemeInfo);
//            }
//            if (Config.INSTANCE.handleFacingSouth && schemeInfo.direction == Direction.SOUTH) {
//                list.add(schemeInfo);
//            }
//            if (Config.INSTANCE.handleFacingWest && schemeInfo.direction == Direction.WEST) {
//                list.add(schemeInfo);
//            }
//            if (Config.INSTANCE.handleFacingEast && schemeInfo.direction == Direction.EAST) {
//                list.add(schemeInfo);
//            }
        }
        // 重新排序
        list.sort((o1, o2) -> {
            int cr = 0;
            int a = o1.piston.level - o2.piston.level;
            if (a != 0) {
                cr = (a > 0) ? 3 : -1;
            } else {
                a = o1.redstoneTorch.level - o2.redstoneTorch.level;
                if (a != 0) {
                    cr = (a > 0) ? 2 : -2;
                } else {
                    a = o1.slimeBlock.level - o2.slimeBlock.level;
                    if (a != 0) {
                        cr = (a > 0) ? 1 : -3;
                    }
                }
            }
            return cr;
        });

        if (list.size() == 0) {
            Debug.info("[%s] 没有可以执行的方案", totalTick);
            MessageUtils.setOverlayMessage(Text.literal(BedrockMinerLang.HANDLE_SEEK.getString().replace("%BlockPos%", pos.toShortString())));
            onSucceed();
            return;
        } else {
            Debug.info("[%s] 查找方案, 已查找到%s个可执行方案", totalTick, list.size());
            currentScheme = list.get(0);
            Debug.info("[%s] 选择：%s, (%s, %s, %s), (%s, %s, %s), (%s), (%s), (%s)",
                    totalTick,
                    currentScheme.direction,
                    currentScheme.piston.level,
                    currentScheme.redstoneTorch.level,
                    currentScheme.slimeBlock.level,
                    currentScheme.piston.facing,
                    currentScheme.redstoneTorch.facing,
                    currentScheme.slimeBlock.facing,
                    currentScheme.piston.pos.toShortString(),
                    currentScheme.redstoneTorch.pos.toShortString(),
                    currentScheme.slimeBlock.pos.toShortString()
            );
        }

//        for (var l : list) {
//            Debug.info("[%s] %s, (%s, %s, %s), (%s, %s, %s), (%s), (%s), (%s)",
//                    totalTick,
//                    l.direction,
//                    l.piston.level,
//                    l.redstoneTorch.level,
//                    l.slimeBlock.level,
//                    l.piston.facing,
//                    l.redstoneTorch.facing,
//                    l.slimeBlock.facing,
//                    l.piston.pos.toShortString(),
//                    l.redstoneTorch.pos.toShortString(),
//                    l.slimeBlock.pos.toShortString()
//            );
//        }
        if (currentScheme != null) {
            state = TaskState.PLACE_SCHEME_BLOCK;
        }
    }

    private void onPlaceSchemeBlocks() {
        Debug.info("[%s] 放置方案方块", totalTick);
        if (currentScheme != null) {
            TaskBlockInfo piston = currentScheme.piston;
            TaskBlockInfo redstoneTorch = currentScheme.redstoneTorch;
            TaskBlockInfo slimeBlock = currentScheme.slimeBlock;
            // 放置
            if (world.getBlockState(piston.pos).isReplaceable()) {
                Direction facing = piston.facing;
                if (facing.getAxis().isHorizontal() && (!piston.modifyLook || !TaskModifyLookInfo.isModify())) {
                    piston.modifyLook = true;
                    setLockLook(piston.facing, TaskState.PLACE_SCHEME_BLOCK);
                    return;
                }
                Debug.info("[%s] 放置活塞", totalTick);
                BlockPlacerUtils.placement(piston.pos, piston.facing, Items.PISTON);
                piston.recycledItems = true;
                state = TaskState.WAIT;
                nextState = TaskState.PLACE_SCHEME_BLOCK;
                TaskModifyLookInfo.reset();
                return;
            }
            if (world.getBlockState(slimeBlock.pos).isReplaceable()) {
                Direction facing = slimeBlock.facing;
                if (facing.getAxis().isHorizontal() && (!slimeBlock.modifyLook || !TaskModifyLookInfo.isModify())) {
                    slimeBlock.modifyLook = true;
                    setLockLook(slimeBlock.facing, TaskState.PLACE_SCHEME_BLOCK);
                    return;
                }
                Debug.info("[%s] 放置粘液块", totalTick);
                BlockPlacerUtils.placement(slimeBlock.pos, slimeBlock.facing, Items.SLIME_BLOCK);
                slimeBlock.recycledItems = true;
                state = TaskState.WAIT;
                nextState = TaskState.PLACE_SCHEME_BLOCK;
                TaskModifyLookInfo.reset();
                return;
            }
            if (world.getBlockState(redstoneTorch.pos).isReplaceable()) {
                if (redstoneTorch.facing.getAxis().isHorizontal() && (!redstoneTorch.modifyLook || !TaskModifyLookInfo.isModify())) {
                    redstoneTorch.modifyLook = true;
                    setLockLook(redstoneTorch.facing, TaskState.PLACE_SCHEME_BLOCK);
                    return;
                }
                Debug.info("[%s] 放置红石火把", totalTick);
                BlockPlacerUtils.placement(redstoneTorch.pos, redstoneTorch.facing, Items.REDSTONE_TORCH);
                redstoneTorch.recycledItems = true;
                unlockLook(TaskState.PLACE_SCHEME_BLOCK);
                return;
            }
            nextState = TaskState.WAIT_GAME_UPDATE;
            state = TaskState.WAIT;
        } else {
            state = TaskState.FAIL;
        }
    }

    private void onWaitGameUpdate() {
        Debug.info("[%s] 等待更新", totalTick);
        if (currentScheme == null) {
            Debug.info("[%s] 没有可执行方案, 准备重新查找", totalTick);
            state = TaskState.SELECT_SCHEME;
            return;
        }
        TaskBlockInfo piston = currentScheme.piston;
        TaskBlockInfo redstoneTorch = currentScheme.redstoneTorch;
        TaskBlockInfo slimeBlock = currentScheme.slimeBlock;
        // 优先检查
        if (!world.getBlockState(pos).isOf(block)) {
            Debug.info("[%s] 执行成功, 目标方块(%s)不存在", totalTick, BlockUtils.getBlockName(block));
            state = TaskState.RECYCLED_ITEMS;
            return;
        }
        if (executed) {
            BlockState blockState = world.getBlockState(piston.pos);
            if (blockState.isOf(Blocks.MOVING_PISTON)) {
                Debug.info("[%s] 活塞正在移动处理", totalTick);
                return;
            }
            if (blockState.isOf(Blocks.PISTON) || blockState.isOf(Blocks.STICKY_PISTON)) {
                state = TaskState.FAIL;
                Debug.info("[%s] 失败了", totalTick);
            }
        } else {
            // 底座检查
            BlockState slimeBlockState = world.getBlockState(slimeBlock.pos);
            if (!(slimeBlockState.isOf(Blocks.SLIME_BLOCK) || Block.sideCoversSmallSquare(world, slimeBlock.pos, slimeBlock.facing))) {
                Debug.info("[%s] 状态错误, 目标方块(%s)不正确, 目标方块应为 %s 或其他完整方块",
                        totalTick,
                        BlockUtils.getBlockName(slimeBlockState.getBlock()),
                        BlockUtils.getBlockName(Blocks.SLIME_BLOCK)
                );
                if (slimeBlockState.isReplaceable()) {
                    state = TaskState.PLACE_SCHEME_BLOCK;
                }
                return;
            }
            // 红石火把检查
            BlockState redstoneTorchState = world.getBlockState(redstoneTorch.pos);
            if (!(redstoneTorchState.isOf(Blocks.REDSTONE_TORCH) || redstoneTorchState.isOf(Blocks.REDSTONE_WALL_TORCH))) {
                Debug.info("[%s] 状态错误, 目标方块(%s)不正确, 目标方块应为 %s 或 %s 方块",
                        totalTick,
                        BlockUtils.getBlockName(redstoneTorchState.getBlock()),
                        BlockUtils.getBlockName(Blocks.REDSTONE_BLOCK),
                        BlockUtils.getBlockName(Blocks.REDSTONE_WALL_TORCH)
                );
                if (redstoneTorchState.isReplaceable()) {
                    state = TaskState.PLACE_SCHEME_BLOCK;
                }
                return;
            }
            // 活塞检查
            BlockState pistonState = world.getBlockState(piston.pos);
            if (!(pistonState.isOf(Blocks.PISTON) || pistonState.isOf(Blocks.STICKY_PISTON))) {
                Debug.info("[%s] 状态错误, 目标方块(%s)不正确, 目标方块应为 %s 或 %s 方块",
                        totalTick,
                        BlockUtils.getBlockName(pistonState.getBlock()),
                        BlockUtils.getBlockName(Blocks.PISTON),
                        BlockUtils.getBlockName(Blocks.STICKY_PISTON)
                );
                if (pistonState.isReplaceable()) {
                    state = TaskState.PLACE_SCHEME_BLOCK;
                }
                return;
            }
            // 检查是否可以执行
            Direction facing = pistonState.get(PistonBlock.FACING);
            boolean extended = pistonState.get(PistonBlock.EXTENDED);
            // 红石火把方向检查
            if (redstoneTorchState.isOf(Blocks.REDSTONE_WALL_TORCH)) {
                Direction redstoneTorchFacing = redstoneTorchState.get(WallRedstoneTorchBlock.FACING);
                if (redstoneTorchFacing != redstoneTorch.facing) {
                    Debug.info("[%s] 状态错误, 红石火把方向与方案方向不一致", totalTick);
                }
            } else if (redstoneTorchState.isOf(Blocks.REDSTONE_TORCH)) {
                if (redstoneTorch.facing == Direction.UP) {
                    Debug.info("[%s] 状态错误, 红石火把方向与方案方向不一致", totalTick);
                }
            } else if (redstoneTorchState.isReplaceable()) {
                state = TaskState.PLACE_SCHEME_BLOCK;
            }
            if (facing == piston.facing && extended) {
                state = TaskState.EXECUTE;
                Debug.info("[%s] 条件充足, 准备执行", totalTick);
            }
        }

    }

    private void onWait() {
        int waitTick = 1;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        ClientPlayerEntity player = client.player;
        if (networkHandler != null && player != null) {
            PlayerListEntry playerListEntry = networkHandler.getPlayerListEntry(player.getUuid());
            if (playerListEntry != null) {
                int latency = playerListEntry.getLatency();
                waitTick += latency / 50;
            }
        }
        if (TaskModifyLookInfo.isModify()) {
            waitTick += 2;
        }
        Debug.info("[%s] 等待 %stick, 当前tick: %s, 下一个状态: %s", totalTick, waitTick, waitTick, waitCount, nextState);
        if (++waitCount > waitTick) {
            if (nextState != null) {
                state = nextState;
                nextState = null;
            } else {
                state = TaskState.WAIT_GAME_UPDATE;
            }
            waitCount = 0;
        } else {
            state = TaskState.WAIT;
        }
    }

    private void onExecute() {
        Debug.info("[%s] 准备执行", totalTick);
        if (executed) {
            onWaitGameUpdate();
        } else if (currentScheme != null) {
            Direction direction = currentScheme.direction.getOpposite();
            TaskBlockInfo piston = currentScheme.piston;
            TaskBlockInfo redstoneTorch = currentScheme.redstoneTorch;
            if ((!executedModifyLook || !TaskModifyLookInfo.isModify()) && direction.getAxis().isHorizontal()) {
                executedModifyLook = true;
                setLockLook(currentScheme.direction.getOpposite(), TaskState.EXECUTE);
                return;
            }
            // 打掉附近红石火把
            BlockPos[] nearbyRedstoneTorch = TaskSchemeFinder.findPistonNearbyRedstoneTorch(piston.pos, world);
            for (BlockPos pos : nearbyRedstoneTorch) {
                if (world.getBlockState(pos).isOf(Blocks.REDSTONE_TORCH) ||
                        world.getBlockState(pos).isOf(Blocks.REDSTONE_WALL_TORCH)) {
                    Debug.info("[%s] 打掉红石火把", totalTick);
                    BlockBreakerUtils.usePistonBreakBlock(pos);
                }
            }
            if (world.getBlockState(redstoneTorch.pos).isOf(Blocks.REDSTONE_TORCH) ||
                    world.getBlockState(redstoneTorch.pos).isOf(Blocks.REDSTONE_WALL_TORCH)) {
                Debug.info("[%s] 打掉红石火把", totalTick);
                BlockBreakerUtils.usePistonBreakBlock(redstoneTorch.pos);
            }
            Debug.info("[%s] 打掉活塞", totalTick);
            BlockBreakerUtils.usePistonBreakBlock(piston.pos);
            Debug.info("[%s] 放置活塞", totalTick);
            BlockPlacerUtils.placement(piston.pos, currentScheme.direction.getOpposite(), Items.PISTON);
            piston.recycledItems = true;
            executed = true;
            unlockLook(TaskState.WAIT_GAME_UPDATE);
        }
    }

    private void onTimeoutHandle() {
        Debug.info("[%s] 超时处理", totalTick);
        state = TaskState.FAIL;
    }

    private void onFail() {
        Debug.info("[%s] 失败", totalTick);
        fail = true;
        state = TaskState.RECYCLED_ITEMS;
    }

    private void onRecycledItems() {
        Debug.info("[%s] 回收物品", totalTick);
        if (currentScheme != null) {
            TaskBlockInfo piston = currentScheme.piston;
            TaskBlockInfo redstoneTorch = currentScheme.redstoneTorch;
            TaskBlockInfo slimeBlock = currentScheme.slimeBlock;
            if (piston.recycledItems && piston.recycledTickCount < recycledItemsTickMaxCount) {
                Debug.info("[%s] 回收活塞", totalTick);
                if (BlockBreakerUtils.usePistonBreakBlock(piston.pos)) {
                    piston.recycledItems = false;
                }
                ++piston.recycledTickCount;
                nextState = TaskState.RECYCLED_ITEMS;
                state = TaskState.WAIT;
                return;
            }
            if (redstoneTorch.recycledItems && redstoneTorch.recycledTickCount < recycledItemsTickMaxCount) {
                Debug.info("[%s] 回收红石火把", totalTick);
                if (BlockBreakerUtils.usePistonBreakBlock(redstoneTorch.pos)) {
                    redstoneTorch.recycledItems = false;
                }
                ++redstoneTorch.recycledTickCount;
                nextState = TaskState.RECYCLED_ITEMS;
                state = TaskState.WAIT;
                return;
            }
            if (slimeBlock.recycledItems && slimeBlock.recycledTickCount < recycledItemsTickMaxCount) {
                Debug.info("[%s] 回收粘液块", totalTick);
                if (BlockBreakerUtils.usePistonBreakBlock(slimeBlock.pos)) {
                    slimeBlock.recycledItems = false;
                }
                ++slimeBlock.recycledTickCount;
                nextState = TaskState.RECYCLED_ITEMS;
                state = TaskState.WAIT;
                return;
            }
        }
        if (!world.getBlockState(pos).isOf(block)) {
            fail = false;
        }
        if (fail && retryCount++ < 2) {
            state = TaskState.INITIALIZE;
            currentScheme = null;
            return;
        }
        onSucceed();
    }

    private void onSucceed() {
        if (TaskModifyLookInfo.isModify()) {
            TaskModifyLookInfo.reset();
        }
        state = TaskState.SUCCESS;
        Debug.info("[%s] 成功", totalTick);
    }

    private boolean canPlaceEntity(BlockState blockState, BlockPos blockPos) {
        boolean b = true;
        var shape = blockState.getCollisionShape(world, blockPos);
        if (!shape.isEmpty()) {
            for (var entity : world.getEntities()) {
                // 过滤掉落物实体
                if (entity instanceof ItemEntity) {
                    continue;
                }
                // 检查实体是否在目标位置内
                if (entity.collidesWithStateAtPos(blockPos, blockState)) {
                    b = false;
                }
            }
        }
        return b;
    }

    private void setLockLook(Direction direction, TaskState nextState) {
        TaskModifyLookInfo.set(direction, this);
        this.state = TaskState.WAIT;
        this.nextState = nextState;
    }

    private void unlockLook(TaskState nextState) {
        TaskModifyLookInfo.reset();
        this.state = TaskState.WAIT;
        this.nextState = nextState;
    }

    public boolean isSucceed() {
        return state == TaskState.SUCCESS;
    }

    public void onClear() {
        unlockLook(TaskState.SUCCESS);
    }
}
