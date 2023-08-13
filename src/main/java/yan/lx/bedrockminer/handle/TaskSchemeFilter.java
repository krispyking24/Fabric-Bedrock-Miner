package yan.lx.bedrockminer.handle;

import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.model.TaskBlockInfo;

/**
 * 任务方案过滤器
 */
public class TaskSchemeFilter {
    public static boolean filterBad(TaskBlockInfo scheme) {
        return false;
    }

    public static @Nullable TaskBlockInfo getOptimal(@Nullable TaskBlockInfo[] schemes) {
        return null;
    }
}
