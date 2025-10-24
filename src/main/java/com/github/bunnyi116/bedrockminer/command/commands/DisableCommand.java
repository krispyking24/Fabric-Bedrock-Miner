package com.github.bunnyi116.bedrockminer.command.commands;

import com.github.bunnyi116.bedrockminer.I18n;
import com.github.bunnyi116.bedrockminer.command.CommandBase;
import com.github.bunnyi116.bedrockminer.config.Config;
import com.github.bunnyi116.bedrockminer.util.MessageUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class DisableCommand extends CommandBase {

    @Override
    public String getName() {
        return "disable";
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandRegistryAccess registryAccess) {
        builder
                .executes(context -> {
                    if (Config.INSTANCE.disable) {
                        Config.INSTANCE.disable = false;
                        MessageUtils.addMessage(I18n.COMMAND_DISABLE_OFF);
                    } else {
                        Config.INSTANCE.disable = true;
                        MessageUtils.addMessage(I18n.COMMAND_DISABLE_ON);
                    }
                    Config.save();
                    return Command.SINGLE_SUCCESS;
                })

                .then(argument("bool", BoolArgumentType.bool())
                        .executes(context -> {
                            Config.INSTANCE.disable = BoolArgumentType.getBool(context, "bool");
                            if (Config.INSTANCE.disable) {
                                MessageUtils.addMessage(I18n.COMMAND_DISABLE_ON);
                            } else {
                                MessageUtils.addMessage(I18n.COMMAND_DISABLE_OFF);
                            }
                            Config.save();
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

}
