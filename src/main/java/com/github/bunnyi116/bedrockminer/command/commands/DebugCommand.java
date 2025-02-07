package com.github.bunnyi116.bedrockminer.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import com.github.bunnyi116.bedrockminer.I18n;
import com.github.bunnyi116.bedrockminer.command.CommandBase;
import com.github.bunnyi116.bedrockminer.config.Config;
import com.github.bunnyi116.bedrockminer.utils.MessageUtils;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class DebugCommand extends CommandBase {

    @Override
    public String getName() {
        return "debug";
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandRegistryAccess registryAccess) {
        builder.then(literal("true").executes(context -> toggleSwitch(true)))
                .then(literal("false").executes(context -> toggleSwitch(false)));
    }

    private int toggleSwitch(boolean b) {
        if (b) {
            MessageUtils.addMessage(I18n.DEBUG_ON);
        } else {
            MessageUtils.addMessage(I18n.DEBUG_OFF);
        }
        Config.INSTANCE.debug = b;
        Config.save();
        return 0;
    }
}
