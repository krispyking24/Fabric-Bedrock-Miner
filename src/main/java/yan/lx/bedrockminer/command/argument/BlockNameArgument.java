package yan.lx.bedrockminer.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.BedrockMinerMod;
import yan.lx.bedrockminer.Debug;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static net.minecraft.command.argument.BlockArgumentParser.INVALID_BLOCK_ID_EXCEPTION;

public class BlockNameArgument implements ArgumentType<Block> {
    private static final DynamicCommandExceptionType INVALID_BLOCK_NAME_EXCEPTION = new DynamicCommandExceptionType(blockName -> Text.translatable("bedrockminer.command.invalidBlockNameException", blockName));
    private static final Collection<String> EXAMPLES = Arrays.asList("Stone", "Bedrock", "石头", "基岩");
    private final RegistryWrapper<Block> registryWrapper;
    @Nullable
    private Function<Identifier, Boolean> filter;

    public BlockNameArgument(CommandRegistryAccess commandRegistryAccess) {
        registryWrapper = commandRegistryAccess.createWrapper(RegistryKeys.BLOCK);
    }

    public static Block getBlock(CommandContext<FabricClientCommandSource> context, String name) {
        return context.getArgument(name, Block.class);
    }

    public Block parse(StringReader reader) throws CommandSyntaxException {
        var i = reader.getCursor();
        while (reader.canRead()) {
            reader.skip();
        }
        // 获取用户输入的字符串内容
        var string = reader.getString().substring(i, reader.getCursor());
        // 检查方块注册表中是否存在该名称
        var optionalBlock = Registries.BLOCK.stream().filter(block -> block.getName().getString().equals(string)).findFirst();
        if (optionalBlock.isEmpty()) {
            reader.setCursor(i);
            throw INVALID_BLOCK_NAME_EXCEPTION.create(string);
        }
        // 已获取到方块信息
        var block = optionalBlock.get();
        // 检查过滤器
        if (filter != null && !filter.apply(Registries.BLOCK.getId(block))) {
            throw INVALID_BLOCK_NAME_EXCEPTION.create(string);
        }
        return block;
    }


    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        StringReader reader = new StringReader(builder.getInput());
        reader.setCursor(builder.getStart());
        // 读取标识符
        var i = reader.getCursor();
        while (reader.canRead()) {
            reader.skip();
        }
        // 获取用户输入的字符串内容
        var string = reader.getString().substring(i, reader.getCursor());
        // 检查方块注册表中是否存在该名称
        Registries.BLOCK.forEach(block -> {
            if (block.getName().getString().startsWith(string)) {
                if (filter != null && filter.apply(Registries.BLOCK.getId(block))) {
                    // 添加建议列表
                    builder.suggest(block.getName().getString());
                }
            }
        });
        return builder.buildFuture();
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public BlockNameArgument setFilter(Function<Identifier, Boolean> filter) {
        this.filter = filter;
        return this;
    }
}
