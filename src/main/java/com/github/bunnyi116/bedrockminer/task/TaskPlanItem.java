package com.github.bunnyi116.bedrockminer.task;

import com.github.bunnyi116.bedrockminer.util.BlockUtils;
import com.github.bunnyi116.bedrockminer.util.ClientPlayerInteractionManagerUtils;
import com.github.bunnyi116.bedrockminer.util.PlayerUtils;
import net.minecraft.block.Block;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import static com.github.bunnyi116.bedrockminer.BedrockMiner.*;


public class TaskPlanItem {
    public final Direction direction;
    public final TaskPlan piston;
    public final TaskPlan redstoneTorch;
    public final TaskPlan slimeBlock;
    public int level;

    public TaskPlanItem(Direction direction, TaskPlan piston, TaskPlan redstoneTorch, TaskPlan slimeBlock) {
        this.direction = direction;
        this.piston = piston;
        this.redstoneTorch = redstoneTorch;
        this.slimeBlock = slimeBlock;
        this.level = switch (direction) {
            case UP -> 10;
            case DOWN -> 20;
            case NORTH, SOUTH, WEST, EAST -> 30;
        };
    }

    public boolean isWorldValid() {
        return World.isValid(piston.pos) && World.isValid(redstoneTorch.pos) && World.isValid(slimeBlock.pos);
    }

    public boolean canInteractWithBlockAt() {
        final var b1 = PlayerUtils.canInteractWithBlockAt(piston.pos, 1.0F);
        final var b2 = PlayerUtils.canInteractWithBlockAt(redstoneTorch.pos, 1.0F);
        if (b1 && b2) {
            final var b3 = PlayerUtils.canInteractWithBlockAt(slimeBlock.pos, 1.0F);
            if (b3 && BlockUtils.isReplaceable(world.getBlockState(slimeBlock.pos))) {
                return true;
            }
            return Block.sideCoversSmallSquare(world, slimeBlock.pos, slimeBlock.facing);
        }
        return false;
    }
}