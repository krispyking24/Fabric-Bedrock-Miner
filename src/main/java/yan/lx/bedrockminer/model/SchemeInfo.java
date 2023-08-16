package yan.lx.bedrockminer.model;

import yan.lx.bedrockminer.model.BlockInfo;

public class SchemeInfo {
    public final BlockInfo piston;
    public final BlockInfo redstoneTorch;
    public final BlockInfo slimeBlock;

    public SchemeInfo(BlockInfo piston, BlockInfo redstoneTorch, BlockInfo slimeBlock) {
        this.piston = piston;
        this.redstoneTorch = redstoneTorch;
        this.slimeBlock = slimeBlock;
    }
}
