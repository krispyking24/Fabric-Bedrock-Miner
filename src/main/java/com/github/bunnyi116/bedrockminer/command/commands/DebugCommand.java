package com.github.bunnyi116.bedrockminer.command.commands;

import com.github.bunnyi116.bedrockminer.APIs;
import com.github.bunnyi116.bedrockminer.I18n;
import com.github.bunnyi116.bedrockminer.command.CommandBase;
import com.github.bunnyi116.bedrockminer.util.MessageUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class DebugCommand extends CommandBase {

    @Override
    public String getName() {
        return "debug";
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandRegistryAccess registryAccess) {
        builder
                .executes(context -> {
                    final boolean b = !APIs.getInstance().getConfig().debug;
                    if (b) {
                        MessageUtils.addMessage(I18n.DEBUG_ON);
                    } else {
                        MessageUtils.addMessage(I18n.DEBUG_OFF);
                    }
                    APIs.getInstance().getConfig().debug = b;
                    APIs.getInstance().getConfig().save();
                    return Command.SINGLE_SUCCESS;
                })

                .then(argument("bool", BoolArgumentType.bool())
                        .executes(context -> {
                            final boolean b = BoolArgumentType.getBool(context, "bool");
                            if (b) {
                                MessageUtils.addMessage(I18n.DEBUG_ON);
                            } else {
                                MessageUtils.addMessage(I18n.DEBUG_OFF);
                            }
                            APIs.getInstance().getConfig().debug = b;
                            APIs.getInstance().getConfig().save();
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }


}
