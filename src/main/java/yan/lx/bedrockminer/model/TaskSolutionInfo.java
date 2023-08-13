package yan.lx.bedrockminer.model;

import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.handle.TaskSchemeFilter;

import java.util.Arrays;

/**
 * 任务解决方案信息（多个任务方块信息，和当前选中一个任务方块信息）
 */
public class TaskSolutionInfo {
    /**
     * 已查找的方案列表
     */
    public final TaskBlockInfo[] schemes;

    /**
     * 选中的方案列表
     */
    public @Nullable TaskBlockInfo select;

    public TaskSolutionInfo(TaskBlockInfo[] schemes, @Nullable TaskBlockInfo select) {
        this.schemes = schemes;
        this.select = select;
    }

    public TaskBlockInfo[] getFilterCurrentlyNotAvailableScheme() {
        return Arrays.stream(schemes).filter(TaskSchemeFilter::filterBad).toArray(TaskBlockInfo[]::new);
    }

    public @Nullable TaskBlockInfo getOptimalScheme() {
        return TaskSchemeFilter.getOptimal(schemes);
    }

    public boolean isEmpty() {
        return schemes.length == 0;
    }
}
