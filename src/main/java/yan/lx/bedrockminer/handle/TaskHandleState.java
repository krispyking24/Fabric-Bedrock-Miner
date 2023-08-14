package yan.lx.bedrockminer.handle;

public enum TaskHandleState {
    /**
     * 初始化
     */
    INITIALIZATION,

    /**
     * 查找解决方案
     */
    FIND_SOLUTION,

    /**
     * 选择解决方案
     */
    SELECT_SOLUTION,

    /**
     * 放置活塞
     */
    PLACE_PISTON,

    /**
     * 放置粘液块
     */
    PLACE_SLIME_BLOCK,

    /**
     * 放置红石火把
     */
    PLACE_REDSTONE_TORCH,

    /**
     * 修改视角
     */
    MODIFY_LOOK,

    /**
     * 修改视角还原
     */
    MODIFY_LOOK_RESTORE,

    /**
     * 开始执行
     */
    START_EXECUTION,

    /**
     * 等待游戏更新
     */
    WAIT_GAME_UPDATE,

    /**
     * 等待网络延迟
     */
    WAIT_NETWORK_LATENCY,

    /**
     * 超时
     */
    TIMEOUT,

    /**
     * 失败
     */
    FAIL,

    /**
     * 回收物品
     */
    RECYCLED_ITEMS,

    /**
     * 完成
     */
    FINISH,
}
