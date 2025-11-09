package com.github.bunnyi116.bedrockminer.task;

import com.github.bunnyi116.bedrockminer.api.ITaskManager;
import com.github.bunnyi116.bedrockminer.config.ConfigManager;
import com.github.bunnyi116.bedrockminer.util.*;
import net.minecraft.block.Block;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.github.bunnyi116.bedrockminer.BedrockMiner.*;
import static com.github.bunnyi116.bedrockminer.I18n.*;

public class TaskManager implements ITaskManager {
    private static volatile @Nullable TaskManager INSTANCE;

    private final ArrayList<Task> pendingBlockTasks = new ArrayList<>();
    private final List<TaskRegion> pendingRegionTasks = new ArrayList<>();
    private @Nullable Task currentTask;
    private boolean working;
    private boolean processing;
    private int resetCount;

    public void tick() {
        if (!gameVariableIsValid()) {
            return;
        }
        if (ConfigManager.getInstance().getConfig().disable || !this.isWorking()) {
            PlayerLookManager.INSTANCE.tick();
            return;
        }
        if (this.pendingBlockTasks.isEmpty() && this.pendingRegionTasks.isEmpty() && ConfigManager.getInstance().getConfig().ranges.isEmpty()) {
            this.currentTask = null;
            return;
        }
        final var setOverlayMessage = currentTask != null;
        if (isAllowExecutionEnvironment(setOverlayMessage)) {
            if (this.currentTask != null) {
                if (this.currentTask.world == world && this.currentTask.canInteractWithBlockAt()) {
                    this.processing = true;
                    this.currentTask.tick();
                    this.resetCount = 0;
                    this.processing = false;
                    if (this.currentTask.isComplete()) {
                        this.pendingBlockTasks.remove(this.currentTask);
                        this.currentTask = null;
                    } else {
                        return;
                    }
                } else if ((this.pendingBlockTasks.size() > 1 || !ConfigManager.getInstance().getConfig().ranges.isEmpty())) {
                    this.currentTask = null;
                    this.resetCount = 0;
                } else if (this.resetCount++ >= 40) {
                    this.currentTask = null;
                    this.resetCount = 0;
                }
            }
            if (this.currentTask == null) {
                final var iterator1 = pendingBlockTasks.iterator();
                while (iterator1.hasNext()) {
                    var task = iterator1.next();
                    if (!task.canInteractWithBlockAt()) {
                        continue;
                    }
                    if (PlayerLookManager.INSTANCE.isModify() && PlayerLookManager.INSTANCE.getTask() != task) {
                        continue;
                    }
                    if (task.world != world) {
                        iterator1.remove();
                        continue;
                    }
                    this.currentTask = task;
                    return;
                }
            }

            if (this.currentTask == null) {
                final var iterator2 = new CombinedIterator<>(ConfigManager.getInstance().getConfig().ranges, pendingRegionTasks);
                while (iterator2.hasNext()) {
                    var range = iterator2.next();
                    if (!range.isForWorld(world)) {
                        continue;
                    }
                    var rangeBox = BlockBox.create(range.pos1, range.pos2);
                    var playerBox = new BlockBox(player.getBlockPos());
                    var playerExpandBox = playerBox.expand((int) PlayerUtils.getBlockInteractionRange());
                    if (rangeBox.intersects(playerExpandBox)) {
                        final var blockInteractionRange = (int) PlayerUtils.getBlockInteractionRange() - 1;
                        for (int y = blockInteractionRange; y > -blockInteractionRange; y--) {
                            for (int x = -blockInteractionRange; x <= blockInteractionRange; x++) {
                                for (int z = -blockInteractionRange; z <= blockInteractionRange; z++) {
                                    final var blockPos = player.getBlockPos().add(x, y, z);
                                    final var box = new BlockBox(blockPos);
                                    if (rangeBox.intersects(box)) {
                                        final var blockState = world.getBlockState(blockPos);
                                        final var block = blockState.getBlock();
                                        if (blockState.isAir() || BlockUtils.isReplaceable(blockState)) {
                                            continue;
                                        }
                                        if (!ConfigManager.getInstance().getConfig().isAllowBlock(block)) {
                                            continue;
                                        }
                                        if (ConfigManager.getInstance().getConfig().isFloorsBlacklist(blockPos)) {
                                            continue;
                                        }
                                        var task = new Task(world, block, blockPos);
                                        if (!task.canInteractWithBlockAt()) {
                                            continue;
                                        }
                                        if (PlayerLookManager.INSTANCE.isModify() && PlayerLookManager.INSTANCE.getTask() != task) {
                                            continue;
                                        }
                                        if (task.world != world) {
                                            iterator2.remove();
                                            continue;
                                        }
                                        this.currentTask = task;
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void clearTask() {
        pendingBlockTasks.clear();
        if (PlayerLookManager.INSTANCE.isModify()) { // 如果有任务正在修改事件, 则还原玩家视角
            PlayerLookManager.INSTANCE.reset();
        }
        MessageUtils.addMessage(COMMAND_TASK_CLEAR);
    }

    public boolean isAllowExecutionEnvironment(boolean setOverlayMessage) {
        var msg = (Text) null;
        if (gameMode.isCreative()) {
            msg = FAIL_MISSING_SURVIVAL;
        }
        if (client.interactionManager != null && !client.interactionManager.getCurrentGameMode().isSurvivalLike()) {
            msg = FAIL_MISSING_SURVIVAL;
        }
        if (InventoryManagerUtils.getInventoryItemCount(Items.PISTON) < 2) {
            msg = FAIL_MISSING_PISTON;
        }
        if (InventoryManagerUtils.getInventoryItemCount(Items.REDSTONE_TORCH) < 1) {
            msg = FAIL_MISSING_REDSTONETORCH;
        }
        if (!InventoryManagerUtils.canInstantlyMinePiston()) {
            msg = FAIL_MISSING_INSTANTMINE;
        }
        if (msg != null && setOverlayMessage) {
            MessageUtils.setOverlayMessage(msg);
            return false;
        }
        return true;
    }

    @Override
    public void addBlockTask(ClientWorld world, BlockPos pos, Block block) {
        if (ConfigManager.getInstance().getConfig().disable || !isWorking()) {
            return;
        }
        if (!isAllowExecutionEnvironment(true)) {
            return;
        }
        if (!gameMode.isSurvivalLike()) {
            return;
        }
        if (!ConfigManager.getInstance().getConfig().isAllowBlock(block)) {
            return;
        }
        if (ConfigManager.getInstance().getConfig().isFloorsBlacklist(pos)) {  // 楼层限制
            var msg = FLOOR_BLACK_LIST_WARN.getString().replace("(#floor#)", String.valueOf(pos.getY()));
            MessageUtils.setOverlayMessage(Text.literal(msg));
            return;
        }
        for (var targetBlock : pendingBlockTasks) {
            if (targetBlock.pos.equals(pos)) {
                return;
            }
        }
        pendingBlockTasks.add(new Task(world, block, pos));
    }

    @Override
    public void removeBlockTask(ClientWorld world, BlockPos pos) {
        final var iterator = pendingBlockTasks.iterator();
        while (iterator.hasNext()) {
            var task = iterator.next();
            if (task.pos.equals(pos)) {
                iterator.remove();
                return;
            }
        }
    }

    @Override
    public void removeBlockTaskAll() {
        pendingBlockTasks.clear();
    }

    @Override
    public void addRegionTask(String name, ClientWorld world, BlockPos pos1, BlockPos pos2) {
        for (TaskRegion range : this.pendingRegionTasks) {
            if (range.name.equals(name)) {
                return;
            }
        }
        this.pendingRegionTasks.add(new TaskRegion(name, world, pos1, pos2));
    }

    @Override
    public void removeRegionTaskAll(String name) {
        final var iterator = pendingRegionTasks.iterator();
        while (iterator.hasNext()) {
            var range = iterator.next();
            if (range.name.equals(name)) {
                iterator.remove();
                return;
            }
        }
    }

    @Override
    public void removeRegionTaskAll() {
        pendingRegionTasks.clear();
    }

    public void switchOnOff(@Nullable Block block) {
        if (ConfigManager.getInstance().getConfig().disable || !ConfigManager.getInstance().getConfig().isAllowBlock(block))
            return;
        this.switchOnOff();
    }

    @Override
    public void switchOnOff() {
        if (this.isWorking()) {
            this.clearTask();
            this.setWorking(false);
        } else {
            if (gameMode.isCreative()) { // 仅生存模式开启
                MessageUtils.addMessage(FAIL_MISSING_SURVIVAL);
                return;
            }
            this.setWorking(true);
            if (!client.isInSingleplayer()) {   // 服务器开启时发送警告提示
                MessageUtils.addMessage(WARN_MULTIPLAYER);
            }
        }
    }

    @Override
    public void setWorking(boolean working) {
        if (working) {
            MessageUtils.addMessage(TOGGLE_ON);
        } else {
            MessageUtils.addMessage(TOGGLE_OFF);
        }
        this.working = working;
    }

    @Override
    public boolean isWorking() {
        return working;
    }

    @Override
    public boolean isProcessing() {
        return processing;
    }

    @Override
    public @Nullable Task getCurrentTask() {
        return currentTask;
    }

    public ArrayList<Task> getPendingBlockTasks() {
        return pendingBlockTasks;
    }

    public List<TaskRegion> getPendingRegionTasks() {
        return pendingRegionTasks;
    }

    public static TaskManager getInstance() {
        if (INSTANCE == null) {
            synchronized (TaskManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TaskManager();
                }
            }
        }
        return INSTANCE;
    }
}
