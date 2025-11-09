package com.github.bunnyi116.bedrockminer.task;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

public class TaskRegion {
    public String name;
    public String dimensionId;
    public BlockPos pos1;
    public BlockPos pos2;

    public TaskRegion(String name, ClientWorld world, BlockPos pos1, BlockPos pos2) {
        this.name = name;
        this.dimensionId = world.getRegistryKey().getValue().toString();
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public boolean isForWorld(ClientWorld world) {
        return this.dimensionId.equals(world.getRegistryKey().getValue().toString());
    }

    public BlockBox getBlockBox() {
        return BlockBox.create(pos1, pos2);
    }
}
