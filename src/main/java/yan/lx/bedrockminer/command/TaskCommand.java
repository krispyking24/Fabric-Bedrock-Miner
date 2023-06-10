package yan.lx.bedrockminer.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import yan.lx.bedrockminer.config.Config;
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
        builder.then(literal("limit")
                .then(argument("limit", IntegerArgumentType.integer(1, 5))
                        .executes(this::toggleSwitch)));
    }


    private int toggleSwitch(CommandContext<FabricClientCommandSource> context) {
        var config = Config.getInstance();
        var limit = IntegerArgumentType.getInteger(context, "limit");
        if (config.taskLimit != limit) {
            config.taskLimit = limit;
            Config.save();
        }
        MessageUtils.addMessage(Text.translatable("bedrockminer.command.limit").getString().replace("%limit%", String.valueOf(limit)));
        return 0;
    }
}
