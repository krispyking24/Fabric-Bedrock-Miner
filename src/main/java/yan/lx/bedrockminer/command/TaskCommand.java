package yan.lx.bedrockminer.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import yan.lx.bedrockminer.BedrockMinerLang;
import yan.lx.bedrockminer.command.argument.BlockPosArgumentType;
import yan.lx.bedrockminer.config.Config;
import yan.lx.bedrockminer.task.TaskManager;
import yan.lx.bedrockminer.utils.MessageUtils;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class TaskCommand extends BaseCommand {

    @Override
    public String getName() {
        return "task";
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandRegistryAccess registryAccess) {
        builder
                .then(literal("add")
                        .then(argument("blockPos", BlockPosArgumentType.blockPos()).executes(this::add)))
                .then(literal("clear").executes(this::clear))
                .then(literal("limit")
                        .then(argument("limit", IntegerArgumentType.integer(1, 5))
                                .executes(this::toggleSwitch)));
    }

    private int add(CommandContext<FabricClientCommandSource> context) {
        var blockPos = BlockPosArgumentType.getBlockPos(context, "blockPos");
        var client = MinecraftClient.getInstance();
        var world = client.world;
        if (world != null) {
            var blockState = world.getBlockState(blockPos);
            var block = blockState.getBlock();

            TaskManager.addTask(block, blockPos, world);
        }
        return 0;
    }

    private int clear(CommandContext<FabricClientCommandSource> context) {
        TaskManager.clearTask();
        MessageUtils.addMessage(BedrockMinerLang.COMMAND_TASK_CLEAR);
        return 0;
    }


    private int toggleSwitch(CommandContext<FabricClientCommandSource> context) {
        var config = Config.INSTANCE;
        var limit = IntegerArgumentType.getInteger(context, "limit");
        if (config.taskLimit != limit) {
            config.taskLimit = limit;
            Config.save();
        }
        var msg = BedrockMinerLang.COMMAND_TASK_LIMIT.getString().replace("%limit%", String.valueOf(limit));
        MessageUtils.addMessage(Text.translatable(msg));
        return 0;
    }
}
