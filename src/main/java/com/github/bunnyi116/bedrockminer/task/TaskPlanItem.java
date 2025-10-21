package com.github.bunnyi116.bedrockminer.task;

import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class TaskPlanItem {
    public final Direction direction;
    public final TaskPlan piston;
    public final TaskPlan redstoneTorch;
    public final TaskPlan slimeBlock;

    public TaskPlanItem(Direction direction, TaskPlan piston, TaskPlan redstoneTorch, TaskPlan slimeBlock) {
        this.direction = direction;
        this.piston = piston;
        this.redstoneTorch = redstoneTorch;
        this.slimeBlock = slimeBlock;
    }

    public boolean isWorldValid() {
        return World.isValid(piston.pos) && World.isValid(redstoneTorch.pos) && World.isValid(slimeBlock.pos);
    }
}
