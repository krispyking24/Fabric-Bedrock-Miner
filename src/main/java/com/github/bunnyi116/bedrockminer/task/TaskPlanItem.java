package com.github.bunnyi116.bedrockminer.task;

import com.github.bunnyi116.bedrockminer.util.ClientPlayerInteractionManagerUtils;
import net.minecraft.block.Block;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import static com.github.bunnyi116.bedrockminer.BedrockMiner.*;


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

    public boolean canInteractWithBlockAt() {
        final var b1 = ClientPlayerInteractionManagerUtils.canInteractWithBlockAt(piston.pos, 1.0F);
        final var b2 = ClientPlayerInteractionManagerUtils.canInteractWithBlockAt(redstoneTorch.pos, 1.0F);
        if (b1 && b2) {
            final var b3 = ClientPlayerInteractionManagerUtils.canInteractWithBlockAt(slimeBlock.pos, 1.0F);
            if (b3 && world.getBlockState(slimeBlock.pos).isReplaceable()) {
                return true;
            }
            return Block.sideCoversSmallSquare(world, slimeBlock.pos, slimeBlock.facing);
        }
        return false;
    }
}