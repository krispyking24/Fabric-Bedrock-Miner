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
import net.minecraft.command.CommandRegistryWrapper;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.Debug;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static net.minecraft.command.argument.BlockArgumentParser.INVALID_BLOCK_ID_EXCEPTION;

public class BlockArgument implements ArgumentType<Block> {
    private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone");
    private final CommandRegistryWrapper<Block> registryWrapper;
    @Nullable
    private Function<Identifier, Boolean> filter;

    public BlockArgument(CommandRegistryAccess commandRegistryAccess) {
        registryWrapper = commandRegistryAccess.createWrapper(Registry.BLOCK_KEY);
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
        var block = registryWrapper.getEntry(RegistryKey.of(Registry.BLOCK_KEY, blockId)).orElseThrow(() -> {
            reader.setCursor(i);
            return INVALID_BLOCK_ID_EXCEPTION.createWithContext(reader, blockId.toString());
        }).value();
        // 检查过滤器
        if (filter != null && !filter.apply(Registry.BLOCK.getId(block))) {
            reader.setCursor(i);
            throw INVALID_BLOCK_ID_EXCEPTION.createWithContext(reader, blockId.toString());
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

    public BlockArgument setFilter(Function<Identifier, Boolean> filter) {
        this.filter = filter;
        return this;
    }

    public static boolean isCharValid(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c == '_' || c == ':' || c == '/' || c == '.' || c == '-';
    }

}
