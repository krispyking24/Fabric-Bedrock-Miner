package yan.lx.bedrockminer.model;

import net.minecraft.util.math.Direction;

public class SchemeInfo {
    public final Direction direction;
    public final BlockInfo piston;
    public final BlockInfo redstoneTorch;
    public final BlockInfo slimeBlock;


    public SchemeInfo(Direction direction, BlockInfo piston, BlockInfo redstoneTorch, BlockInfo slimeBlock) {
        this.direction = direction;
        this.piston = piston;
        this.redstoneTorch = redstoneTorch;
        this.slimeBlock = slimeBlock;
    }
}
