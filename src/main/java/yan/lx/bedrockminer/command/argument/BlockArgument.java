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
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class BlockArgument implements ArgumentType<Block> {
    private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone");
    private final RegistryWrapper<Block> registryWrapper;
    @Nullable
    private Function<Block, Boolean> filter;

    public BlockArgument(CommandRegistryAccess commandRegistryAccess) {
        this.registryWrapper = commandRegistryAccess.createWrapper(RegistryKeys.BLOCK);
    }

    public static Block getBlock(CommandContext<FabricClientCommandSource> context, String name) {
        return context.getArgument(name, Block.class);
    }

    public Block parse(StringReader stringReader) throws CommandSyntaxException {
        RegistryWrapper<Block> tmp;
        if (filter != null) {
            tmp = registryWrapper.filter((x) -> filter.apply(x));
        } else {
            tmp = registryWrapper;
        }
        BlockArgumentParser.BlockResult blockResult = BlockArgumentParser.block(tmp, stringReader, true);
        return new BlockStateArgument(blockResult.blockState(), blockResult.properties().keySet(), blockResult.nbt()).getBlockState().getBlock();
    }



    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        RegistryWrapper<Block> tmp;
        if (filter != null) {
            tmp = registryWrapper.filter((x) -> filter.apply(x));
        } else {
            tmp = registryWrapper;
        }
        return BlockArgumentParser.getSuggestions(tmp, builder, false, true);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public BlockArgument setFilter(Function<Block, Boolean> filter) {
        this.filter = filter;
        return this;
    }
}
