package com.github.bunnyi116.bedrockminer.api;

import com.github.bunnyi116.bedrockminer.Debug;
import com.github.bunnyi116.bedrockminer.config.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.block.Block;
import org.jetbrains.annotations.Nullable;

import java.io.*;

public interface IConfigManager {
    /**
     * 获取配置
     * @return 配置
     */
    Config getConfig();

    /**
     * 加载配置
     * @return 配置实例
     */
    Config loadConfig();

    /**
     * 保存配置
     */
    void saveConfig();
}
