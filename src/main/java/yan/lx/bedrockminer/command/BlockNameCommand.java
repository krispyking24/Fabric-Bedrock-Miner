package yan.lx.bedrockminer.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import yan.lx.bedrockminer.command.argument.BlockNameArgument;
import yan.lx.bedrockminer.command.argument.BlockNameArgument;
import yan.lx.bedrockminer.config.Config;
import yan.lx.bedrockminer.utils.BlockUtils;
import yan.lx.bedrockminer.utils.MessageUtils;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class BlockNameCommand extends BaseCommand {
    @Override
    public String getName() {
        return "blockName";
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandRegistryAccess registryAccess) {
        builder.then(literal("whitelist")
                        .then(literal("add")
                                .then(argument("block", new BlockNameArgument(registryAccess).setFilter(this::filterWhitelist))
                                        .executes(this::addWhitelist)
                                )
                        )
                        .then(literal("remove")
                                .then(argument("block", new BlockNameArgument(registryAccess).setFilter(this::showWhitelist))
                                        .executes(this::removeWhitelist)
                                )
                        ))
                .then(literal("blacklist")
                        .then(literal("add")
                                .then(argument("block", new BlockNameArgument(registryAccess).setFilter(this::filterBlacklist))
                                        .executes(this::addBlacklist)
                                )
                        )
                        .then(literal("remove")
                                .then(argument("block", new BlockNameArgument(registryAccess).setFilter(this::showBlacklist))
                                        .executes(this::removeBlacklist)
                                )
                        )
                );
    }

    private boolean isFilterBlock(Identifier blockId) {
        var block = Registries.BLOCK.get(blockId);
        // 过滤空气和可替换方块
        return block.getDefaultState().isAir() || block.getDefaultState().isReplaceable();
    }

    private Boolean showWhitelist(Identifier blockId) {
        var config = Config.getInstance();
        for (var whitelist : config.blockWhitelist) {
            if (blockId.toString().equals(whitelist)) {
                return true;
            }
        }
        return false;
    }

    private Boolean filterWhitelist(Identifier blockId) {
        if (isFilterBlock(blockId)) {
            return false;
        }
        var config = Config.getInstance();
        for (var whitelist : config.blockWhitelist) {
            if (blockId.toString().equals(whitelist)) {
                return false;
            }
        }
        return true;
    }

    private Boolean showBlacklist(Identifier blockId) {
        var config = Config.getInstance();
        for (var whitelist : config.blockBlacklist) {
            if (blockId.toString().equals(whitelist)) {
                return true;
            }
        }
        return false;
    }

    private Boolean filterBlacklist(Identifier blockId) {
        if (isFilterBlock(blockId)) {
            return false;
        }
        var config = Config.getInstance();
        for (var whitelist : config.blockBlacklist) {
            if (blockId.toString().equals(whitelist)) {
                return false;
            }
        }
        return true;
    }

    private int addWhitelist(CommandContext<FabricClientCommandSource> context) {
        var block = BlockNameArgument.getBlock(context, "block");
        var config = Config.getInstance();
        var id = BlockUtils.getId(block);
        if (!config.blockWhitelist.contains(id)) {
            config.blockWhitelist.add(id);
            Config.save();
            sendChat("bedrockminer.command.block.whitelist.add", block);
        }
        return 0;
    }

    private int removeWhitelist(CommandContext<FabricClientCommandSource> context) {
        var block = BlockNameArgument.getBlock(context, "block");
        var config = Config.getInstance();
        var id = BlockUtils.getId(block);
        if (config.blockWhitelist.contains(id)) {
            config.blockWhitelist.remove(id);
            Config.save();
            sendChat("bedrockminer.command.block.whitelist.remove", block);
        }
        return 0;
    }

    private int addBlacklist(CommandContext<FabricClientCommandSource> context) {
        var block = BlockNameArgument.getBlock(context, "block");
        var config = Config.getInstance();
        var id = BlockUtils.getId(block);
        if (!config.blockBlacklist.contains(id)) {
            config.blockBlacklist.add(id);
            Config.save();
            sendChat("bedrockminer.command.block.blacklist.add", block);
        }
        return 0;
    }

    private int removeBlacklist(CommandContext<FabricClientCommandSource> context) {
        var block = BlockNameArgument.getBlock(context, "block");
        var config = Config.getInstance();
        var id = BlockUtils.getId(block);
        if (config.blockBlacklist.contains(id)) {
            config.blockBlacklist.remove(id);
            Config.save();
            sendChat("bedrockminer.command.block.blacklist.remove", block);
        }
        return 0;
    }

    private void sendChat(String translatableKey, Block block) {
        MessageUtils.addMessage(Text.translatable(translatableKey).getString().replace("%blockName%", block.getName().getString()));
    }
}
