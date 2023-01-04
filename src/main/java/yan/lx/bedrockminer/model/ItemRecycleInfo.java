package yan.lx.bedrockminer.model;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public record ItemRecycleInfo(Block block, BlockPos blockPos) {
    public String getName() {
        return block.getName().getString();
    }
}
