package com.github.bunnyi116.bedrockminer;

import com.github.bunnyi116.bedrockminer.api.IConfigManager;
import com.github.bunnyi116.bedrockminer.api.ITaskManager;
import com.github.bunnyi116.bedrockminer.config.Config;
import com.github.bunnyi116.bedrockminer.config.ConfigManager;
import com.github.bunnyi116.bedrockminer.task.TaskManager;
import org.jetbrains.annotations.Nullable;

public class APIs {
    private static volatile @Nullable APIs INSTANCE;

    /**
     * 获取配置管理器
     */
    public IConfigManager getConfigManager() {
        return ConfigManager.getInstance();
    }

    /**
     * 获取配置
     */
    public Config getConfig() {
        return getConfigManager().getConfig();
    }

    /**
     * 获取任务管理器
     */
    public ITaskManager getTaskManager() {
        return TaskManager.getInstance();
    }

    /**
     * 获取单例实例
     */
    public static APIs getInstance() {
        if (INSTANCE == null) {
            synchronized (APIs.class) {
                if (INSTANCE == null) {
                    INSTANCE = new APIs();
                }
            }
        }
        return INSTANCE;
    }
}
