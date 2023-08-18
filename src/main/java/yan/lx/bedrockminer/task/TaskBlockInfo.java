package yan.lx.bedrockminer.task;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class TaskBlockInfo {
    public final Direction direction;
    public final BlockPos pos;
    public final Direction facing;
    public int level;
    public int recycledTickCount;
    public boolean recycledItems;
    public boolean modifyLook;
    public TaskBlockInfo(Direction direction, BlockPos pos, Direction facing, int level) {
        this.direction = direction;
        this.pos = pos;
        this.facing = facing;
        this.level = level;
    }


}
