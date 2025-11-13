package com.github.bunnyi116.bedrockminer.task;

public enum TaskState {
    INITIALIZE,
    WAIT_GAME_UPDATE(true),
    WAIT_CUSTOM(true),
    FIND,
    PLACE_PISTON,
    PLACE_REDSTONE_TORCH,
    PLACE_SLIME_BLOCK,
    EXECUTE(true),
    RETRY,
    TIMEOUT,
    FAIL,
    RECYCLED_ITEMS(true),
    COMPLETE;

    private final boolean exclusiveTICK;

    TaskState() {
        exclusiveTICK = false;
    }

    TaskState(boolean exclusiveTICK) {
        this.exclusiveTICK = exclusiveTICK;
    }

    public boolean isExclusiveTick() {
        return exclusiveTICK;
    }
}
