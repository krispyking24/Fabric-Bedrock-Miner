package yan.lx.bedrockminer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yan.lx.bedrockminer.command.*;
import yan.lx.bedrockminer.handle.TaskManager;

import java.util.ArrayList;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class BedrockMinerMod implements ModInitializer {
    public static final String MOD_NAME = "Bedrock Miner";
    public static final String MOD_ID = "bedrockminer";
    public static final String COMMAND_PREFIX = "bedrockMiner";
    public static final Logger LOGGER = LoggerFactory.getLogger("Bedrock Miner");

    @Override
    public void onInitialize() {
        registerCommand();
        Debug.info("模组初始化成功");
    }

    private void registerCommand() {
        // 初始化命令实例
        var commands = new ArrayList<BaseCommand>();
        commands.add(new BlockCommand());
        commands.add(new BlockNameCommand());
        commands.add(new DebugCommand());
        commands.add(new TaskCommand());

        // 开始注册
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // 子命令
            for (var command : commands) {
                command.register(dispatcher, registryAccess);
            }
            // 主命令执行
            dispatcher.register(literal(COMMAND_PREFIX).executes(context -> {
                        TaskManager.setWorking(!TaskManager.isWorking());
                        return 0;
                    })
            );
        });
    }

}
