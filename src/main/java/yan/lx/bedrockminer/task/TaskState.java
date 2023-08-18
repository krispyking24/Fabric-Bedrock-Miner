package yan.lx.bedrockminer.task;

public enum TaskState {
    INITIALIZE,
    SELECT_SCHEME, PLACE_SCHEME_BLOCK,
    WAIT_GAME_UPDATE, WAIT,
    EXECUTE,
    TIMEOUT, FAIL, RECYCLED_ITEMS,

    SUCCESS
}
