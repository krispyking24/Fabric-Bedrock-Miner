package yan.lx.bedrockminer.model;

import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.handle.TaskSchemeFinder;

/**
 * 任务解决方案信息（多个任务方块信息，和当前选中一个任务方块信息）
 */
public class TaskSolutionInfo {
    /**
     * 已查找的方案列表
     */
    public final TaskSchemeInfo[] schemes;

    /**
     * 可以执行的方案
     */
    private @Nullable TaskSchemeInfo[] canExecuteScheme;

    /**
     * 上一次选中的索引值
     */
    private int selectedIndex;

    public TaskSolutionInfo(TaskSchemeInfo[] schemes, @Nullable TaskSchemeInfo select) {
        this.schemes = schemes;
    }

    public void setCanExecuteScheme(TaskSchemeInfo[] canExecuteScheme) {
        this.canExecuteScheme = canExecuteScheme;
        this.selectedIndex = 0;
    }

    public void skip() {
        ++selectedIndex;
    }

    public @Nullable TaskSchemeInfo get() {
        if (this.selectedIndex < canExecuteScheme.length) {
            return canExecuteScheme[selectedIndex++];
        }
        return null;
    }

    public @Nullable TaskSchemeInfo peek() {
        if (this.selectedIndex < canExecuteScheme.length) {
            return canExecuteScheme[selectedIndex];
        }
        return null;
    }

    /**
     * 获取所有可以执行的方案
     */
    public TaskSchemeInfo[] getAllCanExecuteScheme() {
        return TaskSchemeFinder.getAllCanExecuteScheme(this);
    }

    /**
     * 获取所有可以执行的方案
     */
    public TaskSchemeInfo[] getOptimalSort() {
        return TaskSchemeFinder.getOptimalSort(this);
    }

    /**
     * 方案是否为空
     */
    public boolean isEmpty() {
        return schemes.length == 0;
    }

    /**
     * 是否已经停止
     */
    public boolean isStop() {
        return canExecuteScheme.length == 0 || selectedIndex > canExecuteScheme.length - 1;
    }
}
