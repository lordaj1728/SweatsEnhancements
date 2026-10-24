package com.sweatsenhancements.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/**
 * Bridges Forge 1.8.9's ICommand onto a Brigadier dispatcher.
 *
 * 1.8.9 has no command library of its own; Brigadier arrived in 1.13. Shading it in gives
 * typed arguments, real error messages with cursor positions and free tab completion -
 * and, because it is the native system from 1.13 on, the command tree itself is portable
 * to the 1.21.11 build.
 *
 * The try/catch here is the part that matters: anything escaping into Forge's handler
 * turns into "An unknown error occured while attempting to perform this command".
 */
public class CommandBridge implements ICommand {

    private final CommandDispatcher<Object> dispatcher = new CommandDispatcher<Object>();

    public CommandBridge() {
        SweatsCommands.register(dispatcher);
        selfCheck();
    }

    /**
     * Parses a couple of known commands at startup and logs the result. A command tree that
     * cannot resolve its own subcommands is the kind of fault that only shows up when someone
     * types the command, so it gets checked on load instead.
     */
    private void selfCheck() {
        try {
            String[] probes = {"se", "se help", "se dump", "se tablist", "se area", "se menu", "se requeue"};
            StringBuilder sb = new StringBuilder();
            for (String probe : probes) {
                boolean ok;
                try {
                    dispatcher.parse(probe, null);
                    ok = true;
                } catch (Throwable t) {
                    ok = false;
                }
                sb.append(ok ? "" : " FAILED:").append(ok ? "ok" : probe);
            }
            System.out.println("[SweatsEnhancements] command tree check: " + sb);
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] command tree check could not run: " + t);
        }
    }

    @Override
    public String getCommandName() {
        return "se";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/se help";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("sweatsenhancements");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        // Forge hands us the arguments without the command name, and Brigadier parses a full
        // command line including its root literal. Without re-attaching "sweatsenhancements " here, "/se dump"
        // reaches the dispatcher as just "dump" and fails as an unknown command at cursor 0.
        String line = "se";
        String tail = join(args);
        if (!tail.trim().isEmpty()) line = line + " " + tail;
        try {
            dispatcher.execute(line, sender);
        } catch (CommandSyntaxException e) {
            reply(sender, EnumChatFormatting.RED + e.getRawMessage().getString());
            if (e.getInput() != null) {
                reply(sender, EnumChatFormatting.GRAY + "  at: /se " + e.getInput()
                        + "   (cursor " + e.getCursor() + ")");
            }
        } catch (Throwable t) {
            reply(sender, EnumChatFormatting.RED + "SweatsEnhancements command failed: " + t);
            t.printStackTrace();
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        try {
            String input = join(args);
            CompletableFuture<Suggestions> future = dispatcher.getCompletionSuggestions(
                    dispatcher.parse(input, sender));
            Suggestions suggestions = future.get();
            List<String> out = new ArrayList<String>();
            for (Suggestion s : suggestions.getList()) {
                out.add(s.getText());
            }
            return out.isEmpty() ? null : out;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(ICommand other) {
        return getCommandName().compareTo(other.getCommandName());
    }

    public static void reply(ICommandSender sender, String text) {
        try {
            sender.addChatMessage(new ChatComponentText(text));
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] could not reply: " + t);
        }
    }

    private static String join(String[] args) {
        StringBuilder sb = new StringBuilder();
        for (String a : args) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(a);
        }
        return sb.toString();
    }
}
