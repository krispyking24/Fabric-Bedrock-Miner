package yan.lx.bedrockminer;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import yan.lx.bedrockminer.command.argument.BlockPosArgumentType;
import yan.lx.bedrockminer.utils.BlockPlacerUtils;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class Test {

    public static void register(LiteralArgumentBuilder<FabricClientCommandSource> root) {
        root.then(literal("test").executes(Test::onCommandExecute)
                .then(argument("blockPos", BlockPosArgumentType.blockPos()).executes(Test::onBlockPos)));
    }

    private static int onBlockPos(CommandContext<FabricClientCommandSource> context) {
        var blockPos = BlockPosArgumentType.getBlockPos(context, "blockPos");
        BlockPlacerUtils.placement(blockPos.up(), Direction.NORTH);
        return 0;
    }

    public static int onCommandExecute(CommandContext<FabricClientCommandSource> context) {
        BlockPlacerUtils.placement(new BlockPos(18,-59,-17), Direction.SOUTH, Items.PISTON);
        return 0;
    }


}
