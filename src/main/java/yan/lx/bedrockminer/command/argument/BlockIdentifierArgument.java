package yan.lx.bedrockminer.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static net.minecraft.command.argument.BlockArgumentParser.INVALID_BLOCK_ID_EXCEPTION;

public class BlockIdentifierArgument implements ArgumentType<Block> {
    private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone");
    private final RegistryWrapper<Block> registryWrapper;
    @Nullable
    private Function<Identifier, Boolean> filter;

    public BlockIdentifierArgument(CommandRegistryAccess commandRegistryAccess) {
        registryWrapper = commandRegistryAccess.createWrapper(RegistryKeys.BLOCK);
    }

    public static Block getBlock(CommandContext<FabricClientCommandSource> context, String name) {
        return context.getArgument(name, Block.class);
    }

    public Block parse(StringReader reader) throws CommandSyntaxException {
        var i = reader.getCursor();
        while (reader.canRead() && isCharValid(reader.peek())) {
            reader.skip();
        }
        var string = reader.getString().substring(i, reader.getCursor());
        var blockId = new Identifier(string);
        var block = registryWrapper.getOptional(RegistryKey.of(RegistryKeys.BLOCK, blockId)).orElseThrow(() -> {
            reader.setCursor(i);
            return INVALID_BLOCK_ID_EXCEPTION.create(blockId.toString());
        }).value();
        // 检查过滤器
        if (filter != null && !filter.apply(Registries.BLOCK.getId(block))) {
            reader.setCursor(i);
            throw INVALID_BLOCK_ID_EXCEPTION.create(blockId.toString());
        }
        return block;
    }


    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        StringReader reader = new StringReader(builder.getInput());
        reader.setCursor(builder.getStart());
        // 读取标识符
        var i = reader.getCursor();
        while (reader.canRead() && isCharValid(reader.peek())) {
            reader.skip();
        }
        var string = reader.getString().substring(i, reader.getCursor());
        var blockId = new Identifier(string);
        // 过滤标识符
        var keys = registryWrapper.streamKeys().filter(x -> {
            var identifier = x.getValue();
            var namespace = identifier.getNamespace();
            var path = identifier.getPath();
            if (blockId.getNamespace().startsWith(namespace) || blockId.getPath().startsWith(path)) {
                if (filter != null) {
                    return filter.apply(identifier);
                } else {
                    return true;
                }
            }
            return false;
        });
        return CommandSource.suggestIdentifiers(keys.map(RegistryKey::getValue), builder);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public BlockIdentifierArgument setFilter(Function<Identifier, Boolean> filter) {
        this.filter = filter;
        return this;
    }

    public static boolean isCharValid(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c == '_' || c == ':' || c == '/' || c == '.' || c == '-';
    }

}
