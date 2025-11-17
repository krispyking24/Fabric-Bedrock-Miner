package com.github.bunnyi116.bedrockminer.command;

import com.github.bunnyi116.bedrockminer.BedrockMiner;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public abstract class CommandBase {
    public abstract String getName();

    public abstract void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess);

    public final void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
        var builder = literal(this.getName());
        build(builder, registryAccess);
        dispatcher.register(literal(BedrockMiner.COMMAND_PREFIX).then(builder));
    }
}
