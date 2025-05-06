package org.land.simplecamera.common.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class LockUnlockArgument implements ArgumentType<Boolean> {
    private static final Collection<String> EXAMPLES = Arrays.asList("lock", "unlock");

    @Override
    public Boolean parse(StringReader reader) throws CommandSyntaxException {
        String arg = reader.readString();
        if ("lock".equalsIgnoreCase(arg)) {
            return true;
        } else if ("unlock".equalsIgnoreCase(arg)) {
            return false;
        } else {
            throw new SimpleCommandExceptionType(Text.literal("Must be either 'lock' or 'unlock'")).create();
        }
    }
    public static boolean getLockState(CommandContext<?> context, String name) {
        return context.getArgument(name, Boolean.class);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(EXAMPLES, builder);
    }
    public static LockUnlockArgument getLockUnlock(CommandContext<ServerCommandSource> context, String name) {
        return context.getArgument(name, LockUnlockArgument.class);
    }
    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static LockUnlockArgument lockUnlock() {
        return new LockUnlockArgument();
    }
}
