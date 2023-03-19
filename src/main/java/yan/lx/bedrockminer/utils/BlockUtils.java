package yan.lx.bedrockminer.utils;

import net.minecraft.block.Block;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class BlockUtils {

    public static String getName(Block block) {
        return block.getName().toString();
    }

    public static Identifier getIdentifier(Block block) {
        return Registry.BLOCK.getId(block);
    }

    public static String getId(Block block) {
        return getIdentifier(block).toString();
    }
}
