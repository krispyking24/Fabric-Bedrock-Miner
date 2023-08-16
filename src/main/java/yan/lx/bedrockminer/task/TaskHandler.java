package yan.lx.bedrockminer.task;

import com.google.common.collect.Queues;
import net.minecraft.block.Block;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.Debug;
import yan.lx.bedrockminer.model.BlockInfo;
import yan.lx.bedrockminer.model.SchemeInfo;
import yan.lx.bedrockminer.utils.BlockPlacerUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;

import static net.minecraft.block.Block.sideCoversSmallSquare;

public class TaskHandler {
    public final ClientWorld world;
    public final Block block;
    public final BlockPos pos;
    public final SchemeInfo[] schemeInfos;
    public final Queue<SchemeInfo> schemeQueues;
    public TaskState state;
    private boolean init;
    private int totalTick;
    private int recycledTick;
    private int retryCount;
    private boolean timeout;
    private boolean recycledItems;
    private boolean fail;
    private boolean succeed;
    private boolean modifyLook;


    public TaskHandler(ClientWorld world, Block block, BlockPos pos) {
        this.world = world;
        this.block = block;
        this.pos = pos;
        this.schemeInfos = SchemeFinder.findAllPossible(pos);
        this.schemeQueues = Queues.newConcurrentLinkedQueue();
        this.state = TaskState.INITIALIZE;
    }

    private void onModifyLook(Direction facing, TaskState nextState) {
        modifyLook = true;
        ModifyLookManager.set(facing);
        state = nextState;
        Debug.info("修改视角");
    }

    private void onRevertLook(TaskState nextState) {
        modifyLook = false;
        ModifyLookManager.reset();
        state = nextState;
        Debug.info("还原视角");
    }

    private void onInit() {
        this.totalTick = 0;
        this.timeout = false;
        this.recycledItems = false;
        this.succeed = false;
        this.init = true;
        this.state = TaskState.SELECT_SCHEME;
    }

    private void onSelectScheme() {
        List<SchemeInfo> list = new ArrayList<>();
        for (SchemeInfo schemeInfo : schemeInfos) {
            BlockInfo piston = schemeInfo.piston;
            BlockInfo redstoneTorch = schemeInfo.redstoneTorch;
            BlockInfo slimeBlock = schemeInfo.slimeBlock;
            // 检查活塞
            if (!world.getBlockState(piston.pos).isReplaceable()
                    || !world.getBlockState(piston.pos.offset(piston.facing)).isReplaceable()) {
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
                ++slimeBlock.level;  // 要放置就降级
            }
            list.add(schemeInfo);
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
        for (var l : list) {
            Debug.info("(%s, %s, %s), (%s, %s, %s), (%s), (%s), (%s)",
                    l.piston.level,
                    l.redstoneTorch.level,
                    l.slimeBlock.level,
                    l.piston.facing,
                    l.redstoneTorch.facing,
                    l.slimeBlock.facing,
                    l.piston.pos.toShortString(),
                    l.redstoneTorch.pos.toShortString(),
                    l.slimeBlock.pos.toShortString()
            );
            schemeQueues.add(l);
        }
        if (!schemeQueues.isEmpty()) {
            state = TaskState.PLACE_SCHEME_BLOCK;
        }
    }

    private void onPlaceSchemeBlocks() {
        var schemeInfo = schemeQueues.peek();
        if (schemeInfo != null) {
            BlockInfo piston = schemeInfo.piston;
            BlockInfo redstoneTorch = schemeInfo.redstoneTorch;
            BlockInfo slimeBlock = schemeInfo.slimeBlock;
            BlockPlacerUtils.placement(slimeBlock.pos, slimeBlock.facing, Items.SLIME_BLOCK);
            BlockPlacerUtils.placement(piston.pos, piston.facing, Items.PISTON);
            BlockPlacerUtils.placement(redstoneTorch.pos, redstoneTorch.facing, Items.REDSTONE_TORCH);
        }
        //TODO: 待实现
    }

    private void onExecute() {
        //TODO: 待实现
    }

    private void onWaitHandle() {
        //TODO: 待实现
    }

    private void onWaitForNetworkLatency() {
        //TODO: 待实现
    }

    private void onTimeoutHandle() {
        state = TaskState.FAIL;
    }

    private void onFail() {
        fail = true;
        state = TaskState.RECYCLED_ITEMS;
    }

    private void onSucceed() {
        Debug.info("成功");
        succeed = true;
    }

    private void onRecycledItems() {
        Debug.info("回收");
        //TODO: 回收物品逻辑, 待补充
        if (fail && retryCount < 0) {
            ++retryCount;
            state = TaskState.INITIALIZE;
        } else {
            succeed = true;
        }
    }

    private void onWaitGameUpdate() {
        Debug.info("等待");
        //TODO: 待实现
    }

    public void onTick() {
        // Debug.info(totalTick);
        if (succeed) {
            return;
        }
        if (!timeout && totalTick > 40) {
            timeout = true;
            state = TaskState.TIMEOUT;
        }
        switch (state) {
            case INITIALIZE -> onInit();
            case SELECT_SCHEME -> onSelectScheme();
            case PLACE_SCHEME_BLOCK -> onPlaceSchemeBlocks();
            case WAIT_GAME_UPDATE -> onWaitGameUpdate();
            case EXECUTE -> onExecute();
            case TIMEOUT -> onTimeoutHandle();
            case FAIL -> onFail();
            case RECYCLED_ITEMS -> onRecycledItems();
        }
        ++totalTick;
    }

    public boolean isSucceed() {
        return succeed;
    }
}
