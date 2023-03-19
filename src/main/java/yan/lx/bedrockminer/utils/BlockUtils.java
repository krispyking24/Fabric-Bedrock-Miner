package yan.lx.bedrockminer.utils;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class BlockUtils {

    public static String getName(Block block) {
        return block.getName().toString();
    }

    public static Identifier getIdentifier(Block block) {
        return Registries.BLOCK.getId(block);
    }

    public static String getId(Block block) {
        return getIdentifier(block).toString();
    }
}
