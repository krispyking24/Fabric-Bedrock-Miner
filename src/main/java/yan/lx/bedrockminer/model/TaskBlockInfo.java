package yan.lx.bedrockminer.model;

import net.minecraft.util.math.Direction;

/**
 * 任务方案方块信息
 */
public class TaskBlockInfo {
    /**
     * 方案的方向（基于目标方块，如果是UP，则为在目标方块上方放置活塞）
     */
    public final Direction direction;

    /**
     * 目标方块
     */
    public final TaskInfo.TargetBlock target;

    /**
     * 活塞方块
     */
    public final TaskInfo.Piston piston;

    /**
     * 红石火把
     */
    public final TaskInfo.RedstoneTorch redstoneTorch;

    /**
     * 基座方块
     */
    public final TaskInfo.BaseBlock block;

    public TaskBlockInfo(Direction direction, TaskInfo.TargetBlock target, TaskInfo.Piston piston, TaskInfo.RedstoneTorch redstoneTorch, TaskInfo.BaseBlock block) {
        this.direction = direction;
        this.target = target;
        this.piston = piston;
        this.redstoneTorch = redstoneTorch;
        this.block = block;
    }
}
