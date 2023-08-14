package yan.lx.bedrockminer.model;

import net.minecraft.util.math.Direction;

/**
 * 任务方案方块信息
 */
public class TaskSchemeInfo {
    /**
     * 方案的方向（基于目标方块，如果是UP，则为在目标方块上方放置活塞）
     */
    public final Direction direction;

    /**
     * 目标方块
     */
    public final TaskBlockInfo.TargetBlock target;

    /**
     * 活塞方块
     */
    public final TaskBlockInfo.Piston piston;

    /**
     * 红石火把
     */
    public final TaskBlockInfo.RedstoneTorch redstoneTorch;

    /**
     * 基座方块
     */
    public final TaskBlockInfo.BaseBlock baseblock;

    public TaskSchemeInfo(Direction direction, TaskBlockInfo.TargetBlock target, TaskBlockInfo.Piston piston, TaskBlockInfo.RedstoneTorch redstoneTorch, TaskBlockInfo.BaseBlock baseblock) {
        this.direction = direction;
        this.target = target;
        this.piston = piston;
        this.redstoneTorch = redstoneTorch;
        this.baseblock = baseblock;
    }
}
