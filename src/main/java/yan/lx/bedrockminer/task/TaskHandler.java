package yan.lx.bedrockminer.task;

import net.minecraft.block.Block;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import yan.lx.bedrockminer.Debug;

public class TaskHandler {
    public final ClientWorld world;
    public final Block block;
    public final BlockPos pos;
    public TaskState state;
    private boolean init;
    private int totalTick;
    private int recycledTick;
    private boolean timeout;
    private boolean recycledItems;
    private boolean fail;
    private boolean succeed;
    private boolean modifyLook;

    public TaskHandler(ClientWorld world, Block block, BlockPos pos) {
        this.world = world;
        this.block = block;
        this.pos = pos;
        this.state = TaskState.INITIALIZE;
    }

    private void onInit() {
        this.totalTick = 0;
        this.timeout = false;
        this.recycledItems = false;
        this.succeed = false;
        this.init = true;
        this.state = TaskState.WAIT_GAME_UPDATE;
    }

    private void onSelectScheme() {
        //TODO: 待实现
    }

    private void onPlaceSchemeBlocks() {
        //TODO: 待实现
    }

    private void onModifyLook(Direction facing, TaskState nextState) {
        Debug.info("修改视角");
        modifyLook = true;
        ModifyLookManager.set(facing);
        state = nextState;
    }

    private void onRevertModifyLook(TaskState nextState) {
        modifyLook = false;
        ModifyLookManager.reset();
        state = nextState;
        Debug.info("还原视角");
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

        if (fail) {
            state = TaskState.INITIALIZE;
        }
    }

    private void onWaitGameUpdate() {
        Debug.info("等待");
        //TODO: 待实现
    }

    public void onTick() {
        Debug.info(totalTick);
        if (succeed) {
            return;
        }
        if (!timeout && totalTick > 60) {
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
    }

    public boolean isSucceed() {
        return succeed;
    }
}
