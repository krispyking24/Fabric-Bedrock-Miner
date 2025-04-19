package com.github.bunnyi116.bedrockminer.task2.block;

import com.github.bunnyi116.bedrockminer.task2.TaskBlock;
import net.minecraft.block.Block;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

public class TaskTargetBlock extends TaskBlock {
    public final Block block;

    public TaskTargetBlock(ClientWorld world, BlockPos pos, Block block) {
        super(world, pos);
        this.block = block;
    }
}
