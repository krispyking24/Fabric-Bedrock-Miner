package yan.lx.bedrockminer.model;

/**
 * 任务方案方块信息
 */
public class TaskBlockInfo {
    /**
     * 目标方块
     */
    public final TaskInfo.TargetTask target;

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
    public final TaskInfo.BaseTask block;

    public TaskBlockInfo(TaskInfo.TargetTask target, TaskInfo.Piston piston, TaskInfo.RedstoneTorch redstoneTorch, TaskInfo.BaseTask block) {
        this.target = target;
        this.piston = piston;
        this.redstoneTorch = redstoneTorch;
        this.block = block;
    }
}
