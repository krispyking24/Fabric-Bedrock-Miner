package yan.lx.bedrockminer.model;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class BlockInfo {
    public final BlockPos pos;
    public final Direction facing;

    public BlockInfo(BlockPos pos, Direction facing) {
        this.pos = pos;
        this.facing = facing;
    }
}
