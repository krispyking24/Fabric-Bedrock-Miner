package yan.lx.bedrockminer;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.ServerCommandSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yan.lx.bedrockminer.utils.BreakingFlowController;
import yan.lx.bedrockminer.utils.Messager;

import static net.minecraft.server.command.CommandManager.*;

public class BedrockMinerMod implements ModInitializer {
    public static final String NAME = "bedrockMiner";
    public static final Logger LOGGER = LoggerFactory.getLogger("Bedrock Miner");

    @Override
    public void onInitialize() {
        BreakingFlowController.allowBlockList.add(Blocks.BEDROCK);            // 基岩
        BreakingFlowController.allowBlockList.add(Blocks.OBSIDIAN);           // 黑曜石
        BreakingFlowController.allowBlockList.add(Blocks.END_PORTAL);         // 末地传送门
        BreakingFlowController.allowBlockList.add(Blocks.END_PORTAL_FRAME);   // 末地传送门-框架
        BreakingFlowController.allowBlockList.add(Blocks.END_GATEWAY);        // 末地折跃门

        registerCommand();

        Debug.info("模组初始化成功");
    }

    private void registerCommand() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = literal(NAME);
            var debug = literal("debug");
            {
                // 输出类型
                debug.then(literal("outputType")
                        // 客户端覆盖栏消息
                        .then(literal("overlayMessage").executes(context -> {
                            Debug.outputType = Debug.OutputType.OVERLAY_MESSAGE;
                            Messager.chat("bedrockminer.command.debug.outputType");
                            return 0;
                        }))
                        // 客户端消息
                        .then(literal("chatMessage").executes(context -> {
                            Debug.outputType = Debug.OutputType.CHAT_MESSAGE;
                            Messager.chat("bedrockminer.command.debug.outputType");
                            return 0;
                        }))
                        // 日志信息
                        .then(literal("loggerInfo").executes(context -> {
                            Debug.outputType = Debug.OutputType.LOGGER_INFO;
                            Messager.chat("bedrockminer.command.debug.outputType");
                            return 0;
                        }))
                );
                // 开
                debug.then(literal("true").executes(context -> {
                    Debug.enable = true;
                    Messager.chat("bedrockminer.command.debug.true");
                    return 0;
                }));
                // 关
                debug.then(literal("false").executes(context -> {
                    Debug.enable = true;
                    Messager.chat("bedrockminer.command.debug.true");
                    return 0;
                }));
            }

            // 子命令添加主命令
            root.then(debug);
            // 注册
            dispatcher.register(root);
        });

    }

}
