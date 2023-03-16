package yan.lx.bedrockminer.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sun.jdi.connect.Connector;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import yan.lx.bedrockminer.Debug;
import yan.lx.bedrockminer.config.Config;
import yan.lx.bedrockminer.utils.Messager;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class DebugCommand extends BaseCommand {

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
            Messager.chat("bedrockminer.command.debug.true");
        } else {
            Messager.chat("bedrockminer.command.debug.false");
        }
        Config.getInstance().debug = b;
        Config.save();
        return 0;
    }
}
