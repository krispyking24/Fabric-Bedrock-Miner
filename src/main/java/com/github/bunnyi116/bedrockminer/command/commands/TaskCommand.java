package com.github.bunnyi116.bedrockminer.command.commands;

import com.github.bunnyi116.bedrockminer.BedrockMiner;
import com.github.bunnyi116.bedrockminer.I18n;
import com.github.bunnyi116.bedrockminer.command.CommandBase;
import com.github.bunnyi116.bedrockminer.command.argument.BlockPosArgumentType;
import com.github.bunnyi116.bedrockminer.config.Config;
import com.github.bunnyi116.bedrockminer.task.TaskManager;
import com.github.bunnyi116.bedrockminer.task.TaskRange;
import com.github.bunnyi116.bedrockminer.util.MessageUtils;
import com.github.bunnyi116.bedrockminer.util.StringReaderUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class TaskCommand extends CommandBase {

    @Override
    public String getName() {
        return "task";
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandRegistryAccess registryAccess) {
        builder
                .then(literal("shortWait")
                        .then(argument("bool", BoolArgumentType.bool())
                                .executes(context -> {
                                    var b = BoolArgumentType.getBool(context, "bool");
                                    if (b) {
                                        MessageUtils.addMessage(I18n.COMMAND_TASK_SHORT_WAIT_SHORT);
                                    } else {
                                        MessageUtils.addMessage(I18n.COMMAND_TASK_SHORT_WAIT_NORMAL);
                                    }
                                    Config.INSTANCE.taskShortWait = b;
                                    Config.save();
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )

                .then(literal("add")
                        .then(argument("name", StringArgumentType.string())
                                .then(argument("blockPos1", BlockPosArgumentType.blockPos())
                                        .then(argument("blockPos2", BlockPosArgumentType.blockPos())
                                                .executes(context -> {
                                                    final var name = StringArgumentType.getString(context, "name");
                                                    final var blockPos1 = BlockPosArgumentType.getBlockPos(context, "blockPos1");
                                                    final var blockPos2 = BlockPosArgumentType.getBlockPos(context, "blockPos2");
                                                    boolean b = true;
                                                    for (final var item : Config.INSTANCE.ranges) {
                                                        if (item.name.equals(name)) {
                                                            b = false;
                                                            break;
                                                        }
                                                    }
                                                    if (b) {
                                                        final var range = new TaskRange(name, BedrockMiner.world, blockPos1, blockPos2);
                                                        Config.INSTANCE.ranges.add(range);
                                                        Config.save();
                                                        MessageUtils.addMessage(Text.literal("已成功添加: " + name));
                                                    }
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
                )

                .then(literal("remove")
                        .then(argument("name", StringArgumentType.string())
                                .suggests((context, suggestionsBuilder) -> {
                                    var reader = new StringReader(suggestionsBuilder.getInput());
                                    reader.setCursor(suggestionsBuilder.getStart());
                                    var input = StringReaderUtils.readUnquotedString(reader);
                                    for (var item : Config.INSTANCE.ranges) {
                                        if (item.name.contains(input)) {
                                            suggestionsBuilder.suggest(item.name);
                                        }
                                    }
                                    return suggestionsBuilder.buildFuture();
                                })
                                .executes(context -> {
                                    final var name = StringArgumentType.getString(context, "name");
                                    @Nullable TaskRange range = null;
                                    for (final var item : Config.INSTANCE.ranges) {
                                        if (item.name.equals(name)) {
                                            range = item;
                                            break;
                                        }
                                    }
                                    if (range != null) {
                                        Config.INSTANCE.ranges.remove(range);
                                        Config.save();
                                        MessageUtils.addMessage(Text.literal("已成功删除: " + name));
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )

                .then(literal("clear").executes(context -> {
                    TaskManager.INSTANCE.clearTask();
                    return Command.SINGLE_SUCCESS;
                }));

    }
}
