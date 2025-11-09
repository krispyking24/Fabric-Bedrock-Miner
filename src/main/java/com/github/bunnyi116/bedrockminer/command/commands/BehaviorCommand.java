package com.github.bunnyi116.bedrockminer.command.commands;

import com.github.bunnyi116.bedrockminer.APIs;
import com.github.bunnyi116.bedrockminer.I18n;
import com.github.bunnyi116.bedrockminer.command.CommandBase;
import com.github.bunnyi116.bedrockminer.command.argument.BlockArgument;
import com.github.bunnyi116.bedrockminer.util.BlockUtils;
import com.github.bunnyi116.bedrockminer.util.MessageUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

import static com.github.bunnyi116.bedrockminer.util.BlockUtils.getBlockId;
import static com.github.bunnyi116.bedrockminer.util.BlockUtils.getBlockName;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class BehaviorCommand extends CommandBase {
    @Override
    public String getName() {
        return "behavior";
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandRegistryAccess registryAccess) {
        builder.then(literal("floor")
                        .then(literal("addTask")
                                .then(argument("floor", IntegerArgumentType.integer())
                                        .executes(this::addFloor)
                                )

                        )
                        .then(literal("remove")
                                .then(argument("floor", IntegerArgumentType.integer())
                                        .executes(this::removeFloor)
                                )
                        )
                        .then(literal("list").executes(this::listFloor)
                        )
                )
                .then(literal("blockWhitelist")
                        .then(literal("addTask")
                                .then(argument("block", new BlockArgument(this::filterBlockWhitelist))
                                        .executes(this::addBlockWhitelist)
                                )
                        )
                        .then(literal("remove")
                                .then(argument("block", new BlockArgument(this::showBlockWhitelist))
                                        .executes(this::removeBlockWhitelist)
                                )
                        )
                );
    }

    private int listFloor(CommandContext<FabricClientCommandSource> context) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < APIs.getInstance().getConfig().floorsBlacklist.size(); i++) {
            Integer floor = APIs.getInstance().getConfig().floorsBlacklist.get(i);
            if (i == APIs.getInstance().getConfig().floorsBlacklist.size() - 1) {
                sb.append(floor);
            } else {
                sb.append(floor).append(",");
            }
        }
        MessageUtils.addMessage(
                Text.literal(I18n.FLOOR_BLACK_LIST_SHOW.getString()
                        .replace("%listFloor%", sb.toString())
                )
        );
        return Command.SINGLE_SUCCESS;
    }

    private int addFloor(CommandContext<FabricClientCommandSource> context) {
        var floor = IntegerArgumentType.getInteger(context, "floor");
        if (!APIs.getInstance().getConfig().floorsBlacklist.contains(floor)) {
            APIs.getInstance().getConfig().floorsBlacklist.add(floor);
            APIs.getInstance().getConfig().save();
        }
        MessageUtils.addMessage(
                Text.literal(I18n.FLOOR_BLACK_LIST_ADD.getString()
                        .replace("%count%", String.valueOf(floor))
                )
        );
        return Command.SINGLE_SUCCESS;
    }

    private int removeFloor(CommandContext<FabricClientCommandSource> context) {
        var floor = IntegerArgumentType.getInteger(context, "floor");
        if (APIs.getInstance().getConfig().floorsBlacklist.contains(floor)) {
            APIs.getInstance().getConfig().floorsBlacklist.remove((Integer) floor);
            APIs.getInstance().getConfig().save();
        }
        MessageUtils.addMessage(
                Text.literal(I18n.FLOOR_BLACK_LIST_REMOVE.getString()
                        .replace("%count%", String.valueOf(floor))
                )
        );
        return Command.SINGLE_SUCCESS;
    }

    private int addBlockWhitelist(CommandContext<FabricClientCommandSource> context) {
        var block = BlockArgument.getBlock(context, "block");
        var blockId = getBlockId(block);
        if (!APIs.getInstance().getConfig().blockWhitelist.contains(blockId)) {
            APIs.getInstance().getConfig().blockWhitelist.add(blockId);
            APIs.getInstance().getConfig().save();
            sendChat(I18n.COMMAND_BLOCK_WHITELIST_ADD, block);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int removeBlockWhitelist(CommandContext<FabricClientCommandSource> context) {
        var block = BlockArgument.getBlock(context, "block");
        var blockId = getBlockId(block);
        if (APIs.getInstance().getConfig().blockWhitelist.contains(blockId)) {
            APIs.getInstance().getConfig().blockWhitelist.remove(blockId);
            APIs.getInstance().getConfig().save();
            sendChat(I18n.COMMAND_BLOCK_WHITELIST_REMOVE, block);
        }
        return Command.SINGLE_SUCCESS;
    }

    private boolean isFilterBlock(Block block) {
        return block.getDefaultState().isAir() || BlockUtils.isReplaceable(block.getDefaultState());
    }

    private Boolean filterBlockWhitelist(Block block) {
        if (isFilterBlock(block)) {
            return true;
        }
        return APIs.getInstance().getConfig().blockWhitelist.contains(getBlockId(block));
    }

    private Boolean showBlockWhitelist(Block block) {
        return !APIs.getInstance().getConfig().blockWhitelist.contains(getBlockId(block));
    }

    private void sendChat(Text text, Block block) {
        var msg = text.getString().replace("#blockName#", getBlockName(block));
        MessageUtils.addMessage(Text.literal(msg));
    }
}
