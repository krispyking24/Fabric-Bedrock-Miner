package com.github.bunnyi116.bedrockminer.api;

import com.github.bunnyi116.bedrockminer.task.Task;
import net.minecraft.block.Block;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public interface ITaskManager {
    /**
     * 添加一个方块任务
     * @param world 所在世界
     * @param pos 所在位置
     * @param block 方块类型(用于检查是否成功)
     */
    void addBlockTask(ClientWorld world, BlockPos pos, Block block);

    /**
     * 移除一个方块任务(基本用不到)
     * @param world 所在世界
     * @param pos 所在位置
     */
    void removeBlockTask(ClientWorld world, BlockPos pos);

    /**
     * 移除所有方块任务
     */
    void removeBlockTaskAll();

    /**
     * 添加一个区域任务
     * @param name 任务名称(无法添加重复任务)
     * @param world 所在世界
     * @param pos1 位置1
     * @param pos2 位置2
     */
    void addRegionTask(String name, ClientWorld world, BlockPos pos1, BlockPos pos2);

    /**
     * 移除一个区域任务
     * @param name 任务名称
     */
    void removeRegionTaskAll(String name);

    /**
     * 移除所有区域任务(不包含配置中的区域任务)
     */
    void removeRegionTaskAll();

    /**
     * 工作切换开关
     * @param block 传入方块类型, 用于检查该方块是否允许切换开关
     */
    void switchToggle(@Nullable Block block);

    /**
     * 工作切换开关
     */
    void switchToggle();

    /**
     * 设置工作状态
     * @param working 是否正在工作
     */
    void setRunning(boolean running);

    /**
     * 是否正在工作
     * @return 正在工作为: TRUE
     */
    boolean isRunning();

    /**
     * 是否正在处理任务
     * @return 正在处理任务为: TRUE
     */
    boolean isProcessing();

    /**
     * 获取当前正在执行的任务
     * @return 任务
     */
    @Nullable Task getCurrentTask();
}
