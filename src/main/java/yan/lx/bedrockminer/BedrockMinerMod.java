package yan.lx.bedrockminer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yan.lx.bedrockminer.command.BaseCommand;
import yan.lx.bedrockminer.command.BlockCommand;
import yan.lx.bedrockminer.command.DebugCommand;
import yan.lx.bedrockminer.config.Config;
import yan.lx.bedrockminer.utils.BreakingFlowController;
import yan.lx.bedrockminer.utils.Messager;

import java.util.ArrayList;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class BedrockMinerMod implements ModInitializer {
    public static final String name = "bedrockMiner";
    public static final String prefix = "bedrockMiner";
    public static final Logger logger = LoggerFactory.getLogger("Bedrock Miner");

    @Override
    public void onInitialize() {
        Config.load();
        registerCommand();

        Debug.info("模组初始化成功");
    }

    private void registerCommand() {
        // 初始化命令实例
        var commands = new ArrayList<BaseCommand>();
        commands.add(new BlockCommand());
        commands.add(new DebugCommand());

        // 开始注册
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            for (var command : commands) {
                command.register(dispatcher, registryAccess);
            }

            dispatcher.register(literal(prefix).executes(context -> {
                        if (BreakingFlowController.isWorking()) {
                            BreakingFlowController.setWorking(false);
                        } else {
                            BreakingFlowController.setWorking(true);
                        }
                        return 0;
                    })
            );
        });
    }

}
