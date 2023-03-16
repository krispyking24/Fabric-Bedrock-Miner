package yan.lx.bedrockminer.command;

import com.ibm.icu.text.MessagePatternUtil;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandRegistryAccess;
import yan.lx.bedrockminer.command.argument.BlockArgument;
import yan.lx.bedrockminer.config.Config;
import yan.lx.bedrockminer.utils.BlockUtils;
import yan.lx.bedrockminer.utils.Messager;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class BlockCommand extends BaseCommand {
    @Override
    public String getName() {
        return "block";
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandRegistryAccess registryAccess) {
        builder.then(literal("add")
                        .then(literal("whitelist")
                                .then(argument("block", new BlockArgument(registryAccess).setFilter(this::filterWhitelist))
                                        .executes(this::addWhitelist)
                                )
                        )
                        .then(literal("blacklist")
                                .then(argument("block", new BlockArgument(registryAccess).setFilter(this::filterBlacklist))
                                        .executes(this::addBlacklist)
                                )
                        )
                )
                .then(literal("remove")
                        .then(literal("whitelist")
                                .then(argument("block", new BlockArgument(registryAccess).setFilter(this::showWhitelist))
                                        .executes(this::removeWhitelist)
                                )
                        )
                        .then(literal("blacklist")
                                .then(argument("block", new BlockArgument(registryAccess).setFilter(this::showBlacklist))
                                        .executes(this::removeBlacklist)
                                )
                        )
                );
    }



    private Boolean showWhitelist(Block block) {
        var config = Config.getInstance();
        if (block.equals(Blocks.AIR)){
            return false;
        }
        for (var whitelist : config.blockWhitelist) {
            if (BlockUtils.getId(block).equals(whitelist)) {
                return true;
            }
        }
        return false;
    }

    private Boolean filterWhitelist(Block block) {
        var config = Config.getInstance();
        if (block.equals(Blocks.AIR)){
            return false;
        }
        for (var whitelist : config.blockWhitelist) {
            if (BlockUtils.getId(block).equals(whitelist)) {
                return false;
            }
        }
        return true;
    }

    private Boolean showBlacklist(Block block) {
        if (block.equals(Blocks.AIR)){
            return false;
        }
        var config = Config.getInstance();
        for (var whitelist : config.blockBlacklist) {
            if (BlockUtils.getId(block).equals(whitelist)) {
                return true;
            }
        }
        return false;
    }

    private Boolean filterBlacklist(Block block) {
        if (block.equals(Blocks.AIR)){
            return false;
        }
        var config = Config.getInstance();
        for (var whitelist : config.blockBlacklist) {
            if (BlockUtils.getId(block).equals(whitelist)) {
                return false;
            }
        }
        return true;
    }

    private int addWhitelist(CommandContext<FabricClientCommandSource> context) {
        var block = BlockArgument.getBlock(context, "block");
        var config = Config.getInstance();
        var id = BlockUtils.getId(block);
        if (!config.blockWhitelist.contains(id)) {
            config.blockWhitelist.add(id);
            Config.save();
            Messager.chat("bedrockminer.command.block.whitelist.add");
        }
        return 0;
    }

    private int removeWhitelist(CommandContext<FabricClientCommandSource> context) {
        var block = BlockArgument.getBlock(context, "block");
        var config = Config.getInstance();
        var id = BlockUtils.getId(block);
        if (config.blockWhitelist.contains(id)) {
            config.blockWhitelist.remove(id);
            Config.save();
            Messager.chat("bedrockminer.command.block.whitelist.remove");
        }
        return 0;
    }

    private int addBlacklist(CommandContext<FabricClientCommandSource> context) {
        var block = BlockArgument.getBlock(context, "block");
        var config = Config.getInstance();
        var id = BlockUtils.getId(block);
        if (!config.blockBlacklist.contains(id)) {
            config.blockBlacklist.add(id);
            Config.save();
            Messager.chat("bedrockminer.command.block.blacklist.add");
        }
        return 0;
    }

    private int removeBlacklist(CommandContext<FabricClientCommandSource> context) {
        var block = BlockArgument.getBlock(context, "block");
        var config = Config.getInstance();
        var id = BlockUtils.getId(block);
        if (config.blockBlacklist.contains(id)) {
            config.blockBlacklist.remove(id);
            Config.save();
            Messager.chat("bedrockminer.command.block.blacklist.remove");
        }
        return 0;
    }
}
